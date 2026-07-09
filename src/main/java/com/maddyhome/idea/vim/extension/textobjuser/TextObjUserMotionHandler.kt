/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

internal enum class Motion { NEXT_START, PREVIOUS_START, NEXT_END, PREVIOUS_END }

/**
 * Moves the caret to the beginning ("move-n" / "move-p") or end ("move-N" / "move-P") of the next or previous match of
 * [pattern].
 */
internal class TextObjUserMotionHandler(
  private val pattern: String,
  private val motion: Motion,
) : ExtensionHandler {
  override val isRepeatable: Boolean get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val matches = VimRegex(pattern).findAll(editor)
    editor.nativeCarets().forEach { caret ->
      val target = targetOffset(matches, caret.offset) ?: return@forEach
      caret.moveToOffset(target)
    }
  }

  private fun targetOffset(matches: List<VimMatchResult.Success>, caretOffset: Int): Int? {
    // A match's end position, as a cursor lands on it, is the last character of the match (endOffset is exclusive).
    fun VimMatchResult.Success.endOffsetInclusive() = range.endOffset - 1
    return when (motion) {
      Motion.NEXT_START -> matches.firstOrNull { it.range.startOffset > caretOffset }?.range?.startOffset
      Motion.PREVIOUS_START -> matches.lastOrNull { it.range.startOffset < caretOffset }?.range?.startOffset
      Motion.NEXT_END -> matches.firstOrNull { it.endOffsetInclusive() > caretOffset }?.endOffsetInclusive()
      Motion.PREVIOUS_END -> matches.lastOrNull { it.endOffsetInclusive() < caretOffset }?.endOffsetInclusive()
    }
  }
}
