/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.targets

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.state.mode.Mode
import kotlin.math.max

/**
 * Port of the [targets.vim](https://github.com/wellle/targets.vim) plugin.
 *
 * Adds seeking text objects for pairs (`( ) { } [ ] < >`), with the `i`/`a`/`I`/`A` modifiers and
 * the `n`/`l` next/last qualifiers, plus the `b` "any block" trigger. More sources (quotes,
 * separators, arguments, tags) are added incrementally.
 */
internal class TargetsExtension : VimExtension {
  override fun getName(): String = "targets"

  override fun init() {
    for ((trigger, source) in PAIR_TRIGGERS) {
      for (modifier in Modifier.entries) {
        for (which in Which.entries) {
          registerMapping(trigger, source, modifier, which)
        }
      }
    }
  }

  private fun registerMapping(trigger: Char, source: TargetSource, modifier: Modifier, which: Which) {
    val keys = modifier.key + which.infix + triggerKeys(trigger)
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(keys),
      owner,
      TargetsHandler(source, modifier, which),
      false,
    )
  }

  // `<` would be parsed as the start of a special key (e.g. `<CR>`); `<lt>` is the literal `<`.
  private fun triggerKeys(trigger: Char) = if (trigger == '<') "<lt>" else trigger.toString()

  /** Whether the command operates on the target around the cursor, the next, or the last one. */
  private enum class Which(val infix: String) {
    CURRENT(""),
    NEXT("n"),
    LAST("l"),
  }

  /**
   * How a located target is turned into the final range:
   * `i` inner, `a` around (with delimiters), `I` inner without surrounding whitespace,
   * `A` around plus adjacent whitespace. [key] is the keystroke that selects it.
   */
  private enum class Modifier(val key: Char) {
    INNER('i'),
    AROUND('a'),
    INNER_TRIMMED('I'),
    AROUND_WHITESPACE('A'),
  }

  /**
   * A source of targets (pair, quote, separator, tag, ...). Each method locates a [DelimiterSpan]
   * relative to the cursor; the [Modifier] then trims it into the operated range.
   */
  private interface TargetSource {
    /** The target around [offset], counting [count] levels outward. */
    fun enclosing(chars: CharSequence, offset: Int, count: Int): DelimiterSpan?

    /** The target strictly larger than the span [start, end), for growing a visual selection. */
    fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan?

    /** The [count]th target after the cursor. */
    fun next(chars: CharSequence, offset: Int, count: Int): DelimiterSpan?

    /** The [count]th target before the cursor. */
    fun last(chars: CharSequence, offset: Int, count: Int): DelimiterSpan?

    /** Seek on the current line: the nearest target forward, else backward. */
    fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan?
  }

  /**
   * A located target, given as the inclusive indices of its opening and closing delimiters.
   * Every source (pair, quote, tag, ...) produces this; the `i`/`a`/`I`/`A` modifiers then turn it
   * into the final [TextRange].
   */
  private data class DelimiterSpan(val open: Int, val close: Int)

  /**
   * Locates a pair target made of one of several open/close delimiter pairs ("any block" uses more
   * than one).
   */
  private class PairSource(private val pairs: List<Pair<Char, Char>>) : TargetSource {

    /** The smallest pair enclosing [offset], counting [count] levels outward. */
    override fun enclosing(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? {
      var best = pairs.mapNotNull { enclosingOf(chars, offset, it.first, it.second) }
        .maxByOrNull { it.open } ?: return null
      repeat(count - 1) {
        best = outerOf(chars, best.open) ?: return null
      }
      return best
    }

    /** The pair strictly larger than the span [start, end), for growing a visual selection. */
    override fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan? {
      val candidates = pairs.mapNotNull { (open, close) ->
        var pair = enclosingOf(chars, start, open, close) ?: return@mapNotNull null
        // Step outward until the inner range is a strict superset of the current selection.
        while (pair.open + 1 >= start && pair.close <= end) {
          pair = outerOf(chars, pair.open) ?: return@mapNotNull null
        }
        pair
      }
      return candidates.maxByOrNull { it.open }
    }

    /** The [count]th pair whose opening delimiter is at or after [offset]. */
    override fun next(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? {
      var result: DelimiterSpan? = null
      var from = offset
      repeat(count) {
        result = pairs.mapNotNull { forwardOf(chars, from, it.first, it.second) }
          .minByOrNull { it.open } ?: return null
        from = result!!.open + 1
      }
      return result
    }

    /** The [count]th pair whose closing delimiter is before [offset]. */
    override fun last(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? {
      var result: DelimiterSpan? = null
      var from = offset
      repeat(count) {
        result = pairs.mapNotNull { backwardOf(chars, from, it.first, it.second) }
          .maxByOrNull { it.close } ?: return null
        from = result!!.close
      }
      return result
    }

    /** Seek on the current line: the nearest pair forward, else backward. */
    override fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan? {
      val forward = pairs.mapNotNull { (open, close) ->
        val openIdx = indexOfOnLine(chars, open, offset, lineEnd)
        if (openIdx < 0) null else matchingClose(chars, openIdx, open, close)?.let { DelimiterSpan(openIdx, it) }
      }.minByOrNull { it.open }
      if (forward != null) return forward

      return pairs.mapNotNull { (open, close) ->
        val closeIdx = lastIndexOfOnLine(chars, close, lineStart, offset)
        if (closeIdx < 0) null else matchingOpen(chars, closeIdx, open, close)?.let { DelimiterSpan(it, closeIdx) }
      }.maxByOrNull { it.close }
    }

    private fun enclosingOf(chars: CharSequence, offset: Int, open: Char, close: Char): DelimiterSpan? {
      if (offset < chars.length && chars[offset] == open) {
        return matchingClose(chars, offset, open, close)?.let { DelimiterSpan(offset, it) }
      }
      if (offset < chars.length && chars[offset] == close) {
        return matchingOpen(chars, offset, open, close)?.let { DelimiterSpan(it, offset) }
      }
      val openIdx = unmatchedOpenBefore(chars, offset, open, close) ?: return null
      val closeIdx = matchingClose(chars, openIdx, open, close) ?: return null
      return if (closeIdx >= offset) DelimiterSpan(openIdx, closeIdx) else null
    }

    private fun outerOf(chars: CharSequence, openIdx: Int): DelimiterSpan? {
      val (open, close) = pairs.first { it.first == chars[openIdx] }
      val outerOpen = unmatchedOpenBefore(chars, openIdx, open, close) ?: return null
      val outerClose = matchingClose(chars, outerOpen, open, close) ?: return null
      return DelimiterSpan(outerOpen, outerClose)
    }

    private fun forwardOf(chars: CharSequence, from: Int, open: Char, close: Char): DelimiterSpan? {
      var i = from
      while (i < chars.length) {
        if (chars[i] == open) {
          return matchingClose(chars, i, open, close)?.let { DelimiterSpan(i, it) }
        }
        i++
      }
      return null
    }

    private fun backwardOf(chars: CharSequence, from: Int, open: Char, close: Char): DelimiterSpan? {
      var i = from - 1
      while (i >= 0) {
        if (chars[i] == close) {
          return matchingOpen(chars, i, open, close)?.let { DelimiterSpan(it, i) }
        }
        i--
      }
      return null
    }
  }

  /**
   * The text object handler. Computes the target range and either selects it (visual mode) or
   * feeds it to the pending operator.
   */
  private inner class TargetsHandler(
    private val source: TargetSource,
    private val modifier: Modifier,
    private val which: Which,
  ) : ExtensionHandler {
    override val isRepeatable: Boolean = false

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val count = max(1, operatorArguments.count0)
      if (editor.mode is Mode.OP_PENDING) {
        KeyHandler.getInstance().keyHandlerState.commandBuilder.addAction(RangeHandler(count))
        return
      }
      editor.nativeCarets().forEach { caret ->
        val range = computeRange(editor, caret, count) ?: return@forEach
        SelectionVimListenerSuppressor.lock().use {
          if (editor.mode is Mode.VISUAL) {
            caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
          } else {
            (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
          }
        }
      }
    }

    private fun computeRange(editor: VimEditor, caret: ImmutableVimCaret, count: Int): TextRange? {
      val chars = editor.text()
      val offset = caret.offset

      val pair = when (which) {
        Which.NEXT -> source.next(chars, offset, count)
        Which.LAST -> source.last(chars, offset, count)
        Which.CURRENT -> {
          if (editor.mode is Mode.VISUAL && caret.selectionStart != caret.selectionEnd) {
            source.growing(chars, caret.selectionStart, caret.selectionEnd)
          } else {
            source.enclosing(chars, offset, count)
              ?: source.seek(chars, offset, editor.getLineStartForOffset(offset), editor.getLineEndForOffset(offset))
          }
        }
      } ?: return null

      return applyModifier(chars, pair, modifier)
    }

    private inner class RangeHandler(private val count: Int) : TextObjectActionHandler() {
      override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE
      override val preserveSelectionAnchor: Boolean = false

      override fun getRange(
        editor: VimEditor,
        caret: ImmutableVimCaret,
        context: ExecutionContext,
        count: Int,
        rawCount: Int,
      ): TextRange? = computeRange(editor, caret, this.count)
    }
  }

  companion object {
    private val PAREN = PairSource(listOf('(' to ')'))
    private val CURLY = PairSource(listOf('{' to '}'))
    private val BRACKET = PairSource(listOf('[' to ']'))
    private val ANGLE = PairSource(listOf('<' to '>'))
    private val ANY_BLOCK = PairSource(listOf('(' to ')', '[' to ']', '{' to '}'))

    private val PAIR_TRIGGERS: Map<Char, PairSource> = mapOf(
      '(' to PAREN, ')' to PAREN,
      '{' to CURLY, '}' to CURLY, 'B' to CURLY,
      '[' to BRACKET, ']' to BRACKET,
      '<' to ANGLE, '>' to ANGLE,
      'b' to ANY_BLOCK,
    )

    private fun matchingClose(chars: CharSequence, openIdx: Int, open: Char, close: Char): Int? {
      var depth = 0
      var i = openIdx
      while (i < chars.length) {
        when (chars[i]) {
          open -> depth++
          close -> {
            depth--
            if (depth == 0) return i
          }
        }
        i++
      }
      return null
    }

    private fun matchingOpen(chars: CharSequence, closeIdx: Int, open: Char, close: Char): Int? {
      var depth = 0
      var i = closeIdx
      while (i >= 0) {
        when (chars[i]) {
          close -> depth++
          open -> {
            depth--
            if (depth == 0) return i
          }
        }
        i--
      }
      return null
    }

    private fun unmatchedOpenBefore(chars: CharSequence, offset: Int, open: Char, close: Char): Int? {
      var depth = 0
      var i = offset - 1
      while (i >= 0) {
        when (chars[i]) {
          close -> depth++
          open -> {
            if (depth == 0) return i
            depth--
          }
        }
        i--
      }
      return null
    }

    private fun indexOfOnLine(chars: CharSequence, ch: Char, from: Int, lineEnd: Int): Int {
      var i = from
      while (i < lineEnd) {
        if (chars[i] == ch) return i
        i++
      }
      return -1
    }

    private fun lastIndexOfOnLine(chars: CharSequence, ch: Char, lineStart: Int, from: Int): Int {
      var i = from - 1
      while (i >= lineStart) {
        if (chars[i] == ch) return i
        i--
      }
      return -1
    }

    private fun applyModifier(chars: CharSequence, span: DelimiterSpan, modifier: Modifier): TextRange {
      val openIdx = span.open
      val closeIdx = span.close
      return when (modifier) {
        Modifier.AROUND -> TextRange(openIdx, closeIdx + 1)
        Modifier.INNER -> TextRange(openIdx + 1, closeIdx)
        Modifier.INNER_TRIMMED -> {
          var start = openIdx + 1
          var end = closeIdx
          while (start < end && chars[start].isInlineWhitespace()) start++
          while (end > start && chars[end - 1].isInlineWhitespace()) end--
          TextRange(start, end)
        }
        Modifier.AROUND_WHITESPACE -> {
          var start = openIdx
          var end = closeIdx + 1
          val withTrailing = expandTrailing(chars, end)
          if (withTrailing > end) {
            end = withTrailing
          } else {
            start = expandLeading(chars, start)
          }
          TextRange(start, end)
        }
      }
    }

    private fun expandTrailing(chars: CharSequence, end: Int): Int {
      var e = end
      while (e < chars.length && chars[e].isInlineWhitespace()) e++
      return e
    }

    private fun expandLeading(chars: CharSequence, start: Int): Int {
      var s = start
      while (s > 0 && chars[s - 1].isInlineWhitespace()) s--
      return s
    }

    private fun Char.isInlineWhitespace() = this == ' ' || this == '\t'
  }
}
