# Project Understanding: PureWPlus (Desktop Client for Pure Writer)

## 1. Available Tooling

Prior to this scan, the environment was inspected for available skills, plugins, subagents, and tools:

| Tool / Add-on | Source / Type | Role & Target Task |
| :--- | :--- | :--- |
| **Graphify** (`/graphify`, `graphify-out/`) | Skill / CLI | Architectural analysis, dependency mapping, call-flow verification, and post-milestone coupling checks. |
| **Ponytail** (`ponytail`, `ponytail-audit`, `ponytail-review`) | Skill / Plugin | Enforcing the "minimum change that earns its place" mindset, guarding against over-engineering and speculative abstractions. |
| **Web Search** (`search_web`) | Native tool | Researching CVE advisories, OpenJFX documentation, Java 17 LTS packaging (`jpackage`), and migration guides. |
| **Git / Shell** (`run_command`) | Native tool | Running test suites, builds, branch management, and atomic commits. |
| **Protocol Test Harness** | Kotlin / JUnit 4 & Netty Embedded | Characterization tests, mock mobile peer server simulating phone TCP communication on port `19621`. |

*Note: No third-party MCP servers or global tools are required; all tasks are covered by existing built-ins and plugins.*

---

## 2. Stack Inventory

- **Runtime & JVM:** Java 8 (`sourceCompatibility = 1.8`) currently configured in Gradle; local runtime powered by BellSoft Liberica JDK 8 Full (`liberica-1.8.0`) configured via `mise.toml`.
- **Language:** Kotlin 1.4.0 (`jvmTarget = "1.8"`).
- **GUI Framework:** JavaFX 8 (FXML layout `editor.fxml`, `editor.css`, `editor-night.css`).
- **Build System:** Gradle 5.6.4 (via wrapper), using `de.dynamicfiles.projects.gradle.plugins:javafx-gradle-plugin:8.8.2`.
- **Dependencies:**
  - `io.netty:netty-all:4.1.118.Final` (upgraded from 4.1.51)
  - `com.google.protobuf:protobuf-java:3.25.5` (upgraded from 3.11.4)
  - `com.google.code.gson:gson:2.10.1` (upgraded from 2.8.5)
  - `junit:junit:4.13.2` (upgraded from 4.12)
  - `io.reactivex.rxjava2:rxjava:2.2.19`
  - `com.jakewharton.rxrelay2:rxrelay:2.1.1`
  - `io.reactivex:rxjavafx:2.0.2`
- **Database & Local Persistence:** Zero database engine. Uses `java.util.prefs.Preferences` storing exactly 2 properties (`darkMode`, `ip`).
- **CI/CD:** `.github/workflows/gradle.yml` running on `windows-latest` with `actions/checkout@v4` and `actions/setup-java@v4` (Zulu 8 `jdk+fx`).

---

## 3. Dependency Audit Table

| Dependency | Current Version | Status / Vulnerability State | Modernization Path |
| :--- | :--- | :--- | :--- |
| **`io.netty:netty-all`** | `4.1.118.Final` | **Secure.** Patched to latest 4.1.x line; all previous CVEs resolved. | Maintain on 4.1.x LTS. |
| **`com.google.protobuf:protobuf-java`** | `3.25.5` | **Secure.** Patched to hardened Protobuf 3.x LTS. | Maintain on 3.25.x line (preserves binary compatibility with generated proto). |
| **`com.google.code.gson:gson`** | `2.10.1` | **Secure.** Patched to latest stable Gson release. | Fully stable. |
| **`junit:junit`** | `4.13.2` | **Secure.** Patched to latest JUnit 4 release. | Upgrade to JUnit 5 in M1 if multi-test fixtures are introduced. |
| **`io.reactivex.rxjava2:rxjava`** | `2.2.19` | **End of Life.** RxJava 2 is EOL. Used only for local event bus (`RxBus`). | Replace with lightweight Kotlin Flow / channels in M2 or maintain as internal bus. |
| **`com.jakewharton.rxrelay2:rxrelay`** | `2.1.1` | **Unmaintained.** Companion to RxJava 2. | Replace alongside RxJava. |
| **`io.reactivex:rxjavafx`** | `2.0.2` | **Unmaintained.** JavaFX reactive scheduler. | Replace with native JavaFX `Platform.runLater` and `Timeline`. |
| **`javafx-gradle-plugin`** | `8.8.2` | **Obsolete.** Cannot run on Java 11/17+; relies on Oracle JDK 8 `ant-javafx.jar`. | Modernize in M1 to official `org.openjfx.javafxplugin` + `jpackage`. |

---

## 4. Baseline Build & Test Results

