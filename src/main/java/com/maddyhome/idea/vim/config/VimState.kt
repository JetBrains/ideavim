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

package com.maddyhome.idea.vim.config

import org.jdom.Element
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author Alex Plate
 */
class VimState {
  var isIdeaJoinNotified by StateProperty("idea-join")
  var isIdeaPutNotified by StateProperty("idea-put")

  fun readData(element: Element) {
    val notifications = element.getChild("notifications")
    map.keys.forEach { name ->
      notifications?.getChild(name)?.getAttributeValue("enabled")?.let {
        map[name] = it.toBoolean()
      }
    }
  }

  fun saveData(element: Element) {
    val notifications = Element("notifications")
    element.addContent(notifications)

    map.forEach { (name, value) ->
      val child = Element(name)
      child.setAttribute("enabled", value.toString())
      notifications.addContent(child)
    }
  }
}

val map by lazy { mutableMapOf<String, Boolean>() }

private class StateProperty(val xmlName: String) : ReadWriteProperty<VimState, Boolean> {

  init {
    map[xmlName] = false
  }

  override fun getValue(thisRef: VimState, property: KProperty<*>): Boolean = map.getOrPut(xmlName) { false }

  override fun setValue(thisRef: VimState, property: KProperty<*>, value: Boolean) {
    map[xmlName] = value
  }
}
