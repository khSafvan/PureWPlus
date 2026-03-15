# PureWPlus (Desktop Client for Pure Writer)

**PureWPlus** is a modernized, resilient, independent open-source desktop companion client for the **Pure Writer (纯纯写作)** mobile application. It enables fast, local real-time bidirectional WiFi synchronization between your Android device and computer without relying on external cloud servers, backed by a **Zero Data Loss Guarantee** and intelligent **Three-Way Merge**.

> [!NOTE]
> **Fork & Disclaimer:** This project is an independent fork of the earlier 1.x desktop client from [PureWriter/desktop](https://github.com/PureWriter/desktop), originally created by Drakeet Xu. This project is **not affiliated with, endorsed by, or sponsored by** Drakeet Xu. "Pure Writer" and "纯纯写作" are trademarks of their respective owners, used here under nominative fair use to describe protocol compatibility. See [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) for full details.

---

## Key Highlights & Modern Features

- **Zero Data Loss Guarantee:** Never wipes your text buffer when disconnecting or switching views. Before any incoming overwrite or restore, an atomic timestamped snapshot is saved to local storage.
- **Intelligent Three-Way Merge (Diff3):** If you make edits offline on desktop while editing on your phone, PureWPlus cleanly merges non-overlapping changes automatically without conflict or data loss. Overlapping edits are flagged with standard conflict markers.
- **Local-First Persistence:** Active documents and snapshots are persisted to standard OS data directories (`~/.local/share/PureWPlus`, `~/Library/Application Support/PureWPlus`, or `%APPDATA%\PureWPlus`).
- **Interactive Backups Drawer (`Ctrl+Shift+B`):** Browse recent snapshots with timestamps and byte sizes, and restore any previous version with a single click.
- **Modern Status Bar:** Real-time connection badge (● Connected / Offline), save status (Saved / Unsaved edits), live word & character counts, and quick-toggle action buttons.
- **Focused Writing Modes:**
  - **Typewriter Scrolling Mode:** Keeps the active cursor line vertically centered as you type.
  - **Distraction-Free Mode (`Ctrl+Shift+D`):** Hides chrome for a serene, uncluttered writing environment.
  - **Fullscreen Mode (`F11`):** True fullscreen writing experience.
- **Export & Backup:** Export your active document to Plain Text (`.txt`), Markdown (`.md`), or clean HTML (`.html`), and bundle all backup history into a `.zip` archive.
- **Complete Internationalization (i18n):** Native support for English, Simplified Chinese (`zh_CN`), and Traditional Chinese (`zh_TW`).
- **Automated Test Suite:** Over 35 comprehensive automated tests covering wire protocol framing, three-way merges, backup engine, cross-platform paths, and live Netty TCP peer integration.

---

## Keyboard Shortcuts

| Shortcut | Action |
| :--- | :--- |
| `Ctrl + S` | Force save active document to local disk |
| `Ctrl + Shift + B` | Toggle Backups & History drawer |
| `Ctrl + Shift + D` | Toggle Distraction-free mode |
| `F11` | Toggle Fullscreen |

---

## Prerequisites

- **Java Development Kit:** JDK 8 with bundled JavaFX (e.g., [BellSoft Liberica JDK 8 Full](https://bell-sw.com/) or [Azul Zulu 8 with JavaFX](https://www.azul.com/downloads/?package=jdk#zulu)).
  - If using [mise](https://mise.jdx.dev/):
    ```bash
    mise use java@liberica-1.8.0
    ```
- **Mobile App:** [Pure Writer for Android](https://play.google.com/store/apps/details?id=com.drakeet.purewriter) v17.7.10 or higher.

---

## Build & Test

### Run Test Suite
```bash
./gradlew test
```

### Build Distribution Fat JAR
```bash
./gradlew build
```
The resulting fat JAR will be available in `build/libs/` and copied to `jar/PureWPlus-0.3.1.jar`.

### Build Native Installers (jfxNative)
Requires an active platform display and JDK 8 with javapackager:
```bash
./gradlew jfxNative
```

---

## How to Connect to Pure Writer on Android

1. Connect your computer and Android phone to the **same WiFi network** (or hotspot).
2. Open **Pure Writer (纯纯写作)** on your Android phone.
3. Tap the **Cloud icon** in the top bar to start the local WiFi sync server. Note the IP address displayed on your phone (e.g., `192.168.1.45`).
4. Launch **PureWPlus** on your desktop.
5. Enter the IP into the input field. If your IP begins with `192.168.1.`, you can simply enter the final digits (e.g., `45`).
6. The desktop client connects automatically, loads your current article, and keeps both devices in sync.

---

## Project Structure

- `proto/`: Protocol Buffer definition (`Protocols.proto`) defining the TCP synchronization protocol.
- `scripts/`: Utility and icon generation scripts (`gen_iconset.py`).
- `sketch/`: Artwork master images and icon source files.
- `src/main/deploy/package/`: Platform-specific native installer assets (Windows, macOS, Linux).
- `src/main/java/com/drakeet/purewriter/protocol/`: Generated Java Protocol Buffer code.
- `src/main/kotlin/com/drakeet/purewriter/`:
  - `net/`: Netty TCP client, Varint32 framing, heartbeat delegate, and message handlers.
  - `storage/`: `AppPaths` (XDG/%APPDATA%), `AppConfig`, `BackupEngine`, `DocumentStore`, and `DocumentExporter`.
  - `sync/`: `Diff3` line merge engine and `SyncCoordinator` two-way synchronization coordinator.
  - `i18n/`: `I18n` runtime and UTF-8 ResourceBundle loader.
  - `model/`: `ArticleMessage`, `WordCountMessage`, `Events`, and `MarkdownOutline`.
  - `PureWriter.kt`: JavaFX application controller.
- `src/main/resources/com/drakeet/purewriter/`:
  - `editor.fxml`: Modern layout with status bar and backups drawer.
  - `editor.css` & `editor-night.css`: Light and night design tokens.
  - `i18n/`: `messages.properties`, `messages_zh_CN.properties`, `messages_zh_TW.properties`.
- `src/test/kotlin/com/drakeet/purewriter/`: Full automated test suite (35+ unit & integration test cases).

---

## License

This software is distributed under the terms of the **GNU General Public License v3.0**.  
See [LICENSE](file:///home/zack/Workshop/PureWPlus/LICENSE) and [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) for full terms and copyright notices.
