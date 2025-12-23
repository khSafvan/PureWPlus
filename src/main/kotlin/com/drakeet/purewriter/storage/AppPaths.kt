/*
 * Copyright (C) 2019 Drakeet Xu <drakeet@drakeet.com>
 * Modifications Copyright (C) 2026 PureWPlus Project Contributors
 *
 * This file is part of Pure Writer Desktop / PureWPlus
 *
 * Pure Writer Desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rebase-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rebase-server. If not, see <http://www.gnu.org/licenses/>.
 */

package com.drakeet.purewriter.storage

import java.io.File

data class AppPathConfig(
  val dataDir: File,
  val configDir: File,
  val cacheDir: File
) {
  val documentsDir: File get() = File(dataDir, "documents")
  val backupsDir: File get() = File(dataDir, "backups")
  val configFile: File get() = File(configDir, "config.json")

  fun ensureDirectoriesExist() {
    dataDir.mkdirs()
    configDir.mkdirs()
    cacheDir.mkdirs()
    documentsDir.mkdirs()
    backupsDir.mkdirs()
  }
}

object AppPaths {
  private const val APP_DIR_NAME = "PureWPlus"

  @Volatile
  var customConfig: AppPathConfig? = null

  fun current(): AppPathConfig {
    return customConfig ?: resolve(
      osName = System.getProperty("os.name", ""),
      env = System.getenv(),
      userHome = System.getProperty("user.home", ".")
    )
  }

  val dataDir: File get() = current().dataDir
  val configDir: File get() = current().configDir
  val cacheDir: File get() = current().cacheDir
  val documentsDir: File get() = current().documentsDir
  val backupsDir: File get() = current().backupsDir
  val configFile: File get() = current().configFile

  fun resolve(
    osName: String,
    env: Map<String, String>,
    userHome: String
  ): AppPathConfig {
    val lowerOs = osName.toLowerCase()
    return when {
      lowerOs.contains("win") -> {
        val appData = env["APPDATA"]?.takeIf { it.isNotBlank() }
          ?: File(userHome, "AppData\\Roaming").path
        val localAppData = env["LOCALAPPDATA"]?.takeIf { it.isNotBlank() }
          ?: File(userHome, "AppData\\Local").path

        val baseDir = File(appData, APP_DIR_NAME)
        val cache = File(File(localAppData, APP_DIR_NAME), "Cache")
        AppPathConfig(
          dataDir = baseDir,
          configDir = baseDir,
          cacheDir = cache
        )
      }
      lowerOs.contains("mac") -> {
        val appSupport = File(userHome, "Library/Application Support/$APP_DIR_NAME")
        val cache = File(userHome, "Library/Caches/$APP_DIR_NAME")
        AppPathConfig(
          dataDir = appSupport,
          configDir = appSupport,
          cacheDir = cache
        )
      }
      else -> {
        // Linux / Unix / BSD
        val xdgDataHome = env["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() }
        val dataDir = if (xdgDataHome != null) {
          File(xdgDataHome, APP_DIR_NAME)
        } else {
          File(userHome, ".local/share/$APP_DIR_NAME")
        }

        val xdgConfigHome = env["XDG_CONFIG_HOME"]?.takeIf { it.isNotBlank() }
        val configDir = if (xdgConfigHome != null) {
          File(xdgConfigHome, APP_DIR_NAME)
        } else {
          File(userHome, ".config/$APP_DIR_NAME")
        }

        val xdgCacheHome = env["XDG_CACHE_HOME"]?.takeIf { it.isNotBlank() }
        val cacheDir = if (xdgCacheHome != null) {
          File(xdgCacheHome, APP_DIR_NAME)
        } else {
          File(userHome, ".cache/$APP_DIR_NAME")
        }

        AppPathConfig(
          dataDir = dataDir,
          configDir = configDir,
          cacheDir = cacheDir
        )
      }
    }
  }
}
