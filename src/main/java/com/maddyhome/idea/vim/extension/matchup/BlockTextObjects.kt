/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchup

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.action.change.delete.DeleteMotionAction
import com.maddyhome.idea.vim.action.copy.YankMotionAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.matchit.Block
import com.maddyhome.idea.vim.extension.matchit.Delimiter
import com.maddyhome.idea.vim.extension.matchit.FileTypePatterns
import com.maddyhome.idea.vim.extension.matchit.findSurroundingBlocks
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlin.math.max
import kotlin.math.min

/**
 * `i%` and `a%`. Without a count they work on the "any block" around the cursor, the part of a block between two of
 * its neighbouring delimiters, e.g. from "if" to "else". A count of 1 takes the whole block, from its opening to its
 * closing delimiter, and a bigger count the any block of an outer block.
 */
internal class BlockTextObjectHandler(private val isInner: Boolean) : ExtensionHandler {
  override val isRepeatable: Boolean
    get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
    val count0 = operatorArguments.count0

    if (editor.mode is Mode.OP_PENDING) {
      commandBuilder.addAction(BlockTextObject(isInner, count0, commandBuilder.forcedMotion))
      return
    }
    if (editor.mode !is Mode.VISUAL) return

    editor.nativeCarets().forEach { caret ->
      val selection = caret.selectionStart..max(caret.selectionStart, caret.selectionEnd - 1)
      val textObject = findBlockTextObject(editor.ij, isInner, count0, selection.first, selection, operator = null)
      if (textObject !is BlockSelection) return@forEach
      SelectionVimListenerSuppressor.lock {
        caret.vimSetSelection(textObject.start, textObject.end, true)
      }
    }
  }
}

private class BlockTextObject(
  private val isInner: Boolean,
  private val count0: Int,
  private val forcedMotion: SelectionType?,
) : TextObjectActionHandler() {
  override var visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override val supportsLinewiseDeletePromotion: Boolean = false

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val operatorAction = injector.vimState.executingCommand?.action
    val operator = PendingOperator(
      isLinewise = operatorAction is DeleteMotionAction || operatorAction is YankMotionAction,
      isDelete = operatorAction is DeleteMotionAction,
      forcedMotion = forcedMotion,
    )
    return when (val textObject = findBlockTextObject(editor.ij, isInner, count0, caret.offset, null, operator)) {
      null -> null
      is EmptyBlockSelection -> TextRange(textObject.offset, textObject.offset)
      is BlockSelection -> {
        visualType = if (textObject.isLinewise) TextObjectVisualType.LINE_WISE else TextObjectVisualType.CHARACTER_WISE
        TextRange(textObject.start, min(textObject.end + 1, editor.fileSize().toInt()))
      }
    }
  }
}

/**
 * The operator the text object is used with. [isLinewise] operators turn an inner block that spans more than two
 * lines into the whole lines between its delimiters.
 */
private class PendingOperator(val isLinewise: Boolean, val isDelete: Boolean, val forcedMotion: SelectionType?)

private sealed interface BlockTextObjectResult

/**
 * A selection from [start] to [end], both included. [end] may be on a line break, which then is selected too.
 */
private data class BlockSelection(val start: Int, val end: Int, val isLinewise: Boolean = false) : BlockTextObjectResult

/**
 * An operator on an empty block, such as "()", does nothing but put the cursor at [offset].
 */
private data class EmptyBlockSelection(val offset: Int) : BlockTextObjectResult

private const val MAX_LOCAL_COUNT = 8

private val HTML_CLOSING_TAG = Regex("""^/\w+\s*>?$""", RegexOption.IGNORE_CASE)

/**
 * In Visual mode, [selection] is the current selection, and the text object is retried with bigger blocks until it
 * selects more than that.
 */
