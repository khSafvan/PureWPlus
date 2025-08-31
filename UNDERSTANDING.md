# Project Understanding: Pure Writer Desktop (PureWPlus)

## 1. Available Tooling

Prior to conducting this scan, the environment was inspected for available skills, plugins, subagents, and tools. The following tooling will be utilized across tasks:

| Tool / Add-on | Source / Type | Role & Target Task |
| :--- | :--- | :--- |
| **Graphify** (`/graphify`, `graphify-out/`) | Skill / CLI | Codebase structure analysis, community detection, call path tracing, and ensuring zero unintended coupling. |
| **Ponytail** (`ponytail`, `ponytail-audit`, `ponytail-review`) | Skill / Plugin | Enforcing the "minimum change that works" mindset, guarding against speculative abstractions, and reviewing diffs. |
| **Web Search** (`search_web`) | Native tool | Researching CVE advisories, library migration paths, release changelogs, and dependency compatibility matrices. |
| **Git / Shell** (`run_command`) | Native sandbox / elevated tool | Executing baseline tests, characterization tests, git branch management, and atomic commits. |
| **Direct Contract Testing** | Kotlin / JUnit 4 | Writing contract and characterization tests for the Protobuf + Netty + JSON wire format. |

*Note: No third-party MCP servers or global tools are required; all tasks are covered by existing built-ins and plugins.*

---

## 2. Stack Inventory

- **Runtime & JVM:** Java 8 (`sourceCompatibility = 1.8`). Relies on JavaFX bundled in Oracle JDK 8 or Zulu 8 `jdk+fx`.
- **Language:** Kotlin 1.4.0 (released August 2020, targeting JVM 1.8).
- **GUI Framework:** JavaFX 8 (FXML layout `editor.fxml`, light/night CSS stylesheets, custom font loading).
- **Build System:** Gradle 5.6.4 (via `gradlew` wrapper), using `javafx-gradle-plugin:8.8.2` (de.dynamicfiles.projects.gradle.plugins).
- **Lockfile:** None. Dynamic dependencies declared in `build.gradle`.
- **Database & Persistence:** No SQL or NoSQL database. Local state (dark mode, saved IP) is stored using standard OS-level Java Preferences (`java.util.prefs.Preferences.userNodeForPackage`).
- **CI/CD:** `.github/workflows/gradle.yml` configured on `windows-latest` with `actions/setup-java@v1` (JDK 1.8 with `java-package: jdk+fx`), executing `./gradlew jfxNative --scan`.
- **Third-Party Repositories:** `build.gradle` references Alibaba JCenter mirror (`https://maven.aliyun.com/repository/jcenter`), which points to the retired JCenter service (sunset May 2021).

---

## 3. Dependency Audit Table

