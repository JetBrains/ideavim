/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionScope

/**
 * COMPATIBILITY-LAYER: Moved out of class and to a different package
 * Please see: https://jb.gg/zo8n0r
 */
@Deprecated("Please use com.maddyhome.idea.vim.options.ToggleOption")
// Used by ideavim-sneak 1.2.0, which uses isSet()
// (Used by which-key 0.6.2, which is broken due to changed T in CommandPartNode<T>)
public open class ToggleOption(private val option: com.maddyhome.idea.vim.options.ToggleOption) {
  /**
   * COMPATIBILITY-LAYER: Method added
   * Please see: https://jb.gg/zo8n0r
   */
  public fun isSet(): Boolean = injector.optionGroup.getOptionValue(option, OptionScope.GLOBAL).asBoolean()
}
