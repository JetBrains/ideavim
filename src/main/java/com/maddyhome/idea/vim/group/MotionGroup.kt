/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.MathUtil
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.anyNonWhitespace
import com.maddyhome.idea.vim.api.getLeadingCharacterOffset
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeColumn
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.api.normalizeVisualColumn
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.Motion.AdjustedOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.listener.AppCodeTemplates.onMovement
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.newapi.IjExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.annotations.Range
import java.io.File

/**
 * This handles all motion related commands and marks
 */
class MotionGroup : VimMotionGroupBase() {
  override fun onAppCodeMovement(editor: VimEditor, caret: VimCaret, offset: Int, oldOffset: Int) {
    onMovement((editor as IjVimEditor).editor, (caret as IjVimCaret).caret, oldOffset < offset)
  }

  private fun selectEditor(editor: Editor, mark: Mark): Editor? {
    val virtualFile = markToVirtualFile(mark)
    return virtualFile?.let { selectEditor(editor, it) }
  }

  private fun markToVirtualFile(mark: Mark): VirtualFile? {
    val protocol = mark.protocol
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol)
    return fileSystem.findFileByPath(mark.filename)
  }

  private fun selectEditor(editor: Editor, file: VirtualFile): Editor? {
    return VimPlugin.getFile().selectEditor(editor.project, file)
  }

  override fun moveCaretToMatchingPair(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val pos =
      SearchHelper.findMatchingPairOnCurrentLine((editor as IjVimEditor).editor, (caret as IjVimCaret).caret)
    return if (pos >= 0) {
      pos
    } else {
      -1
    }
  }

  /**
   * This moves the caret to the start of the next/previous camel word.
   *
   * @param editor The editor to move in
   * @param caret  The caret to be moved
   * @param count  The number of words to skip
   * @return position
   */
  fun moveCaretToNextCamel(editor: Editor, caret: Caret, count: Int): Int {
    return if (caret.offset == 0 && count < 0 || caret.offset >= editor.fileSize - 1 && count > 0) {
      -1
    } else {
      SearchHelper.findNextCamelStart(editor, caret, count)
    }
  }

  /**
   * This moves the caret to the start of the next/previous camel word.
   *
   * @param editor The editor to move in
   * @param caret  The caret to be moved
   * @param count  The number of words to skip
   * @return position
   */
  fun moveCaretToNextCamelEnd(editor: Editor, caret: Caret, count: Int): Int {
    return if (caret.offset == 0 && count < 0 || caret.offset >= editor.fileSize - 1 && count > 0) {
      -1
    } else {
      SearchHelper.findNextCamelEnd(editor, caret, count)
    }
  }

  override fun moveCaretToFirstDisplayLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    normalizeToScreen: Boolean
  ): Int {
    return moveCaretToScreenLocation(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      ScreenLocation.TOP,
      count - 1,
      normalizeToScreen
    )
  }

  override fun moveCaretToLastDisplayLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    normalizeToScreen: Boolean
  ): Int {
    return moveCaretToScreenLocation(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      ScreenLocation.BOTTOM,
      count - 1,
      normalizeToScreen
    )
  }

  override fun moveCaretToMiddleDisplayLine(editor: VimEditor, caret: ImmutableVimCaret): Int {
    return moveCaretToScreenLocation(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      ScreenLocation.MIDDLE,
      0,
      false
    )
  }

  override fun moveCaretToFileMark(editor: VimEditor, ch: Char, toLineStart: Boolean): Int {
    val mark = VimPlugin.getMark().getFileMark(editor, ch) ?: return -1
    val line = mark.line
    return if (toLineStart) {
      moveCaretToLineStartSkipLeading(editor, line)
    } else {
      editor.bufferPositionToOffset(BufferPosition(line, mark.col, false))
    }
  }

  override fun moveCaretToMark(editor: VimEditor, ch: Char, toLineStart: Boolean): Int {
    val mark = VimPlugin.getMark().getMark(editor, ch) ?: return -1
    val vf = EditorHelper.getVirtualFile((editor as IjVimEditor).editor) ?: return -1
    val line = mark.line
    if (vf.path == mark.filename) {
      return if (toLineStart) {
        moveCaretToLineStartSkipLeading(editor, line)
      } else {
        editor.bufferPositionToOffset(BufferPosition(line, mark.col, false))
      }
    }
    val selectedEditor = selectEditor(editor.editor, mark)
    if (selectedEditor != null) {
      for (caret in selectedEditor.caretModel.allCarets) {
        IjVimCaret(caret!!).moveToOffset(
          if (toLineStart) {
            moveCaretToLineStartSkipLeading(IjVimEditor(selectedEditor), line)
          } else {
            selectedEditor.logicalPositionToOffset(LogicalPosition(line, mark.col))
          }
        )
      }
    }
    return -2
  }

  override fun moveCaretToJump(editor: VimEditor, count: Int): Int {
    val spot = VimPlugin.getMark().getJumpSpot()
    val (line, col, fileName) = VimPlugin.getMark().getJump(count) ?: return -1
    val vf = EditorHelper.getVirtualFile((editor as IjVimEditor).editor) ?: return -1
    val lp = BufferPosition(line, col, false)
    val lpnative = LogicalPosition(line, col, false)
    return if (vf.path != fileName) {
      val newFile = LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'))
        ?: return -2
      val newEditor = selectEditor(editor.editor, newFile)
      if (newEditor != null) {
        if (spot == -1) {
          VimPlugin.getMark().addJump(editor, false)
        }
        IjVimCaret(newEditor.caretModel.currentCaret).moveToOffset(
          IjVimEditor(newEditor).normalizeOffset(newEditor.logicalPositionToOffset(lpnative), false)
        )
      }
      -2
    } else {
      if (spot == -1) {
        VimPlugin.getMark().addJump(editor, false)
      }
      editor.bufferPositionToOffset(lp)
    }
  }

  override fun moveCaretToCurrentDisplayLineMiddle(editor: VimEditor, caret: ImmutableVimCaret): Motion {
    val width = EditorHelper.getApproximateScreenWidth((editor as IjVimEditor).editor) / 2
    val len = editor.lineLength(editor.currentCaret().getBufferPosition().line)
    return moveCaretToColumn(editor, caret, Math.max(0, Math.min(len - 1, width)), false)
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
    val col =
      EditorHelper.getVisualColumnAtLeftOfDisplay((editor as IjVimEditor).editor, caret.getVisualPosition().line)
    return moveCaretToColumn(editor, caret, col, false)
  }

  override fun moveCaretToCurrentDisplayLineStartSkipLeading(
    editor: VimEditor,
    caret: ImmutableVimCaret
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    val col =
      EditorHelper.getVisualColumnAtLeftOfDisplay((editor as IjVimEditor).editor, caret.getVisualPosition().line)
    val logicalLine = caret.getLine().line
    return editor.getLeadingCharacterOffset(logicalLine, col)
  }

  override fun moveCaretToCurrentDisplayLineEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    allowEnd: Boolean
  ): Motion {
    val col =
      EditorHelper.getVisualColumnAtRightOfDisplay((editor as IjVimEditor).editor, caret.getVisualPosition().line)
    return moveCaretToColumn(editor, caret, col, allowEnd)
  }

  override fun moveCaretToLineWithSameColumn(
    editor: VimEditor,
    logicalLine: Int,
    caret: ImmutableVimCaret
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    var col = (caret as IjVimCaret).caret.vimLastColumn
    var line = logicalLine
    if (logicalLine < 0) {
      line = 0
      col = 0
    } else if (logicalLine >= editor.lineCount()) {
      line = editor.normalizeLine(editor.lineCount() - 1)
      col = editor.lineLength(line)
    }
    val newPos = LogicalPosition(line, editor.normalizeColumn(line, col, false))
    return (editor as IjVimEditor).editor.logicalPositionToOffset(newPos)
  }

  override fun moveCaretToLineWithStartOfLineOption(
    editor: VimEditor,
    line: Int,
    caret: ImmutableVimCaret
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return if (VimPlugin.getOptionService().isSet(
        OptionScope.LOCAL(editor),
        OptionConstants.startoflineName,
        OptionConstants.startoflineName
      )
    ) {
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
        tabbedPane.selectedIndex = value
      } else {
        val tabIndex = (value + tabbedPane.selectedIndex) % tabbedPane.tabCount
        tabbedPane.selectedIndex = if (tabIndex < 0) tabIndex + tabbedPane.tabCount else tabIndex
      }
    }
  }

  override fun moveCaretGotoPreviousTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int {
    val project = (editor as IjVimEditor).editor.project ?: return editor.currentCaret().offset.point
    val currentWindow = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow
    switchEditorTab(currentWindow, if (rawCount >= 1) -rawCount else -1, false)
    return editor.currentCaret().offset.point
  }

  override fun moveCaretGotoNextTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int {
    val absolute = rawCount >= 1
    val project = (editor as IjVimEditor).editor.project ?: return editor.currentCaret().offset.point
    val currentWindow = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow
    switchEditorTab(currentWindow, if (absolute) rawCount - 1 else 1, absolute)
    return editor.currentCaret().offset.point
  }

  override fun moveCaretToLinePercent(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return moveCaretToLineWithStartOfLineOption(
      editor,
      editor.normalizeLine((editor.lineCount() * MathUtil.clamp(count, 0, 100) + 99) / 100 - 1),
      caret
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
    normalizeToScreen: Boolean
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    val scrollOffset = if (normalizeToScreen) getNormalizedScrollOffset(editor) else 0
    val editor1: VimEditor = IjVimEditor(editor)
    val maxVisualLine = editor1.getVisualLineCount()
    val topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
    val topScrollOff = if (topVisualLine > 0) scrollOffset else 0
    val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
    val bottomScrollOff = if (bottomVisualLine < maxVisualLine - 1) scrollOffset else 0
    val topMaxVisualLine = if (normalizeToScreen) bottomVisualLine - bottomScrollOff else maxVisualLine
    val bottomMinVisualLine = if (normalizeToScreen) topVisualLine + topScrollOff else 0
    var targetVisualLine = 0
    targetVisualLine = when (screenLocation) {
      ScreenLocation.TOP -> Math.min(topVisualLine + Math.max(topScrollOff, visualLineOffset), topMaxVisualLine)
      ScreenLocation.MIDDLE -> EditorHelper.getVisualLineAtMiddleOfScreen(editor)
      ScreenLocation.BOTTOM -> Math.max(
        bottomVisualLine - Math.max(bottomScrollOff, visualLineOffset),
        bottomMinVisualLine
      )
    }
    val targetLogicalLine = IjVimEditor(editor).visualLineToBufferLine(targetVisualLine)
    return moveCaretToLineWithStartOfLineOption(IjVimEditor(editor), targetLogicalLine, IjVimCaret(caret))
  }

  companion object {
    @JvmStatic
    fun getMotionRange2(
      editor: Editor,
      caret: Caret,
      context: DataContext?,
      argument: Argument,
      operatorArguments: OperatorArguments
    ): TextRange? {
      var start: Int
      var end: Int
      if (argument.type === Argument.Type.OFFSETS) {
        val offsets = argument.offsets[IjVimCaret(caret)] ?: return null
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
            IjVimEditor(editor),
            IjVimCaret(caret),
            IjExecutionContext(context!!),
            cmd.argument,
            operatorArguments.withCount0(raw)
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
          val range = action.getRange(
            IjVimEditor(editor),
            IjVimCaret(caret),
            IjExecutionContext(context!!),
            cnt,
            raw,
            cmd.argument
          ) ?: return null
          start = range.startOffset
          end = range.endOffset
          if (cmd.isLinewiseMotion()) end--
        } else {
          throw RuntimeException(
            "Commands doesn't take " + cmd.action.javaClass.simpleName + " as an operator"
          )
        }
      }

      // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
      val id = argument.motion.action.id
      if (id == VimChangeGroupBase.VIM_MOTION_WORD_RIGHT || id == VimChangeGroupBase.VIM_MOTION_BIG_WORD_RIGHT || id == VimChangeGroupBase.VIM_MOTION_CAMEL_RIGHT) {
        val text = editor.document.charsSequence.subSequence(start, end).toString()
        val lastNewLine = text.lastIndexOf('\n')
        if (lastNewLine > 0) {
          if (!IjVimEditor(editor).anyNonWhitespace(end, -1)) {
            end = start + lastNewLine
          }
        }
      }
      return TextRange(start, end)
    }

    fun moveCaretToView(editor: Editor) {
      val scrollOffset = getNormalizedScrollOffset(editor)
      val topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor)
      val bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      val caretVisualLine = editor.caretModel.visualPosition.line
      val editor1: VimEditor = IjVimEditor(editor)
      val lastVisualLine = editor1.getVisualLineCount() - 1
      val newVisualLine: Int
      newVisualLine = if (caretVisualLine < topVisualLine + scrollOffset) {
        IjVimEditor(editor).normalizeVisualLine(topVisualLine + scrollOffset)
      } else if (bottomVisualLine < lastVisualLine && caretVisualLine > bottomVisualLine - scrollOffset) {
        IjVimEditor(editor).normalizeVisualLine(bottomVisualLine - scrollOffset)
      } else {
        caretVisualLine
      }
      val sideScrollOffset = getNormalizedSideScrollOffset(editor)
      val oldColumn = editor.caretModel.visualPosition.column
      var col = oldColumn
      if (col >= IjVimEditor(editor).lineLength(IjVimEditor(editor).currentCaret().getBufferPosition().line) - 1) {
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
      newColumn = IjVimEditor(editor).normalizeVisualColumn(newVisualLine, newColumn, editor.isEndAllowed)
      if (newVisualLine != caretVisualLine || newColumn != oldColumn) {
        val offset = editor.visualPositionToOffset(VisualPosition(newVisualLine, newColumn))
        IjVimCaret(editor.caretModel.primaryCaret).moveToOffset(offset)
        editor.caretModel.primaryCaret.vimLastColumn = col
      }
    }

    private fun getNormalizedScrollOffset(editor: Editor): Int {
      val scrollOffset = (VimPlugin.getOptionService().getOptionValue(
        OptionScope.LOCAL(IjVimEditor(editor)),
        OptionConstants.scrolloffName,
        OptionConstants.scrolloffName
      ) as VimInt).value
      return EditorHelper.normalizeScrollOffset(editor, scrollOffset)
    }

    private fun getNormalizedSideScrollOffset(editor: Editor): Int {
      val sideScrollOffset = (VimPlugin.getOptionService().getOptionValue(
        OptionScope.LOCAL(IjVimEditor(editor)),
        OptionConstants.sidescrolloffName,
        OptionConstants.sidescrolloffName
      ) as VimInt).value
      return EditorHelper.normalizeSideScrollOffset(editor, sideScrollOffset)
    }

    fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
      ExEntryPanel.deactivateAll()
      val fileEditor = event.oldEditor
      if (fileEditor is TextEditor) {
        val editor = fileEditor.editor
        getInstance(editor).clear()
        if (getInstance(IjVimEditor(editor)).mode === VimStateMachine.Mode.VISUAL) {
          IjVimEditor(editor).exitVisualMode()
          getInstance().reset(IjVimEditor(editor))
        }
      }
    }
  }
}