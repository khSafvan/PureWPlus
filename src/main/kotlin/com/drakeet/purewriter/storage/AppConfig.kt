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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.prefs.Preferences

data class AppConfig(
  var ip: String = "",
  var darkMode: Boolean = false,
  var autoBackup: Boolean = true,
  var backupRetentionDays: Int = 30,
  var lastDocumentId: String? = null
)

class ConfigManager(
  private val configFileProvider: () -> File = { AppPaths.configFile },
  private val legacyPreferences: Preferences? = null
) {
  private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
  private val lock = Any()

  @Volatile
  var config: AppConfig = loadOrCreate()
    private set

  fun loadOrCreate(): AppConfig {
    synchronized(lock) {
      val file = configFileProvider()
      if (file.exists() && file.length() > 0) {
        try {
          FileReader(file).use { reader ->
            val parsed = gson.fromJson(reader, AppConfig::class.java)
            if (parsed != null) {
              config = parsed
              return parsed
            }
          }
        } catch (e: Exception) {
          // If reading failed (e.g. invalid json), fallback to migration or defaults
        }
      }

      val migrated = migrateFromPreferencesIfNeeded()
      save(migrated)
      return migrated
    }
  }

  private fun migrateFromPreferencesIfNeeded(): AppConfig {
    val prefs = legacyPreferences ?: try {
      Preferences.userNodeForPackage(ConfigManager::class.java)
    } catch (e: Exception) {
      null
    }

    val base = AppConfig()
    if (prefs != null) {
      try {
        val legacyIp = prefs.get("ip", "")
        val legacyDark = prefs.getBoolean("darkMode", false)
        if (legacyIp.isNotEmpty() || legacyDark) {
          base.ip = legacyIp
          base.darkMode = legacyDark
        }
      } catch (e: Exception) {
        // Ignore preference reading errors
      }
    }
    return base
  }

  fun save(target: AppConfig = config) {
    synchronized(lock) {
      val file = configFileProvider()
      file.parentFile?.mkdirs()
      FileWriter(file).use { writer ->
        gson.toJson(target, writer)
      }
      config = target
    }
  }

  fun update(block: AppConfig.() -> Unit) {
    synchronized(lock) {
      config.block()
      save(config)
    }
  }

  companion object {
    val default: ConfigManager by lazy { ConfigManager() }
  }
}
