/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.spellchecker.SpellCheckerManager
import com.maddyhome.idea.vim.api.SpellcheckerService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij

class IjSpellcheckerService : SpellcheckerService {
  override fun addWordToDictionary(word: String, editor: VimEditor) {
    val project = editor.ij.project ?: return
    val manager = SpellCheckerManager.getInstance(project)
    manager.acceptWordAsCorrect(word, project = project)
  }
}