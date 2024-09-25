# Project Understanding: Pure Writer Desktop (PureWPlus)

## 1. What the Project Does
Pure Writer Desktop is a lightweight desktop companion application for the Android text editor **Pure Writer (纯纯写作)**. It facilitates real-time, bidirectional WiFi synchronization between mobile and desktop devices.
- **Sync mechanism:** A Netty socket client on the desktop connects to the Pure Writer mobile app's built-in TCP server on port `19621`.
- **Protocol:** Messages are framed with Protobuf varint32 headers, containing a Protobuf outer envelope (`PureWriterProtocol.Message`) with JSON-encoded payloads (`ArticleMessage`, `WordCountMessage`, `Ping`/`Pong` heartbeats).
- **Editor UX:** Built with JavaFX, featuring auto-reconnect, dark mode switching, Chinese punctuation IP auto-formatting, traditional/simplified Chinese handling, and text/selection synchronization without echo feedback loops.

---

## 2. Tech Stack
- **Language:** Kotlin 1.4.0 (`jvmTarget = "1.8"`), Java 8 (Protobuf generated classes)
- **GUI Framework:** JavaFX 8 (FXML layout `editor.fxml`, light/dark CSS, custom embedded Source Han Sans font)
- **Build System:** Gradle 5.6.4 (via wrapper), `javafx-gradle-plugin:8.8.2`
- **Networking:** Netty 4.1.51.Final (`NioSocketChannel`, `ProtobufVarint32FrameDecoder`, `ProtobufDecoder`/`ProtobufEncoder`)
- **Serialization:** Google Protocol Buffers 3.11.4 (`protobuf-java`), Google Gson 2.8.5
- **Event Bus & Concurrency:** RxJava 2.2.19, RxRelay 2.1.1, RxJavaFX 2.0.2
- **Localization:** `opencc4j:1.6.0` (Open Chinese Convert for Traditional Chinese conversion)
- **Packaging:** JavaFX native bundler (`jfxNative` generating OS installers via `src/main/deploy/package/`)

---

## 3. Current Directory Tree and Folder Purposes

```
PureWPlus/
├── .github/workflows/          # GitHub Actions CI workflow (Windows native package build)
├── gradle/wrapper/             # Gradle wrapper bootstrap jar and properties (Gradle 5.6.4)
├── proto/                      # Protobuf schema definition file
│   ├── lite/                   # [DEAD] Outdated generated Java Protobuf Lite sources
│   └── out/                    # [DEAD] Outdated generated Java Protobuf full sources
├── sketch/                     # Design assets, high-res app icon, and macOS icns generator script
├── src/
│   └── main/
│       ├── deploy/package/     # OS installer icons (Linux PNG, macOS ICNS, Windows ICO/BMP)
│       ├── java/               # Active generated Java Protobuf classes (PureWriterProtocol.java)
│       ├── kotlin/             # Kotlin application source code (flat package: com.drakeet.purewriter)
│       └── resources/          # JavaFX assets (FXML, light/night CSS, TTF font, moon icon)
├── build.gradle                # Gradle build script defining dependencies, packaging, and tasks
├── gradle.properties           # Gradle JVM arguments and project properties
├── gradlew / gradlew.bat       # Gradle wrapper execution scripts
├── LICENSE                     # GNU General Public License v3.0
├── README.md                   # Project description and quickstart instructions
└── settings.gradle             # Project name definition ('PureWriter')
```

### One-Line Purpose Per Folder
- `.github/workflows/`: Automated CI pipeline configuring JDK 1.8 with JavaFX to build native binaries.
- `gradle/wrapper/`: Pins project build to Gradle 5.6.4.
- `proto/`: Contains `Protocols.proto` contract specifying the wire format between phone and desktop.
- `proto/lite/`: Stale pre-generated Protobuf Lite classes (redundant with `src/main/java`).
- `proto/out/`: Stale pre-generated full Protobuf classes (redundant with `src/main/java`).
- `sketch/`: Artwork staging area containing `Pure Writer.png`, `Pure Writer.icns`, and `gen_iconset.py`.
- `src/main/deploy/package/`: Platform native bundle icons for Windows, macOS, and Linux required by `javafx-gradle-plugin`.
- `src/main/java/com/drakeet/purewriter/protocol/`: Java source code generated from `Protocols.proto`.
- `src/main/kotlin/com/drakeet/purewriter/`: Application controller, networking, state, and bus logic.
- `src/main/resources/com/drakeet/purewriter/`: UI markup, styling, fonts, and images loaded at runtime by JavaFX.

---

## 4. Graphify Dependency & Community Analysis

Graphify extracted **455 nodes and 803 edges** across **33 communities**.

### God Nodes
1. `PureWriterProtocol.Message.Builder` (degree: 51) – Protobuf message builder with methods across the codebase.
2. `PureWriterProtocol.Message` (degree: 39–40) – Core protocol wire object.
3. `PureWriter` (degree: 20) – Main JavaFX UI controller tying together editor UI, socket lifecycle, and event subscriptions.

### Functional Clusters
- **UI & Presentation (`PureWriter`, `Main`, `DayNights`, `Globals`, `Settings`, `TextAreas`):**
  Owns window staging, FXML binding, dark mode styling, and input listening.
- **Networking & Transport (`Client`, `SDK`):**
  Configures Netty `Bootstrap`, manages `NioEventLoopGroup`, handles socket connection to port `19621`, and provides outbound send extensions on `Channel`.
- **Protocol & Inbound Handling (`MessageHandler`, `PingPongDelegate`):**
  Processes incoming Protobuf envelopes from Netty, triggers periodic ping/pong heartbeats to keep the TCP channel alive, and manages disconnection states.
