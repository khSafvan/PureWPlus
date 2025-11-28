# PureWPlus Modernization Plan: Road to a Modern, Resilient Desktop Companion

**Project:** PureWPlus (Fork of Pure Writer Desktop)  
**License:** GNU General Public License v3.0 (GPL-3.0)  
**Philosophy:** Minimum change that earns its place (Ponytail mindset), zero data loss, strict mobile protocol compatibility, and local-first resilience.

---

## Architecture & Hard Constraints

1. **Frozen Mobile Wire Protocol:**  
   The companion Android app communicates over raw TCP on port `19621` with `ProtobufVarint32` framing, an outer `PureWriterProtocol.Message` envelope, and JSON payloads. This protocol **must not be altered**. All sync enhancements (three-way merge, local revision history, conflict resolution UI, offline queues) exist on the desktop or operate additively.
2. **Zero Data Loss Guarantee:**  
   The current 1.x desktop app operates purely in RAM and wipes text on disconnect or crash. Modernized PureWPlus guarantees that **any operation that could overwrite text creates a timestamped local backup first**, with continuous debounced persistence to disk.
3. **Legal & Licensing:**  
   GPL-3.0 preserved throughout; all original headers retained with 2026 PureWPlus modification notices; full rebranding with original assets and [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) disclaimer.

---

## Milestone Overview

```
M1 Foundation: Build, Java 17 LTS, Packaging, Data Dirs, Mock Mobile Peer
       │
       ▼
M2 Sync Engine: Two-Way Sync, Three-Way Text Merge, Conflict UI, Backups
       │
       ▼
M3 UI Overhaul: Design System, Sidebar, Multi-Doc, Typewriter & Focus Modes
       │
       ▼
M4 Quality of Life: Autosave, Outline, Regex Search, Importers/Exporters, CI Installers
       │
       ▼
M5 Finish: Documentation, Real-Device Test Checklist, Release Verification
```

---

## Milestone 1 (M1): Foundation

**Goal:** Modernize runtime to Java 17 LTS + OpenJFX, establish standard platform data directories, replace obsolete packaging with `jpackage`, and build a simulated mobile peer test harness.

| Task | Files Affected | Tool / Add-on | Risk | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **1.1 Java 17 LTS & OpenJFX Build Modernization**<br>Upgrade Gradle wrapper to 8.5+, adopt official `org.openjfx.javafxplugin:0.1.0`, upgrade Kotlin compiler to 1.9+, and configure standard `jpackage` bundling to replace dead `javafx-gradle-plugin:8.8.2`. | `build.gradle`, `settings.gradle`, `gradle/wrapper/*` | Manual / Web search | Medium | `./gradlew test` passes on Java 17; `./gradlew build` produces executable jar and jpackage installer. |
| **1.2 Standard Platform Data Directories (`AppPaths`)**<br>Implement OS-standard paths:<br>• Linux: `${XDG_DATA_HOME:-~/.local/share}/PureWPlus`<br>• macOS: `~/Library/Application Support/PureWPlus`<br>• Windows: `%APPDATA%\PureWPlus`<br>Includes one-time automatic migration from legacy Java Preferences (`ip`, `darkMode`) into `config.json`, plus a `--data-dir` CLI override. | `src/main/kotlin/.../storage/AppPaths.kt`, `Settings.kt` | Manual | Low | Unit tests verify path resolution on Linux/macOS/Windows; verify preferences migrated without loss. |
| **1.3 Mock Mobile Peer Test Harness (`MockMobilePeer`)**<br>Build a Netty-based mock mobile server listening on localhost port 19621 that speaks the frozen ProtobufVarint32 protocol. Supports simulated article pushes, message dropped events, network disconnects, and packet delays. | `src/test/kotlin/.../testutil/MockMobilePeer.kt` | Direct Kotlin / Netty | Low | Unit tests connect `Client` to `MockMobilePeer`, verifying full handshake, article sync, and disconnect handling. |
| **1.4 CI Workflow Matrix**<br>Modernize `.github/workflows/gradle.yml` with a multi-OS build matrix (`ubuntu-latest`, `macos-latest`, `windows-latest`) on Java 17 Temurin, executing automated contract and harness tests. | `.github/workflows/gradle.yml` | Manual | Low | GitHub Actions runs and passes across all 3 platforms. |

---

## Milestone 2 (M2): Sync Engine (The Core Feature)

**Goal:** Transform the ephemeral memory-only mirror into a reliable, offline-tolerant, conflict-resolving two-way sync engine.

