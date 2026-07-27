/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.spellchecker.SpellCheckerManager
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.SpellcheckerService
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ij

class IjSpellcheckerService : SpellcheckerService {
  override fun addWordToDictionary(word: String, editor: VimEditor) {
    val project = editor.ij.project ?: return
    val manager = SpellCheckerManager.getInstance(project)
    manager.acceptWordAsCorrect(word, project = project)
  }

  override fun selectSuggestion(word: String, editor: VimEditor, caret: VimCaret) {
    val project = editor.ij.project ?: return
    val range = injector.searchHelper.findWordAtOrFollowingCursor(editor, caret, isBigWord = false) ?: return

    val suggestions = SpellCheckerManager.getInstance(project).getSuggestions(word)
    if (suggestions.isEmpty()) {
      injector.messages.showMessage(editor, "No spelling suggestions for '$word'")
      return
    }

    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(suggestions)
      .setTitle("Change '$word' to")
      .setItemChosenCallback { chosen ->
        WriteCommandAction.runWriteCommandAction(project) {
          (editor as MutableVimEditor).replaceString(range.startOffset, range.endOffset, chosen)
        }
      }
      .createPopup()
      .showInBestPositionFor(editor.ij)
  }

  override fun removeWordFromDictionary(word: String, editor: VimEditor) {
    val project = editor.ij.project ?: return
    val manager = SpellCheckerManager.getInstance(project)
    var dictionaryWords = manager.userDictionaryWords
    dictionaryWords = dictionaryWords - word
    manager.updateUserDictionary(dictionaryWords)
  }
}
