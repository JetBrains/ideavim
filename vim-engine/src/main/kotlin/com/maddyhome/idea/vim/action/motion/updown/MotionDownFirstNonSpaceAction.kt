/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.QuickFixJump
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.vimscript.model.commands.quickfix.showPosition

@CommandOrMotion(
  keys = ["+", "<C-M>"],
  modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING],
  description = "Move [count] lines downward, to the first non-blank character, linewise."
)
class MotionDownFirstNonSpaceAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.moveCaretToRelativeLineStartSkipLeading(editor, caret, operatorArguments.count1).toMotion()
  }
}

// FIXME I should not exist (see class above)
@CommandOrMotion(
  keys = ["<CR>"],
  modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING],
  description = "Move [count] lines downward, to the first non-blank character. In a live template, jump to the next template variable. In the :actionlist window, execute the action on the line."
)
class EnterNormalAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (editor.getVirtualBufferKind() == VirtualBufferKind.QuickFix) {
      if (caret.id == editor.primaryCaret().id) jumpToQuickFixItemOnLine(editor, caret)
      return Motion.NoMotion
    }
    if (editor.getVirtualBufferKind() == VirtualBufferKind.ActionList) {
      if (caret.id == editor.primaryCaret().id) executeActionOnLine(editor, caret)
      return Motion.NoMotion
    }
    if (editor.isInHistoryWindow()) {
      // Vim's cmdwin has a single caret. Multi-caret on the cmdwin must not re-dispatch.
      if (caret.id == editor.primaryCaret().id) {
        injector.searchWindowGroup.executeCurrentLineAndClose(editor, caret, context)
      }
      return Motion.NoMotion
    }
    val templateState = injector.templateManager.getTemplateState(editor)
    return if (templateState != null) {
      injector.actionExecutor.executeAction(
        editor,
        name = injector.actionExecutor.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE,
        context = context
      )
      Motion.NoMotion
    } else {
      injector.motion.moveCaretToRelativeLineStartSkipLeading(editor, caret, operatorArguments.count1).toMotion()
    }
  }
}

/**
 * `<CR>` in the quickfix window jumps to the item on the caret line, in the window the quickfix window was opened from.
 */
private fun jumpToQuickFixItemOnLine(editor: VimEditor, caret: ImmutableVimCaret) {
  // Read the line before moving to the other window
  val number = caret.getBufferPosition().line + 1
  val from = injector.virtualBufferGroup.selectOriginalWindow(editor) ?: editor.getCmdwinOriginalEditor() ?: editor
  val jump = injector.quickFixService.jumpToItem(from, number)
  if (jump is QuickFixJump.Moved) showPosition(jump)
}

/**
 * `<CR>` in the `:actionlist` window executes the action on the caret line in the window the list was opened from.
 */
private fun executeActionOnLine(editor: VimEditor, caret: ImmutableVimCaret) {
  // Read the line before moving to the other window
  val actionId = editor.getLineText(caret.getBufferPosition().line).trim().substringBefore(' ')
  // The header line isn't an action
  val action = injector.actionExecutor.getAction(actionId) ?: return
  // Without another window, the original editor is a tab next to the list, and the action works on the selected tab
  val from = injector.virtualBufferGroup.selectOriginalWindow(editor)
    ?: editor.getCmdwinOriginalEditor()?.also { selectEditor(it) }
    ?: editor
  val context = injector.executionContextManager.getEditorExecutionContext(from)
  if (injector.application.isUnitTest()) {
    injector.actionExecutor.executeAction(from, action, context)
  } else {
    injector.application.runAfterGotFocus { injector.actionExecutor.executeAction(from, action, context) }
  }
}

private fun selectEditor(editor: VimEditor) {
  val path = editor.getPath() ?: return
  val protocol = editor.extractProtocol() ?: return
  injector.file.selectEditor(editor.projectId, path, protocol)
}
