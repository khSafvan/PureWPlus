# Project Restructure Plan: Pure Writer Desktop

This restructure plan applies the **Ponytail mindset**: the minimum restructure that fixes real problems, with zero speculative abstraction layers, no empty folders for the future, and minimal churn on upstream diffs.

---

## 1. Target Folder Tree and Reasoning

Tied directly to the Graphify community clusters and functional separation:
- **Cluster 1 & 2 (Network & Protocol):** Socket lifecycle (`Client`), protocol framing (`MessageHandler`), heartbeats (`PingPongDelegate`), and channel extensions (`SDK`) moved together to `net/`.
- **Cluster 3 (Events & Message Models):** Data transfer classes (`ArticleMessage`, `WordCountMessage`, `Events`) grouped into `model/`.
- **Root UI & Application (`com.drakeet.purewriter`):** `Main`, `PureWriter`, `Settings`, `DayNights`, `Globals`, `TextAreas`, `Jsons`, and `RxBus` remain at the package root to preserve user preferences nodes, JavaFX FXML controller lookups, and build entry points.
- **Top-Level Cleanliness:** Stale generated Protobuf directories (`proto/lite/`, `proto/out/`) removed; utility script `gen_iconset.py` separated from design graphics into `scripts/`.

### Target Tree

```
PureWPlus/
├── .github/workflows/
│   └── gradle.yml
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── proto/
│   └── Protocols.proto         # Clean protobuf definition (stale lite/ and out/ pruned)
├── scripts/
│   └── gen_iconset.py          # Standalone macOS iconset generator script
├── sketch/
│   ├── Pure Writer.icns        # High-res master icon
│   └── Pure Writer.png         # Source raster graphic
├── src/
│   └── main/
│       ├── deploy/package/     # UNCHANGED: required by javafx-gradle-plugin
│       │   ├── linux/PureWriter.png
│       │   ├── macosx/Pure Writer.icns
│       │   └── windows/
│       ├── java/com/drakeet/purewriter/protocol/
│       │   └── PureWriterProtocol.java
│       ├── kotlin/com/drakeet/purewriter/
│       │   ├── model/          # Message models & event signals
│       │   │   ├── ArticleMessage.kt
│       │   │   ├── Events.kt
│       │   │   └── WordCountMessage.kt
│       │   ├── net/            # Netty socket client & message dispatching
│       │   │   ├── Client.kt
│       │   │   ├── MessageHandler.kt
│       │   │   ├── PingPongDelegate.kt
│       │   │   └── SDK.kt
│       │   ├── DayNights.kt    # UI theme styling
│       │   ├── Globals.kt      # Global state & localization helpers
│       │   ├── Jsons.kt        # JSON serialization helpers
│       │   ├── Log.kt          # Debug logging
│       │   ├── Main.kt         # Application entry point
│       │   ├── PureWriter.kt   # JavaFX FXML Controller
│       │   ├── RxBus.kt        # Internal event bus
│       │   ├── Settings.kt     # Preferences persistence
│       │   └── TextAreas.kt    # TextArea UI extensions
│       └── resources/com/drakeet/purewriter/
│           ├── SourceHanSansCN-Light.ttf
│           ├── editor-night.css
│           ├── editor.css
│           ├── editor.fxml
│           └── moon.png
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
└── settings.gradle
```

---

## 2. Table of Every Move and Rename

| Old Path | New Path | Action | Why |
| :--- | :--- | :--- | :--- |
| `proto/lite/` (entire folder) | *(deleted)* | Delete | Dead code: 725 lines of duplicate Protobuf Lite classes unreferenced by build or sources. |
| `proto/out/` (entire folder) | *(deleted)* | Delete | Dead code: 1,133 lines of duplicate Protobuf classes unreferenced by build or sources. |
| `sketch/gen_iconset.py` | `scripts/gen_iconset.py` | `git mv` | Separates executable build utility scripts from graphic design assets. |
| `src/main/kotlin/com/drakeet/purewriter/ArticleMessage.kt` | `src/main/kotlin/com/drakeet/purewriter/model/ArticleMessage.kt` | `git mv` | Groups data transfer messages together; isolates wire payload model. |
| `src/main/kotlin/com/drakeet/purewriter/WordCountMessage.kt` | `src/main/kotlin/com/drakeet/purewriter/model/WordCountMessage.kt` | `git mv` | Groups data transfer messages together; isolates wire payload model. |
| `src/main/kotlin/com/drakeet/purewriter/Events.kt` | `src/main/kotlin/com/drakeet/purewriter/model/Events.kt` | `git mv` | Groups bus event signal types with payload models. |
| `src/main/kotlin/com/drakeet/purewriter/Client.kt` | `src/main/kotlin/com/drakeet/purewriter/net/Client.kt` | `git mv` | Isolates Netty bootstrap and socket connection management. |
| `src/main/kotlin/com/drakeet/purewriter/MessageHandler.kt` | `src/main/kotlin/com/drakeet/purewriter/net/MessageHandler.kt` | `git mv` | Isolates inbound Netty pipeline decoding and channel event handling. |
| `src/main/kotlin/com/drakeet/purewriter/PingPongDelegate.kt` | `src/main/kotlin/com/drakeet/purewriter/net/PingPongDelegate.kt` | `git mv` | Isolates socket heartbeat management and timeout disconnection. |
| `src/main/kotlin/com/drakeet/purewriter/SDK.kt` | `src/main/kotlin/com/drakeet/purewriter/net/SDK.kt` | `git mv` | Isolates outbound Channel message formatting and send extensions. |

