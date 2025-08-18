# Project Modernization Plan: Pure Writer Desktop

This modernization plan adheres strictly to the **Ponytail mindset**: conservative changes only, smallest version jumps that fix real vulnerabilities and build failures, zero speculative rewrites, and absolute preservation of the frozen mobile TCP API contract.

---

## 1. Tiers of Work

### Tier 1: Must Do (Security, EOL, and Build Reliability)
*Only things that are actively broken, insecure, or obsolete.*

1. **Contract Safety Step (Prerequisite):**
   - Write characterization/contract tests (`src/test/kotlin/.../ProtocolContractTest.kt`) to lock in the exact byte framing, Protobuf envelope schema, and JSON payload serialization before touching any dependencies.
2. **Security Vulnerability Upgrades (Patch/Minor bumps only):**
   - `io.netty:netty-all`: `4.1.51.Final` &rarr; `4.1.118.Final` (Fixes 6+ CVEs including request smuggling, buffer vulnerabilities, and DoS; zero breaking API changes).
   - `com.google.protobuf:protobuf-java`: `3.11.4` &rarr; `3.25.5` (Fixes CVE-2024-7254 recursion DoS and memory CVEs; remains in Protobuf 3.x line with 100% binary & wire compatibility).
   - `com.google.code.gson:gson`: `2.8.5` &rarr; `2.10.1` (Fixes CVE-2022-25647 DoS; drop-in replacement).
   - `junit:junit`: `4.12` &rarr; `4.13.2` (Fixes CVE-2020-15250 temporary folder vulnerability).
3. **Remove Dead JCenter Repository:**
   - Remove `https://maven.aliyun.com/repository/jcenter` from `build.gradle` (retired in May 2021).
4. **CI Workflow Action Modernization:**
   - Update `.github/workflows/gradle.yml` from deprecated `actions/checkout@v1` and `actions/setup-java@v1` to `@v4`.

---

### Tier 2: Should Do (Low-Risk, High-Value Cleanups)
*Pruning bloat and unmaintained dead code without altering behavior.*

1. **Prune Dead Code:**
   - Delete unused `class Pong` in `model/Events.kt`.
   - Remove commented-out `// compile "com.squareup.okio:okio:2.3.0"` from `build.gradle`.
2. **Replace `opencc4j` with Static String Map / Standard ResourceBundle:**
   - `com.github.houbb:opencc4j:1.6.0` pulls in multiple heavy dictionary dependencies purely to translate 4 static Chinese UI labels.
   - Replacing this with standard literal lookup eliminates ~10MB of dependency bloat with zero network/runtime risk.
3. **Automate Contract Testing in CI:**
   - Add `./gradlew test` to GitHub Actions workflow to ensure the mobile contract is guarded on every push.

---

### Tier 3: Won't Do (Rejected as Over-Engineering)
*Features and refactors that don't earn their risk.*

1. **REJECTED: Rewriting GUI to Compose Desktop or Electron.**
   - *Rationale:* JavaFX 8 works reliably and consumes minimal resources. A GUI framework rewrite violates Ponytail and introduces months of UI regressions for no user gain.
2. **REJECTED: Upgrading to Protobuf 4.x / 5.x.**
   - *Rationale:* Protobuf 4.x/5.x introduces breaking descriptor API changes and requires re-generating all Java code. Staying on `3.25.5` provides all security fixes while remaining 100% compatible with existing generated classes.
3. **REJECTED: Migrating RxJava 2 to Kotlin Coroutines & Flow.**
   - *Rationale:* While RxJava 2 is EOL, `RxBus.kt` is only 50 lines of internal event dispatching and does not face the network. Replacing it with Coroutines touches all UI event handlers without improving stability.
4. **REJECTED: Splitting into Multi-Module Gradle Project.**
   - *Rationale:* With fewer than 20 source files, a multi-module architecture is needless complexity.
5. **REJECTED: Altering TCP Port 19621 or Wire Framing.**
   - *Rationale:* Hard constraint: the mobile app's API contract is frozen.

---

## 2. Upgrades Detail Table

| Package / Artifact | Current Version | Target Version | Why Necessary | Breaking Changes / Notes | How to Verify | Add-on Used |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`io.netty:netty-all`** | `4.1.51.Final` | `4.1.118.Final` | Fixes CVE-2021-21290, CVE-2021-37136, CVE-2021-37137, CVE-2021-43797, CVE-2024-29025. | None. Minor patch within 4.1.x line. All channel handlers, socket bootstrap, and pipeline APIs are 100% backward compatible. | Contract tests pass; client boots and connects to TCP socket. | Manual / Web search |
| **`com.google.protobuf:protobuf-java`** | `3.11.4` | `3.25.5` | Fixes CVE-2024-7254 (StackOverflow DoS) and CPU consumption CVEs. | None. 3.25.5 is the final hardened LTS of the Protobuf 3.x line. Fully compatible with generated `PureWriterProtocol.java`. | Serialization contract tests confirm byte-for-byte envelope compatibility. | Manual / Web search |
| **`com.google.code.gson:gson`** | `2.8.5` | `2.10.1` | Fixes CVE-2022-25647 (DoS during deserialization). | None. 100% backward compatible for POJO serialization of `ArticleMessage` and `WordCountMessage`. | Contract tests verify JSON string serialization. | Manual / Web search |
| **`junit:junit`** | `4.12` | `4.13.2` | Fixes CVE-2020-15250 (TemporaryFolder security flaw). | None. Drop-in replacement for standard JUnit 4 annotations. | Contract test suite compiles and passes under `./gradlew test`. | Manual |
| **`actions/setup-java`** | `v1` | `v4` | GitHub Actions `v1` is deprecated and fails on modern Node runtimes. | Syntax update in YAML (`distribution: 'temurin'`). | GitHub Actions workflow validation. | Manual |
| **`actions/checkout`** | `v1` | `v4` | Deprecated action version. | Standard update. | GitHub Actions workflow validation. | Manual |

