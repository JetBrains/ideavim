/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("ClassName")

package com.maddyhome.idea.vim.config.migration

import com.intellij.openapi.application.PathManager
import org.jetbrains.annotations.NonNls

interface VersionDetector {
  fun extractVersion(): Int?
}

object `Detect versions 3, 4, 5, 6` : VersionDetector {

  @NonNls val pattern = "state version=\"(.)\"".toRegex()

  override fun extractVersion(): Int? {
    val configFile = PathManager.getOptionsFile("vim_settings")
    if (!configFile.exists()) return null

    var versionThief: Int? = null
    configFile.forEachLine {
      val foundVersion = getVersionFromLine(it)
      if (foundVersion != null) {
        versionThief = foundVersion
        return@forEachLine
      }
    }
    return versionThief
  }

  fun getVersionFromLine(line: String): Int? {
    val res = pattern.find(line)
    if (res != null && res.groupValues.size == 2) {
      return res.groupValues[1].toIntOrNull()
    }
    return null
  }
}
