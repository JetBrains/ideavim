/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.MathUtil.clamp
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.addJump
import com.maddyhome.idea.vim.api.anyNonWhitespace
import com.maddyhome.idea.vim.api.getLeadingCharacterOffset
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeColumn
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.api.normalizeVisualColumn
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.Motion.AdjustedOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.getNormalizedScrollOffset
import com.maddyhome.idea.vim.helper.getNormalizedSideScrollOffset
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.listener.AppCodeTemplates
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.annotations.Range
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * This handles all motion related commands and marks
 */
internal class MotionGroup : VimMotionGroupBase() {
  override fun onAppCodeMovement(editor: VimEditor, caret: VimCaret, offset: Int, oldOffset: Int) {
    AppCodeTemplates.onMovement(editor.ij, caret.ij, oldOffset < offset)
  }

  private fun selectEditor(project: Project, mark: Mark): Editor? {
    val virtualFile = markToVirtualFile(mark) ?: return null
    return selectEditor(project, virtualFile)
  }

  private fun markToVirtualFile(mark: Mark): VirtualFile? {
    val protocol = mark.protocol
    val fileSystem: VirtualFileSystem? = VirtualFileManager.getInstance().getFileSystem(protocol)
    return fileSystem?.findFileByPath(mark.filepath)
  }

  private fun selectEditor(project: Project?, file: VirtualFile) =
    VimPlugin.getFile().selectEditor(project, file)

  override fun moveCaretToMatchingPair(editor: VimEditor, caret: ImmutableVimCaret): Motion {
    return SearchHelper.findMatchingPairOnCurrentLine(editor.ij, caret.ij).toMotionOrError()
  }

  override fun moveCaretToFirstDisplayLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    normalizeToScreen: Boolean,
  ): Int {
    return moveCaretToScreenLocation(editor.ij, caret.ij, ScreenLocation.TOP, count - 1, normalizeToScreen)
  }

  override fun moveCaretToLastDisplayLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    normalizeToScreen: Boolean,
  ): Int {
    return moveCaretToScreenLocation(editor.ij, caret.ij, ScreenLocation.BOTTOM, count - 1, normalizeToScreen)
  }

  override fun moveCaretToMiddleDisplayLine(editor: VimEditor, caret: ImmutableVimCaret): Int {
    return moveCaretToScreenLocation(editor.ij, caret.ij, ScreenLocation.MIDDLE, 0, false)
  }

  override fun moveCaretToMark(caret: ImmutableVimCaret, ch: Char, toLineStart: Boolean): Motion {
    val markService = injector.markService
    val mark = markService.getMark(caret, ch) ?: return Motion.Error

    val caretEditor = caret.editor
    val caretVirtualFile = EditorHelper.getVirtualFile((caretEditor as IjVimEditor).editor)

    val line = mark.line

    if (caretVirtualFile!!.path == mark.filepath) {
      val offset = if (toLineStart) {
        moveCaretToLineStartSkipLeading(caretEditor, line)
      } else {
        caretEditor.bufferPositionToOffset(BufferPosition(line, mark.col, false))
      }
      return offset.toMotionOrError()
    }

    val project = caretEditor.editor.project
    val markEditor = selectEditor(project!!, mark)
    if (markEditor != null) {
      // todo should we move all the carets or only one?
      for (carett in markEditor.caretModel.allCarets) {
        val offset = if (toLineStart) {
          moveCaretToLineStartSkipLeading(IjVimEditor(markEditor), line)
        } else {
          // todo should it be the same as getting offset above?
          markEditor.logicalPositionToOffset(LogicalPosition(line, mark.col))
        }
        IjVimCaret(carett!!).moveToOffset(offset)
      }
    }
    return Motion.Error
  }

  override fun moveCaretToJump(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion {
    val jumpService = injector.jumpService
    val spot = jumpService.getJumpSpot()
    val (line, col, fileName) = jumpService.getJump(count) ?: return Motion.Error
    val vf = EditorHelper.getVirtualFile(editor.ij) ?: return Motion.Error
    val lp = BufferPosition(line, col, false)
    val lpNative = LogicalPosition(line, col, false)
    return if (vf.path != fileName) {
      val newFile = LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'))
        ?: return Motion.Error
      selectEditor(editor.ij.project, newFile)?.let { newEditor ->
        if (spot == -1) {
          jumpService.addJump(editor, false)
        }
        newEditor.vim.let {
          it.currentCaret().moveToOffset(it.normalizeOffset(newEditor.logicalPositionToOffset(lpNative), false))
        }
      }
      Motion.Error
    } else {
      if (spot == -1) {
        jumpService.addJump(editor, false)
      }
      editor.bufferPositionToOffset(lp).toMotionOrError()
    }
  }

  override fun moveCaretToCurrentDisplayLineMiddle(editor: VimEditor, caret: ImmutableVimCaret): Motion {
    val width = EditorHelper.getApproximateScreenWidth(editor.ij) / 2
    val len = editor.lineLength(editor.currentCaret().getBufferPosition().line)
    return moveCaretToColumn(editor, caret, max(0, min(len - 1, width)), false)
  }

  override fun moveCaretToColumn(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowEnd: Boolean): Motion {
    val line = caret.getLine().line
    val column = editor.normalizeColumn(line, count, allowEnd)
    val offset = editor.bufferPositionToOffset(BufferPosition(line, column, false))
    return if (column != count) {
      AdjustedOffset(offset, count)
    } else {
      AbsoluteOffset(offset)
    }
  }

  override fun moveCaretToCurrentDisplayLineStart(editor: VimEditor, caret: ImmutableVimCaret): Motion {
    val col = EditorHelper.getVisualColumnAtLeftOfDisplay(editor.ij, caret.getVisualPosition().line)
    return moveCaretToColumn(editor, caret, col, false)
  }

  override fun moveCaretToCurrentDisplayLineStartSkipLeading(
    editor: VimEditor,
    caret: ImmutableVimCaret,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    val col = EditorHelper.getVisualColumnAtLeftOfDisplay(editor.ij, caret.getVisualPosition().line)
    val bufferLine = caret.getLine().line
    return editor.getLeadingCharacterOffset(bufferLine, col)
  }

  override fun moveCaretToCurrentDisplayLineEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    allowEnd: Boolean,
  ): Motion {
    val col = EditorHelper.getVisualColumnAtRightOfDisplay(editor.ij, caret.getVisualPosition().line)
    return moveCaretToColumn(editor, caret, col, allowEnd)
  }

  override fun moveCaretToLineWithSameColumn(
    editor: VimEditor,
    line: Int,
    caret: ImmutableVimCaret,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    var c = caret.vimLastColumn
    var l = line
    if (l < 0) {
      l = 0
      c = 0
    } else if (l >= editor.lineCount()) {
      l = editor.normalizeLine(editor.lineCount() - 1)
      c = editor.lineLength(l)
    }
    val newPos = BufferPosition(l, editor.normalizeColumn(l, c, false))
    return editor.bufferPositionToOffset(newPos)
  }

  override fun moveCaretToLineWithStartOfLineOption(
    editor: VimEditor,
    line: Int,
    caret: ImmutableVimCaret,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return if (injector.options(editor).startofline) {
      moveCaretToLineStartSkipLeading(editor, line)
    } else {
      moveCaretToLineWithSameColumn(editor, line, caret)
    }
  }

  /**
   * If 'absolute' is true, then set tab index to 'value', otherwise add 'value' to tab index with wraparound.
   */
  private fun switchEditorTab(editorWindow: EditorWindow?, value: Int, absolute: Boolean) {
    if (editorWindow != null) {
      val tabbedPane = editorWindow.tabbedPane
      if (absolute) {
        tabbedPane.setSelectedIndex(value)
      } else {
        val tabIndex = (value + tabbedPane.selectedIndex) % tabbedPane.tabCount
        val newIndex = if (tabIndex < 0) tabIndex + tabbedPane.tabCount else tabIndex
        tabbedPane.setSelectedIndex(newIndex)
      }
    }
  }

  override fun moveCaretGotoPreviousTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int {
    val project = editor.ij.project ?: return editor.currentCaret().offset.point
    val currentWindow = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow
    switchEditorTab(currentWindow, if (rawCount >= 1) -rawCount else -1, false)
    return editor.currentCaret().offset.point
  }

  override fun moveCaretGotoNextTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int {
    val absolute = rawCount >= 1
    val project = editor.ij.project ?: return editor.currentCaret().offset.point
    val currentWindow = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow
    switchEditorTab(currentWindow, if (absolute) rawCount - 1 else 1, absolute)
    return editor.currentCaret().offset.point
  }

  override fun moveCaretToLinePercent(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return moveCaretToLineWithStartOfLineOption(
      editor,
      editor.normalizeLine((editor.lineCount() * clamp(count, 0, 100) + 99) / 100 - 1),
      caret,
    )
  }

  private enum class ScreenLocation {
    TOP, MIDDLE, BOTTOM
  }

  // visualLineOffset is a zero based offset to subtract from the direction of travel, where zero is the same as a count
  // of 1. I.e. 1L = L, which is an offset of zero. 2L is an offset of 1 extra line
  // When normalizeToScreen is true, the offset is bounded to the current screen dimensions, and scrolloff is applied.
  // When false, the offset is used directly, and scrolloff is not applied. This is used for op pending motions
  // (scrolloff is applied after)
  private fun moveCaretToScreenLocation(
    editor: Editor,
    caret: Caret,
    screenLocation: ScreenLocation,
    visualLineOffset: Int,
    normalizeToScreen: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    val vimEditor = editor.vim
    val scrollOffset = if (normalizeToScreen) getNormalizedScrollOffset(editor) else 0
    val maxVisualLine = vimEditor.getVisualLineCount()
    val topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
    val topScrollOff = if (topVisualLine > 0) scrollOffset else 0
    val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
    val bottomScrollOff = if (bottomVisualLine < maxVisualLine - 1) scrollOffset else 0
    val topMaxVisualLine = if (normalizeToScreen) bottomVisualLine - bottomScrollOff else maxVisualLine
    val bottomMinVisualLine = if (normalizeToScreen) topVisualLine + topScrollOff else 0
    val targetVisualLine = when (screenLocation) {
      ScreenLocation.TOP -> min(topVisualLine + max(topScrollOff, visualLineOffset), topMaxVisualLine)
      ScreenLocation.MIDDLE -> EditorHelper.getVisualLineAtMiddleOfScreen(editor)
      ScreenLocation.BOTTOM -> max(bottomVisualLine - max(bottomScrollOff, visualLineOffset), bottomMinVisualLine)
    }
    val targetLogicalLine = vimEditor.visualLineToBufferLine(targetVisualLine)
    return moveCaretToLineWithStartOfLineOption(vimEditor, targetLogicalLine, caret.vim)
  }

  companion object {
    @JvmStatic
    fun getMotionRange2(
      editor: Editor,
      caret: Caret,
      context: DataContext?,
      argument: Argument,
      operatorArguments: OperatorArguments,
    ): TextRange? {
      var start: Int
      var end: Int
      if (argument.type === Argument.Type.OFFSETS) {
        val offsets = argument.offsets[caret.vim] ?: return null
        val (first, second) = offsets.getNativeStartAndEnd()
        start = first
        end = second
      } else {
        val cmd = argument.motion
        // Normalize the counts between the command and the motion argument
        val cnt = cmd.count * operatorArguments.count1
        val raw = if (operatorArguments.count0 == 0 && cmd.rawCount == 0) 0 else cnt
        if (cmd.action is MotionActionHandler) {
          val action = cmd.action as MotionActionHandler

          // This is where we are now
          start = caret.offset

          // Execute the motion (without moving the cursor) and get where we end
          val motion = action.getHandlerOffset(
            editor.vim,
            caret.vim,
            IjEditorExecutionContext(context!!),
            cmd.argument,
            operatorArguments.withCount0(raw),
          )

          // Invalid motion
          if (Motion.Error == motion) return null
          if (Motion.NoMotion == motion) return null
          end = (motion as AbsoluteOffset).offset

          // If inclusive, add the last character to the range
          if (action.motionType === MotionType.INCLUSIVE && end < editor.fileSize) {
            if (start > end) {
              start++
            } else {
              end++
            }
          }
        } else if (cmd.action is TextObjectActionHandler) {
          val action = cmd.action as TextObjectActionHandler
          val range =
            action.getRange(editor.vim, caret.vim, IjEditorExecutionContext(context!!), cnt, raw, cmd.argument) ?: return null
          start = range.startOffset
          end = range.endOffset
          if (cmd.isLinewiseMotion()) end--
        } else {
          throw RuntimeException(
            "Commands doesn't take " + cmd.action.javaClass.simpleName + " as an operator",
          )
        }
      }

      // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
      val id = argument.motion.action.id
      if (id == VimChangeGroupBase.VIM_MOTION_WORD_RIGHT || id == VimChangeGroupBase.VIM_MOTION_BIG_WORD_RIGHT || id == VimChangeGroupBase.VIM_MOTION_CAMEL_RIGHT) {
        val text = editor.document.charsSequence.subSequence(start, end).toString()
        val lastNewLine = text.lastIndexOf('\n')
        if (lastNewLine > 0) {
          if (!editor.vim.anyNonWhitespace(end, -1)) {
            end = start + lastNewLine
          }
        }
      }
      return TextRange(start, end)
    }

    fun moveCaretToView(editor: Editor) {
      val vimEditor: VimEditor = editor.vim
      val scrollOffset = getNormalizedScrollOffset(editor)
      val topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
      val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      val caretVisualLine = editor.caretModel.visualPosition.line
      val lastVisualLine = vimEditor.getVisualLineCount() - 1
      val newVisualLine = if (caretVisualLine < topVisualLine + scrollOffset) {
        vimEditor.normalizeVisualLine(topVisualLine + scrollOffset)
      } else if (bottomVisualLine < lastVisualLine && caretVisualLine > bottomVisualLine - scrollOffset) {
        vimEditor.normalizeVisualLine(bottomVisualLine - scrollOffset)
      } else {
        caretVisualLine
      }
      val sideScrollOffset = getNormalizedSideScrollOffset(editor)
      val oldColumn = editor.caretModel.visualPosition.column
      var col = oldColumn
      if (col >= vimEditor.lineLength(vimEditor.currentCaret().getBufferPosition().line) - 1) {
        col = editor.caretModel.primaryCaret.vimLastColumn
      }
      val leftVisualColumn = EditorHelper.getVisualColumnAtLeftOfDisplay(editor, newVisualLine)
      val rightVisualColumn = EditorHelper.getVisualColumnAtRightOfDisplay(editor, newVisualLine)
      val caretColumn = col
      var newColumn = caretColumn

      // TODO: Visual column arithmetic will be inaccurate as it include columns for inlays and folds
      if (leftVisualColumn > 0 && caretColumn < leftVisualColumn + sideScrollOffset) {
        newColumn = leftVisualColumn + sideScrollOffset
      } else if (caretColumn > rightVisualColumn - sideScrollOffset) {
        newColumn = rightVisualColumn - sideScrollOffset
      }
      if (newVisualLine == caretVisualLine && newColumn != caretColumn) {
        col = newColumn
      }
      newColumn = vimEditor.normalizeVisualColumn(newVisualLine, newColumn, editor.vim.isEndAllowed)
      if (newVisualLine != caretVisualLine || newColumn != oldColumn) {
        val offset = editor.visualPositionToOffset(VisualPosition(newVisualLine, newColumn))
        vimEditor.primaryCaret().moveToOffset(offset)
        editor.caretModel.primaryCaret.vimLastColumn = col
      }
    }

    fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
      ExEntryPanel.deactivateAll()
      val fileEditor = event.oldEditor
      if (fileEditor is TextEditor) {
        val editor = fileEditor.editor
        ExOutputModel.getInstance(editor).clear()
        editor.vim.let { vimEditor ->
          if (VimStateMachine.getInstance(vimEditor).mode === VimStateMachine.Mode.VISUAL) {
            vimEditor.exitVisualMode()
            KeyHandler.getInstance().reset(vimEditor)
          }
        }
      }
    }
  }
}
