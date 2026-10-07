/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.multiplecursors.CaseSensitivity
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

@NlsSafe
private const val CASE_SETTING = "VM_case_setting"

/**
 * `g:VM_case_setting`: `'smart'`, `'sensitive'` or `'ignore'`. Any other value, including the default empty string,
 * follows 'ignorecase' and 'smartcase'.
 *
 * vim-visual-multi sets 'ignorecase' and 'smartcase' for the duration of the session. We resolve them the same way,
 * but leave the options of the user untouched.
 */
internal object VmCaseSetting : CaseSensitivity {

  override fun ignoresCase(text: String): Boolean {
    val (ignoreCase, smartCase) = when (readSetting()) {
      "smart" -> true to true
      "sensitive" -> false to false
      "ignore" -> true to false
      else -> injector.globalOptions().let { it.ignorecase to it.smartcase }
    }
    return ignoreCase && !(smartCase && text.any(Char::isUpperCase))
  }

  /**
   * vim-visual-multi compares the value ignoring case
   */
  private fun readSetting(): String? {
    val setting = VimPlugin.getVariableService().getGlobalVariableValue(CASE_SETTING) as? VimString
    return setting?.value?.lowercase()
  }
}
