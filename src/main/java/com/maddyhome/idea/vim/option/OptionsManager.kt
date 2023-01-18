/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("DEPRECATION", "unused")

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper

/**
 * COMPATIBILITY-LAYER: Added a class and package
 * Please see: https://jb.gg/zo8n0r
 */
@Deprecated("Use VimInjector.optionService")
// Used by:
// ideavim-sneak 1.2.0 which is current - ignorecase + smartcase, both just access isSet property
// IdeaVim-EasyMotion 1.9 + 1.10 - OptionsManger.iskeyword.toRegex() (plus more in tests, which are already broken)
// (which-key 0.6.2 uses timeout + timeoutlen, now removed. That plugin version is broken due to other changes)
object OptionsManager {
  val ignorecase: ToggleOption
    get() = ToggleOption(OptionConstants.ignorecase)
  val smartcase: ToggleOption
    get() = ToggleOption(OptionConstants.smartcase)
  val iskeyword: KeywordOption
    get() = KeywordOption(KeywordOptionHelper)
}

@Deprecated("No longer used. Use StringOption or KeywordOptionHelper")
class KeywordOption(val helper: KeywordOptionHelper) {
  fun toRegex(): List<String> {
    return helper.toRegex()
  }
}
