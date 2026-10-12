/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

/**
 * The `g:clever_f_*` variables. They are read on every use, so changing them takes effect immediately, except for
 * [notOverwritesStandardMappings] which only matters when the extension is initialised.
 */
internal object CleverFSettings {
  val acrossNoLine: Boolean get() = flag("clever_f_across_no_line")
  val ignoreCase: Boolean get() = flag("clever_f_ignore_case")
  val smartCase: Boolean get() = flag("clever_f_smart_case")
  val fixKeyDirection: Boolean get() = flag("clever_f_fix_key_direction")
  val markChar: Boolean get() = flag("clever_f_mark_char", default = true)
  val notOverwritesStandardMappings: Boolean get() = flag("clever_f_not_overwrites_standard_mappings")
  val timeoutMs: Int get() = variable("clever_f_timeout_ms")?.toVimNumber()?.value ?: 0
  val charsMatchAnySigns: String get() = variable("clever_f_chars_match_any_signs")?.toVimString()?.value ?: ""

  /** The keys which repeat the last searched character, or null to use the default `<CR>` */
  val repeatLastCharInputs: List<String>?
    get() = (variable("clever_f_repeat_last_char_inputs") as? VimList)?.values?.map { it.toVimString().value }

  private fun flag(name: String, default: Boolean = false): Boolean =
    variable(name)?.toVimNumber()?.booleanValue ?: default

  private fun variable(name: String): VimDataType? = VimPlugin.getVariableService().getGlobalVariableValue(name)
}
