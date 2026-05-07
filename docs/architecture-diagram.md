# Architecture Diagram

```mermaid
flowchart TD
  UI["React Sidebar UI"]
  Palette["Command Palette"]
  Commands["Command Layer"]
  Domain["Task Domain"]
  Repo["Task Repository Interface"]
  WebStore["Web localStorage Adapter"]
  Sqlite["Tauri SQLite Adapter"]
  Runtime["Desktop Runtime Interface"]
  WebRuntime["Web Runtime Adapter"]
  TauriRuntime["Tauri Runtime Adapter"]
  ElectronRuntime["Electron Runtime Adapter (Fallback)"]
  Suggest["Suggestion Engine"]
  AI["AI Provider Adapter"]
  MCP["MCP Client Adapter"]
  Perms["Permission + Audit Layer"]
  Sources["External Sources"]

  UI --> Commands
  Palette --> Commands
  Commands --> Domain
  Domain --> Repo
  Repo --> WebStore
  Repo --> Sqlite
  Commands --> Runtime
  Runtime --> WebRuntime
  Runtime --> TauriRuntime
  Runtime --> ElectronRuntime
  Commands --> Suggest
  Suggest --> Perms
  Perms --> AI
  Perms --> MCP
  MCP --> Sources
  AI --> Suggest
  Suggest --> Commands
```

## Important Boundary

The React UI does not import Tauri, Electron, MCP servers, or AI SDKs directly.
Everything goes through command, runtime, repository, and permission layers.

