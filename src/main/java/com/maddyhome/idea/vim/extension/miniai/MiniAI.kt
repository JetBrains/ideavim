/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.miniai

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.EnumSet

/**
 * A simplified imitation of mini.ai approach for motions "aq", "iq", "ab", "ib".
 * Instead of "closest" text object, we apply a "cover-or-next" logic:
 *
 * 1) Among all candidate pairs, pick any that cover the caret (start <= caret < end).
 *    Among those, pick the smallest range.
 * 2) If none cover the caret, pick the "next" pair (start >= caret) that is closest.
 * 3) If none are next, pick the "previous" pair (end <= caret) that is closest.
 *
 * For "i" text object, we shrink the boundaries inward by one character on each side.
 */
class MiniAI : VimExtension {

  companion object {
    // <Plug> mappings
    private const val PLUG_AQ = "<Plug>mini-ai-aq"
    private const val PLUG_IQ = "<Plug>mini-ai-iq"
    private const val PLUG_AB = "<Plug>mini-ai-ab"
    private const val PLUG_IB = "<Plug>mini-ai-ib"

    // Actual user key sequences
    private const val KEY_AQ = "aq"
    private const val KEY_IQ = "iq"
    private const val KEY_AB = "ab"
    private const val KEY_IB = "ib"
  }

  override fun getName() = "mini-ai"

  override fun init() {
    registerMappings()
  }

  private fun registerMappings() {
    fun createHandler(
      rangeFunc: (VimEditor, ImmutableVimCaret, Boolean) -> TextRange?
    ): ExtensionHandler = object : ExtensionHandler {
      override val isRepeatable = true
      override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
        addAction(PortedMiniAiAction(rangeFunc))
      }
    }

    listOf(
      // Outer quotes
      PLUG_AQ to createHandler { e, c, _ -> findQuoteRange(e, c, isOuter = true) },
      // Inner quotes
      PLUG_IQ to createHandler { e, c, _ -> findQuoteRange(e, c, isOuter = false) },
      // Outer brackets
      PLUG_AB to createHandler { e, c, _ -> findBracketRange(e, c, isOuter = true) },
      // Inner brackets
      PLUG_IB to createHandler { e, c, _ -> findBracketRange(e, c, isOuter = false) }
    ).forEach { (plug, handler) ->
      putExtensionHandlerMapping(MappingMode.XO, injector.parser.parseKeys(plug), owner, handler, false)
    }

    // Map user keys -> <Plug> keys
    listOf(
      KEY_AQ to PLUG_AQ,
      KEY_IQ to PLUG_IQ,
      KEY_AB to PLUG_AB,
      KEY_IB to PLUG_IB
    ).forEach { (key, plug) ->
      putKeyMapping(MappingMode.XO, injector.parser.parseKeys(key), owner, injector.parser.parseKeys(plug), true)
    }
  }
}

// A text object action that uses the "mini.ai-like" picking strategy.
private class PortedMiniAiAction(
  private val rangeFunc: (VimEditor, ImmutableVimCaret, Boolean) -> TextRange?
) : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false
  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int
  ): TextRange? = rangeFunc(editor, caret, true).also {
    // We don't do 'count' expansions here. If you wanted to replicate
    // mini.ai's multi-level expansions, you'd call the "rangeFunc" multiple
    // times re-feeding the last output as reference, etc.
  }
}

// Utility to register action in KeyHandler
private fun addAction(action: TextObjectActionHandler) {
  KeyHandler.getInstance().keyHandlerState.commandBuilder.addAction(action)
}

/* -------------------------------------------------------------------------
 * Core mini.ai-like logic
 * ------------------------------------------------------------------------- */

/**
 * Find a text range for quotes (", ', `) around the caret using a "cover-or-next" approach.
 * - If one or more pairs **cover** the caret, pick the *smallest* covering pair.
 * - Else pick the "next" pair whose start >= caret offset (closest).
 * - Else pick the "previous" pair whose end <= caret offset (closest).
 *
 * If [isOuter] == false (i.e. 'iq'), shrink the final range by 1 char on each side.
 */
private fun findQuoteRange(editor: VimEditor, caret: ImmutableVimCaret, isOuter: Boolean): TextRange? {
  val text = editor.text()
  val caretOffset = caret.offset
  val caretLine = caret.getLine()

  // 1) Gather quotes in *this caret's line*
  val lineStart = editor.getLineStartOffset(caretLine)
  val lineEnd = editor.getLineEndOffset(caretLine)
  val lineText = text.substring(lineStart, lineEnd)
  val lineRanges = gatherAllQuoteRanges(lineText).map {
    TextRange(it.startOffset + lineStart, it.endOffset + lineStart)
  }

  val localBest = pickBestRange(lineRanges, caretOffset)
  if (localBest != null) {
    return adjustRangeForInnerOuter(localBest, isOuter)
  }

  // 2) Fallback: entire buffer
  val allRanges = gatherAllQuoteRanges(text)
  val bestOverall = pickBestRange(allRanges, caretOffset) ?: return null
  return adjustRangeForInnerOuter(bestOverall, isOuter)
}

