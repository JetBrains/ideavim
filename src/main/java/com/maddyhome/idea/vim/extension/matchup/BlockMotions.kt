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
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.matchit.Block
import com.maddyhome.idea.vim.extension.matchit.findNextOpeningDelimiter
import com.maddyhome.idea.vim.extension.matchit.findOpeningDelimiterAt
import com.maddyhome.idea.vim.extension.matchit.findPreviousOpeningDelimiter
import com.maddyhome.idea.vim.extension.matchit.findSurroundingBlocks
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlin.math.max
import kotlin.math.min

internal enum class BlockMotion {
  /** `[%` */
  START,

  /** `]%` */
  END,

  /** `z%` */
  INSIDE,

  /** `Z%`, which has no operator-pending mapping. */
  INSIDE_PREVIOUS,
}

internal class BlockMotionHandler(private val motion: BlockMotion) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
    val count = operatorArguments.count1
    commandBuilder.resetCount()

    if (editor.mode is Mode.OP_PENDING) {
      val isForcedCharacterwise = commandBuilder.forcedMotion == SelectionType.CHARACTER_WISE
      commandBuilder.addAction(BlockMotionTextObject(motion, count, isForcedCharacterwise))
      return
    }

    editor.sortedCarets().forEach { caret ->
      val offset = findMotionOffset(editor.ij, caret.offset, count, motion) ?: return@forEach
      injector.jumpService.saveJumpLocation(editor)
      caret.moveToOffset(offset)
    }
  }
}

/**
 * With an operator, the motions act on the characters between the cursor and the target, both ends included, the
 * way the text would be selected in Visual mode. `[%` and `]%` leave out the cursor and the closing word, while `z%`
 * stops right before the text inside the block. A forced characterwise motion (`dv]%`) includes them.
 */
private class BlockMotionTextObject(
  private val motion: BlockMotion,
  private val count: Int,
  private val isForcedCharacterwise: Boolean,
) : TextObjectActionHandler() {
  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override val supportsLinewiseDeletePromotion: Boolean = false

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val ijEditor = editor.ij
    val caretOffset = caret.offset
    val text = ijEditor.document.charsSequence
    val target = when (motion) {
      BlockMotion.START -> findBlock(ijEditor, caretOffset, this.count) { it.opening.startOffset }
        ?.opening?.startOffset

      BlockMotion.END -> if (isForcedCharacterwise) {
        findBlock(ijEditor, caretOffset, this.count) { it.closing.endOffset - 1 }?.closing?.endOffset?.minus(1)
      } else {
        findBlock(ijEditor, caretOffset, this.count) { it.closing.startOffset - 1 }?.closing?.startOffset?.minus(1)
      }

      BlockMotion.INSIDE -> findInsideOffset(ijEditor, caretOffset, this.count)?.let {
        if (isForcedCharacterwise) it else previousPosition(text, skipWhitespace(text, it))
      }

      BlockMotion.INSIDE_PREVIOUS -> findInsidePreviousOffset(ijEditor, caretOffset, this.count)
    } ?: return null

    val cursorEnd = if (motion == BlockMotion.START && !isForcedCharacterwise) {
      previousPosition(text, caretOffset)
    } else {
      caretOffset
    }
    return TextRange(min(target, cursorEnd), min(max(target, cursorEnd) + 1, text.length))
  }
}

private fun findMotionOffset(editor: Editor, caretOffset: Int, count: Int, motion: BlockMotion): Int? {
  return when (motion) {
    BlockMotion.START -> findBlock(editor, caretOffset, count) { it.opening.startOffset }?.opening?.startOffset
    BlockMotion.END -> findBlock(editor, caretOffset, count) { it.closing.endOffset - 1 }?.closing?.endOffset?.minus(1)
    BlockMotion.INSIDE -> findInsideOffset(editor, caretOffset, count)?.let {
      skipIndent(editor.document.charsSequence, it)
    }

    BlockMotion.INSIDE_PREVIOUS -> findInsidePreviousOffset(editor, caretOffset, count)
  }
}

