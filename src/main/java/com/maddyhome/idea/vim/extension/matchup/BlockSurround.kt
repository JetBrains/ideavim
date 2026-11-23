/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchup

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getMappingInfo
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.matchit.Block
import com.maddyhome.idea.vim.extension.matchit.findSurroundingBlocks
import com.maddyhome.idea.vim.extension.surround.surroundAfter
import com.maddyhome.idea.vim.newapi.ij

/**
 * `ds%` deletes the opening and the closing word of the [count]th block around the cursor, and `cs%` replaces them.
 * Middle words, such as "else", stay. The deleted words go to the register, separated by a space.
 *
 * `cs%` reads one character, and a bracket of [REPLACEMENT_PAIRS] puts the pair around the block, any other character
 * goes on both sides. With the surround extension on, the character is a surround target instead, so "t" asks for a
 * tag, for instance.
 */
internal class BlockSurroundHandler(private val isChange: Boolean) : ExtensionHandler {
  override val isRepeatable: Boolean = true

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val caret = editor.primaryCaret()
    val block = findSurroundingBlocks(editor.ij, caret.offset).getOrNull(operatorArguments.count1 - 1) ?: return
    if (!isChange) {
      replaceWords(editor, context, caret, block, "", "")
      return
    }
    val c = injector.keyGroup.getChar(editor)?.takeUnless { it in CANCEL_CHARS } ?: return
    if (isSurroundEnabled()) {
      surroundAfter(caret, context, c) { replaceWords(editor, context, caret, block, "", "") }
    } else {
      val (left, right) = replacementPair(c)
      replaceWords(editor, context, caret, block, left, right)
    }
  }

  /**
   * Replaces the opening word of [block] with [left] and the closing one with [right], and returns the range between
   * them.
   */
  private fun replaceWords(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    block: Block,
    left: String,
    right: String,
  ): TextRange {
    val opening = block.opening
    val closing = block.closing
    val text = editor.text()
    val deleted = "${text.subSequence(opening.startOffset, opening.endOffset)} " +
      text.subSequence(closing.startOffset, closing.endOffset)
    injector.application.runWriteAction {
      val mutableEditor = editor as MutableVimEditor
      mutableEditor.replaceString(closing.startOffset, closing.endOffset, right)
      mutableEditor.replaceString(opening.startOffset, opening.endOffset, left)
    }
    injector.registerGroup.storeText(editor, context, injector.registerGroup.currentRegister, deleted)

    val closingStart = closing.startOffset - (opening.endOffset - opening.startOffset) + left.length
    injector.markService.setChangeMarks(caret, TextRange(opening.startOffset, closingStart + right.length))
    caret.moveToOffset(opening.startOffset)
    return TextRange(opening.startOffset, closingStart)
  }

  private fun replacementPair(c: Char): Pair<String, String> {
    val index = REPLACEMENT_PAIRS.indexOf(c)
    if (index < 0) return c.toString() to c.toString()
    return REPLACEMENT_PAIRS[index / 2 * 2].toString() to REPLACEMENT_PAIRS[index / 2 * 2 + 1].toString()
  }

  private fun isSurroundEnabled(): Boolean {
    val keys = injector.parser.parseKeys("<Plug>VSurround")
    return injector.keyGroup.getMappingInfo(keys, MappingMode.VISUAL) != null
  }

  private companion object {
    const val REPLACEMENT_PAIRS = "()<>[]{}«»“”"
    val CANCEL_CHARS = setOf('\u001b', '\u0003')
  }
}