| Dependency | Current Version | Release Year | Known Vulnerabilities / CVEs | Maintenance & Compatibility Status |
| :--- | :--- | :--- | :--- | :--- |
| **`io.netty:netty-all`** | `4.1.51.Final` | 2020 | **Multiple High/Critical CVEs:**<br>• CVE-2021-21290 (HTTP request smuggling)<br>• CVE-2021-37136 (Bzip2 decompression DoS)<br>• CVE-2021-37137 (SNI buffer / TLS renegotiation)<br>• CVE-2021-43797 (Control character header validation)<br>• CVE-2024-29025 (HttpPostRequestDecoder DoS) | **Outdated.** `netty-all` is deprecated by upstream Netty; should be updated or trimmed to core modules (`netty-transport`, `netty-codec`, `netty-handler`). |
| **`com.google.protobuf:protobuf-java`** | `3.11.4` | 2020 | **High/Moderate CVEs:**<br>• CVE-2024-7254 (Unbounded recursion / StackOverflow DoS via nested groups)<br>• CVE-2022-3510 & CVE-2022-3509 (CPU consumption DoS in message extensions)<br>• CVE-2022-3171 (Parsing CPU consumption DoS) | **Outdated.** Version 3.11.4 is 6+ years old. Fixed in modern `3.25.x` and `4.x`. |
| **`com.google.code.gson:gson`** | `2.8.5` | 2018 | **Moderate CVE:**<br>• CVE-2022-25647 (Deserialization of untrusted data causing DoS) | **Outdated.** Modern stable is `2.10.x` or `2.11.0` (zero breaking changes for standard POJO serialization). |
| **`io.reactivex.rxjava2:rxjava`** | `2.2.19` | 2020 | None directly flagged. | **End of Life.** RxJava 2 reached official EOL in Feb 2021. Replaced by RxJava 3. Used here solely for a local event bus (`RxBus`) and a 1s timer. |
| **`com.jakewharton.rxrelay2:rxrelay`** | `2.1.1` | 2018 | None. | **Unmaintained.** Companion to RxJava 2. |
| **`io.reactivex:rxjavafx`** | `2.0.2` | 2018 | None. | **Unmaintained since 2018.** JavaFX reactive bindings. |
| **`com.github.houbb:opencc4j`** | `1.6.0` | 2019 | None. | **Over-engineered.** Bundles large dictionary tables solely to translate 4 static Chinese UI labels to Traditional Chinese at runtime. |
| **`junit:junit`** | `4.12` | 2014 | • CVE-2020-15250 (TemporaryFolder vulnerability) | **Outdated.** JUnit 4.12 should be updated to `4.13.2` (or JUnit 5), but has zero tests currently written against it. |
| **`javafx-gradle-plugin`** | `8.8.2` | 2018 | Relies on dead Oracle Java 8 javapackager APIs. | **Dead/Unmaintained.** Cannot run on modern Gradle 7+ or JDK 11+ without replacement (e.g. `org.openjfx.javafxplugin`). |
| **`kotlin-gradle-plugin`** | `1.4.0` | 2020 | Unsupported on modern Gradle 7, 8, 9. | **End of Life.** Kotlin 1.4 compiler is obsolete. |

---

## 4. Baseline Build & Test Results

1. **Gradle Build Status:**
   - `./gradlew --version` / `./gradlew build` fails in the sandbox environment because:
     1. Gradle wrapper is locked to Gradle 5.6.4. The sandbox restricts outbound network calls, preventing the download of `gradle-5.6.4-all.zip`.
     2. Host environment `JAVA_HOME` was set to an invalid path (`~/.local/share/mise/installs/java/temurin-17.0.21+9`, which only contained `lib/` without `bin/java`).
     3. The only functional JDK detected on the host is JetBrains Runtime 25 (`~/.gradle/jdks/jetbrains_s_r_o_-25-amd64-linux.2`), but Gradle 5.6.4 cannot run on Java 25.
2. **Test Suite Status:**
   - **0 test suites exist.** The directory `src/test/` does not exist in the repository.
   - Despite `junit:junit:4.12` being in `build.gradle`, there are no automated regression or contract tests.
3. **Application Execution:**
   - Application cannot be launched directly in headless terminal mode without JavaFX 8 / OpenJFX runtime and an active display.

---

## 5. Mobile API Contract Specification (FROZEN)

The desktop application acts as a TCP client connecting to the Pure Writer Android app, which runs an embedded TCP socket server. **This contract is frozen and must not be altered.**

### A. Transport & Framing
- **Protocol:** Raw TCP Socket
- **Port:** `19621`
- **Framing Pipeline:**
  - Inbound: `ProtobufVarint32FrameDecoder` &rarr; `ProtobufDecoder(PureWriterProtocol.Message)` &rarr; `MessageHandler`
  - Outbound: `ProtobufVarint32LengthFieldPrepender` &rarr; `ProtobufEncoder`
- **Authentication:** None. Direct plaintext TCP connection over local WiFi.
- **Session / Heartbeat:**
  - Kept alive by TCP keep-alive (`ChannelOption.SO_KEEPALIVE = true`) and application-level ping/pong.
  - Desktop sends a `Ping` message every 1.0 second.
  - If no message (or `Pong`) is received within 3.0 seconds, desktop forcibly closes the socket and triggers disconnect state.

### B. Wire Envelope (`PureWriterProtocol.Message`)
Defined in `proto/Protocols.proto` (`proto3` syntax):
```protobuf
message Message {
  int32 id = 1;          // Message sequence / correlation ID
  int32 version = 2;     // Protocol version: always 2
  string contentType = 3;// Content format: always "JSON"
  string messageType = 4;// Message identifier string
  string content = 5;    // JSON-serialized payload body
  bytes data = 6;        // Optional binary data (unused in 1.x)
  uint64 time = 7;       // Milliseconds since epoch
}
```

