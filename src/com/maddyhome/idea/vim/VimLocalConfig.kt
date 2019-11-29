/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.VimPlugin.STATE_VERSION
import org.jdom.Element

/**
 * @author Alex Plate
 */

@State(name = "VimLocalSettings", storages = [
    Storage("\$APP_CONFIG$$/vim_local_settings.xml", roamingType = RoamingType.DISABLED, deprecated = true),
    Storage("\$APP_CONFIG$/vim_local_settings.xml", roamingType = RoamingType.DISABLED)
  ])
class VimLocalConfig : PersistentStateComponent<Element> {
  override fun getState(): Element {
    val element = Element("ideavim-local")

    val state = Element("state")
    state.setAttribute("version", STATE_VERSION.toString())
    element.addContent(state)

    VimPlugin.getMark().saveData(element)
    VimPlugin.getRegister().saveData(element)
    VimPlugin.getSearch().saveData(element)
    VimPlugin.getHistory().saveData(element)
    return element
  }

  override fun loadState(state: Element) {
    VimPlugin.getMark().readData(state)
    VimPlugin.getRegister().readData(state)
    VimPlugin.getSearch().readData(state)
    VimPlugin.getHistory().readData(state)
  }
}
