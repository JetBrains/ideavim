/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.text

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.Command.Type
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["z="], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class SelectMisspelledWordSuggestionAction : VimActionHandler.ForEachCaret() {

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val range = injector.searchHelper.findWordAtOrFollowingCursor(editor, caret, isBigWord = false) ?: return false
    val word = editor.getText(range.startOffset, range.endOffset)
    if (word.isEmpty()) return false
    injector.spellcheckerService.selectSuggestion(word, editor, caret)
    return true
  }

  override val type: Type
    get() = Type.OTHER_WRITABLE
}
