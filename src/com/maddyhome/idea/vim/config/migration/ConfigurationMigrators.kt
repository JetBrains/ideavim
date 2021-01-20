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

package com.maddyhome.idea.vim.config.migration

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element

interface ConfigMigrator {
  val fromVersion: Int
  val toVersion: Int
  fun versionUp()
}

@Suppress("ClassName")
object `Version 6 to 7 config migration` : ConfigMigrator {
  override val fromVersion: Int = 6
  override val toVersion: Int = 7

  private val local = arrayOf("VimHistorySettings", "VimMarksSettings", "VimRegisterSettings", "VimSearchSettings")

  override fun versionUp() {
    val configFile = PathManager.getOptionsFile("vim_settings")
    if (!configFile.exists()) return

    val configuration = JDOMUtil.load(configFile)

    val (localElement, sharedElement) = performMigration(configuration)

    JDOMUtil.write(sharedElement, configFile)
    JDOMUtil.write(localElement, PathManager.getOptionsFile("vim_settings_local"))
  }

  fun performMigration(configuration: Element): Pair<Element, Element> {

    val (local, shared) = configuration.getChildren("component").partition { it.getAttribute("name").value in local }

    val sharedElement = Element("application")
    shared.forEach {
      it.detach()
      sharedElement.addContent(it)
    }

    val localElement = Element("application")
    local.forEach {
      it.detach()
      localElement.addContent(it)
    }

    return localElement to sharedElement
  }
}

