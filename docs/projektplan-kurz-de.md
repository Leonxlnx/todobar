# Todobar Projektplan Kurzfassung

Todobar soll ein echtes Desktop-Tool werden, nicht nur eine Browser-App. Die
aktuelle Browser-Version ist nur die Vorschau, damit wir UI und Produktlogik
schnell testen koennen.

## Ziel

Eine rechte Sidebar, die per Shortcut ueberall aufgeht:

- macOS und Windows zuerst
- schnell, clean, ruhig animiert
- Aufgaben schnell erfassen
- Today, Inbox, Month Plan und Later
- local-first, also ohne Account nutzbar
- spaeter AI und MCP, aber kontrolliert

## Runtime Entscheidung

Start mit Tauri v2.

Warum:

- leichter als Electron
- passt gut zu einem kleinen Desktop-Utility
- Rust-Backend fuer lokale Daten, Shortcuts, Tray, Fensterposition
- Capability-System passt zu einem sicheren Open-Source-Tool

Electron bleibt als Fallback, falls Tauri bei Fenster/Fokus/Multi-Monitor zu
unzuverlaessig ist.

## Was zuerst gebaut werden sollte

Milestone 0:

- saubere Task-Domain
- Storage-Adapter statt direkt `localStorage`
- Runtime-Adapter fuer Web/Tauri/Electron
- Command-System
- Command Palette Grundstruktur
- Suggestion Queue fuer AI-Vorschlaege
- aktuelle Sidebar weiter funktionierend halten

Danach Milestone 1:

- Tauri einbauen
- rechte native Sidebar
- globale Shortcuts
- Tray/Menu Bar
- Settings-Fenster
- Launch at Login
- Multi-Monitor-Test

## AI Richtung

AI soll nicht einfach Dinge veraendern.

AI soll Vorschlaege machen:

- "Plane meinen heutigen Tag"
- "Splitte diese Aufgabe"
- "Raeume meine Inbox auf"
- "Finde Blocker"

Der User bestaetigt oder lehnt ab.

## MCP Richtung

MCP soll spaeter Quellen verbinden:

- GitHub
- Kalender
- lokale Markdown-Dateien
- Notion/Docs
- Linear/Jira

Wichtig: keine versteckten Reads, keine stillen Writes. Alles bekommt Scope,
Quelle und Audit-Log.

## Widgets

Windows/macOS Widgets sind moeglich, aber nicht der erste Schritt.

Zuerst muss die echte Desktop-Sidebar perfekt sein. Widgets spaeter nur fuer:

- Today Count
- naechste Aufgabe
- Focus Task
- Shortcut zur App

## Open Source Richtung

Apache-2.0 ist die beste Default-Lizenz:

- permissiv
- patentfreundlicher als MIT
- gut fuer Plugin-/Connector-Oekosystem

## Wichtigste Risiken

- Shortcut wird von anderer App blockiert
- Fenster erscheint auf falschem Monitor
- Fokus springt komisch zurueck
- Windows DPI/Multi-Monitor Bugs
- unsignierte Builds wirken nicht vertrauenswuerdig
- AI/MCP liest zu viel Kontext
- Tool wird zu einem ueberladenen Dashboard

## Klare Produktregel

Todobar bleibt ein schnelles, lokales Sidebar-Tool. AI und MCP machen es
intelligenter, aber sie uebernehmen nicht ungefragt die Kontrolle.

