/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.util.*

/**
 * @author smartbomb
 */
@ExCommand(
  command = "actionl[ist]",
  description = "List IDE action IDs and their shortcuts, keeping only lines that contain the optional search text. The list opens in a read-only window, an already open one is reused. <CR> in that window executes the action on the cursor line in the window the list was opened from."
)
internal data class ActionListCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val searchPattern = argument.trim().lowercase(Locale.getDefault()).split("*")
    val actionManager = ActionManager.getInstance()

    val actions = actionManager.getActionIdList("")
      .sortedWith(String.CASE_INSENSITIVE_ORDER)
      .map { actionName ->
        val shortcuts = actionManager.getAction(actionName).shortcutSet.shortcuts.joinToString(" ") {
          if (it is KeyboardShortcut) injector.parser.toKeyNotation(it.firstKeyStroke) else it.toString()
        }
        if (shortcuts.isBlank()) actionName else "${actionName.padEnd(50)} $shortcuts"
      }
      .filter { line -> searchPattern.all { it in line.lowercase(Locale.getDefault()) } }

    val text = buildString {
      appendLine(injector.messages.message("command.action.list.header"))
      actions.forEach {
        appendLine(it)
      }
    }

    injector.virtualBufferGroup.open(context, editor, ActionListBuffer, text, line = 0)
    return ExecutionResult.Success
  }
}

/**
 * The read-only window `:actionlist` opens to show the IDE actions. An already open one is reused.
 *
 * `<CR>` executes the action on the caret line in the window the list was opened from.
 */
internal data object ActionListBuffer : VirtualBufferKind(), VirtualBufferKind.EnterHandler {
  override val fileName: String = "[Action List]"
  override val readOnly: Boolean = true
  override val reuseOpen: Boolean = true

  override fun onEnter(buffer: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext) {
    // Read the line before moving to the other window
    val actionId = buffer.getLineText(caret.getBufferPosition().line).trim().substringBefore(' ')
    // The header line isn't an action
    val action = injector.actionExecutor.getAction(actionId) ?: return
    // Without another window, the original editor is a tab next to the list, and the action works on the selected tab
    val from = injector.virtualBufferGroup.selectOriginalWindow(buffer)
      ?: buffer.getCmdwinOriginalEditor()?.also { selectEditor(it) }
      ?: buffer
    val fromContext = injector.executionContextManager.getEditorExecutionContext(from)
    if (injector.application.isUnitTest()) {
      injector.actionExecutor.executeAction(from, action, fromContext)
    } else {
      injector.application.runAfterGotFocus { injector.actionExecutor.executeAction(from, action, fromContext) }
    }
  }

  private fun selectEditor(editor: VimEditor) {
    val path = editor.getPath() ?: return
    val protocol = editor.extractProtocol() ?: return
    injector.file.selectEditor(editor.projectId, path, protocol)
  }
}