/**
 * The [count]th block around the cursor. When the cursor already is where the motion would take it, on the start of
 * the innermost opening word for instance, that block doesn't count.
 */
private fun findBlock(editor: Editor, caretOffset: Int, count: Int, target: (Block) -> Int): Block? {
  val blocks = findSurroundingBlocks(editor, caretOffset)
  val innermost = blocks.firstOrNull() ?: return null
  val index = if (target(innermost) == caretOffset) count else count - 1
  return blocks.getOrNull(index)
}

/**
 * The position right after the [count]th opening delimiter, starting with the one the cursor is on.
 */
private fun findInsideOffset(editor: Editor, caretOffset: Int, count: Int): Int? {
  val text = editor.document.charsSequence
  var delimiter = findOpeningDelimiterAt(editor, caretOffset)
    ?: findNextOpeningDelimiter(editor, caretOffset, caretOffset)
    ?: return null
  var target = nextPosition(text, delimiter.endOffset - 1)
  repeat(count - 1) {
    delimiter = findNextOpeningDelimiter(editor, target, caretOffset) ?: return null
    target = nextPosition(text, delimiter.endOffset - 1)
  }
  return target
}

/**
 * The position right after the nearest opening delimiter that comes before the cursor, skipping the indent after it.
 * Each repetition starts over from where the search for the previous one stopped, which for a block found on the first
 * try is the same place, so a count rarely makes a difference.
 */
private fun findInsidePreviousOffset(editor: Editor, caretOffset: Int, count: Int): Int? {
  val text = editor.document.charsSequence
  var searchOffset = caretOffset
  var target: Int? = null
  repeat(count) {
    findOpeningDelimiterAt(editor, searchOffset)?.let { searchOffset = previousPosition(text, it.startOffset) }
    for (attempt in 0 until 2) {
      val delimiter = findPreviousOpeningDelimiter(editor, searchOffset, caretOffset) ?: return null
      val inside = skipIndent(text, nextPosition(text, delimiter.endOffset - 1))
      target = inside
      if (inside < caretOffset) break
      searchOffset = max(delimiter.startOffset - 1, 0)
    }
  }
  return target
}

/**
 * The character after [offset], moving to the start of the next line from the last character of a line.
 */
private fun nextPosition(text: CharSequence, offset: Int): Int {
  val next = if (offset + 1 < text.length && text[offset + 1] == '\n') offset + 2 else offset + 1
  return min(next, max(text.length - 1, 0))
}

/**
 * The character before [offset], moving to the last character of the previous line from the start of a line. An empty
 * previous line has no characters, so its line break stands in for one.
 */
private fun previousPosition(text: CharSequence, offset: Int): Int {
  if (offset == 0) return 0
  if (text[offset - 1] != '\n') return offset - 1
  val previousLineIsEmpty = offset - 1 == 0 || text[offset - 2] == '\n'
  return if (previousLineIsEmpty) offset - 1 else offset - 2
}

/**
 * The first character from [offset] on that isn't a space or a tab. A line ending in whitespace continues onto the
 * next line.
 */
private fun skipWhitespace(text: CharSequence, offset: Int): Int {
  var current = offset
  while (current < text.length && text[current].isSpaceOrTab()) {
    val next = nextPosition(text, current)
    if (next <= current) return current + 1
    current = next
  }
  return current
}

private fun skipIndent(text: CharSequence, offset: Int): Int {
  var lineStart = offset
  while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
  if ((lineStart..offset).any { it >= text.length || !text[it].isSpaceOrTab() }) return offset

  var firstNonBlank = offset
  while (firstNonBlank < text.length && text[firstNonBlank].isSpaceOrTab()) firstNonBlank++
  return if (firstNonBlank < text.length && text[firstNonBlank] != '\n') firstNonBlank else firstNonBlank - 1
}

private fun Char.isSpaceOrTab(): Boolean = this == ' ' || this == '\t'
