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

package com.maddyhome.idea.vim.extension

import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class ExtensionBeanClass : BaseKeyedLazyInstance<VimExtension>() {

  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("name")
  var name: String? = null

  /**
   * List of aliases for the extension. These aliases are used to support `Plug` and `Plugin` commands.
   * Technically, it would be enough to save here github link and short version of it ('author/plugin'),
   *   but it may contain more aliases just in case.
   */
  @Tag("aliases")
  @XCollection
  var aliases: List<Alias>? = null

  override fun getImplementationClassName(): String? = implementation
}

@Tag("alias")
class Alias {
  @Attribute("name")
  var name: String? = null
}
