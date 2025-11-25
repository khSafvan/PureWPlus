# PureWPlus (Desktop Client for Pure Writer)

**PureWPlus** is a modernized, independent open-source desktop companion client for the **Pure Writer (纯纯写作)** mobile application. It enables fast, local real-time bidirectional WiFi synchronization between your Android device and computer without relying on external cloud servers.

> [!NOTE]
> **Fork & Disclaimer:** This project is an independent fork of the earlier 1.x desktop client from [PureWriter/desktop](https://github.com/PureWriter/desktop), originally created by Drakeet Xu. This project is **not affiliated with, endorsed by, or sponsored by** Drakeet Xu. "Pure Writer" and "纯纯写作" are trademarks of their respective owners, used here under nominative fair use to describe protocol compatibility. See [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) for full details.

---

## Features

- **Direct WiFi Synchronization:** Zero-cloud, encrypted local TCP socket pairing on port `19621`.
- **Bidirectional Real-Time Editing:** Instant text, cursor position, and title synchronization between phone and desktop.
- **Dark Mode Support:** Quick toggle between light and night themes.
- **Modernized & Hardened Dependencies:** Security-patched Netty 4.1.x, Google Protocol Buffers 3.25.x, and Gson 2.10.x.
- **Automated Protocol Contract Tests:** Strict test suite guaranteeing wire compatibility with the Android Pure Writer app.

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

### Run Contract Tests
```bash
./gradlew test
```

### Build Distribution JAR
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
- `sketch/`: Artwork master images and macOS icns files.
- `src/main/deploy/package/`: Platform-specific native installer icons (Windows, macOS, Linux).
- `src/main/java/com/drakeet/purewriter/protocol/`: Generated Java Protocol Buffer code.
- `src/main/kotlin/com/drakeet/purewriter/`:
  - `net/`: Netty socket client, protocol framing, message handlers, and heartbeat logic.
  - `model/`: Data transfer messages (`ArticleMessage`, `WordCountMessage`, `Events`).
  - Application entry (`Main.kt`), FXML controller (`PureWriter.kt`), settings, and UI themes.
- `src/main/resources/com/drakeet/purewriter/`: JavaFX FXML markup, styles, fonts, and icons.
- `src/test/kotlin/com/drakeet/purewriter/`: Protocol contract tests (`ProtocolContractTest.kt`).

---

## License

This software is distributed under the terms of the **GNU General Public License v3.0**.  
See [LICENSE](file:///home/zack/Workshop/PureWPlus/LICENSE) and [FORK.md](file:///home/zack/Workshop/PureWPlus/FORK.md) for full terms and copyright notices.
