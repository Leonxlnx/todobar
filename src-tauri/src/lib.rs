use tauri::{
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Emitter, Manager,
};
use tauri_plugin_global_shortcut::{GlobalShortcutExt, ShortcutState};

const TRAY_TOGGLE_EVENT: &str = "todobar-tray-toggle";
const TRAY_SETTINGS_EVENT: &str = "todobar-tray-settings";

fn focus_main_window(app: &tauri::AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.set_always_on_top(true);
        let _ = window.unminimize();
        let _ = window.set_focus();
    }
}

fn emit_to_main(app: &tauri::AppHandle, event: &str) {
    focus_main_window(app);
    let _ = app.emit_to("main", event, ());
}

fn setup_tray(app: &tauri::App) -> tauri::Result<()> {
    let toggle = MenuItem::with_id(
        app,
        "toggle",
        "Open / Close Todobar",
        true,
        Some("Alt+T"),
    )?;
    let settings = MenuItem::with_id(app, "settings", "Settings", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit Todobar", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&toggle, &settings, &quit])?;
    let icon = app.default_window_icon().cloned();

    let mut tray = TrayIconBuilder::with_id("todobar")
        .tooltip("Todobar")
        .menu(&menu)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, event| match event.id().as_ref() {
            "toggle" => emit_to_main(app, TRAY_TOGGLE_EVENT),
            "settings" => emit_to_main(app, TRAY_SETTINGS_EVENT),
            "quit" => app.exit(0),
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if matches!(
                event,
                TrayIconEvent::Click {
                    button: MouseButton::Left,
                    button_state: MouseButtonState::Up,
                    ..
                }
            ) {
                emit_to_main(tray.app_handle(), TRAY_TOGGLE_EVENT);
            }
        });

    if let Some(icon) = icon {
        tray = tray.icon(icon);
    }

    tray.build(app)?;
    Ok(())
}

fn setup_global_shortcuts(app: &tauri::App) {
    for shortcut in ["Alt+T", "Alt+Shift+T"] {
        let result = app
            .global_shortcut()
            .on_shortcut(shortcut, move |app, _shortcut, event| {
                if event.state() == ShortcutState::Pressed {
                    emit_to_main(app, TRAY_TOGGLE_EVENT);
                }
            });

        if let Err(error) = result {
            eprintln!("failed to register global shortcut {shortcut}: {error}");
        }
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_always_on_top(true);
                let _ = window.unminimize();
                let _ = window.set_focus();
            }
        }))
        .plugin(tauri_plugin_autostart::Builder::new().build())
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .plugin(tauri_plugin_notification::init())
        .setup(|app| {
            setup_tray(app)?;
            setup_global_shortcuts(app);

            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_decorations(false);
                let _ = window.set_resizable(false);
                let _ = window.set_always_on_top(true);
                let _ = window.set_skip_taskbar(true);
                let _ = window.set_background_color(Some(tauri::utils::config::Color(0, 0, 0, 0)));

                if let Ok(Some(monitor)) = window.current_monitor() {
                    let monitor_size = monitor.size();
                    let monitor_position = monitor.position();
                    let scale_factor = monitor.scale_factor();
                    let window_width = (442.0_f64 * scale_factor).round() as u32;
                    let tab_width = (42.0_f64 * scale_factor).round() as u32;
                    let closed_offset = (2.0_f64 * scale_factor).round() as i32;

                    let _ = window
                        .set_size(tauri::PhysicalSize::new(window_width, monitor_size.height));
                    let _ = window.set_position(tauri::PhysicalPosition::new(
                        monitor_position.x + monitor_size.width as i32 - tab_width as i32
                            + closed_offset,
                        monitor_position.y,
                    ));
                }
            }

            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
