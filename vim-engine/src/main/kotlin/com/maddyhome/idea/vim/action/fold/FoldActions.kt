/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.fold

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFoldRegion
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["zM"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimCollapseAllRegions : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_COLLAPSE_ALL_REGIONS,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["za"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimExpandCollapseToggleRegion : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_EXPAND_COLLAPSE_TOGGLE,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zc"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimCollapseRegion : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_COLLAPSE_REGION,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zC"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimCollapseRegionRecursively : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_COLLAPSE_REGION_RECURSIVELY,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zR"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimExpandAllRegions : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_EXPAND_ALL_REGIONS,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zo"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimExpandRegion : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_EXPAND_REGION,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zO"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimExpandRegionRecursively : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.actionExecutor.executeAction(
      editor,
      name = injector.actionExecutor.ACTION_EXPAND_REGION_RECURSIVELY,
      context = context
    )
    return true
  }
}

@CommandOrMotion(keys = ["zA"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimToggleRegionRecursively : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val caret = editor.currentCaret()
    val foldRegion = findFoldRegionAtCaret(editor, caret.offset)
    val actionName = getToggleAction(foldRegion)
    injector.actionExecutor.executeAction(
      editor,
      name = actionName,
      context = context
    )

    return true
  }
}

private fun findFoldRegionAtCaret(editor: VimEditor, caretOffset: Int): VimFoldRegion? {
  val line = editor.offsetToBufferPosition(caretOffset).line
  return editor.getFoldRegionAtLine(line)
}

private fun getToggleAction(foldRegion: VimFoldRegion?): String = if (foldRegion == null || foldRegion.isExpanded) {
  injector.actionExecutor.ACTION_COLLAPSE_REGION_RECURSIVELY
} else {
  injector.actionExecutor.ACTION_EXPAND_REGION_RECURSIVELY
}
