/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

internal class ExtensionBeanClass : BaseKeyedLazyInstance<VimExtension>() {

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
internal class Alias {
  @Attribute("name")
  var name: String? = null
}