---

## 3. Every File That Must Change as a Result

### Code Files Requiring Import or Package Updates:
1. **`src/main/kotlin/com/drakeet/purewriter/model/ArticleMessage.kt`**:
   - Change package to `package com.drakeet.purewriter.model`.
2. **`src/main/kotlin/com/drakeet/purewriter/model/WordCountMessage.kt`**:
   - Change package to `package com.drakeet.purewriter.model`.
3. **`src/main/kotlin/com/drakeet/purewriter/model/Events.kt`**:
   - Change package to `package com.drakeet.purewriter.model`.
4. **`src/main/kotlin/com/drakeet/purewriter/net/Client.kt`**:
   - Change package to `package com.drakeet.purewriter.net`.
   - Add imports for `com.drakeet.purewriter.Log`.
5. **`src/main/kotlin/com/drakeet/purewriter/net/MessageHandler.kt`**:
   - Change package to `package com.drakeet.purewriter.net`.
   - Add imports for `com.drakeet.purewriter.RxBus`, `com.drakeet.purewriter.Log`, `com.drakeet.purewriter.jsonTo`, and `com.drakeet.purewriter.model.*`.
6. **`src/main/kotlin/com/drakeet/purewriter/net/PingPongDelegate.kt`**:
   - Change package to `package com.drakeet.purewriter.net`.
   - Add imports for `com.drakeet.purewriter.RxBus`, `com.drakeet.purewriter.Log`, and `com.drakeet.purewriter.model.ChannelActive`.
7. **`src/main/kotlin/com/drakeet/purewriter/net/SDK.kt`**:
   - Change package to `package com.drakeet.purewriter.net`.
   - Add imports for `com.drakeet.purewriter.toJson` and `com.drakeet.purewriter.model.ArticleMessage`.
8. **`src/main/kotlin/com/drakeet/purewriter/PureWriter.kt`**:
   - Add imports for `com.drakeet.purewriter.net.*` and `com.drakeet.purewriter.model.*`.

### Configuration, Build, and Documentation Files:
- **`README.md`**: Update documentation if referencing `sketch/gen_iconset.py` or repository tree structure.
- **`build.gradle`**: No changes required. `Main-Class` (`com.drakeet.purewriter.Main`) remains untouched at the package root.
- **`editor.fxml`**: No changes required. Controller remains `com.drakeet.purewriter.PureWriter`.
- **CI / Workflows (`.github/workflows/gradle.yml`)**: No changes required.

---

## 4. Moves Considered and Rejected (Ponytail Rationale)

1. **Rejected: Moving `Main.kt` and `PureWriter.kt` into `ui/` subpackage.**
   - *Why rejected:* Moving `PureWriter.kt` breaks `fx:controller="com.drakeet.purewriter.PureWriter"` in `editor.fxml`. Moving `Main.kt` breaks `'Main-Class': 'com.drakeet.purewriter.Main'` in `build.gradle` and breaks relative resource loading (`getResource("editor.fxml")`). The churn introduces high runtime breakage risk for cosmetic folder nesting.
2. **Rejected: Moving `Settings.kt` into a `config/` or `data/` subpackage.**
   - *Why rejected:* `Settings.kt` calls `Preferences.userNodeForPackage(Settings::class.java)`. Moving it changes the OS preferences node path, erasing stored IP and dark mode preference for existing users.
