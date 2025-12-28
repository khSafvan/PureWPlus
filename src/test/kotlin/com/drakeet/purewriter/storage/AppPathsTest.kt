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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppPathsTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testLinuxPathResolution() {
    val home = "/home/testuser"
    val env = mapOf(
      "XDG_DATA_HOME" to "/custom/data",
      "XDG_CONFIG_HOME" to "/custom/config",
      "XDG_CACHE_HOME" to "/custom/cache"
    )
    val paths = AppPaths.resolve(osName = "Linux", env = env, userHome = home)
    assertEquals(File("/custom/data/PureWPlus"), paths.dataDir)
    assertEquals(File("/custom/config/PureWPlus"), paths.configDir)
    assertEquals(File("/custom/cache/PureWPlus"), paths.cacheDir)
    assertEquals(File("/custom/data/PureWPlus/documents"), paths.documentsDir)
    assertEquals(File("/custom/data/PureWPlus/backups"), paths.backupsDir)
    assertEquals(File("/custom/config/PureWPlus/config.json"), paths.configFile)
  }

  @Test
  fun testLinuxFallbackPaths() {
    val home = "/home/testuser"
    val paths = AppPaths.resolve(osName = "Linux", env = emptyMap(), userHome = home)
    assertEquals(File("/home/testuser/.local/share/PureWPlus"), paths.dataDir)
    assertEquals(File("/home/testuser/.config/PureWPlus"), paths.configDir)
    assertEquals(File("/home/testuser/.cache/PureWPlus"), paths.cacheDir)
  }

  @Test
  fun testMacOSPathResolution() {
    val home = "/Users/testuser"
    val paths = AppPaths.resolve(osName = "Mac OS X", env = emptyMap(), userHome = home)
    assertEquals(File("/Users/testuser/Library/Application Support/PureWPlus"), paths.dataDir)
    assertEquals(File("/Users/testuser/Library/Application Support/PureWPlus"), paths.configDir)
    assertEquals(File("/Users/testuser/Library/Caches/PureWPlus"), paths.cacheDir)
  }

  @Test
  fun testWindowsPathResolution() {
    val home = "C:\\Users\\testuser"
    val env = mapOf(
      "APPDATA" to "C:\\Users\\testuser\\AppData\\Roaming",
      "LOCALAPPDATA" to "C:\\Users\\testuser\\AppData\\Local"
    )
    val paths = AppPaths.resolve(osName = "Windows 10", env = env, userHome = home)
    assertEquals(File(env["APPDATA"]!!, "PureWPlus"), paths.dataDir)
    assertEquals(File(env["APPDATA"]!!, "PureWPlus"), paths.configDir)
    assertEquals(File(File(env["LOCALAPPDATA"]!!, "PureWPlus"), "Cache"), paths.cacheDir)
  }

  @Test
  fun testConfigManagerPersistence() {
    val configFile = File(tempFolder.root, "test-config.json")
    val manager = ConfigManager(configFileProvider = { configFile })

    assertEquals("", manager.config.ip)
    assertFalse(manager.config.darkMode)

    manager.update {
      ip = "192.168.1.100"
      darkMode = true
    }

    assertTrue(configFile.exists())

    // Reload with a fresh manager instance
    val reloaded = ConfigManager(configFileProvider = { configFile })
    assertEquals("192.168.1.100", reloaded.config.ip)
    assertTrue(reloaded.config.darkMode)
    assertTrue(reloaded.config.autoBackup)
  }
}
