/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

/**
 * Check ignorecase and smartcase options to see if a case insensitive search should be performed with the given pattern.
 *
 * When ignorecase is not set, this will always return false - perform a case sensitive search.
 *
 * Otherwise, check smartcase. When set, the search will be case insensitive if the pattern contains only lowercase
 * characters, and case sensitive (returns false) if the pattern contains any lowercase characters.
 *
 * The smartcase option can be ignored, e.g. when searching for the whole word under the cursor. This always performs a
 * case insensitive search, so `\<Work\>` will match `Work` and `work`. But when choosing the same pattern from search
 * history, the smartcase option is applied, and `\<Work\>` will only match `Work`.
 */
internal fun shouldIgnoreCase(pattern: String, ignoreSmartCaseOption: Boolean): Boolean {
  val sc = injector.globalOptions().smartcase && !ignoreSmartCaseOption
  return injector.globalOptions().ignorecase && !(sc && containsUpperCase(pattern))
}

private fun containsUpperCase(pattern: String): Boolean {
  for (i in pattern.indices) {
    if (Character.isUpperCase(pattern[i]) && (i == 0 || pattern[i - 1] != '\\')) {
      return true
    }
  }
  return false
}
