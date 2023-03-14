/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("DEPRECATION", "unused")

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.Options
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
public object OptionsManager {
  public val ignorecase: ToggleOption
    get() = ToggleOption(Options.ignorecase)
  public val smartcase: ToggleOption
    get() = ToggleOption(Options.smartcase)
  public val iskeyword: KeywordOption
    get() = KeywordOption(KeywordOptionHelper)
}

@Deprecated("No longer used. Use StringOption or KeywordOptionHelper")
public class KeywordOption(public val helper: KeywordOptionHelper) {
  public fun toRegex(): List<String> {
    return helper.toRegex()
  }
}