---

## 3. Contract Safety Step: Characterization Tests

Before making any dependency bumps or code deletions, we will establish a baseline contract test suite:
- **File:** `src/test/kotlin/com/drakeet/purewriter/ProtocolContractTest.kt`
- **What it verifies:**
  1. **Envelope Contract:** `PureWriterProtocol.Message` serialization with `id`, `version = 2`, `contentType = "JSON"`, `messageType`, `content`, and `time`.
  2. **ArticleMessage Contract:** Verifies JSON payload parses exact fields: `title`, `content`, `selectionStart`, `selectionEnd`.
  3. **WordCountMessage Contract:** Verifies JSON payload parses exact field: `text`.
  4. **Ping/Pong Contract:** Verifies Ping packet payload shape and timeout triggers.
  5. **Framing Contract:** Verifies Netty `ProtobufVarint32LengthFieldPrepender` and `ProtobufVarint32FrameDecoder` round-trip accurately without truncation.

---

## 4. Execution Plan in Small Batches

```
[Branch: modernize]
       │
       ▼
Batch 1: Establish Contract Tests
(Add ProtocolContractTest.kt; verify baseline passes)
       │
       ▼
Batch 2: Dependency Security Patches (Tier 1)
(Bump Netty, Protobuf-Java, Gson, JUnit; remove dead JCenter repository)
       │
       ▼
Batch 3: Dead Code & Helper Cleanups (Tier 2)
(Prune Pong class, dead build.gradle lines; replace opencc4j with direct string map)
       │
       ▼
Batch 4: CI Modernization & Verification
(Update GitHub Actions to v4; run graphify update .; finalize docs)
```

### Batch 1: Establish Contract Tests
- Add `src/test/kotlin/com/drakeet/purewriter/ProtocolContractTest.kt`.
- Cover serialization, deserialization, and message framing.
- **Verification:** Run tests and confirm baseline pass.
- **Commit:** `test: add frozen mobile protocol contract tests`

### Batch 2: Dependency Security Patches
- Bump `netty-all` &rarr; `4.1.118.Final`.
- Bump `protobuf-java` &rarr; `3.25.5`.
- Bump `gson` &rarr; `2.10.1`.
- Bump `junit` &rarr; `4.13.2`.
- Remove dead JCenter Maven repository.
- **Verification:** Run contract tests; confirm zero wire regressions.
- **Commit:** `fix(deps): update netty, protobuf, and gson to secure patch versions`

### Batch 3: Prune Dead Code & Heavy Dependencies
- Remove unused `class Pong` in `model/Events.kt`.
- Replace `opencc4j` with static string map in `Globals.kt` and remove dependency from `build.gradle`.
- Remove commented-out dependencies.
- **Verification:** Run contract tests and UI string check.
- **Commit:** `refactor: prune dead Pong event and replace opencc4j with static map`

### Batch 4: CI & Final Verification
- Update `.github/workflows/gradle.yml` to `actions/checkout@v4` and `actions/setup-java@v4`.
- Add test step to CI.
- Run `graphify update .` to ensure graph is clean.
- Update `README.md`.
- **Commit:** `ci: modernize GitHub Actions workflow to v4`

---

## 5. Tooling Allocation Per Task

| Task | Tool / Add-on |
| :--- | :--- |
| Contract Test Creation | Direct Kotlin / JUnit 4 |
| Dependency Vulnerability Verification | Web search (`search_web`) against CVE databases |
| Cleanliness & YAGNI Review | `ponytail-review` / `ponytail-audit` |
| Graph Integrity Check | `graphify update .` |
| Git Commits & Branching | Direct `run_command` (git) |

---

## 6. Risks, Unknowns, and Questions for You

### Risks:
- **Build Environment JDK Compatibility:** The project build script is designed around Java 8 (`sourceCompatibility = 1.8`, `javafx-gradle-plugin:8.8.2`). Upgrading dependencies within their supported 1.8-compatible minor lines (`netty 4.1.x`, `protobuf 3.25.x`, `gson 2.10.x`) ensures Java 8 compatibility is preserved.

### Questions for You:
1. **Target Java Runtime:** Do you intend to keep running/building this on Java 8 (using Zulu 8 or Liberica 8 with bundled JavaFX), or would you prefer a future phase to migrate to Java 17 LTS + OpenJFX? *(Recommended: keep Java 8 for now to avoid the extensive build plugin migration that Java 17 requires).*
2. **`opencc4j` Removal:** Are you happy to replace `opencc4j` with a lightweight static translation dictionary for the 4 static Chinese UI labels, dropping ~10MB of transitive bloat?