| Task | Files Affected | Tool / Add-on | Risk | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **2.1 Document Storage & Metadata Store**<br>Persist documents as plain text with JSON sidecars (`.meta.json`) in `data_dir/documents/`. Tracks `contentHash` (SHA-256), `baseRevisionHash`, `lastModified`, and `syncStatus` (`SYNCED`, `DIRTY`, `CONFLICT`). | `storage/DocumentStore.kt`, `model/Document.kt` | Manual | Medium | Unit tests verify CRUD operations, atomic writes, and hash computation across CJK and UTF-8 text. |
| **2.2 Pre-Overwrite Backup & Local History Engine**<br>Before *any* external sync write or local update overwrites a document, automatically store a timestamped snapshot in `data_dir/backups/<doc_id>/`. Provide revision rollback API. | `storage/BackupEngine.kt`, `storage/HistoryManager.kt` | Ponytail review (keep minimal) | Low | Unit tests assert that modifying text generates a valid recoverable backup file. |
| **2.3 Three-Way Merge Engine (Diff3)**<br>Implement an in-memory line-based three-way text merge (`diff3` algorithm):<br>• `base`: Common ancestor text from last successful sync.<br>• `local`: Current desktop text.<br>• `remote`: Incoming phone text.<br>Cleanly merges non-overlapping edits automatically; flags conflicting overlapping hunks. | `sync/MergeEngine.kt`, `sync/Diff3.kt` | Manual / Unit tests | High | Comprehensive test suite covering non-overlapping edits, simultaneous edits, whitespace/CRLF differences, and CJK text. |
| **2.4 Conflict Resolution UI**<br>Provide a side-by-side visual conflict resolution dialog:<br>• Left: Mobile version<br>• Right: Desktop version<br>• Center: Merged preview<br>Actions: "Use Phone", "Use Desktop", "Keep Both (save conflict copy)", or accept individual hunks. Never silently overwrite. | `ui/ConflictDialog.fxml`, `ui/ConflictDialog.kt` | JavaFX | Medium | Test with simulated peer sending conflicting text; verify user can choose either branch or keep both. |
| **2.5 Offline Queue & Auto-Reconnect**<br>Queue local edits while disconnected. When TCP re-establishes, perform three-way merge against phone state. Auto-reconnect with exponential backoff (1s, 2s, 4s, max 15s). Remember multiple devices/networks. | `sync/SyncCoordinator.kt`, `net/Client.kt` | Netty / Kotlin | Medium | Test dropping connection with `MockMobilePeer`, editing offline, reconnecting, and confirming sync recovery. |
| **2.6 Watched Folder Sync (Non-Mobile Target)**<br>Optional local directory watcher enabling synchronization with Syncthing, Nextcloud, or Dropbox without modifying phone protocol. | `sync/FolderWatcher.kt` | Java NIO WatchService | Low | Test file change detection in watched directory. |

---

## Milestone 3 (M3): UI Overhaul & Design System

**Goal:** Modern, clean, distraction-free writing environment with coherent typography, responsive layout, dark/light theme, and accessibility.

| Task | Files Affected | Tool / Add-on | Risk | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **3.1 Modern Design System & CSS Architecture**<br>Refactor CSS into modular stylesheets (`base.css`, `theme-light.css`, `theme-dark.css`) using CSS variables and clean color/spacing scales. Support System / Light / Dark modes. Crisp SVG-rendered icons. | `resources/.../styles/*` | UI Skills / CSS | Low | Visual inspection across light and dark themes on HiDPI display. |
| **3.2 Main Window Redesign (Sidebar & Status Bar)**<br>• Left sidebar: document list with search filter, word count preview, and sync state badges.<br>• Multi-document tabs or quick switcher (`Ctrl+Tab`).<br>• Bottom status bar: Sync indicator (`Connected`, `Syncing`, `Offline`, `Conflict`), live word/char count, cursor line/col. | `resources/.../editor.fxml`, `PureWriter.kt` | JavaFX FXML | Medium | Functional UI test verifying tab switching, search filtering, and status bar updates. |
| **3.3 Focused Writing Modes**<br>• Distraction-Free mode (hides sidebar, status bar, and toolbar).<br>• Typewriter mode (keeps active cursor line centered vertically).<br>• Fullscreen mode (`F11`).<br>• Adjustable editor margins (600px, 800px, full), line height, and font family. | `ui/EditorView.kt`, `PureWriter.kt` | JavaFX | Low | Verify typewriter scrolling and distraction-free hotkeys. |
| **3.4 Settings Dialog & Command Palette**<br>• Centralized Settings dialog (General, Sync, Appearance, Shortcuts, About).<br>• Command Palette (`Ctrl+P` / `Cmd+P`) for fast keyboard action discovery. | `ui/SettingsDialog.kt`, `ui/CommandPalette.kt` | JavaFX | Low | Open palette via shortcut, type command, verify execution. |
| **3.5 Accessibility & Externalized i18n**<br>• Replace hardcoded string ternaries with standard `ResourceBundle` (`messages_en.properties`, `messages_zh_CN.properties`, `messages_zh_TW.properties`).<br>• High-contrast focus rings and keyboard navigation. | `resources/.../messages*.properties`, `Globals.kt` | i18n bundle | Low | Switch locale to English/Chinese and verify complete UI translation. |

---

## 4. Milestone 4 (M4): Quality of Life & Creature Comforts

**Goal:** Frictionless writing workflow, autosave recovery, document outline, search/replace, and native cross-platform packages.

