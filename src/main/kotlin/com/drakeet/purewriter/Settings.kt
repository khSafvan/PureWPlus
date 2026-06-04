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

package com.drakeet.purewriter

import com.drakeet.purewriter.storage.ConfigManager

/**
 * Modernized Settings delegating to ConfigManager, maintaining backwards compatibility.
 */
object Settings {

  private val configManager get() = ConfigManager.default

  var darkMode: Boolean
    get() = configManager.config.darkMode
    set(value) {
      configManager.update { darkMode = value }
    }

  var ip: String
    get() = configManager.config.ip
    set(value) {
      configManager.update { ip = value }
    }

  var ollamaHost: String
    get() = configManager.config.ollamaHost
    set(value) {
      configManager.update { ollamaHost = value }
    }

  var ollamaModel: String
    get() = configManager.config.ollamaModel
    set(value) {
      configManager.update { ollamaModel = value }
    }
}