/** Adjust final range if user requested 'inner' (i.e. skip bounding chars). */
private fun adjustRangeForInnerOuter(range: TextRange, isOuter: Boolean): TextRange? {
  if (isOuter) return range
  // For 'inner', skip bounding chars if possible
  if (range.endOffset - range.startOffset < 2) return null
  return TextRange(range.startOffset + 1, range.endOffset - 1)
}

/**
 * Gather all "balanced" pairs for single/double/backtick quotes in the entire text.
 * For simplicity, we treat each ["]...["] or [']...['] or [`]...[`] as one range,
 * ignoring complicated cases of escaping, multi-line, etc.
 */
private fun gatherAllQuoteRanges(text: CharSequence): List<TextRange> {
  val results = mutableListOf<TextRange>()
  val patterns = listOf(
    "\"([^\"]*)\"",
    "'([^']*)'",
    "`([^`]*)`"
  )
  for (p in patterns) {
    Regex(p).findAll(text).forEach {
      results.add(TextRange(it.range.first, it.range.last + 1))
    }
  }
  return results
}

/**
 * Find a text range for brackets using a "cover-or-next" approach.
 * We treat bracket pairs ( (), [], {}, <> ) in a naive balanced scanning way.
 * If [isOuter] is false, we shrink boundaries to skip the bracket chars.
 */
private fun findBracketRange(editor: VimEditor, caret: ImmutableVimCaret, isOuter: Boolean): TextRange? {
  val text = editor.text()
  val caretOffset = caret.offset
  val caretLine = caret.getLine()

  // 1) Gather bracket pairs in *this caret's line*
  val lineStart = editor.getLineStartOffset(caretLine)
  val lineEnd = editor.getLineEndOffset(caretLine)
  val bracketChars = listOf('(', ')', '[', ']', '{', '}', '<', '>')
  // Gather local line bracket pairs
  val lineText = text.substring(lineStart, lineEnd)
  val lineRanges = gatherAllBracketRanges(lineText, bracketChars).map {
    // Shift each range's offsets to the global text
    TextRange(it.startOffset + lineStart, it.endOffset + lineStart)
  }

  // Pick the best match on this line
  val localBest = pickBestRange(lineRanges, caretOffset)
  if (localBest != null) {
    return adjustRangeForInnerOuter(localBest, isOuter)
  }

  // 2) Fallback: gather bracket pairs in the entire file
  val allRanges = gatherAllBracketRanges(text, bracketChars)
  val bestOverall = pickBestRange(allRanges, caretOffset) ?: return null

  return adjustRangeForInnerOuter(bestOverall, isOuter)
}

/**
 * Gathers naive "balanced bracket" ranges for the given bracket pairs.
 * This is a simplified stack-based approach scanning the entire file text.
 */
private fun gatherAllBracketRanges(
  text: CharSequence,
  brackets: List<Char>
): List<TextRange> {
  val pairs = mapOf('(' to ')', '[' to ']', '{' to '}', '<' to '>')
  val results = mutableListOf<TextRange>()
  val stack = ArrayDeque<Int>()         // offsets of open bracket
  val bracketTypeStack = ArrayDeque<Char>() // store which bracket

  text.forEachIndexed { i, ch ->
    if (pairs.containsKey(ch)) {
      // Opening bracket
      stack.addLast(i)
      bracketTypeStack.addLast(ch)
    } else {
      // Maybe a closing bracket?
      val top = bracketTypeStack.lastOrNull() ?: '\u0000'
      if (pairs[top] == ch) {
        // Balanced pair
        val openPos = stack.removeLast()
        bracketTypeStack.removeLast()
        results.add(TextRange(openPos, i + 1)) // i+1 for endOffset
      }
    }
  }
  return results
}

/**
 * Picks best range among [candidates] in a “cover-or-next” approach:
 *   1) Among those covering [caretOffset], pick the narrowest.
 *   2) Else pick the "next" bracket whose start >= caret, if any (closest).
 *   3) Else pick the "previous" bracket whose end <= caret, if any (closest).
 */
private fun pickBestRange(candidates: List<TextRange>, caretOffset: Int): TextRange? {
  if (candidates.isEmpty()) return null
  val covering = mutableListOf<TextRange>()
  val nextOnes = mutableListOf<TextRange>()
  val prevOnes = mutableListOf<TextRange>()

  for (r in candidates) {
    if (r.startOffset <= caretOffset && caretOffset < r.endOffset) {
      covering.add(r)
    } else if (r.startOffset >= caretOffset) {
      nextOnes.add(r)
    } else if (r.endOffset <= caretOffset) {
      prevOnes.add(r)
    }
  }

  // 1) Covering, smallest width
  if (covering.isNotEmpty()) {
    return covering.minByOrNull { it.endOffset - it.startOffset }
  }

  // 2) Next (closest by startOffset)
  if (nextOnes.isNotEmpty()) {
    return nextOnes.minByOrNull { kotlin.math.abs(it.startOffset - caretOffset) }
  }

  // 3) Previous (closest by endOffset)
  if (prevOnes.isNotEmpty()) {
    return prevOnes.minByOrNull { kotlin.math.abs(it.endOffset - caretOffset) }
  }

  return null
}
