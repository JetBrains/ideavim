/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.codeInsight.actions.AsyncActionExecutionService.Companion.getInstance
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.impl.TextRangeInterval
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.containers.ContainerUtil
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.anyNonWhitespace
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.IndentConfig.Companion.create
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.MotionGroup.Companion.getMotionRange2
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.commandContinuation
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.changeCase
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.NumberType
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.moveToInlayAwareLogicalPosition
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.key.KeyHandlerKeeper.Companion.getInstance
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.vimscript.model.commands.SortOption
import org.jetbrains.annotations.TestOnly
import java.math.BigInteger
import java.util.*
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

/**
 * Provides all the insert/replace related functionality
 */
public class ChangeGroup : VimChangeGroupBase() {
  private val insertListeners = ContainerUtil.createLockFreeCopyOnWriteList<VimInsertListener>()
  private val listener: EditorMouseListener = object : EditorMouseListener {
    override fun mouseClicked(event: EditorMouseEvent) {
      val editor = event.editor
      if (editor.inInsertMode) {
        clearStrokes(IjVimEditor(editor))
      }
    }
  }

  public fun editorCreated(editor: Editor?, disposable: Disposable) {
    EventFacade.getInstance().addEditorMouseListener(editor!!, listener, disposable)
  }

  override fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char) {
    val editor = (vimEditor as IjVimEditor).editor
    val ijContext = context.ij
    val doc = vimEditor.editor.document
    CommandProcessor.getInstance().executeCommand(
      editor.project, {
        ApplicationManager.getApplication()
          .runWriteAction { getInstance().originalHandler.execute(editor, key, ijContext) }
      }, "", doc,
      UndoConfirmationPolicy.DEFAULT, doc
    )
    injector.scroll.scrollCaretIntoView(vimEditor)
  }

  /**
   * If this is REPLACE mode we need to turn off OVERWRITE before and then turn OVERWRITE back on after sending the
   * "ENTER" key.
   */
  override fun processEnter(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
  ) {
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = true
    }
    try {
      val continuation = (context.context as UserDataHolder).getUserData(commandContinuation)
      val ijEditor = editor.ij
      val ij = context.ij
      val ijCaret = caret.ij
      if (continuation != null) {
        continuation.execute(ijEditor, ijCaret, ij)
      } else {
        EnterAction().handler.execute(ijEditor, ijCaret, ij)
      }
    } finally {
      if (editor.mode is Mode.REPLACE) {
        editor.insertMode = false
      }
    }
  }

  override fun getDeleteRangeAndType2(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Pair<TextRange, SelectionType>? {
    val range = getMotionRange2(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      (context as IjEditorExecutionContext).context,
      argument,
      operatorArguments
    )
      ?: return null

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    var type: SelectionType
    type = if (argument.motion.isLinewiseMotion()) {
      SelectionType.LINE_WISE
    } else {
      SelectionType.CHARACTER_WISE
    }
    val motion = argument.motion
    if (!isChange && !motion.isLinewiseMotion()) {
      val start = editor.offsetToBufferPosition(range.startOffset)
      val end = editor.offsetToBufferPosition(range.endOffset)
      if (start.line != end.line) {
        val offset1 = range.startOffset
        if (!editor.anyNonWhitespace(offset1, -1)) {
          val offset = range.endOffset
          if (!editor.anyNonWhitespace(offset, 1)) {
            type = SelectionType.LINE_WISE
          }
        }
      }
    }
    return Pair(range, type)
  }

  /**
   * Toggles the case of count characters
   *
   * @param editor The editor to change
   * @param caret  The caret on which the operation is performed
   * @param count  The number of characters to change
   * @return true if able to change count characters
   */
  override fun changeCaseToggleCharacter(editor: VimEditor, caret: VimCaret, count: Int): Boolean {
    val allowWrap = injector.options(editor).whichwrap.contains("~")
    var motion = injector.motion.getHorizontalMotion(editor, caret, count, true, allowWrap)
    if (motion is Motion.Error) return false
    changeCase(editor, caret, caret.offset.point, (motion as AbsoluteOffset).offset, CharacterHelper.CASE_TOGGLE)
    motion = injector.motion.getHorizontalMotion(
      editor,
      caret,
      count,
      false,
      allowWrap
    ) // same but without allow end because we can change till end, but can't move caret there
    if (motion is AbsoluteOffset) {
      caret.moveToOffset(editor.normalizeOffset(motion.offset, false))
    }
    return true
  }

  override fun blockInsert(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    append: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val lines = getLinesCountInVisualBlock(editor, range)
    val startPosition = editor.offsetToBufferPosition(range.startOffset)
    val mode = operatorArguments.mode
    val visualBlockMode = mode is VISUAL && mode.selectionType === SelectionType.BLOCK_WISE
    for (caret in editor.carets()) {
      val line = startPosition.line
      var column = startPosition.column
      if (!visualBlockMode) {
        column = 0
      } else if (append) {
        column += range.maxLength
        if (caret.vimLastColumn == VimMotionGroupBase.LAST_COLUMN) {
          column = VimMotionGroupBase.LAST_COLUMN
        }
      }
      val lineLength = editor.lineLength(line)
      if (column < VimMotionGroupBase.LAST_COLUMN && lineLength < column) {
        val pad =
          EditorHelper.pad((editor as IjVimEditor).editor, (context as IjEditorExecutionContext).context, line, column)
        val offset = editor.getLineEndOffset(line)
        insertText(editor, caret, offset, pad)
      }
      if (visualBlockMode || !append) {
        (caret as IjVimCaret).caret.moveToInlayAwareLogicalPosition(LogicalPosition(line, column))
      }
      if (visualBlockMode) {
        setInsertRepeat(lines, column, append)
      }
    }
    if (visualBlockMode || !append) {
      insertBeforeCursor(editor, context)
    } else {
      insertAfterCursor(editor, context)
    }
    return true
  }

  /**
   * Changes the case of all the characters in the range
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param range  The range to change
   * @param type   The case change type (TOGGLE, UPPER, LOWER)
   * @return true if able to delete the text, false if not
   */
  override fun changeCaseRange(editor: VimEditor, caret: VimCaret, range: TextRange, type: Char): Boolean {
    val starts = range.startOffsets
    val ends = range.endOffsets
    for (i in ends.indices.reversed()) {
      changeCase(editor, caret, starts[i], ends[i], type)
    }
    caret.moveToOffset(range.startOffset)
    return true
  }

  /**
   * This performs the actual case change.
   *
   * @param editor The editor to change
   * @param start  The start offset to change
   * @param end    The end offset to change
   * @param type   The type of change (TOGGLE, UPPER, LOWER)
   */
  private fun changeCase(editor: VimEditor, caret: VimCaret, start: Int, end: Int, type: Char) {
    var start = start
    var end = end
    if (start > end) {
      val t = end
      end = start
      start = t
    }
    end = editor.normalizeOffset(end, true)
    val chars = editor.text()
    val sb = StringBuilder()
    for (i in start until end) {
      sb.append(changeCase(chars[i], type))
    }
    replaceText(editor, caret, start, end, sb.toString())
  }

  private fun restoreCursor(editor: VimEditor, caret: VimCaret, startLine: Int) {
    if (caret != editor.primaryCaret()) {
      (editor as IjVimEditor).editor.caretModel.addCaret(
        editor.editor.offsetToVisualPosition(injector.motion.moveCaretToLineStartSkipLeading(editor, startLine)), false
      )
    }
  }

  /**
   * Changes the case of all the character moved over by the motion argument.
   *
   * @param editor   The editor to change
   * @param caret    The caret on which motion pretends to be performed
   * @param context  The data context
   * @param type     The case change type (TOGGLE, UPPER, LOWER)
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  override fun changeCaseMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext?,
    type: Char,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val range = injector.motion.getMotionRange(
      editor, caret, context!!, argument,
      operatorArguments
    )
    return range != null && changeCaseRange(editor, caret, range, type)
  }

  override fun reformatCodeMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val range = injector.motion.getMotionRange(
      editor, caret, context, argument,
      operatorArguments
    )
    return range != null && reformatCodeRange(editor, caret, range)
  }

  override fun reformatCodeSelection(editor: VimEditor, caret: VimCaret, range: VimSelection) {
    val textRange = range.toVimTextRange(true)
    reformatCodeRange(editor, caret, textRange)
  }

  private fun reformatCodeRange(editor: VimEditor, caret: VimCaret, range: TextRange): Boolean {
    val starts = range.startOffsets
    val ends = range.endOffsets
    val firstLine = editor.offsetToBufferPosition(range.startOffset).line
    for (i in ends.indices.reversed()) {
      val startOffset = editor.getLineStartForOffset(starts[i])
      val offset = ends[i] - if (startOffset == ends[i]) 0 else 1
      val endOffset = editor.getLineEndForOffset(offset)
      reformatCode(editor, startOffset, endOffset)
    }
    val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
    caret.moveToOffset(newOffset)
    return true
  }

  private fun reformatCode(editor: VimEditor, start: Int, end: Int) {
    val project = (editor as IjVimEditor).editor.project ?: return
    val file = PsiUtilBase.getPsiFileInEditor(editor.editor, project) ?: return
    val textRange = com.intellij.openapi.util.TextRange.create(start, end)
    CodeStyleManager.getInstance(project).reformatText(file, listOf(textRange))
  }

  override fun autoIndentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ) {
    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments)
    if (range != null) {
      autoIndentRange(
        editor, caret, context,
        TextRange(range.startOffset, range.endOffsetInclusive)
      )
    }
  }

  override fun autoIndentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
  ) {
    val startOffset = editor.getLineStartForOffset(range.startOffset)
    val endOffset = editor.getLineEndForOffset(range.endOffset)
    val ijEditor = (editor as IjVimEditor).editor

    // FIXME: Here we do selection, and it is not a good idea, because it updates primary selection in Linux
    // FIXME: I'll leave here a dirty fix that restores primary selection, but it would be better to rewrite this method
    var primaryTextAndTransferableData: Pair<String, List<Any>?>? = null
    try {
      if (injector.registerGroup.isPrimaryRegisterSupported()) {
        primaryTextAndTransferableData = injector.clipboardManager.getPrimaryTextAndTransferableData()
      }
    } catch (e: Exception) {
      // FIXME: [isPrimaryRegisterSupported()] is not implemented perfectly, so there might be thrown an exception after trying to access the primary selection
      logger.warn("False positive X11 primary selection support")
    }
    ijEditor.selectionModel.vimSetSystemSelectionSilently(startOffset, endOffset)
    val project = ijEditor.project
    val actionExecution = {
      val joinLinesAction = injector.nativeActionManager.indentLines
      if (joinLinesAction != null) {
        injector.actionExecutor.executeAction(editor, joinLinesAction, context)
      }
    }
    val afterAction = {
      val firstLine = editor.offsetToBufferPosition(
        min(startOffset.toDouble(), endOffset.toDouble()).toInt()
      ).line
      val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
      caret.moveToOffset(newOffset)
      restoreCursor(editor, caret, (caret as IjVimCaret).caret.logicalPosition.line)
    }
    if (project != null) {
      getInstance(project)
        .withExecutionAfterAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES, actionExecution, afterAction)
    } else {
      actionExecution.invoke()
      afterAction.invoke()
    }
    try {
      if (primaryTextAndTransferableData != null) {
        injector.clipboardManager.setPrimaryText(
          primaryTextAndTransferableData.first,
          primaryTextAndTransferableData.first,
          primaryTextAndTransferableData.second ?: emptyList()
        )
      }
    } catch (e: Exception) {
      // FIXME: [isPrimaryRegisterSupported()] is not implemented perfectly, so there might be thrown an exception after trying to access the primary selection
    }
  }

  override fun indentLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    lines: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    val start = caret.offset.point
    val end = injector.motion.moveCaretToRelativeLineEnd(editor, caret, lines - 1, true)
    indentRange(editor, caret, context, TextRange(start, end), 1, dir, operatorArguments)
  }

  override fun indentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments)
    if (range != null) {
      indentRange(editor, caret, context, range, 1, dir, operatorArguments)
    }
  }

  override fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    if (logger.isDebugEnabled) {
      logger.debug("count=$count")
    }

    // Remember the current caret column
    val intendedColumn = caret.vimLastColumn
    val indentConfig = create((editor as IjVimEditor).editor, (context as IjEditorExecutionContext).context)
    val sline = editor.offsetToBufferPosition(range.startOffset).line
    val endLogicalPosition = editor.offsetToBufferPosition(range.endOffset)
    val eline = if (endLogicalPosition.column == 0) max((endLogicalPosition.line - 1).toDouble(), 0.0)
      .toInt() else endLogicalPosition.line
    if (range.isMultiple) {
      val from = editor.offsetToBufferPosition(range.startOffset).column
      if (dir == 1) {
        // Right shift blockwise selection
        val indent = indentConfig.createIndentByCount(count)
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            insertText(editor, caret, spos, indent)
          }
        }
      } else {
        // Left shift blockwise selection
        val chars = editor.text()
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            val epos = BufferPosition(l, from + indentConfig.getTotalIndent(count) - 1, false)
            val wsoff = editor.bufferPositionToOffset(spos)
            val weoff = editor.bufferPositionToOffset(epos)
            var pos: Int
            pos = wsoff
            while (pos <= weoff) {
              if (charType(editor, chars[pos], false) !== CharacterHelper.CharacterType.WHITESPACE) {
                break
              }
              pos++
            }
            if (pos > wsoff) {
              deleteText(editor, TextRange(wsoff, pos), null, caret, operatorArguments, true)
            }
          }
        }
      }
    } else {
      // Shift non-blockwise selection
      for (l in sline..eline) {
        val soff = editor.getLineStartOffset(l)
        val eoff = editor.getLineEndOffset(l, true)
        val woff = injector.motion.moveCaretToLineStartSkipLeading(editor, l)
        val col = editor.offsetToBufferPosition(woff).column
        val limit = max(0.0, (col + dir * indentConfig.getTotalIndent(count)).toDouble())
          .toInt()
        if (col > 0 || soff != eoff) {
          val indent = indentConfig.createIndentBySize(limit)
          replaceText(editor, caret, soff, woff, indent)
        }
      }
    }
    if (!editor.editor.inInsertMode) {
      if (!range.isMultiple) {
        // The caret has moved, so reset the intended column before trying to get the expected offset
        val newCaret = caret.setVimLastColumnAndGetCaret(intendedColumn)
        val offset = injector.motion.moveCaretToLineWithStartOfLineOption(editor, sline, caret)
        newCaret.moveToOffset(offset)
      } else {
        caret.moveToOffset(range.startOffset)
      }
    }
  }

  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @param sortOptions     The option to sort the range
   * @return true if able to sort the text, false if not
   */
  override fun sortRange(
    editor: VimEditor, caret: VimCaret, range: LineRange, lineComparator: Comparator<String>,
    sortOptions: SortOption,
  ): Boolean {
    val startLine = range.startLine
    val endLine = range.endLine
    val count = endLine - startLine + 1
    if (count < 2) {
      return false
    }
    val startOffset = editor.getLineStartOffset(startLine)
    val endOffset = editor.getLineEndOffset(endLine)
    return sortTextRange(editor, caret, startOffset, endOffset, lineComparator, sortOptions)
  }

  /**
   * Sorts a text range with a comparator. Returns true if a replace was performed, false otherwise.
   *
   * @param editor         The editor to replace text in
   * @param start          The starting position for the sort
   * @param end            The ending position for the sort
   * @param lineComparator The comparator to use to sort
   * @param sortOption     The option to sort the range
   * @return true if able to sort the text, false if not
   */
  private fun sortTextRange(
    editor: VimEditor,
    caret: VimCaret,
    start: Int,
    end: Int,
    lineComparator: Comparator<String>,
    sortOption: SortOption,
  ): Boolean {
    val selectedText = (editor as IjVimEditor).editor.document.getText(TextRangeInterval(start, end))
    val lines: MutableList<String> = selectedText.split("\n").sortedWith(lineComparator).toMutableList()
    if (sortOption.unique) {
      val iterator = lines.iterator()
      var previous: String? = null
      while (iterator.hasNext()) {
        val current = iterator.next()
        if (current == previous || sortOption.ignoreCase && current.equals(previous, ignoreCase = true)) {
          iterator.remove()
        } else {
          previous = current
        }
      }
    }
    if (lines.size < 1) {
      return false
    }
    replaceText(editor, caret, start, end, StringUtil.join(lines, "\n"))
    return true
  }

  /**
   * Perform increment and decrement for numbers in visual mode
   *
   *
   * Flag [avalanche] marks if increment (or decrement) should be performed in avalanche mode
   * (for v_g_Ctrl-A and v_g_Ctrl-X commands)
   *
   * @return true
   */
  override fun changeNumberVisualMode(
    editor: VimEditor,
    caret: VimCaret,
    selectedRange: TextRange,
    count: Int,
    avalanche: Boolean,
  ): Boolean {

    val nf: List<String> = injector.options(editor).nrformats
    val alpha = nf.contains("alpha")
    val hex = nf.contains("hex")
    val octal = nf.contains("octal")
    val numberRanges = SearchHelper.findNumbersInRange((editor as IjVimEditor).editor, selectedRange, alpha, hex, octal)
    val newNumbers: MutableList<String?> = ArrayList()
    for (i in numberRanges.indices) {
      val numberRange = numberRanges[i]
      val iCount = if (avalanche) (i + 1) * count else count
      val newNumber = changeNumberInRange(editor, numberRange, iCount, alpha, hex, octal)
      newNumbers.add(newNumber)
    }
    for (i in newNumbers.indices.reversed()) {
      // Replace text bottom up. In other direction ranges will be desynchronized after inc numbers like 99
      val (first) = numberRanges[i]
      val newNumber = newNumbers[i]
      replaceText(editor, caret, first.startOffset, first.endOffset, newNumber!!)
    }
    (caret as IjVimCaret).caret.moveToInlayAwareOffset(selectedRange.startOffset)
    return true
  }

  override fun changeNumber(editor: VimEditor, caret: VimCaret, count: Int): Boolean {
    val nf: List<String> = injector.options(editor).nrformats
    val alpha = nf.contains("alpha")
    val hex = nf.contains("hex")
    val octal = nf.contains("octal")
    val range =
      SearchHelper.findNumberUnderCursor((editor as IjVimEditor).editor, (caret as IjVimCaret).caret, alpha, hex, octal)
    if (range == null) {
      logger.debug("no number on line")
      return false
    }
    val newNumber = changeNumberInRange(editor, range, count, alpha, hex, octal)
    return if (newNumber == null) {
      false
    } else {
      replaceText(editor, caret, range.first.startOffset, range.first.endOffset, newNumber)
      caret.caret.moveToInlayAwareOffset(range.first.startOffset + newNumber.length - 1)
      true
    }
  }

  override fun reset() {
    strokes.clear()
    repeatCharsCount = 0
    if (lastStrokes != null) {
      lastStrokes!!.clear()
    }
  }

  override fun saveStrokes(newStrokes: String?) {
    val chars = newStrokes!!.toCharArray()
    strokes.add(chars)
  }

  private fun changeNumberInRange(
    editor: VimEditor,
    range: Pair<TextRange, NumberType>,
    count: Int,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
  ): String? {
    val text = editor.getText(range.first)
    val numberType = range.second
    if (logger.isDebugEnabled) {
      logger.debug("found range $range")
      logger.debug("text=$text")
    }
    var number = text
    if (text.isEmpty()) {
      return null
    }
    var ch = text[0]
    if (hex && NumberType.HEX == numberType) {
      if (!text.lowercase(Locale.getDefault()).startsWith(HEX_START)) {
        throw RuntimeException("Hex number should start with 0x: $text")
      }
      for (i in text.length - 1 downTo 2) {
        val index = "abcdefABCDEF".indexOf(text[i])
        if (index >= 0) {
          lastLower = index < 6
          break
        }
      }
      var num = BigInteger(text.substring(2), 16)
      num = num.add(BigInteger.valueOf(count.toLong()))
      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = BigInteger(MAX_HEX_INTEGER, 16).add(BigInteger.ONE).add(num)
      }
      number = num.toString(16)
      number = number.padStart(text.length - 2, '0')
      if (!lastLower) {
        number = number.uppercase(Locale.getDefault())
      }
      number = text.substring(0, 2) + number
    } else if (octal && NumberType.OCT == numberType && text.length > 1) {
      if (!text.startsWith("0")) throw RuntimeException("Oct number should start with 0: $text")
      var num = BigInteger(text, 8).add(BigInteger.valueOf(count.toLong()))
      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = BigInteger("1777777777777777777777", 8).add(BigInteger.ONE).add(num)
      }
      number = num.toString(8)
      number = "0" + number.padStart(text.length - 1, '0')
    } else if (alpha && NumberType.ALPHA == numberType) {
      if (!Character.isLetter(ch)) throw RuntimeException("Not alpha number : $text")
      ch += count.toChar().code
      if (Character.isLetter(ch)) {
        number = ch.toString()
      }
    } else if (NumberType.DEC == numberType) {
      if (ch != '-' && !Character.isDigit(ch)) throw RuntimeException("Not dec number : $text")
      var pad = ch == '0'
      var len = text.length
      if (ch == '-' && text[1] == '0') {
        pad = true
        len--
      }
      var num = BigInteger(text)
      num = num.add(BigInteger.valueOf(count.toLong()))
      number = num.toString()
      if (!octal && pad) {
        var neg = false
        if (number[0] == '-') {
          neg = true
          number = number.substring(1)
        }
        number = number.padStart(len, '0')
        if (neg) {
          number = "-$number"
        }
      }
    }
    return number
  }

  public fun addInsertListener(listener: VimInsertListener) {
    insertListeners.add(listener)
  }

  public fun removeInsertListener(listener: VimInsertListener) {
    insertListeners.remove(listener)
  }

  override fun notifyListeners(editor: VimEditor) {
    insertListeners.forEach(Consumer { listener: VimInsertListener -> listener.insertModeStarted((editor as IjVimEditor).editor) })
  }

  @TestOnly
  override fun resetRepeat() {
    setInsertRepeat(0, 0, false)
  }

  private companion object {
    private val logger = logger<ChangeGroup>()
  }
}
