/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package com.maddyhome.idea.vim.config.migration

import com.intellij.openapi.application.PathManager
import org.jetbrains.annotations.NonNls

internal interface VersionDetector {
  fun extractVersion(): Int?
}

internal object `Detect versions 3, 4, 5, 6` : VersionDetector {

  @NonNls
  val pattern = "state version=\"(.)\"".toRegex()

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