- **Event Bus & Models (`RxBus`, `Events`, `ArticleMessage`, `WordCountMessage`, `Jsons`):**
  Decouples the Netty background thread from JavaFX's UI thread using `RxRelay` and `JavaFxScheduler.platform()`.

### End-to-End Execution Flow
1. **Startup:** `Main.kt` loads `editor.fxml`. JavaFX instantiates `PureWriter` as controller. `Main` applies `editor.css` and sets dark mode from `Settings.darkMode`.
2. **Connecting:** User inputs IP in `ipView`. `ipObserver` sanitizes input (e.g. replaces Chinese period `。` with `.`, expands short IPs starting with `192.168.1.x`) and launches `Client.start(ip, 19621)` on an IO thread.
3. **Channel Activation:** Netty connects. `MessageHandler.channelActive()` fires `RxBus.post(ChannelActive(true))`.
4. **Heartbeat:** `PureWriter` runs a 1-second interval timer. When connected, `PingPongDelegate.ping(channel)` writes a `Ping` JSON message; if no `pong()` arrives within 3 seconds, the socket disconnects and prompts the user.
5. **Inbound Sync:** Phone sends `ArticleMessage` -> Netty decodes -> `MessageHandler` posts to `RxBus` -> `PureWriter` receives on UI thread, sets `isSyncing = true` to suppress feedback loops, updates `contentView` text and selection, updates stage title, and resets `isSyncing = false`.
6. **Outbound Sync:** User edits `contentView` -> listener triggers -> if not `isSyncing`, calls `channel.sendArticle()` -> sends updated title, content, and cursor selection to phone.

---

## 5. Hidden Couplings (Non-Graph References)

1. **FXML Controller Class Name:**
   - `src/main/resources/com/drakeet/purewriter/editor.fxml` (line 25): `fx:controller="com.drakeet.purewriter.PureWriter"`.
   - Changing the package or class name of `PureWriter` without updating `editor.fxml` causes runtime crash on startup (`ClassNotFoundException`).
2. **Relative Classpath Resource Lookups:**
   - `Main.kt` loads resources via `javaClass.getResource("editor.fxml")`, `getResource("editor.css")`, `getResource("editor-night.css")`.
   - `PureWriter.kt` loads `javaClass.getResourceAsStream("SourceHanSansCN-Light.ttf")` and `javaClass.getResourceAsStream("moon.png")`.
   - Because `javaClass.getResource("...")` uses relative paths, the resources must remain in the classpath directory matching the calling class package (`com/drakeet/purewriter/`) or use absolute leading slashes (`/com/drakeet/purewriter/...`).
3. **Persistent User Preferences (Java Preferences API):**
   - `Settings.kt` uses `Preferences.userNodeForPackage(Settings::class.java)`.
   - The OS stores preferences keyed by package name (e.g. `/com/drakeet/purewriter`). Moving `Settings` to another package resets all saved preferences (saved IP and dark mode toggle) for existing users.
4. **Build Manifest and JFX Launcher:**
   - `build.gradle` (line 44): `attributes 'Main-Class': 'com.drakeet.purewriter.Main'`.
   - `build.gradle` (line 55): `jfx { mainClass = 'com.drakeet.purewriter.Main' }`.
   - Moving `Main` requires changing both references.
5. **Packaging Icons Convention:**
   - `src/main/deploy/package/{linux,macosx,windows}` is a rigid path convention enforced by `javafx-gradle-plugin` (Oracle javapackager). Moving this breaks native installer generation (`jfxNative`).
6. **Task `generateJar` Output Coupling:**
   - `build.gradle` (lines 88–98) copies `build/libs/PureWriter-${VERSION}.jar` into root directory `jar/` and cleans `jar/`.

---

## 6. Dead and Redundant Code

1. **`proto/lite/` and `proto/out/` (1,858 lines):**
   Unused generated Java files (`PureWriterProtocol.java`) committed to git. Gradle compiles only `src/main/java/com/drakeet/purewriter/protocol/PureWriterProtocol.java`.
2. **`Events.kt` `Pong` Class:**
   `class Pong` is dead code; the ping-pong mechanism in `MessageHandler.kt` calls `PingPongDelegate.pong()` directly.
3. **`Log.kt`:**
   Logging utility with hardcoded `private const val DEBUG = false` that produces no output.
4. **`build.gradle` Unused Dependencies:**
   - `testCompile 'junit:junit:4.12'` while zero test files exist in the project.
   - Commented-out dependency `// compile "com.squareup.okio:okio:2.3.0"`.

---

## 7. Baseline Build & Verification Status

- **Environment Baseline:**
  - Build script requires JDK 1.8 with bundled JavaFX and Gradle 5.6.4.
  - The current host has JetBrains JBR 25 and an incomplete mise install (`temurin-17`), with no local cache of Gradle 5.6.4.
  - Executing `./gradlew --version` fails because the Gradle 5.6.4 wrapper distribution must be downloaded from `services.gradle.org`, which is blocked in sandbox mode.
  - CI workflow (`.github/workflows/gradle.yml`) confirms the target runtime: Windows runner with `actions/setup-java@v1`, `java-version: 1.8`, `java-package: jdk+fx`.
- **Test Baseline:** 0 tests (no test source directory exists).
- **Verification Strategy for Restructuring:**
  1. **Classpath & Resource Validation:** Verify that every resource (`editor.fxml`, CSS, TTF, PNG) continues to resolve accurately with the controller and main class.
  2. **FXML Controller Binding Integrity:** Verify `fx:controller` attribute strictly matches the controller's fully qualified name.
  3. **Build Script & Packaging Alignment:** Ensure `Main-Class`, `jfx` plugin settings, and `src/main/deploy/package/` remain valid.
