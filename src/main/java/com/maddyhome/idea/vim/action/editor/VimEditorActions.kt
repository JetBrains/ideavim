/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actions.TabAction
import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.IdeActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.key.VimActionsPromoter
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

/**
 * Invoke the IDE's "EditorTab" action
 *
 * Insert mode handler for `<Tab>` and `<C-I>`. This will invoke the IDE's "EditorTab" action, which will insert the
 * tab or the appropriate number of spaces.
 *
 * Note that `Tab` has special handling in [VimActionsPromoter]. Typically, the promoter makes sure that
 * [VimShortcutKeyAction] is the first action to be evaluated and potentially invoked. However, when the list of
 * possible actions for the shortcut includes [TabAction], the promoter will actually demote [VimShortcutKeyAction] so
 * that it is invoked almost last, second only to [TabAction]. This means the user has the chance to invoke context
 * specific IDE `Tab` actions without the Vim commands interfering, e.g., accepting LLM output, Next Edit Suggestions,
 * expanding Live Templates, etc.
 *
 * In Normal mode, the Vim handler for `Tab` will not insert a tab but move around the jump list. In Insert mode (below)
 * it invokes "EditorTab" and inserts the text. In both cases, [VimShortcutKeyAction] handles the shortcut and the
 * default [TabAction] is not involved. The benefit of this is that we can now map `<Tab>` in both Normal and Insert
 * modes.
 *
 * Also, by inserting `Tab` with our action, we will correctly update the scroll position to keep the caret visible,
 * applying `'scrolloff'` and `'sidescrolloff'`.
 */
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
