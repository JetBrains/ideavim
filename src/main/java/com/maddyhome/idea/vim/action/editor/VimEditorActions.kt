/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.IdeActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import java.util.*

@CommandOrMotion(keys = ["<Del>"], modes = [Mode.INSERT])
internal class VimEditorDelete : IdeActionHandler(IdeActions.ACTION_EDITOR_DELETE) {
  override val type: Command.Type = Command.Type.DELETE
}

@CommandOrMotion(keys = ["<Down>", "<kDown>"], modes = [Mode.INSERT])
internal class VimEditorDown : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN) {
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val undo = injector.undo
    when (undo) {
      is VimKeyBasedUndoService -> undo.setMergeUndoKey()
      is VimTimestampBasedUndoService -> {
        val nanoTime = System.nanoTime()
        editor.forEachCaret { undo.endInsertSequence(it, it.offset, nanoTime) }
      }
    }
    return super.execute(editor, context, cmd, operatorArguments)
  }
}

@CommandOrMotion(keys = ["<Tab>", "<C-I>"], modes = [Mode.INSERT])
internal class VimEditorTab : IdeActionHandler(IdeActions.ACTION_EDITOR_TAB) {
  override val type: Command.Type = Command.Type.INSERT
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

@CommandOrMotion(keys = ["<Up>", "<kUp>"], modes = [Mode.INSERT])
internal class VimEditorUp : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP) {
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val undo = injector.undo
    when (undo) {
      is VimKeyBasedUndoService -> undo.setMergeUndoKey()
      is VimTimestampBasedUndoService -> {
        val nanoTime = System.nanoTime()
        editor.forEachCaret { undo.endInsertSequence(it, it.offset, nanoTime) }
      }
    }
    return super.execute(editor, context, cmd, operatorArguments)
  }
}

@CommandOrMotion(keys = ["K"], modes = [Mode.NORMAL])
internal class VimQuickJavaDoc : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(editor, IdeActions.ACTION_QUICK_JAVADOC, context)
    return true
  }
}
