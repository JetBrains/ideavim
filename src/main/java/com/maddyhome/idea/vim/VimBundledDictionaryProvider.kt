/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.spellchecker.BundledDictionaryProvider

internal class VimBundledDictionaryProvider : BundledDictionaryProvider {
  override fun getBundledDictionaries(): Array<String> = arrayOf("/dictionaries/ideavim.dic")
}