| Task | Files Affected | Tool / Add-on | Risk | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **4.1 Debounced Autosave & Crash Recovery**<br>Continuous debounced autosave (500ms) with a local recovery journal. If the process is killed abruptly, text is restored upon restart. | `storage/AutosaveManager.kt` | Kotlin | Low | Kill process during typing; relaunch and verify text recovery prompt appears. |
| **4.2 Find & Replace Bar**<br>In-document search bar supporting case sensitivity, whole word, regex, match count indicator, and replace-all. | `ui/SearchBar.kt` | JavaFX | Low | Unit and UI tests for regex find/replace. |
| **4.3 Markdown Document Outline**<br>Real-time outline parser displaying clickable heading hierarchy (`#`, `##`, `###`) in the sidebar for long-form navigation. | `ui/DocumentOutline.kt` | Kotlin / Regex | Low | Type markdown headings; verify outline tree updates and clicking jumps cursor to heading. |
| **4.4 Import & Export**<br>Export documents to Markdown (`.md`), Plain Text (`.txt`), and HTML. Export entire library as a zip archive. Drag-and-drop file import into editor. | `export/DocumentExporter.kt` | Java IO / Zip | Low | Export document and inspect generated file contents. |
| **4.5 Native Packaging via CI**<br>Configure CI builds for platform installers using `jpackage`:<br>• Linux: `.deb` and `.AppImage`<br>• Windows: `.msi` / `.exe`<br>• macOS: `.dmg` | `.github/workflows/package.yml` | Gradle / jpackage | Medium | Artifacts generated in GitHub Actions releases; verify installation on test OS. |

---

## 5. Milestone 5 (M5): Finish & Release Verification

**Goal:** Final documentation, comprehensive test review, real-device test protocol, and release readiness.

| Task | Files Affected | Tool / Add-on | Risk | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **5.1 Documentation & Fork Notices**<br>Finalize `README.md`, `FORK.md`, architecture docs, and user connection guide with screenshots. | `README.md`, `FORK.md` | Markdown | Low | Docs review. |
| **5.2 Knowledge Graph Refresh**<br>Run `graphify update .` to ensure the final architecture matches community boundaries and introduces no cross-layer coupling. | `graphify-out/` | Graphify | Low | Inspect graph report and verify cohesion scores. |
| **5.3 Real-Device Manual Verification Checklist**<br>Execute the end-to-end testing protocol against a physical Android device running Pure Writer. | Documentation | Manual | Medium | Pass all verification checklist items before release. |

---

## 6. Tier 3: What We Chose NOT to Do (and Why)

1. **REJECTED: Rewriting GUI in Compose Desktop, Electron, or Flutter.**  
   *Rationale:* JavaFX 17+ is lightweight, cross-platform, starts in under a second, consumes <60MB RAM, and already has working FXML/CSS foundations. A framework rewrite violates Ponytail, risks months of churn, and introduces high regression probability.
2. **REJECTED: Cloud Relay / Remote Webhook Sync.**  
   *Rationale:* Pure Writer users value privacy and zero-cloud operation. Local WiFi TCP sync keeps user data entirely private and eliminates server hosting/maintenance costs.
3. **REJECTED: Modifying the Mobile Wire Schema.**  
   *Rationale:* Modifying existing wire envelopes or message numbers would immediately break compatibility with the official Android application.
4. **REJECTED: Heavy External NLP / Syntax Parser Dependencies.**  
   *Rationale:* Retain fast regex/lightweight parsers for Markdown outlines and search instead of heavy AST parsing dependencies.

---

## 7. Risks & Mitigation Matrix

| Risk | Severity | Mitigation |
| :--- | :--- | :--- |
| **Three-way merge data corruption** | High | Every merge operation writes a pre-merge backup snapshot to disk before applying edits; diff3 algorithm tested with extensive property-based test cases. |
| **JavaFX 17 packaging complexity** | Medium | Use modern, maintained `jpackage` via Gradle rather than deprecated legacy Java 8 javapackager. |
| **Concurrent socket disconnects** | Medium | Offline queue stores edits in persistent storage, not RAM; reconnection initiates reconciliation using content hashes. |
| **GPL compliance & Trademark infringement** | Medium | Complete rebrand to PureWPlus, distinct icons and assets, clear "Unofficial fork" disclaimers in `FORK.md` and About dialog. |

---

## 8. Open Questions for You

1. **Conflict Default Behavior:** When both phone and desktop edit the exact same sentence while disconnected, should PureWPlus default to popping up the side-by-side diff resolution dialog, or automatically create a "Conflict Copy" and notify you? *(Recommended: create conflict copy automatically with a banner offering side-by-side resolution).*
2. **Local Library Storage Format:** Do you prefer articles stored as a folder of clean human-readable `.md` / `.txt` files with hidden metadata, or inside a local SQLite database? *(Recommended: clean folder of `.md` files so other tools like Obsidian or Syncthing can read them directly).*
