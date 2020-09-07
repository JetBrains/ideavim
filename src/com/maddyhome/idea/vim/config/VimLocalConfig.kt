/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.VimPlugin
import org.jdom.Element

/**
 * @author Alex Plate
 */

@State(name = "VimLocalSettings", storages = [
  Storage("\$APP_CONFIG$$/vim_local_settings.xml", roamingType = RoamingType.DISABLED, deprecated = true),
  Storage("\$APP_CONFIG$/vim_local_settings.xml", roamingType = RoamingType.DISABLED, deprecated = true)
])
@Deprecated("The data from this class will be stored in vim_settings")
class VimLocalConfig : PersistentStateComponent<Element> {
  override fun getState(): Element? = null

  override fun loadState(state: Element) {
    // This is initialization of state from the legacy configuration structure.
    // This code should be performed only once on settings migration.
    //   After the migration is done, the file with settings gets removed and this method won't be called again.
    VimPlugin.getMark().readData(state)
    VimPlugin.getRegister().readData(state)
    VimPlugin.getSearch().readData(state)
    VimPlugin.getHistory().readData(state)
  }

  companion object {
    fun initialize() {
      @Suppress("DEPRECATION")
      ServiceManager.getService(VimLocalConfig::class.java)
    }
  }
}
