/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.lookup

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.action.change.VimRedoBuffer
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.IdeLookup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.state.mode.CtrlXCompletionMode

/**
 * What the completion popup keys do while a popup is open.
 *
 * Each of these is registered with `@CommandOrMotion(lookup = true)` for the same keys as its normal counterpart, and
 * [com.maddyhome.idea.vim.key.consumers.CommandKeyConsumer] picks between the two when the key is matched. `<C-Y>`
 * accepting an item and `<C-Y>` inserting the character above the caret are two different commands that happen to
 * share a keystroke, so they are two handlers rather than one with a branch in it.
 *
 * All of them drop their own key from the redo buffer. Moving around a popup changes no text, and accepting an item
 * changes text that the completion records as keys of its own (`recordSynthesizedKeys` in `IdeaSpecifics`), so `.`
 * must not replay the key itself - with no popup open it would run the normal command instead, which for `<C-Y>`
 * inserts a stray character and for `<C-N>` runs whatever the IDE keymap binds to Ctrl+N.
 */
internal abstract class LookupActionHandler : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  final override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val lookup = injector.lookupManager.getActiveLookup(editor) ?: return false
    VimRedoBuffer.dropLastKey()
    return executeOnLookup(editor, context, lookup)
  }

  abstract fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean
}

/** `<C-Y>` with a popup open: take the selected item. */
@CommandOrMotion(keys = ["<C-Y>"], modes = [Mode.INSERT], lookup = true)
internal class AcceptLookupAction : LookupActionHandler() {
  override fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean {
    lookup.accept(editor.primaryCaret(), context)
    return true
  }
}

/** `<C-E>` with a popup open: dismiss it, keeping what was typed. */
@CommandOrMotion(keys = ["<C-E>"], modes = [Mode.INSERT], lookup = true)
internal class CloseLookupAction : LookupActionHandler() {
  override fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean {
    lookup.close(editor.primaryCaret(), context)
    return true
  }
}

/** `<C-N>` with a popup open: select the next item. */
@CommandOrMotion(keys = ["<C-N>"], modes = [Mode.INSERT], lookup = true)
internal class CycleLookupDownAction : LookupActionHandler() {
  override fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean {
    lookup.down(editor.primaryCaret(), context)
    return true
  }
}

/** `<C-P>` with a popup open: select the previous item. */
@CommandOrMotion(keys = ["<C-P>"], modes = [Mode.INSERT], lookup = true)
internal class CycleLookupUpAction : LookupActionHandler() {
  override fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean {
    lookup.up(editor.primaryCaret(), context)
    return true
  }
}

/**
 * `<C-L>` / `<C-F>` with a popup open: select the next matching line or file path.
 *
 * These only mean anything inside `CTRL-X` completion, so without the sub-mode they decline and the normal handler
 * runs - which is what a bare `<C-L>` in Insert mode should do.
 */
internal abstract class CycleCtrlXLookupAction(private val mode: CtrlXCompletionMode) : LookupActionHandler() {
  override fun executeOnLookup(editor: VimEditor, context: ExecutionContext, lookup: IdeLookup): Boolean {
    if (injector.vimState.ctrlXCompletionMode == CtrlXCompletionMode.NONE) return false
    injector.vimState.ctrlXCompletionMode = mode
    lookup.down(editor.primaryCaret(), context)
    return true
  }
}

@CommandOrMotion(keys = ["<C-L>"], modes = [Mode.INSERT], lookup = true)
internal class CycleLineCompletionLookupAction : CycleCtrlXLookupAction(CtrlXCompletionMode.WHOLE_LINE)

@CommandOrMotion(keys = ["<C-F>"], modes = [Mode.INSERT], lookup = true)
internal class CycleFilePathCompletionLookupAction : CycleCtrlXLookupAction(CtrlXCompletionMode.FILE_PATH)
