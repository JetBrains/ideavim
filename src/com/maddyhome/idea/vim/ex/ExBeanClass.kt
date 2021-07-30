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

package com.maddyhome.idea.vim.ex

import com.intellij.openapi.diagnostic.logger
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.VimPlugin

class ExBeanClass : BaseKeyedLazyInstance<CommandHandler>() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("names")
  var names: String? = null

  override fun getImplementationClassName(): String? = implementation

  fun register() {
    if (this.pluginDescriptor.pluginId != VimPlugin.getPluginId()) {
      logger<ExBeanClass>().error("IdeaVim doesn't accept contributions to `vimActions` extension points. Please create a plugin using `VimExtension`. Plugin to blame: ${this.pluginDescriptor.pluginId}")
      return
    }
    ExCommand.addHandler(this)
  }
}

interface ComplicatedNameExCommand {
  val names: Array<CommandName>
}
