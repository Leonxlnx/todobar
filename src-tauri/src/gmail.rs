use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine as _};
use rand::{rngs::OsRng, RngCore};
use reqwest::blocking::Client;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::{
    io::{Read, Write},
    net::TcpListener,
    thread,
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
};
use url::Url;

const GMAIL_SCOPE: &str = "https://www.googleapis.com/auth/gmail.readonly";
const GOOGLE_AUTH_URL: &str = "https://accounts.google.com/o/oauth2/v2/auth";
const GOOGLE_TOKEN_URL: &str = "https://oauth2.googleapis.com/token";
const GMAIL_PROFILE_URL: &str = "https://gmail.googleapis.com/gmail/v1/users/me/profile";
const GMAIL_THREADS_URL: &str = "https://gmail.googleapis.com/gmail/v1/users/me/threads";
const KEYRING_SERVICE: &str = "todobar.gmail";
const KEYRING_USER: &str = "oauth";

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GmailConnectionStatus {
    pub state: String,
    pub account_email: Option<String>,
    pub sync_state: String,
    pub message: String,
    pub scope: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GmailSuggestion {
    pub thread_id: String,
    pub subject: String,
    pub from: String,
    pub date: String,
    pub snippet: String,
    pub gmail_url: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GmailSuggestionsResponse {
    pub account_email: String,
    pub fetched_at: String,
    pub suggestions: Vec<GmailSuggestion>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct StoredGmailToken {
    account_email: String,
    access_token: String,
    refresh_token: String,
    expires_at_ms: u64,
    scope: String,
    token_type: String,
}

#[derive(Debug)]
struct GmailOAuthConfig {
    client_id: String,
    client_secret: Option<String>,
}

#[derive(Debug, Deserialize)]
struct GoogleTokenResponse {
    access_token: String,
    expires_in: Option<u64>,
    refresh_token: Option<String>,
    scope: Option<String>,
    token_type: Option<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GmailProfileResponse {
    email_address: String,
}

#[derive(Debug, Deserialize)]
struct GmailThreadListResponse {
    threads: Option<Vec<GmailThreadListItem>>,
}

#[derive(Debug, Deserialize)]
struct GmailThreadListItem {
    id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GmailThreadResponse {
    id: String,
    messages: Option<Vec<GmailMessage>>,
    snippet: Option<String>,
}

#[derive(Debug, Deserialize)]
struct GmailMessage {
    payload: Option<GmailPayload>,
    snippet: Option<String>,
}

#[derive(Debug, Deserialize)]
struct GmailPayload {
    headers: Option<Vec<GmailHeader>>,
}

#[derive(Debug, Deserialize)]
struct GmailHeader {
    name: String,
    value: String,
}

fn now_ms() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}

fn now_iso() -> String {
    format!("{}", now_ms())
}

fn oauth_config() -> Result<GmailOAuthConfig, String> {
    let client_id = std::env::var("TODOBAR_GMAIL_CLIENT_ID")
        .ok()
        .filter(|value| !value.trim().is_empty())
        .or_else(|| {
            option_env!("TODOBAR_GMAIL_CLIENT_ID")
                .map(str::to_owned)
                .filter(|value| !value.trim().is_empty())
        })
        .ok_or_else(|| {
            "Gmail OAuth is not configured in this build. Maintainers need to set TODOBAR_GMAIL_CLIENT_ID.".to_string()
        })?;

    let client_secret = std::env::var("TODOBAR_GMAIL_CLIENT_SECRET")
        .ok()
        .filter(|value| !value.trim().is_empty())
        .or_else(|| {
            option_env!("TODOBAR_GMAIL_CLIENT_SECRET")
                .map(str::to_owned)
                .filter(|value| !value.trim().is_empty())
        });

    Ok(GmailOAuthConfig {
        client_id,
        client_secret,
    })
}

fn keyring_entry() -> Result<keyring::Entry, String> {
    keyring::Entry::new(KEYRING_SERVICE, KEYRING_USER)
        .map_err(|error| format!("Could not open OS credential store: {error}"))
}

fn read_stored_token() -> Result<Option<StoredGmailToken>, String> {
    let entry = keyring_entry()?;

    match entry.get_password() {
        Ok(password) => serde_json::from_str::<StoredGmailToken>(&password)
            .map(Some)
            .map_err(|error| format!("Stored Gmail token is unreadable: {error}")),
        Err(keyring::Error::NoEntry) => Ok(None),
        Err(error) => Err(format!("Could not read Gmail token from OS credential store: {error}")),
    }
}

fn write_stored_token(token: &StoredGmailToken) -> Result<(), String> {
    let entry = keyring_entry()?;
    let encoded = serde_json::to_string(token)
        .map_err(|error| format!("Could not serialize Gmail token: {error}"))?;

    entry
        .set_password(&encoded)
        .map_err(|error| format!("Could not save Gmail token to OS credential store: {error}"))
}

fn delete_stored_token() -> Result<(), String> {
    let entry = keyring_entry()?;

    match entry.delete_credential() {
        Ok(()) | Err(keyring::Error::NoEntry) => Ok(()),
        Err(error) => Err(format!("Could not remove Gmail token from OS credential store: {error}")),
    }
}

fn status_from_token(token: Option<StoredGmailToken>) -> Result<GmailConnectionStatus, String> {
    if let Some(token) = token {
        return Ok(GmailConnectionStatus {
            state: "connected".to_string(),
            account_email: Some(token.account_email),
            sync_state: if token.expires_at_ms <= now_ms() {
                "refresh-ready".to_string()
            } else {
                "idle".to_string()
            },
            message: "Gmail is connected read-only.".to_string(),
            scope: GMAIL_SCOPE.to_string(),
        });
    }

    if oauth_config().is_err() {
        return Ok(GmailConnectionStatus {
            state: "unconfigured".to_string(),
            account_email: None,
            sync_state: "idle".to_string(),
            message: "Gmail login is built, but this app build does not include Todobar's Google OAuth client ID yet.".to_string(),
            scope: GMAIL_SCOPE.to_string(),
        });
    }

    Ok(GmailConnectionStatus {
        state: "disconnected".to_string(),
        account_email: None,
        sync_state: "idle".to_string(),
        message: "Connect Gmail to review unread inbox suggestions.".to_string(),
        scope: GMAIL_SCOPE.to_string(),
    })
}

fn random_urlsafe_bytes(byte_len: usize) -> String {
    let mut bytes = vec![0_u8; byte_len];
    OsRng.fill_bytes(&mut bytes);
    URL_SAFE_NO_PAD.encode(bytes)
}

fn pkce_challenge(verifier: &str) -> String {
    let digest = Sha256::digest(verifier.as_bytes());
    URL_SAFE_NO_PAD.encode(digest)
}

fn listen_for_oauth_callback(listener: TcpListener, expected_state: String) -> Result<String, String> {
    listener
        .set_nonblocking(true)
        .map_err(|error| format!("Could not prepare local OAuth callback: {error}"))?;

    let deadline = Instant::now() + Duration::from_secs(180);

    loop {
        if Instant::now() > deadline {
            return Err("Gmail sign-in timed out. Try Connect Gmail again.".to_string());
        }

        match listener.accept() {
            Ok((mut stream, _addr)) => {
                let _ = stream.set_read_timeout(Some(Duration::from_secs(5)));
                let mut buffer = [0_u8; 8192];
                let read_count = stream
                    .read(&mut buffer)
                    .map_err(|error| format!("Could not read OAuth callback: {error}"))?;
                let request = String::from_utf8_lossy(&buffer[..read_count]);
                let request_line = request.lines().next().unwrap_or_default();
                let target = request_line
                    .split_whitespace()
                    .nth(1)
                    .ok_or_else(|| "OAuth callback was malformed.".to_string())?;
                let callback_url = Url::parse(&format!("http://127.0.0.1{target}"))
                    .map_err(|error| format!("OAuth callback URL was malformed: {error}"))?;
                let mut code = None;
                let mut state = None;
                let mut oauth_error = None;

                for (key, value) in callback_url.query_pairs() {
                    match key.as_ref() {
                        "code" => code = Some(value.to_string()),
                        "state" => state = Some(value.to_string()),
                        "error" => oauth_error = Some(value.to_string()),
                        _ => {}
                    }
                }

                let html = if oauth_error.is_none() && state.as_deref() == Some(expected_state.as_str()) {
                    "<!doctype html><title>Todobar Gmail connected</title><body style=\"font-family:system-ui;margin:40px\"><h1>Gmail connected</h1><p>You can close this tab and return to Todobar.</p></body>"
                } else {
                    "<!doctype html><title>Todobar Gmail sign-in failed</title><body style=\"font-family:system-ui;margin:40px\"><h1>Gmail sign-in failed</h1><p>Please return to Todobar and try again.</p></body>"
                };
                let response = format!(
                    "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
                    html.as_bytes().len(),
                    html,
                );
                let _ = stream.write_all(response.as_bytes());
                let _ = stream.flush();

                if let Some(error) = oauth_error {
                    return Err(format!("Google rejected the Gmail sign-in: {error}"));
                }

                if state.as_deref() != Some(expected_state.as_str()) {
                    return Err("Gmail sign-in state did not match. Try again.".to_string());
                }

                return code.ok_or_else(|| "Google did not return an OAuth code.".to_string());
            }
            Err(error) if error.kind() == std::io::ErrorKind::WouldBlock => {
                thread::sleep(Duration::from_millis(100));
            }
            Err(error) => return Err(format!("OAuth callback failed: {error}")),
        }
    }
}

fn exchange_code_for_token(
    client: &Client,
    config: &GmailOAuthConfig,
    code: &str,
    redirect_uri: &str,
    verifier: &str,
) -> Result<GoogleTokenResponse, String> {
    let mut form = vec![
        ("client_id", config.client_id.as_str()),
        ("code", code),
        ("code_verifier", verifier),
        ("grant_type", "authorization_code"),
        ("redirect_uri", redirect_uri),
    ];

    if let Some(secret) = config.client_secret.as_deref() {
        form.push(("client_secret", secret));
    }

    let response = client
        .post(GOOGLE_TOKEN_URL)
        .form(&form)
        .send()
        .map_err(|error| format!("Could not exchange Gmail OAuth code: {error}"))?;
    let status = response.status();

    if !status.is_success() {
        let body = response.text().unwrap_or_default();
        return Err(format!("Google token exchange failed ({status}): {body}"));
    }

    response
        .json::<GoogleTokenResponse>()
        .map_err(|error| format!("Could not parse Google token response: {error}"))
}

fn refresh_access_token(client: &Client, token: &StoredGmailToken) -> Result<StoredGmailToken, String> {
    let config = oauth_config()?;
    let mut form = vec![
        ("client_id", config.client_id.as_str()),
        ("grant_type", "refresh_token"),
        ("refresh_token", token.refresh_token.as_str()),
    ];

    if let Some(secret) = config.client_secret.as_deref() {
        form.push(("client_secret", secret));
    }

    let response = client
        .post(GOOGLE_TOKEN_URL)
        .form(&form)
        .send()
        .map_err(|error| format!("Could not refresh Gmail access: {error}"))?;
    let status = response.status();

    if !status.is_success() {
        let body = response.text().unwrap_or_default();
        return Err(format!(
            "Gmail authorization needs reconnect ({status}). Google response: {body}"
        ));
    }

    let refreshed = response
        .json::<GoogleTokenResponse>()
        .map_err(|error| format!("Could not parse refreshed Gmail token: {error}"))?;
    let next = StoredGmailToken {
        account_email: token.account_email.clone(),
        access_token: refreshed.access_token,
        refresh_token: token.refresh_token.clone(),
        expires_at_ms: now_ms() + refreshed.expires_in.unwrap_or(3600) * 1000 - 60_000,
        scope: refreshed.scope.unwrap_or_else(|| token.scope.clone()),
        token_type: refreshed.token_type.unwrap_or_else(|| token.token_type.clone()),
    };

    write_stored_token(&next)?;
    Ok(next)
}

fn ensure_access_token(client: &Client) -> Result<StoredGmailToken, String> {
    let token = read_stored_token()?.ok_or_else(|| "Gmail is not connected.".to_string())?;

    if token.expires_at_ms > now_ms() + 60_000 {
        return Ok(token);
    }

    refresh_access_token(client, &token)
}

fn fetch_profile(client: &Client, access_token: &str) -> Result<GmailProfileResponse, String> {
    let response = client
        .get(GMAIL_PROFILE_URL)
        .bearer_auth(access_token)
        .send()
        .map_err(|error| format!("Could not read Gmail profile: {error}"))?;
    let status = response.status();

    if !status.is_success() {
        let body = response.text().unwrap_or_default();
        return Err(format!("Could not read Gmail profile ({status}): {body}"));
    }

    response
        .json::<GmailProfileResponse>()
        .map_err(|error| format!("Could not parse Gmail profile: {error}"))
}

fn header_value(headers: &[GmailHeader], name: &str) -> String {
    headers
        .iter()
        .find(|header| header.name.eq_ignore_ascii_case(name))
        .map(|header| header.value.clone())
        .unwrap_or_default()
}

fn simplify_sender(value: &str) -> String {
    value
        .split('<')
        .next()
        .unwrap_or(value)
        .trim()
        .trim_matches('"')
        .to_string()
}

fn fetch_thread(client: &Client, access_token: &str, thread_id: &str, account_email: &str) -> Result<GmailSuggestion, String> {
    let url = format!("{GMAIL_THREADS_URL}/{thread_id}");
    let response = client
        .get(url)
        .bearer_auth(access_token)
        .query(&[
            ("format", "metadata"),
            ("metadataHeaders", "Subject"),
            ("metadataHeaders", "From"),
            ("metadataHeaders", "Date"),
        ])
        .send()
        .map_err(|error| format!("Could not read Gmail thread {thread_id}: {error}"))?;
    let status = response.status();

    if !status.is_success() {
        let body = response.text().unwrap_or_default();
        return Err(format!("Could not read Gmail thread {thread_id} ({status}): {body}"));
    }

    let thread = response
        .json::<GmailThreadResponse>()
        .map_err(|error| format!("Could not parse Gmail thread {thread_id}: {error}"))?;
    let message = thread.messages.as_ref().and_then(|messages| messages.last());
    let headers = message
        .and_then(|message| message.payload.as_ref())
        .and_then(|payload| payload.headers.as_ref())
        .map(Vec::as_slice)
        .unwrap_or(&[]);
    let subject = header_value(headers, "Subject");
    let from = simplify_sender(&header_value(headers, "From"));
    let date = header_value(headers, "Date");
    let snippet = thread
        .snippet
        .or_else(|| message.and_then(|message| message.snippet.clone()))
        .unwrap_or_default();
    let gmail_url = format!(
        "https://mail.google.com/mail/u/?authuser={}#inbox/{}",
        urlencoding(account_email),
        thread.id
    );

    Ok(GmailSuggestion {
        thread_id: thread.id,
        subject: if subject.is_empty() {
            "(no subject)".to_string()
        } else {
            subject
        },
        from: if from.is_empty() {
            "Unknown sender".to_string()
        } else {
            from
        },
        date,
        snippet,
        gmail_url,
    })
}

fn urlencoding(value: &str) -> String {
    url::form_urlencoded::byte_serialize(value.as_bytes()).collect()
}

#[tauri::command]
pub fn gmail_status() -> Result<GmailConnectionStatus, String> {
    status_from_token(read_stored_token()?)
}

#[tauri::command]
pub async fn gmail_connect() -> Result<GmailConnectionStatus, String> {
    tauri::async_runtime::spawn_blocking(|| {
        let config = oauth_config()?;
        let listener = TcpListener::bind(("127.0.0.1", 0))
            .map_err(|error| format!("Could not start local OAuth callback: {error}"))?;
        let port = listener
            .local_addr()
            .map_err(|error| format!("Could not read local OAuth callback port: {error}"))?
            .port();
        let redirect_uri = format!("http://127.0.0.1:{port}/oauth/gmail/callback");
        let verifier = random_urlsafe_bytes(48);
        let challenge = pkce_challenge(&verifier);
        let state = random_urlsafe_bytes(24);
        let mut auth_url = Url::parse(GOOGLE_AUTH_URL)
            .map_err(|error| format!("Could not build Google OAuth URL: {error}"))?;

        auth_url.query_pairs_mut()
            .append_pair("access_type", "offline")
            .append_pair("client_id", &config.client_id)
            .append_pair("code_challenge", &challenge)
            .append_pair("code_challenge_method", "S256")
            .append_pair("include_granted_scopes", "true")
            .append_pair("prompt", "consent")
            .append_pair("redirect_uri", &redirect_uri)
            .append_pair("response_type", "code")
            .append_pair("scope", GMAIL_SCOPE)
            .append_pair("state", &state);

        open::that(auth_url.as_str())
            .map_err(|error| format!("Could not open Google sign-in in the browser: {error}"))?;

        let code = listen_for_oauth_callback(listener, state)?;
        let client = Client::builder()
            .timeout(Duration::from_secs(30))
            .build()
            .map_err(|error| format!("Could not create Gmail HTTP client: {error}"))?;
        let token_response = exchange_code_for_token(
            &client,
            &config,
            &code,
            &redirect_uri,
            &verifier,
        )?;
        let refresh_token = token_response.refresh_token.ok_or_else(|| {
            "Google did not return a refresh token. Disconnect Gmail in your Google Account permissions and try again.".to_string()
        })?;
        let profile = fetch_profile(&client, &token_response.access_token)?;
        let stored = StoredGmailToken {
            account_email: profile.email_address,
            access_token: token_response.access_token,
            refresh_token,
            expires_at_ms: now_ms() + token_response.expires_in.unwrap_or(3600) * 1000 - 60_000,
            scope: token_response.scope.unwrap_or_else(|| GMAIL_SCOPE.to_string()),
            token_type: token_response.token_type.unwrap_or_else(|| "Bearer".to_string()),
        };

        write_stored_token(&stored)?;
        status_from_token(Some(stored))
    })
    .await
    .map_err(|error| format!("Gmail sign-in task failed: {error}"))?
}

#[tauri::command]
pub fn gmail_disconnect() -> Result<GmailConnectionStatus, String> {
    delete_stored_token()?;
    status_from_token(None)
}

#[tauri::command]
pub async fn gmail_fetch_unread(limit: Option<u8>) -> Result<GmailSuggestionsResponse, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let client = Client::builder()
            .timeout(Duration::from_secs(30))
            .build()
            .map_err(|error| format!("Could not create Gmail HTTP client: {error}"))?;
        let token = ensure_access_token(&client)?;
        let max_results = limit.unwrap_or(8).clamp(1, 20).to_string();
        let response = client
            .get(GMAIL_THREADS_URL)
            .bearer_auth(&token.access_token)
            .query(&[
                ("q", "in:inbox is:unread newer_than:30d"),
                ("maxResults", max_results.as_str()),
            ])
            .send()
            .map_err(|error| format!("Could not fetch unread Gmail threads: {error}"))?;
        let status = response.status();

        if !status.is_success() {
            let body = response.text().unwrap_or_default();
            return Err(format!("Could not fetch unread Gmail threads ({status}): {body}"));
        }

        let list = response
            .json::<GmailThreadListResponse>()
            .map_err(|error| format!("Could not parse unread Gmail threads: {error}"))?;
        let mut suggestions = Vec::new();

        for thread in list.threads.unwrap_or_default() {
            match fetch_thread(
                &client,
                &token.access_token,
                &thread.id,
                &token.account_email,
            ) {
                Ok(suggestion) => suggestions.push(suggestion),
                Err(error) => log::warn!("{error}"),
            }
        }

        Ok(GmailSuggestionsResponse {
            account_email: token.account_email,
            fetched_at: now_iso(),
            suggestions,
        })
    })
    .await
    .map_err(|error| format!("Gmail sync task failed: {error}"))?
}