- **Build Status:** Builds and packages successfully under Liberica JDK 8 via `./gradlew build`.
- **Test Status:** 8/8 contract tests passing in [`ProtocolContractTest.kt`](file:///home/zack/Workshop/PureWPlus/src/test/kotlin/com/drakeet/purewriter/ProtocolContractTest.kt) (`BUILD SUCCESSFUL in 211ms`).
- **Execution:** Headless builds pass; GUI execution requires active display.

---

## 5. Precise Mobile Sync Protocol & Storage Contract (FROZEN)

The desktop application acts as a TCP socket client connecting to the Pure Writer Android app, which runs an embedded TCP server. **This contract is frozen and must never be broken.**

### A. Discovery & Connection
- **Mechanism:** User enters the phone's local WiFi IP address in `ipView`.
  - Automatic normalization: replaces Chinese full-width periods (`。`) with `.`.
  - Subnet shortcut: single integers (e.g. `45`) expand to `192.168.1.45`.
- **Port:** Fixed TCP port `19621`.
- **Handshake / Authentication:** **None.** No token, no password, no TLS. Immediate plaintext socket connection upon TCP connect.
- **Protocol Version:** Envelope field `version = 2`.
- **Heartbeat / Keepalive:**
  - Netty `ChannelOption.SO_KEEPALIVE = true`.
  - Desktop sends a `Ping` message every 1.0s.
  - If no incoming packet (or `Pong`) arrives within 3.0s, desktop disconnects socket and resets UI.

### B. Wire Envelope & Framing
Framed using Netty `ProtobufVarint32FrameDecoder` / `ProtobufVarint32LengthFieldPrepender` wrapping `PureWriterProtocol.Message`:
```protobuf
message Message {
  int32 id = 1;          // 0 for Ping/Heartbeat, 1 for ArticleMessage
  int32 version = 2;     // Always 2
  string contentType = 3;// Always "JSON"
  string messageType = 4;// Message identifier string
  string content = 5;    // JSON-serialized string
  bytes data = 6;        // Optional raw bytes (unused in 1.x)
  uint64 time = 7;       // Milliseconds epoch timestamp
}
```

### C. Message Flows

1. **Open / Edit Article (`ArticleMessage`):**
   - **Phone &rarr; Desktop:** When the user opens or edits an article on mobile, phone sends `ArticleMessage`:
     ```json
     {
       "title": "My Article",
       "content": "Body text...",
       "selectionStart": 0,
       "selectionEnd": 0
     }
     ```
   - **Desktop &rarr; Phone:** When user types in desktop `TextArea`, `invalidate()` sends `ArticleMessage` with current text, title, and selection.
   - **Echo Suppression:** Desktop uses `isSyncing` boolean flags (`isSyncingTitle`, `isSyncingContent`, `isSyncingSelection`) to prevent feedback loops when applying incoming changes.
2. **Empty State (`EmptyArticleMessage`):**
   - **Phone &rarr; Desktop:** Sent when mobile closes the active article. Desktop clears screen and displays placeholder.
3. **Word Count (`WordCountMessage`):**
   - **Phone &rarr; Desktop:** Sent by mobile with word count string (e.g. `{"text": "125 字"}`). Desktop displays in window title bar.
4. **Heartbeat (`Ping` / `Pong`):**
   - **Desktop &rarr; Phone:** Desktop sends `messageType = "Ping"`, `content = "\"\""`.
   - **Phone &rarr; Desktop:** Any incoming packet from phone resets the 3s timeout.
5. **Disconnect (`DisconnectMessage`):**
   - **Phone &rarr; Desktop:** Sent when phone terminates sync. Desktop immediately disconnects socket.
6. **List / Delete / Save:**
   - **DOES NOT EXIST in 1.x.** There are no messages for listing articles, deleting articles, or querying file trees. The phone controls which single article is currently active.

### D. Concurrency & Disconnection Behavior (The Critical Data-Loss Hazard)
- **Concurrent Edits:** **Last write wins.** There is no conflict detection, no three-way merge, no revision tracking, and no hashing. If user edits on desktop while phone edits, the latest incoming message blindly overwrites the buffer.
- **Connection Drop Mid-Edit:** When TCP connection drops, desktop calls `showIpViews()`, which runs `contentView.replaceText("")`. **The entire unsynced text in the desktop buffer is instantly wiped from memory with zero backup.**

### E. Storage on Disk per OS (Current State)
- **Article Storage:** **None.** Articles are never written to disk. The 1.x desktop app operates purely in RAM.
- **Settings Storage:** Stored via `java.util.prefs.Preferences` (`darkMode`, `ip`):
  - **Linux:** `~/.java/.userPrefs/com/drakeet/purewriter/prefs.xml`
  - **macOS:** `~/Library/Preferences/com.drakeet.purewriter.plist`
  - **Windows:** Registry `HKCU\Software\JavaSoft\Prefs\com\drakeet\purewriter`

---

## 6. UI & UX Audit

| Aspect | Current Implementation | Issues & Limitations |
| :--- | :--- | :--- |
| **Screens & Windows** | Single window defined in `editor.fxml`. | No multi-document tabs, no sidebar, no settings screen, no about dialog. |
| **Custom Controls** | None. Standard `TextArea`, `Label`, `TextField`, `ImageView`. | `TextArea` lacks line numbers, heading outlines, typewriter scrolling, or markdown styling. |
| **Styling** | `editor.css` and `editor-night.css`. | Minimal CSS. Missing typography scale, focus rings, and proper spacing tokens. |
| **HiDPI** | Relies on default JavaFX scaling. | Hardcoded fixed window bounds (968x615) and fixed font point sizes. |
| **Dark Mode** | Toggled via moon image button; swaps stylesheets. | No detection of system OS light/dark preference; hardcoded night stylesheet. |
| **Keyboard Handling** | Default JavaFX text editing. | No shortcuts (no Ctrl+S, Ctrl+F, Ctrl+P command palette, no focus shortcuts). |
| **i18n & Localization** | Scattered ternary checks (`if (isZh)`) in Kotlin files. | No `ResourceBundle` `.properties` files. Difficult to add new languages. |
| **Accessibility** | Basic JavaFX default. | No screen-reader labels, no keyboard accessibility for theme switch, no focus indicators. |

---

## 7. Legal Audit Cross-Reference

Full audit documented in [LEGAL_AUDIT.md](file:///home/zack/Workshop/PureWPlus/LEGAL_AUDIT.md).
- Root `LICENSE` (GPL-3.0) and Drakeet Xu copyright notices intact.
- Rebranded to **PureWPlus** with [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) disclaimer.
- Replaced all proprietary trademarked graphics with clean open-source assets.
