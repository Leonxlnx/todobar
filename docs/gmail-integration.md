# Gmail Integration

Todobar's first Gmail connector uses direct Gmail API OAuth from the native
Tauri layer. MCP remains an advanced future integration path, not the normal
user flow.

## User Flow

1. Open Todobar settings.
2. Open Connectors.
3. Click Connect Gmail.
4. The system browser opens Google's OAuth consent screen.
5. The user approves read-only Gmail access.
6. Todobar receives the OAuth callback through a local loopback redirect.
7. The native layer exchanges the code for tokens.
8. Tokens are stored in the OS credential store.
9. Todobar can read recent unread Inbox threads and show local suggestions.

Normal users should not paste MCP endpoints, OAuth client IDs, or secrets.

## Scope

The first version requests only:

```text
https://www.googleapis.com/auth/gmail.readonly
```

Todobar can:

- read recent unread Inbox threads
- show subject, sender, date, snippet, and a Gmail link
- convert a selected thread into a local Todobar task
- ignore a suggestion locally

Todobar cannot:

- send email
- compose drafts
- delete email
- archive email
- label email
- mark email read
- mutate Gmail in any way

## Native OAuth Architecture

The native Tauri layer owns OAuth and Gmail API calls:

- `gmail_connect`: opens the browser, handles the loopback callback, exchanges
  the OAuth code, reads the Gmail profile email, and stores tokens securely.
- `gmail_status`: reports disconnected, connected, reconnect, or unconfigured
  state without exposing tokens.
- `gmail_fetch_unread`: refreshes access if needed, reads recent unread Inbox
  threads, and returns suggestion data.
- `gmail_disconnect`: deletes the stored OAuth credential.

The frontend never receives access tokens or refresh tokens.

## Secure Token Storage

Tokens are serialized only inside the native layer and stored through the
`keyring` crate:

- Windows: Credential Manager
- macOS: Keychain

Browser/localStorage is used only for UI state such as ignored suggestion IDs
and connector activity entries. It must not contain OAuth tokens.

## Maintainer OAuth Setup

For local development, create a Google OAuth desktop client and run Todobar with:

```powershell
$env:TODOBAR_GMAIL_CLIENT_ID="your-client-id.apps.googleusercontent.com"
npm run tauri:dev
```

If your Google client requires a secret, keep it native-only:

```powershell
$env:TODOBAR_GMAIL_CLIENT_SECRET="your-client-secret"
```

The native flow uses a loopback redirect like:

```text
http://127.0.0.1:<random-port>/oauth/gmail/callback
```

The port is chosen at runtime.

Primary references:

- Google OAuth for desktop apps:
  <https://developers.google.com/identity/protocols/oauth2/native-app>
- Gmail threads list:
  <https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.threads/list>
- Gmail threads get:
  <https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.threads/get>

## Public Release Notes

A public Gmail-enabled app may need Google OAuth verification before broad
distribution. Until verification is complete, Google may show unverified-app
warnings or restrict test users depending on the OAuth app publishing state.

Before a public Gmail release:

- configure the production OAuth consent screen
- add the app name, support email, privacy policy, and authorized domain
- request only `gmail.readonly`
- submit for Google verification if required
- test token revocation and reconnect flows on Windows and macOS

## Privacy Boundary

Every Gmail read triggered by the UI is recorded in Todobar's connector activity
area. Gmail-derived tasks are local-first and stay in Todobar's local task store.

The current connector is intentionally read-only. Write actions should be added
only as separate, explicit, permissioned features with their own scopes and UI.
