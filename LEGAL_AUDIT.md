# Legal and Licensing Audit: Pure Writer Desktop (PureWPlus Fork)

**Audit Date:** November 24, 2026  
**Upstream Project:** [PureWriter/desktop](https://github.com/PureWriter/desktop)  
**Upstream Author:** Drakeet Xu (drakeet@drakeet.com)  
**Primary License:** GNU General Public License v3.0 (GPL-3.0)  

---

## 1. License Files & Copyright Headers Audit

### A. Root License
- The root `LICENSE` file contains the complete, unmodified text of the **GNU General Public License Version 3, 29 June 2007** (35,146 bytes).
- **Status:** **Present and verified.** Must remain in the root directory unmodified.

### B. Source Code Copyright Headers
The majority of Kotlin source files and Java files carry the original GPL-3.0 preamble:
```
/*
 * Copyright (C) 2019 Drakeet Xu <drakeet@drakeet.com>
 *
 * This file is part of Pure Writer Desktop
 *
 * Pure Writer Desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ...
 */
```
*Note: A copy-paste relic in the upstream notice mentions `rebase-server` in the warranty and distribution disclaimer lines.*

### C. File-by-File Header Audit
| File | Copyright Header Status | Author Noted |
| :--- | :--- | :--- |
| `src/main/java/.../PureWriterProtocol.java` | Standard Protobuf generated comment | Generated |
| `src/main/kotlin/.../Main.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../PureWriter.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../DayNights.kt` | Missing full header (has `@author Drakeet Xu`) | Drakeet Xu |
| `src/main/kotlin/.../Globals.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../Jsons.kt` | **Present** (GPL-3.0) | drakeet |
| `src/main/kotlin/.../Log.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../RxBus.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../Settings.kt` | Missing full header (has `@author Drakeet Xu`) | Drakeet Xu |
| `src/main/kotlin/.../TextAreas.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../model/ArticleMessage.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../model/Events.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../model/WordCountMessage.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../net/Client.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../net/MessageHandler.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/kotlin/.../net/PingPongDelegate.kt` | Missing full header (has `@author Drakeet Xu`) | Drakeet Xu |
| `src/main/kotlin/.../net/SDK.kt` | **Present** (GPL-3.0) | Drakeet Xu |
| `src/main/resources/.../editor.fxml` | **Present** (GPL-3.0 XML comment) | Drakeet Xu |
| `scripts/gen_iconset.py` | Attribution comment to VikingOSX & retifrav.github.io | VikingOSX |
| `build.gradle`, `settings.gradle` | No header | — |
| `proto/Protocols.proto` | No header | — |

**Confirmation:** **No license files or copyright headers were removed or deleted.**

---

## 2. Dependency Licenses & GPL-3.0 Compatibility

Every direct dependency declared in `build.gradle` has been audited for licensing and compatibility with GPL-3.0:

| Dependency | Version | Declared License | GPL-3.0 Compatibility | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | `1.4.0` | **Apache-2.0** | **Compatible** | Permissive; standard runtime library. |
| `io.netty:netty-all` | `4.1.51.Final` | **Apache-2.0** | **Compatible** | Permissive. |
| `com.google.protobuf:protobuf-java` | `3.11.4` | **BSD-3-Clause** | **Compatible** | Permissive; explicitly compatible with GPL-3.0. |
| `io.reactivex.rxjava2:rxjava` | `2.2.19` | **Apache-2.0** | **Compatible** | Permissive. |
| `com.jakewharton.rxrelay2:rxrelay` | `2.1.1` | **Apache-2.0** | **Compatible** | Permissive. |
| `com.google.code.gson:gson` | `2.8.5` | **Apache-2.0** | **Compatible** | Permissive. |
| `io.reactivex:rxjavafx` | `2.0.2` | **Apache-2.0** | **Compatible** | Permissive. |
| `com.github.houbb:opencc4j` | `1.6.0` | **Apache-2.0** | **Compatible** | Permissive NLP dictionary library. |
| `junit:junit` | `4.12` | **EPL-1.0** | **N/A (Test only)** | Eclipse Public License 1.0 is generally incompatible with GPL if bundled in binary distributions, but JUnit is declared as `testCompile` only and is never distributed in the application jar/installer. |
| `javafx-gradle-plugin` | `8.8.2` | **Apache-2.0** | **N/A (Build only)** | Build tool plugin; not bundled in distribution. |

**Conclusion:** All distributed runtime dependencies are licensed under Apache-2.0 or BSD-3-Clause. There are **zero GPL-3.0 license incompatibilities**.

---

## 3. Bundled Assets Audit (Icons, Fonts, Graphics)

| Asset Path | Description | Identified Origin / License | Risk / Action |
| :--- | :--- | :--- | :--- |
| `src/main/resources/.../SourceHanSansCN-Light.ttf` | Chinese UI Font (思源黑体) | **SIL Open Font License 1.1 (OFL-1.1)** (Adobe / Google) | **Low Risk.** OFL 1.1 permits bundling with software. Must not be sold independently or modified without renaming. |
| `src/main/resources/.../moon.png` | Dark mode moon icon (30x30) | **Unknown origin.** Likely third-party icon. | **Medium Risk.** Should be replaced with a verified permissive icon (e.g. Lucide/Feather moon icon under MIT). |
| `sketch/Pure Writer.png` | Master brand graphic | Proprietary artwork / trademark of Drakeet Xu | **High Risk.** Must be replaced for fork distribution. |
| `sketch/Pure Writer.icns` | Master macOS icon | Proprietary artwork / trademark of Drakeet Xu | **High Risk.** Must be replaced. |
| `src/main/deploy/package/linux/PureWriter.png` | Linux desktop icon | Proprietary trademark of Drakeet Xu | **High Risk.** Must be replaced. |
| `src/main/deploy/package/macosx/Pure Writer.icns` | macOS application icon | Proprietary trademark of Drakeet Xu | **High Risk.** Must be replaced. |
| `src/main/deploy/package/windows/Pure Writer.ico` | Windows executable icon | Proprietary trademark of Drakeet Xu | **High Risk.** Must be replaced. |
| `src/main/deploy/package/windows/Pure Writer-setup-icon.bmp` | Windows installer banner | Proprietary trademark of Drakeet Xu | **High Risk.** Must be replaced. |
| `scripts/gen_iconset.py` | macOS ICNS generator | Python script by VikingOSX (Apple Community, "No Warranty") | **Low Risk.** Permissible build utility. |

---

## 4. Brand and Trademark Occurrences

Under trademark law, a copyright license (such as GPL-3.0) grants rights to the software code, but does **not** grant rights to use the original project's trademarks, logos, or commercial identities in a way that implies endorsement or official affiliation.

### Places Where the Brand Appears:
1. **Application & Product Name:**
   - `"Pure Writer"`
   - `"纯纯写作"` (Chinese registered trademark of Drakeet Xu)
   - `"純純寫作"` / `"純純執筆"` (Traditional Chinese trademarks)
   - `"Pure Writer Desktop"`
2. **Build & Package Identifiers:**
   - `build.gradle`: `group 'com.drakeet.purewriter'`
   - `build.gradle`: `jfx { appName = "Pure Writer" }`
   - `build.gradle`: `vendor = 'drakeet.com'`
   - Output JAR: `PureWriter-${VERSION}.jar`
   - Java Preferences node: `Preferences.userNodeForPackage(Settings::class.java)` (`/com/drakeet/purewriter`)
3. **Window Titles & In-App UI Strings:**
   - `Main.kt`: Window title defaults to `stageTitle` (`"纯纯写作"` or `"Pure Writer"`).
   - `PureWriter.kt`: `mainStage.title = "Pure Writer"`.
   - `PureWriter.kt`: `showEmpty()` displays:
     - `"请在纯纯写作手机版中打开一篇文章"` / `"Please open an article on Pure Writer mobile"`
     - `"纯纯写作桌面版 v$version"` / `"Pure Writer Desktop v$version"`
   - `PureWriter.kt`: `showIpViews()` displays:
     - `"请打开纯纯写作 Android 版并点击其顶部的云图标获得 IP 地址填于下方"`
     - `"Please open Pure Writer for Android..."`
     - `"纯纯写作桌面版 v$version  ⇋  纯纯写作 v$appVersion+"`
4. **README & Store Badges:**
   - Links to official commercial site: `https://writer.drakeet.com/`
   - Links to Google Play and Apple App Store listings for Pure Writer.
   - Hosted screenshot showing the Pure Writer trademarked brand.

---

## 5. Hardcoded Third-Party & Author URLs / Services

| Location | URL / Reference | Owner / Purpose | Action |
| :--- | :--- | :--- | :--- |
| `build.gradle` (line 56) | `vendor = 'drakeet.com'` | Drakeet Xu | Replace with fork project identifier. |
| `build.gradle` (lines 6, 15) | `https://maven.aliyun.com/repository/jcenter` | Alibaba / JCenter (Defunct) | Remove defunct JCenter repository. |
| `README.md` (lines 3, 7, 9, 10, 11, 12) | `https://writer.drakeet.com/*` | Official Pure Writer domain | Clarify distinction between upstream commercial service and this fork. |
| `README.md` (lines 11, 12) | App Store & Google Play links | Drakeet Xu's store listings | Retain as reference for companion mobile app, but disclaim affiliation. |
| `README.md` (line 21) | `https://user-images.githubusercontent.com/...` | Hosted screenshot on Drakeet's account | Replace with screenshot of fork. |
| `scripts/gen_iconset.py` (line 4) | `https://retifrav.github.io/blog/...` | Blog credit link | Keep attribution. |

---

## 6. Proposals (For Review & Approval)

### A. Rebranding Plan
To comply with trademark obligations while maintaining protocol compatibility:
1. **Name:** Adopt **PureWPlus** (or **PureW+ Desktop**) as the application name.
2. **Compatibility Statement:** Use nominative fair use to describe compatibility without infringing trademarks:
   > *"PureWPlus Desktop is an independent open-source desktop client compatible with the Pure Writer (纯纯写作) Android application."*
3. **Build Settings:**
   - Change `appName = "PureWPlus"` in `build.gradle`.
   - Change `vendor = "PureWPlus Project"` (or your name/organization) in `build.gradle`.
   - Change JAR output name to `PureWPlus-${VERSION}.jar`.
4. **Window Title & UI Copy:**
   - Set stage title to `"PureWPlus"`.
   - Update in-app prompts to reference *"Pure Writer mobile"* strictly as the compatible target app, avoiding claims that this software is "Pure Writer Desktop".

### B. Modification-Notice Approach (GPL-3.0 § 5(a))
GPL-3.0 Section 5(a) requires:
> *"The work must carry prominent notices stating that you modified it, and giving a relevant date."*

**Recommended Approach:**
1. Leave all existing `Copyright (C) 2019 Drakeet Xu <drakeet@drakeet.com>` notices completely untouched.
2. Add a prominent notice to modified files:
   ```kotlin
   /*
    * Modifications Copyright (C) 2026 PureWPlus Project Contributors
    * Modified under the terms of the GNU General Public License v3.0.
    */
   ```
3. Maintain the root `LICENSE` file as the authoritative GPL-3.0 license.

### C. Draft `FORK.md` / `NOTICE`
Create a top-level `FORK.md` file with the following text:

```markdown
# Fork & Trademark Notice

This project, **PureWPlus**, is an independent fork of **Pure Writer Desktop** (`PureWriter/desktop`), originally created by Drakeet Xu (<drakeet@drakeet.com>) and licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

### Trademark Notice
"Pure Writer", "纯纯写作", and associated logos are trademarks of Drakeet Xu. This project is an independent open-source fork and is **not affiliated with, endorsed by, or sponsored by** Drakeet Xu or the official Pure Writer project.

The names "Pure Writer" and "纯纯写作" are used solely for the purpose of identifying protocol and data compatibility with the Pure Writer mobile application under nominative fair use.

### License & Modifications
In accordance with GPL-3.0 Section 5(a), this fork includes modifications made in 2026 by the PureWPlus Project contributors, including:
- Reorganization of package architecture into modular network and model layers.
- Security updates for Netty, Protocol Buffers, and Gson dependencies.
- Automated protocol contract tests.
- Rebranding and asset replacements to avoid trademark confusion.

See `LICENSE` for the full GPL-3.0 terms.
```

### D. List of Assets to Replace

| File | Reason | Replacement Source |
| :--- | :--- | :--- |
| `src/main/resources/.../moon.png` | Unknown origin/license | Standard MIT-licensed Lucide / Feather icon (`moon.png`). |
| `src/main/deploy/package/linux/PureWriter.png` | Trademarked logo of Pure Writer | New original PureWPlus icon (512x512 PNG). |
| `src/main/deploy/package/macosx/Pure Writer.icns` | Trademarked logo of Pure Writer | New original PureWPlus icon (.icns). |
| `src/main/deploy/package/windows/Pure Writer.ico` | Trademarked logo of Pure Writer | New original PureWPlus icon (.ico). |
| `src/main/deploy/package/windows/Pure Writer-setup-icon.bmp` | Trademarked logo of Pure Writer | New original PureWPlus setup graphic (.bmp). |
| `sketch/Pure Writer.png` & `.icns` | Trademarked original graphics | Archive or replace with new source artwork. |
