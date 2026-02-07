/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.isLineEmpty
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.moveToMotion
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.api.setVisualSelectionMarks
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.isBlock
import com.maddyhome.idea.vim.state.mode.isChar
import com.maddyhome.idea.vim.state.mode.isLine
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class VimPutBase : VimPut {
  @VimLockLabel.SelfSynchronized
  override fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    updateVisualMarks: Boolean,
    saveToRegister: Boolean,
  ): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    deleteSelectedText(editor, context, data, saveToRegister)
    val processedText = processText(null, data) ?: return false
    putTextAndSetCaretPosition(editor, context, processedText, data, additionalData)

    if (updateVisualMarks) {
      wrapInsertedTextWithVisualMarks(editor.currentCaret(), data)
    }

    return true
  }

  private fun collectPreModificationData(editor: VimEditor, data: PutData): Map<String, Any> {
    return if (data.visualSelection != null && data.visualSelection.typeInEditor.isBlock) {
      val vimSelection = data.visualSelection.caretsAndSelections.getValue(editor.primaryCaret())
      val selStart = editor.offsetToBufferPosition(vimSelection.vimStart)
      val selEnd = editor.offsetToBufferPosition(vimSelection.vimEnd)
      mapOf(
        "startColumnOfSelection" to min(selStart.column, selEnd.column),
        "selectedLines" to abs(selStart.line - selEnd.line),
        "firstSelectedLine" to min(selStart.line, selEnd.line),
      )
    } else {
      mutableMapOf()
    }
  }

  /**
   * see ":h gv":
   * After using "p" or "P" in Visual mode the text that was put will be selected
   */
  private fun wrapInsertedTextWithVisualMarks(caret: VimCaret, data: PutData) {
    val textLength: Int = data.textData?.rawText?.length ?: return
    val caretsAndSelections = data.visualSelection?.caretsAndSelections ?: return
    val selection = caretsAndSelections[caret] ?: caretsAndSelections.entries.firstOrNull()?.value ?: return

    val leftIndex = min(selection.vimStart, selection.vimEnd)
    val rightIndex = leftIndex + textLength - 1
    val rangeForMarks = TextRange(leftIndex, rightIndex)

    injector.markService.setVisualSelectionMarks(caret, rangeForMarks)
  }

  @VimLockLabel.SelfSynchronized
  private fun deleteSelectedText(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    data: PutData,
    saveToRegister: Boolean,
  ): VimCaret? {
    if (data.visualSelection == null) return null
    if (!caret.isValid) return null

    // TODO [vakhitov] what if it is ImmutableCaret and it was changed from the one added to [caretsAndSelections]? We should introduce caret ids that won't change
    val selectionForCaret = data.visualSelection.caretsAndSelections[caret] ?: return null

    val range = selectionForCaret.toVimTextRange(false).normalize()
    injector.application.runWriteAction {
      injector.changeGroup.deleteRange(editor, context, caret, range, selectionForCaret.type, false, saveToRegister)
    }
    return caret.moveToInlayAwareOffset(range.startOffset)
  }

  @VimLockLabel.SelfSynchronized
  private fun deleteSelectedText(editor: VimEditor, context: ExecutionContext, data: PutData, saveToRegister: Boolean) {
    if (data.visualSelection == null) return

    data.visualSelection.caretsAndSelections.entries.sortedByDescending { it.key.getBufferPosition() }
      .forEach { (caret, selection) ->
        if (!caret.isValid) return@forEach
        val range = selection.toVimTextRange(false).normalize()

        injector.application.runWriteAction {
          injector.changeGroup.deleteRange(editor, context, caret, range, selection.type, false, saveToRegister)
        }
        caret.moveToInlayAwareOffset(range.startOffset)
      }
  }

  private fun processText(caret: VimCaret?, data: PutData): ProcessedTextData? {
    var text = data.textData?.rawText ?: run {
      if (caret == null) return null
      if (data.visualSelection != null) {
        val offset = caret.offset
        injector.markService.setMark(caret, MARK_CHANGE_POS, offset)
        injector.markService.setChangeMarks(caret, TextRange(offset, offset + 1))
      }
      return null
    }

    if (data.visualSelection?.typeInEditor?.isLine == true && data.textData.typeInRegister.isChar) text += "\n"

    // TODO: shouldn't it be adjusted when we are storing the text?
    if (data.textData.typeInRegister.isLine && text.isNotEmpty() && text.last() != '\n') text += '\n'

    if (data.textData.typeInRegister.isChar && text.lastOrNull() == '\n' && data.visualSelection?.typeInEditor?.isLine == false) {
      text =
        text.dropLast(1)
    }

    return ProcessedTextData(
      data.textData.registerChar,
      data.textData.copiedText.updateText(text),
      data.textData.typeInRegister,
    )
  }

  protected fun moveCaretToEndPosition(
    editor: VimEditor,
    caret: VimCaret,
    startOffset: Int,
    endOffset: Int,
    typeInRegister: SelectionType,
    modeInEditor: SelectionType,
    caretAfterInsertedText: Boolean,
  ): VimCaret {
    val cursorMode = when (typeInRegister) {
      SelectionType.BLOCK_WISE -> when (modeInEditor) {
        SelectionType.LINE_WISE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "startOffset"
      }

      SelectionType.LINE_WISE -> if (caretAfterInsertedText) "postEndOffset" else "startOffsetSkipLeading"
      SelectionType.CHARACTER_WISE -> when (modeInEditor) {
        SelectionType.LINE_WISE -> if (caretAfterInsertedText) "postEndOffset" else "startOffset"
        else -> if (caretAfterInsertedText) "preLineEndOfEndOffset" else "preEndOffset"
      }
    }

    return when (cursorMode) {
      "startOffset" -> caret.moveToOffset(startOffset)
      "preEndOffset" -> caret.moveToOffset(endOffset - 1)
      "startOffsetSkipLeading" -> {
        val updated = caret.moveToOffset(startOffset)
        updated.moveToOffset(injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, updated))
      }

      "postEndOffset" -> caret.moveToOffset(endOffset + 1)
      "preLineEndOfEndOffset" -> {
        var rightestPosition = editor.getLineEndForOffset(endOffset - 1)
        if (editor.mode !is Mode.INSERT) --rightestPosition // it's not possible to place a caret at the end of the line in any mode except insert
        val pos = min(endOffset, rightestPosition)
        caret.moveToOffset(pos)
      }

      else -> error("Unexpected mode")
    }
  }

  override fun doIndent(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    startOffset: Int,
    endOffset: Int,
  ): Int {
    TODO("Not yet implemented")
  }

  private fun putTextCharacterwise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: SelectionType,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Pair<Int, VimCaret> {
    var updatedCaret = caret.moveToOffset(startOffset)
    val insertedText = text.repeat(count)
    updatedCaret = injector.changeGroup.insertText(editor, updatedCaret, insertedText)

    val endOffset = if (indent) {
      doIndent(editor, updatedCaret, context, startOffset, startOffset + insertedText.length)
    } else {
      startOffset + insertedText.length
    }
    updatedCaret = moveCaretToEndPosition(editor, updatedCaret, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset to updatedCaret
  }

  private fun putTextLinewise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: SelectionType,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Pair<Int, VimCaret> {
    val overlappedCarets = ArrayList<VimCaret>(editor.carets().size)
    for (possiblyOverlappedCaret in editor.carets()) {
      if (possiblyOverlappedCaret.offset != startOffset || possiblyOverlappedCaret == caret) continue

      val updated = possiblyOverlappedCaret.moveToMotion(
        injector.motion.getHorizontalMotion(editor, possiblyOverlappedCaret, 1, true),
      )
      overlappedCarets.add(updated)
    }

    val endOffset = putTextCharacterwise(
      editor, caret, context, text, type, mode, startOffset, count, indent,
      cursorAfter,
    )

    for (overlappedCaret in overlappedCarets) {
      overlappedCaret.moveToMotion(
        injector.motion.getHorizontalMotion(editor, overlappedCaret, -1, true),
      )
    }

    return endOffset
  }

  private fun getMaxSegmentLength(text: String): Int {
    val tokenizer = StringTokenizer(text, "\n")
    var maxLen = 0
    while (tokenizer.hasMoreTokens()) {
      val s = tokenizer.nextToken()
      maxLen = max(s.length, maxLen)
    }
    return maxLen
  }

  private fun putTextBlockwise(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: SelectionType,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Pair<Int, VimCaret> {
    val startPosition = editor.offsetToBufferPosition(startOffset)
    val currentColumn = if (mode == SelectionType.LINE_WISE) 0 else startPosition.column
    var currentLine = startPosition.line

    val lineCount = text.getLineBreakCount() + 1
    var updated = caret
    if (currentLine + lineCount >= editor.nativeLineCount()) {
      val limit = currentLine + lineCount - editor.nativeLineCount()
      repeat(limit) {
        updated = updated.moveToOffset(editor.fileSize().toInt())
        updated = injector.changeGroup.insertText(editor, updated, "\n")
      }
    }

    val maxLen = getMaxSegmentLength(text)
    val tokenizer = StringTokenizer(text, "\n")
    var endOffset = startOffset
    while (tokenizer.hasMoreTokens()) {
      var segment = tokenizer.nextToken()
      var origSegment = segment

      if (segment.length < maxLen) {
        segment += " ".repeat(maxLen - segment.length)

        if (currentColumn != 0 && currentColumn < editor.lineLength(currentLine)) {
          origSegment = segment
        }
      }

      val pad = injector.engineEditorHelper.pad(editor, currentLine, currentColumn)

      val insertOffset = editor.bufferPositionToOffset(BufferPosition(currentLine, currentColumn))
      updated = updated.moveToOffset(insertOffset)
      val insertedText = origSegment + segment.repeat(count - 1)
      updated = injector.changeGroup.insertText(editor, updated, insertedText)
      endOffset += insertedText.length

      if (mode == SelectionType.LINE_WISE) {
        updated = updated.moveToOffset(endOffset)
        updated = injector.changeGroup.insertText(editor, updated, "\n")
        ++endOffset
      } else {
        if (pad.isNotEmpty()) {
          updated = updated.moveToOffset(insertOffset)
          updated = injector.changeGroup.insertText(editor, updated, pad)
          endOffset += pad.length
        }
      }

      ++currentLine
    }

    if (indent) endOffset = doIndent(editor, updated, context, startOffset, endOffset)
    updated = moveCaretToEndPosition(editor, updated, startOffset, endOffset, type, mode, cursorAfter)

    return endOffset to updated
  }

  private fun putTextInternal(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    text: String,
    type: SelectionType,
    mode: SelectionType,
    startOffset: Int,
    count: Int,
    indent: Boolean,
    cursorAfter: Boolean,
  ): Pair<Int, VimCaret> {
    return when (type) {
      SelectionType.CHARACTER_WISE -> putTextCharacterwise(
        editor,
        caret,
        context,
        text,
        type,
        mode,
        startOffset,
        count,
        indent,
        cursorAfter,
      )

      SelectionType.LINE_WISE -> putTextLinewise(
        editor,
        caret,
        context,
        text,
        type,
        mode,
        startOffset,
        count,
        indent,
        cursorAfter,
      )

      else -> putTextBlockwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
    }
  }

  @VimLockLabel.SelfSynchronized
  protected fun prepareDocumentAndGetStartOffsets(
    vimEditor: VimEditor,
    vimCaret: VimCaret,
    typeInRegister: SelectionType,
    data: PutData,
    additionalData: Map<String, Any>,
  ): List<Int> {
    val application = injector.application
    val visualSelection = data.visualSelection
    if (visualSelection != null) {
      return when {
        visualSelection.typeInEditor.isChar && typeInRegister.isLine -> {
          application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret, vimCaret.offset, "\n") }
          listOf(vimCaret.offset + 1)
        }

        visualSelection.typeInEditor.isBlock -> {
          val firstSelectedLine = additionalData["firstSelectedLine"] as Int
          val selectedLines = additionalData["selectedLines"] as Int
          val startColumnOfSelection = additionalData["startColumnOfSelection"] as Int
          val line = (if (data.insertTextBeforeCaret) firstSelectedLine else firstSelectedLine + selectedLines)
            .coerceAtMost(vimEditor.lineCount() - 1)
          when (typeInRegister) {
            SelectionType.LINE_WISE -> when {
              data.insertTextBeforeCaret -> listOf(vimEditor.getLineStartOffset(line))
              else -> {
                val pos = vimEditor.getLineEndOffset(line, true)
                application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret, pos, "\n") }
                listOf(pos + 1)
              }
            }

            SelectionType.CHARACTER_WISE -> (firstSelectedLine + selectedLines downTo firstSelectedLine)
              .map { vimEditor.bufferPositionToOffset(BufferPosition(it, startColumnOfSelection)) }

            SelectionType.BLOCK_WISE -> listOf(
              vimEditor.bufferPositionToOffset(
                BufferPosition(
                  firstSelectedLine,
                  startColumnOfSelection,
                ),
              ),
            )
          }
        }

        visualSelection.typeInEditor.isLine -> {
          val lastChar = if (vimEditor.fileSize() > 0) {
            vimEditor.getText(TextRange(vimEditor.fileSize().toInt() - 1, vimEditor.fileSize().toInt()))[0]
          } else {
            null
          }
          if (vimCaret.offset == vimEditor.fileSize().toInt() && vimEditor.fileSize()
              .toInt() != 0 && lastChar != '\n'
          ) {
            application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret, vimCaret.offset, "\n") }
            listOf(vimCaret.offset + 1)
          } else {
            listOf(vimCaret.offset)
          }
        }

        else -> listOf(vimCaret.offset)
      }
    } else {
      if (data.insertTextBeforeCaret) {
        return when (typeInRegister) {
          SelectionType.LINE_WISE -> listOf(injector.motion.moveCaretToCurrentLineStart(vimEditor, vimCaret))
          else -> listOf(vimCaret.offset)
        }
      }

      var startOffset: Int
      val line = if (data.putToLine < 0) vimCaret.getBufferPosition().line else data.putToLine
      when (typeInRegister) {
        SelectionType.LINE_WISE -> {
          startOffset = if (data.putBeforeLine) {
            injector.motion.moveCaretToLineStart(vimEditor, line)
          } else {
            min(vimEditor.text().length, injector.motion.moveCaretToLineEnd(vimEditor, line, true) + 1)
          }
          // At the end of a notebook cell the next symbol is a guard,
          // so we add a newline to be able to paste. Fixes VIM-2577
          if (startOffset > 0 && vimEditor.document.getOffsetGuard(startOffset) != null) {
            application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret, (startOffset - 1), "\n") }
          }
          if (startOffset > 0 && startOffset == vimEditor.text().length && vimEditor.text()[startOffset - 1] != '\n') {
            application.runWriteAction { (vimEditor as MutableVimEditor).insertText(vimCaret, startOffset, "\n") }
            startOffset++
          }
        }

        else -> {
          startOffset = vimCaret.offset
          if (!vimEditor.isLineEmpty(line, false)) {
            startOffset++
          }
        }
      }

      return if (startOffset > vimEditor.text().length) listOf(vimEditor.text().length) else listOf(startOffset)
    }
  }

  @VimLockLabel.SelfSynchronized
  private fun putForCaret(
    editor: VimEditor,
    caret: VimCaret,
    data: PutData,
    additionalData: Map<String, Any>,
    context: ExecutionContext,
    text: ProcessedTextData,
  ): VimCaret {
    var updated = caret
    notifyAboutIdeaPut(editor)
    if (data.visualSelection?.typeInEditor?.isLine == true && editor.isOneLineMode()) return updated
    val startOffsets = prepareDocumentAndGetStartOffsets(editor, updated, text.typeInRegister, data, additionalData)

    startOffsets.forEach { startOffset ->
      val selectionType = data.visualSelection?.typeInEditor ?: SelectionType.CHARACTER_WISE
      val (endOffset, updatedCaret) = putTextInternal(
        editor, updated, context, text.copiedText.text, text.typeInRegister, selectionType,
        startOffset, data.count, data.indent, data.caretAfterInsertedText,
      )
      updated = updatedCaret
      injector.markService.setChangeMarks(updatedCaret, TextRange(startOffset, endOffset))
      updated = moveCaretToEndPosition(
        editor,
        updatedCaret,
        startOffset,
        endOffset,
        text.typeInRegister,
        selectionType,
        data.caretAfterInsertedText,
      )
    }
    return updated
  }

  @VimLockLabel.SelfSynchronized
  override fun putTextForCaret(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    data: PutData,
    updateVisualMarks: Boolean,
    modifyRegister: Boolean,
  ): Boolean {
    val additionalData = collectPreModificationData(editor, data)
    var currentCaret = caret
    data.visualSelection?.let {
      currentCaret = deleteSelectedText(
        editor,
        context,
        caret,
        data,
        modifyRegister,
      ) ?: return false
    }
    val processedText = processText(currentCaret, data) ?: return false
    currentCaret = putForCaret(editor, currentCaret, data, additionalData, context, processedText)
    if (updateVisualMarks) {
      wrapInsertedTextWithVisualMarks(currentCaret, data)
    }
    return true
  }

  @VimLockLabel.SelfSynchronized
  override fun putTextAndSetCaretPosition(
    editor: VimEditor,
    context: ExecutionContext,
    text: ProcessedTextData,
    data: PutData,
    additionalData: Map<String, Any>,
  ) {
    val visualSelection = data.visualSelection
    val selectionType = visualSelection?.typeInEditor ?: SelectionType.CHARACTER_WISE
    if (injector.globalOptions().clipboard.contains(OptionConstants.clipboard_ideaput)) {
      val idePasteProvider = getProviderForPasteViaIde(editor, text.typeInRegister, data)
      if (idePasteProvider != null) {
        logger.debug("Perform put via idea paste")
        putTextViaIde(idePasteProvider, editor, context, text, selectionType, data, additionalData)
        return
      }
    }

    logger.debug("Perform put via plugin")
    val myCarets = visualSelection?.caretsAndSelections?.keys?.sortedByDescending { it.getBufferPosition() }
      ?: editor.sortedNativeCarets().reversed()
    myCarets.forEach { caret -> putForCaret(editor, caret, data, additionalData, context, text) }
  }

  companion object {
    val logger: VimLogger by lazy { vimLogger<VimPutBase>() }
  }
}

// This is taken from StringUtil of IntelliJ IDEA
private fun CharSequence.getLineBreakCount(): Int {
  var count = 0
  var i = 0
  while (i < length) {
    val c = this[i]
    if (c == '\n') {
      count++
    } else if (c == '\r') {
      if (i + 1 < length && this[i + 1] == '\n') {
        i++
      }
      count++
    }
    i++
  }
  return count
}