3. **Rejected: Moving `RxBus.kt`, `Jsons.kt`, `TextAreas.kt`, `Log.kt` into `common/` or `util/`.**
   - *Why rejected:* Violates Ponytail. Creates a speculative "kitchen-sink" utility folder for 3 tiny files that only have 1 caller each.
4. **Rejected: Moving `src/main/deploy/package/` into `resources/deploy/`.**
   - *Why rejected:* Oracle's `javapackager` and `javafx-gradle-plugin` mandate the `src/main/deploy/package` convention for native bundle icons. Moving it breaks native packaging (`./gradlew jfxNative`).
5. **Rejected: Moving `PureWriterProtocol.java` from `src/main/java/` to `src/main/kotlin/` or renaming package.**
   - *Why rejected:* Generated code generated by `Protocols.proto` specifies `option java_package = "com.drakeet.purewriter.protocol"`. Keeping it in `src/main/java/com/drakeet/purewriter/protocol/` preserves standard Java/Protobuf directory conventions.

---

## 5. Risks and Unknowns

| Risk | Severity | Mitigation |
| :--- | :--- | :--- |
| **JavaFX FXML Controller breakage** | High | Keep `PureWriter.kt` at package `com.drakeet.purewriter` so `editor.fxml` controller binding requires zero changes. |
| **User Preferences Reset** | High | Keep `Settings.kt` in `com.drakeet.purewriter` to guarantee `Preferences.userNodeForPackage` remains identical. |
| **Resource resolution failure** | Medium | No resource paths or package namespaces of `Main` are altered. |
| **Gradle compilation failure** | Low | Package changes in `net/` and `model/` are validated with explicit cross-package Kotlin imports. |

---

## 6. Execution Steps in Small Batches

### Batch 1: Prune Stale Code & Extract Scripts
- Remove obsolete directories `proto/lite/` and `proto/out/`.
- Move `sketch/gen_iconset.py` -> `scripts/gen_iconset.py`.
- **Validation:** Git status check; verify `proto/Protocols.proto` remains intact.
- **Commit:** `refactor: prune stale proto build artifacts and move iconset script`

### Batch 2: Restructure Network Layer (`net/`)
- Create `src/main/kotlin/com/drakeet/purewriter/net/`.
- `git mv` `Client.kt`, `MessageHandler.kt`, `PingPongDelegate.kt`, `SDK.kt` into `net/`.
- Update package statements to `com.drakeet.purewriter.net` and add necessary imports.
- Update `PureWriter.kt` to import `com.drakeet.purewriter.net.*`.
- **Validation:** Code review of diffs and package references.
- **Commit:** `refactor: move network transport components to net package`

### Batch 3: Restructure Models & Events (`model/`)
- Create `src/main/kotlin/com/drakeet/purewriter/model/`.
- `git mv` `ArticleMessage.kt`, `WordCountMessage.kt`, `Events.kt` into `model/`.
- Update package statements to `com.drakeet.purewriter.model`.
- Update `PureWriter.kt`, `MessageHandler.kt`, `SDK.kt` imports.
- **Validation:** Code review of diffs and import graph.
- **Commit:** `refactor: move message models and event signals to model package`

### Batch 4: Graphify Graph Refresh & Docs
- Run `graphify update .` to update knowledge graph and ensure no unintended cross-cluster coupling was introduced.
- Update `README.md` if necessary.
- **Commit:** `docs: update knowledge graph and repository documentation`

---

## 7. Impact on Future Merges from Upstream

- **Minimizing Divergence:**
  - `Main.kt`, `PureWriter.kt`, `Settings.kt`, `build.gradle`, and `editor.fxml` remain at their original paths. Any upstream bug fixes or UI updates to the editor controller can merge cleanly with standard 3-way git merge.
  - Using `git mv` ensures git's rename-detection heuristic (`merge.renameLimit` / `-Xrename-threshold`) tracks files moved into `net/` and `model/` across upstream cherry-picks or merges.
- **Deleted Artifacts:**
  - Upstream rarely touches `proto/lite/` or `proto/out/`. If an upstream merge re-adds them, a simple `git rm -rf proto/lite proto/out` resolves it instantly.

---

## 8. Ponytail Plan Review

- `delete:` 1,858 lines of dead proto code.
- `yagni:` Avoided creating 4 tiny speculative subpackages (`ui`, `config`, `common`, `util`).
- `shrink:` 0 unnecessary abstraction layers introduced.
- Net impact: cleaner root, clear separation of Network vs Model vs UI, zero runtime regression risk.