private fun findBlockTextObject(
  editor: Editor,
  isInner: Boolean,
  count0: Int,
  cursorOffset: Int,
  selection: IntRange?,
  operator: PendingOperator?,
): BlockTextObjectResult? {
  val isVisual = selection != null
  val text = TextPositions(editor.document.charsSequence)
  val blocks = findSurroundingBlocks(editor, cursorOffset)
  val isHtmlLike = FileTypePatterns.isHtmlLike(EditorHelper.getVirtualFile(editor))

  var result: BlockSelection? = null
  for ((isLocal, extraCount) in attempts(isInner, count0)) {
    val block = blocks.getOrNull(max(count0, 1) + extraCount - 1) ?: return null
    val (opening, closing) = if (isLocal) nearestDelimiters(block, cursorOffset) else block.opening to block.closing
    val openingText = text.chars.subSequence(opening.startOffset, opening.endOffset)
    val closingText = text.chars.subSequence(closing.startOffset, closing.endOffset)
    val isHtmlTag = isHtmlLike && HTML_CLOSING_TAG.matches(closingText)
    val lineCount = text.line(closing.startOffset) - text.line(opening.startOffset) + 1

    if (!isVisual && isInner && closing.startOffset == text.next(opening.endOffset - 1)) {
      return EmptyBlockSelection(closing.startOffset)
    }
    if (isVisual && isInner && selection == opening.startOffset..closing.startOffset) continue

    var start: Int
    var end: Int
    var isLinewise = false
    if (isInner) {
      start = text.next(opening.endOffset - 1)
      var closingStartsLine = text.isLineStart(closing.startOffset)
      end = text.previous(closing.startOffset)

      if (lineCount < 2 && isHtmlTag && !(isVisual && start == end)) {
        end = text.previous(end)
        if (!openingText.endsWith(">")) start = text.next(start)
      }

      // Don't select the indent of the closing delimiter's line.
      while (text.isInIndent(end)) {
        end = text.previous(text.lineStart(end))
        closingStartsLine = true
      }

      if (isVisual && closingStartsLine) end = text.lineEnd(end)

      if (operator != null) {
        if (closingStartsLine) end = text.next(end)

        var isInclusive = !closingStartsLine && start <= end
        if (operator.forcedMotion == SelectionType.CHARACTER_WISE) isInclusive = !isInclusive

        // The exclusive motion rules of `:help exclusive`, for the end of the block at the start of a line.
        if (operator.forcedMotion == null && text.isLineStart(end) && lineCount > 1 && !isInclusive) {
          val previousLine = text.line(end) - 1
          if (text.isLineStart(start) || text.isInIndent(start - 1)) {
            isLinewise = true
            isInclusive = true
            end = text.lineStartOfLine(previousLine)
          } else {
            end = text.lineEndOfLine(previousLine)
            if (end > text.lineStartOfLine(previousLine)) {
              end--
              isInclusive = true
            }
          }
        }

        if (!isInclusive) end = text.previous(end)
      }

      if (lineCount > 2 && operator?.isLinewise == true && closingText.length > 1) {
        if (!text.isLineStart(start)) start = text.lineStartOfLine(text.line(start) + 1)
        end = text.lineEndOfLine(text.line(closing.startOffset) - 1)
      }

      if (operator != null && start > end) return EmptyBlockSelection(start)
    } else {
      start = opening.startOffset
      end = closing.endOffset - 1

      if (isHtmlTag) {
        start--
        if (!closingText.endsWith(">")) end++
      }

      // Deleting a block that takes up whole lines deletes the lines.
      if (operator?.isDelete == true && lineCount > 1 && text.isBlankAfter(end) &&
        text.isBlankBeforeColumn(end, text.column(start))
      ) {
        start = text.lineStart(start)
        end = text.lineEnd(end)
      }
    }

    result = BlockSelection(start, end, isLinewise)
    if (selection != null && selection.first != selection.last &&
      (selection == start..end || start > selection.first || selection.last > end)
    ) {
      continue
    }
    break
  }
  return result
}

/**
 * Without a count, every attempt first tries the any block and then the whole block.
 */
private fun attempts(isInner: Boolean, count0: Int): List<Pair<Boolean, Int>> {
  return if (count0 == 1 || count0 > MAX_LOCAL_COUNT) {
    (0..(if (isInner) 3 else 2)).map { false to it }
  } else {
    (0..(if (isInner) 2 else 1)).flatMap { listOf(true to it, false to it) }
  }
}

/**
 * The two neighbouring delimiters of [block] around [offset].
 */
private fun nearestDelimiters(block: Block, offset: Int): Pair<Delimiter, Delimiter> {
  return block.delimiters.zipWithNext()
    .firstOrNull { (previous, next) -> previous.startOffset <= offset && offset <= next.endOffset - 1 }
    ?: (block.opening to block.delimiters[1])
}

/**
 * Character positions the way Vim's cursor sees them: a line break is only a position on an empty line or at the end
 * of a selection, otherwise moving from the last character of a line goes to the first one of the next line.
 */
private class TextPositions(val chars: CharSequence) {
  fun lineStart(offset: Int): Int {
    var start = min(offset, chars.length)
    while (start > 0 && chars[start - 1] != '\n') start--
    return start
  }

  fun lineEnd(offset: Int): Int {
    var end = offset
    while (end < chars.length && chars[end] != '\n') end++
    return end
  }

  fun line(offset: Int): Int = (0 until min(offset, chars.length)).count { chars[it] == '\n' }

  fun column(offset: Int): Int = offset - lineStart(offset)

  fun lineStartOfLine(line: Int): Int {
    var offset = 0
    repeat(line) { offset = lineEnd(offset) + 1 }
    return min(offset, chars.length)
  }

  fun lineEndOfLine(line: Int): Int = lineEnd(lineStartOfLine(line))

  fun isLineStart(offset: Int): Boolean = offset == lineStart(offset)

  fun next(offset: Int): Int {
    val lineEnd = lineEnd(offset)
    return if (offset + 1 < lineEnd) offset + 1 else min(lineEnd + 1, chars.length)
  }

  fun previous(offset: Int): Int {
    if (offset == 0) return 0
    if (chars[offset - 1] != '\n') return offset - 1
    val previousLineIsEmpty = offset - 1 == 0 || chars[offset - 2] == '\n'
    return if (previousLineIsEmpty) offset - 1 else offset - 2
  }

  fun isInIndent(offset: Int): Boolean {
    if (offset < 0 || offset >= lineEnd(offset)) return false
    return (lineStart(offset)..offset).all { chars[it] == ' ' || chars[it] == '\t' }
  }

  fun isBlankAfter(offset: Int): Boolean = (offset + 1 until lineEnd(offset)).all { chars[it].isWhitespace() }

  /**
   * Whether the first [column] characters of the line with [offset] are blank.
   */
  fun isBlankBeforeColumn(offset: Int, column: Int): Boolean {
    val lineStart = lineStart(offset)
    return (lineStart until min(lineStart + column, lineEnd(offset))).all { chars[it].isWhitespace() }
  }
}