### C. Message Types & Payload Shapes

#### 1. `ArticleMessage` (Bidirectional)
- **Role:** Synchronizes article text, title, and cursor selection between mobile and desktop.
- **Direction:** Mobile &rarr; Desktop (when mobile article opens/updates) | Desktop &rarr; Mobile (when user types on desktop).
- **Envelope:** `id = 1`, `version = 2`, `contentType = "JSON"`, `messageType = "ArticleMessage"`.
- **JSON Payload:**
  ```json
  {
    "title": "Document Title",
    "content": "Article body text...",
    "selectionStart": 0,
    "selectionEnd": 0
  }
  ```
- **Code Reference:** `model/ArticleMessage.kt`, `net/MessageHandler.kt`, `net/SDK.kt`.

#### 2. `EmptyArticleMessage` (Inbound: Mobile &rarr; Desktop)
- **Role:** Signals that no article is currently open on the mobile device.
- **Direction:** Mobile &rarr; Desktop.
- **Envelope:** `version = 2`, `contentType = "JSON"`, `messageType = "EmptyArticleMessage"`, `content = ""`.
- **Desktop Behavior:** Clears content view and displays placeholder prompt.
- **Code Reference:** `model/Events.kt`, `net/MessageHandler.kt`, `PureWriter.kt`.

#### 3. `WordCountMessage` (Inbound: Mobile &rarr; Desktop)
- **Role:** Provides real-time word count string from mobile editor.
- **Direction:** Mobile &rarr; Desktop.
- **Envelope:** `version = 2`, `contentType = "JSON"`, `messageType = "WordCountMessage"`.
- **JSON Payload:**
  ```json
  {
    "text": "125 字"
  }
  ```
- **Desktop Behavior:** Appends word count to window title bar (`"$title · $text"`).
- **Code Reference:** `model/WordCountMessage.kt`, `net/MessageHandler.kt`.

#### 4. `Ping` (Outbound: Desktop &rarr; Mobile)
- **Role:** Periodic keepalive heartbeat.
- **Direction:** Desktop &rarr; Mobile.
- **Envelope:** `id = 0`, `version = 2`, `contentType = "JSON"`, `messageType = "Ping"`, `content = "\"\""`.
- **Code Reference:** `net/PingPongDelegate.kt`, `net/SDK.kt`.

#### 5. `Pong` / Any Inbound Activity (Inbound: Mobile &rarr; Desktop)
- **Role:** Heartbeat acknowledgement.
- **Direction:** Mobile &rarr; Desktop.
- **Behavior:** Receipt of any non-disconnect packet resets the 3-second disconnect timer in `PingPongDelegate.pong()`.

#### 6. `DisconnectMessage` (Inbound: Mobile &rarr; Desktop)
- **Role:** Graceful termination request from mobile.
- **Direction:** Mobile &rarr; Desktop.
- **Envelope:** `messageType = "DisconnectMessage"`.
- **Desktop Behavior:** Closes Netty channel context immediately.

---

## 6. Dead Code & Dead Services

1. **Dead `Pong` Class:** `com.drakeet.purewriter.model.Events.kt` declares `class Pong`, but the ping-pong mechanism calls `PingPongDelegate.pong()` directly on any packet.
2. **`Log.kt` Debug Wrapper:** Has `private const val DEBUG = false` hardcoded; prints nothing in production.
3. **Dead Repositories in `build.gradle`:** Alibaba JCenter mirror (`https://maven.aliyun.com/repository/jcenter`) points to retired infrastructure.
4. **Unused Dependencies:** `junit:junit:4.12` declared under `testCompile` with zero test suites.
5. **Commented-out Code:** `// compile "com.squareup.okio:okio:2.3.0"` in `build.gradle`.

---

## 7. Secrets and Hardcoded Configuration

- **Secrets:** No API keys, tokens, passwords, or private certificates exist in the repository.
- **Hardcoded Endpoints/Config:**
  - Port `19621`: The fixed TCP port used by the Android server (`Client.start(ip, 19621)`).
  - Version assertion: `PureWriter.kt` hardcodes `appVersion = "17.7.10"`.
  - Fallback Subnet: `PureWriter.kt` auto-prefixes single-octet inputs with `192.168.1.` (e.g. `45` becomes `192.168.1.45`).
