/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.config.migration

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import kotlin.io.path.exists

internal interface ConfigMigrator {
  val fromVersion: Int
  val toVersion: Int
  fun versionUp()
}

@Suppress("ClassName")
internal object `Version 6 to 7 config migration` : ConfigMigrator {
  override val fromVersion: Int = 6
  override val toVersion: Int = 7

  private val local = arrayOf("VimHistorySettings", "VimMarksSettings", "VimRegisterSettings", "VimSearchSettings")

  override fun versionUp() {
    val configFile = PathManager.getOptionsFile("vim_settings").toPath()
    if (!configFile.exists()) return

    val configuration = JDOMUtil.load(configFile)

    val (localElement, sharedElement) = performMigration(configuration)

    JDOMUtil.write(sharedElement, configFile)
    JDOMUtil.write(localElement, PathManager.getOptionsFile("vim_settings_local").toPath())
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
