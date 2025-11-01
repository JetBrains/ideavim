/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.state.mode.Mode

@CommandOrMotion(keys = ["o"], modes = [com.intellij.vim.annotations.Mode.NORMAL])
class InsertNewLineBelowAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    insertNewLineBelow(editor, context)
    return true
  }
}

@CommandOrMotion(keys = ["O"], modes = [com.intellij.vim.annotations.Mode.NORMAL])
class InsertNewLineAboveAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    insertNewLineAbove(editor, context)
    return true
  }
}

private fun insertNewLineAbove(editor: VimEditor, context: ExecutionContext) {
  if (editor.isOneLineMode()) return

  // See also EditorStartNewLineBefore. That will move the caret to line start, call EditorEnter to create a new line,
  //   and then move up and call EditorLineEnd. We get better indent positioning by going to the line end of the
  //   previous line and hitting enter, especially with plain text files.
  // However, we'll use EditorStartNewLineBefore in PyCharm notebooks where the last character of the previous line
  //   may be locked with a guard

  // Note that we're deliberately bypassing MotionGroup.moveCaret to avoid side effects, most notably unnecessary
  // scrolling
  val firstLiners: MutableSet<VimCaret> = HashSet()
  val moves: MutableSet<Pair<VimCaret, Int>> = HashSet()
  for (caret in editor.nativeCarets()) {
    val offset: Int
    if (caret.getBufferPosition().line == 0) {
      // Fake indenting for the first line. Works well for plain text to match the existing indent
      offset = injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, caret)
      firstLiners.add(caret)
    } else {
      offset = injector.motion.moveCaretToLineEnd(editor, caret.getBufferPosition().line - 1, true)
    }
    moves.add(Pair(caret, offset))
  }

  // Check if the "last character on previous line" has a guard
  // This is actively used in pycharm notebooks https://youtrack.jetbrains.com/issue/VIM-2495
  val hasGuards = moves.stream().anyMatch { (_, second): Pair<VimCaret?, Int?> ->
    editor.document.getOffsetGuard(second!!) != null
  }
  if (!hasGuards) {
    for ((first, second) in moves) {
      first.moveToOffsetNative(second)
    }
    injector.changeGroup.initInsert(editor, context, Mode.INSERT)
    injector.changeGroup.runEnterAction(editor, context)
    for (caret in editor.nativeCarets()) {
      if (firstLiners.contains(caret)) {
        val offset = injector.motion.moveCaretToLineEnd(editor, 0, true)
        caret.moveToOffset(offset)
      }
    }
  } else {
    injector.changeGroup.initInsert(editor, context, Mode.INSERT)
    injector.changeGroup.runEnterAboveAction(editor, context)
  }
  injector.scroll.scrollCaretIntoView(editor)
}

/**
 * Begin insert after the current line by creating a new blank line below the current line
 * for all carets
 * @param editor  The editor to insert into
 * @param context The data context
 */
private fun insertNewLineBelow(editor: VimEditor, context: ExecutionContext) {
  if (editor.isOneLineMode()) return

  for (caret in editor.nativeCarets()) {
    caret.moveToOffset(injector.motion.moveCaretToCurrentLineEnd(editor, caret))
  }

  injector.changeGroup.initInsert(editor, context, Mode.INSERT)
  injector.changeGroup.runEnterAction(editor, context)
  injector.scroll.scrollCaretIntoView(editor)
}
