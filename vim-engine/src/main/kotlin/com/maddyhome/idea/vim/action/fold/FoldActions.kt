/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.fold

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFoldRegion
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler

@CommandOrMotion(keys = ["zM"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimCollapseAllRegions : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    FoldState.closeAllFolds(editor)
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
    FoldState.openAllFolds(editor)
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
    val foldRegion = findFoldRegionAtLine(editor, caret.offset) ?: return true
    val actionName = getToggleAction(foldRegion)
    injector.actionExecutor.executeAction(
      editor,
      name = actionName,
      context = context
    )

    return true
  }
}

@CommandOrMotion(keys = ["zr"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimIncrementFoldLevel : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val count = cmd.count.coerceAtLeast(1)
    val currentLevel = FoldState.getFoldLevel(editor)
    FoldState.setFoldLevel(editor, currentLevel + count)
    return true
  }
}

@CommandOrMotion(keys = ["zm"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimDecrementFoldLevel : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val currentLevel = FoldState.getFoldLevel(editor)
    val newLevel = (currentLevel - cmd.count).coerceAtLeast(0)
    FoldState.setFoldLevel(editor, newLevel)
    return true
  }
}

@CommandOrMotion(keys = ["zf"], modes = [Mode.NORMAL])
class VimCreateFoldMotionAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_WRITABLE
  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument ?: return false
    val range = injector.motion.getMotionRange(editor, editor.primaryCaret(), context, argument, operatorArguments)
      ?: return false
    val endOffset = getEndOffset(range, argument, editor)

    editor.createFoldRegion(range.startOffset, endOffset, collapse = true)
    return true
  }

  private fun getEndOffset(
    range: TextRange,
    argument: Argument,
    editor: VimEditor,
  ): Int {
    // if last char is \n we want to exclude it so next line won't be collapsed
    if ((argument as Argument.Motion).isLinewiseMotion() && editor.charAt(range.endOffset - 1) == '\n') {
      return range.endOffset - 1
    }
    return range.endOffset
  }
}

@CommandOrMotion(keys = ["zf"], modes = [Mode.VISUAL])
class VimCreateFoldVisualAction : VisualOperatorActionHandler.ForEachCaret() {

  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val textRange = range.toVimTextRange(true)
    editor.createFoldRegion(textRange.startOffset, textRange.endOffset, collapse = true)
    return true
  }
}

@CommandOrMotion(keys = ["zd"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimDeleteFoldAtCursor : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return editor.deleteFoldRegionAtOffset(editor.currentCaret().offset)
  }
}

@CommandOrMotion(keys = ["zD"], modes = [Mode.NORMAL, Mode.VISUAL])
class VimDeleteFoldsRecursivelyAtCursor : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return editor.deleteFoldRegionsRecursivelyAtOffset(editor.currentCaret().offset)
  }
}

private fun findFoldRegionAtLine(editor: VimEditor, caretOffset: Int): VimFoldRegion? {
  val line = editor.offsetToBufferPosition(caretOffset).line
  val lineRegion = editor.getFoldRegionAtLine(line)
  if (lineRegion != null) return lineRegion
  return editor.getFoldRegionsAtOffset(caretOffset).firstOrNull()
}

private fun getToggleAction(foldRegion: VimFoldRegion): String = if (foldRegion.isExpanded) {
  injector.actionExecutor.ACTION_COLLAPSE_REGION_RECURSIVELY
} else {
  injector.actionExecutor.ACTION_EXPAND_REGION_RECURSIVELY
}

@CommandOrMotion(keys = ["zj"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class VimNextFold : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val currentLine = editor.offsetToBufferPosition(caret.offset).line
    val foldStartLines = findFoldStartLines(editor, currentLine)
    val count = operatorArguments.count1

    if (foldStartLines.size < count) {
      return Motion.NoMotion
    }

    val targetLine = foldStartLines[count - 1]
    return Motion.AbsoluteOffset(editor.getLineStartOffset(targetLine))
  }
}

@CommandOrMotion(keys = ["zk"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class VimPreviousFold : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val currentLine = editor.offsetToBufferPosition(caret.offset).line
    val foldEndLines = findFoldEndLines(editor, currentLine)
    val count = operatorArguments.count1

    if (foldEndLines.size < count) {
      return Motion.NoMotion
    }

    val targetLine = foldEndLines[count - 1]
    return Motion.AbsoluteOffset(editor.getLineStartOffset(targetLine))
  }
}

private fun findFoldStartLines(editor: VimEditor, currentLine: Int): List<Int> =
  editor.getAllFoldRegions()
    .map { fold -> getFoldStartLine(fold, editor) }
    .filter { it > currentLine }
    .distinct()
    .sorted()


private fun findFoldEndLines(editor: VimEditor, currentLine: Int): List<Int> =
  editor.getAllFoldRegions()
    .map { editor.offsetToBufferPosition(it.endOffset).line }
    .filter { it < currentLine }
    .distinct()
    .sortedDescending()

/**
 * Gets the line number where a fold visually starts.
 *
 * IDE fold regions typically have their startOffset at the first character of the folded content
 * (e.g., the newline after `{`), not at the fold marker itself. Subtracting 1 from the offset
 * ensures we get the line containing the fold marker (e.g., the line with `{`), which matches
 * Vim's behavior where `zj` navigates to the line where the fold starts visually.
 */
private fun getFoldStartLine(fold: VimFoldRegion, editor: VimEditor): Int =
  if (fold.startOffset > 0) {
    editor.offsetToBufferPosition(fold.startOffset - 1).line
  } else {
    editor.offsetToBufferPosition(fold.startOffset).line
  }
