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
    for ((trigger, source) in TRIGGERS) {
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

  // Some trigger characters clash with the key-notation parser and need their escaped forms:
  // `<` starts a special key (e.g. `<CR>`), `|` separates commands, `\` is the escape character.
  private fun triggerKeys(trigger: Char) = when (trigger) {
    '<' -> "<lt>"
    '|' -> "<Bar>"
    '\\' -> "<Bslash>"
    else -> trigger.toString()
  }

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

    /** The nearest target whose opening delimiter is at or after [from], or null if none. */
    fun nearestForward(chars: CharSequence, from: Int): DelimiterSpan?

    /** The nearest target whose closing delimiter is before [from], or null if none. */
    fun nearestBackward(chars: CharSequence, from: Int): DelimiterSpan?

    /** Seek on the current line: the nearest target forward, else backward. */
    fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan?

    /**
     * Turns a located [span] into the operated range for the given [modifier]. The default is the
     * balanced-delimiter behaviour shared by pairs and quotes (`a`/`A` include both delimiters);
     * sources whose delimiters are not balanced (e.g. separators) override this.
     */
    fun toRange(chars: CharSequence, span: DelimiterSpan, modifier: Modifier): TextRange =
      applyBalancedModifier(chars, span, modifier)

    /** The [count]th target after the cursor, stepping forward one [nearestForward] at a time. */
    fun next(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? =
      step(count, offset) { from -> nearestForward(chars, from)?.let { it to it.open + 1 } }

    /** The [count]th target before the cursor, stepping backward one [nearestBackward] at a time. */
    fun last(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? =
      step(count, offset) { from -> nearestBackward(chars, from)?.let { it to it.close } }

    /**
     * Repeats [findFrom] [count] times, each step starting where the previous match told it to
     * resume. Returns the last match, or null as soon as a step finds nothing.
     */
    private fun step(
      count: Int,
      start: Int,
      findFrom: (from: Int) -> Pair<DelimiterSpan, Int>?,
    ): DelimiterSpan? {
      var from = start
      var result: DelimiterSpan? = null
      repeat(count) {
        val (span, nextFrom) = findFrom(from) ?: return null
        result = span
        from = nextFrom
      }
      return result
    }
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
        best = outerOf(chars, best) ?: return null
      }
      return best
    }

    /** The pair strictly larger than the span [start, end), for growing a visual selection. */
    override fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan? {
      val candidates = pairs.mapNotNull { (open, close) ->
        var pair = enclosingOf(chars, start, open, close) ?: return@mapNotNull null
        // Step outward until the inner range is a strict superset of the current selection.
        while (pair.open + 1 >= start && pair.close <= end) {
          pair = outerOf(chars, pair) ?: return@mapNotNull null
        }
        pair
      }
      return candidates.maxByOrNull { it.open }
    }

    /** The nearest pair whose opening delimiter is at or after [from]. */
    override fun nearestForward(chars: CharSequence, from: Int): DelimiterSpan? =
      pairs.mapNotNull { forwardOf(chars, from, it.first, it.second) }.minByOrNull { it.open }

    /** The nearest pair whose closing delimiter is before [from]. */
    override fun nearestBackward(chars: CharSequence, from: Int): DelimiterSpan? =
      pairs.mapNotNull { backwardOf(chars, from, it.first, it.second) }.maxByOrNull { it.close }

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

    /**
     * The innermost pair (of any configured kind) that strictly encloses [span]. For "any block"
     * this lets a count cross bracket kinds, e.g. `2ib` from inside `()` nested in `{}` reaches the
     * `{}`.
     */
    private fun outerOf(chars: CharSequence, span: DelimiterSpan): DelimiterSpan? =
      pairs.mapNotNull { (open, close) ->
        val outerOpen = unmatchedOpenBefore(chars, span.open, open, close) ?: return@mapNotNull null
        val outerClose = matchingClose(chars, outerOpen, open, close) ?: return@mapNotNull null
        if (outerClose > span.close) DelimiterSpan(outerOpen, outerClose) else null
      }.maxByOrNull { it.open }

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
   * Locates a quote target for one or more quote characters ("any quote" uses more than one).
   *
   * Quotes don't nest: on each line the quote characters are paired in order from the start of the
   * line (1st with 2nd, 3rd with 4th, ...). This is what lets `ci"` skip the "false" gap between
   * two strings and operate on the real string instead.
   */
  private class QuoteSource(private val quotes: List<Char>) : TargetSource {

    override fun enclosing(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? =
      quotes.mapNotNull { containing(chars, offset, it) }.maxByOrNull { it.open }

    // Quotes don't nest, but the first text object in visual mode must still select the quote pair
    // enclosing the initial one-character selection. There is no larger target beyond that.
    override fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan? =
      quotes.mapNotNull { quote ->
        pairsOnLineOf(chars, start, quote).firstOrNull { it.open <= start && it.close + 1 >= end }
      }.maxByOrNull { it.open }

    override fun nearestForward(chars: CharSequence, from: Int): DelimiterSpan? =
      quotes.mapNotNull { firstPairAfter(chars, from, it) }.minByOrNull { it.open }

    override fun nearestBackward(chars: CharSequence, from: Int): DelimiterSpan? =
      quotes.mapNotNull { firstPairBefore(chars, from, it) }.maxByOrNull { it.close }

    override fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan? =
      nearestForward(chars, offset) ?: nearestBackward(chars, offset)

    private fun containing(chars: CharSequence, offset: Int, quote: Char): DelimiterSpan? =
      pairsOnLineOf(chars, offset, quote).firstOrNull { offset in it.open..it.close }

    private fun firstPairAfter(chars: CharSequence, from: Int, quote: Char): DelimiterSpan? =
      pairsOnLineOf(chars, from, quote).firstOrNull { it.open >= from }

    private fun firstPairBefore(chars: CharSequence, from: Int, quote: Char): DelimiterSpan? =
      pairsOnLineOf(chars, from, quote).lastOrNull { it.close < from }

    /** All quote pairs on the line containing [anchor], paired in order from the line start. */
    private fun pairsOnLineOf(chars: CharSequence, anchor: Int, quote: Char): List<DelimiterSpan> {
      val lineStart = lineStartOf(chars, anchor)
      val lineEnd = lineEndOf(chars, anchor)
      val indices = (lineStart until lineEnd).filter { chars[it] == quote }
      return indices.chunked(2).filter { it.size == 2 }.map { DelimiterSpan(it[0], it[1]) }
    }
  }

  /**
   * Locates a separator target: the text between two consecutive instances of a single separator
   * character (`,`, `.`, `;`, ...) on the same line. Unlike pairs, the delimiters are not balanced,
   * so the `a`/`A` modifiers include only the leading separator (leaving a proper list behind).
   */
  private class SeparatorSource(private val separator: Char) : TargetSource {

    override fun enclosing(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? =
      pairsOnLineOf(chars, offset).firstOrNull { it.open <= offset && offset < it.close }

    override fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan? =
      enclosing(chars, start, 1)

    override fun nearestForward(chars: CharSequence, from: Int): DelimiterSpan? =
      pairsOnLineOf(chars, from).firstOrNull { it.open >= from }

    override fun nearestBackward(chars: CharSequence, from: Int): DelimiterSpan? =
      pairsOnLineOf(chars, from).lastOrNull { it.close < from }

    override fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan? =
      nearestForward(chars, offset) ?: nearestBackward(chars, offset)

    override fun toRange(chars: CharSequence, span: DelimiterSpan, modifier: Modifier): TextRange =
      when (modifier) {
        Modifier.INNER -> TextRange(span.open + 1, span.close)
        Modifier.INNER_TRIMMED -> trimmedRange(chars, span.open + 1, span.close)
        // An item: leading separator plus contents, but not the trailing one.
        Modifier.AROUND -> TextRange(span.open, span.close)
        // Both separators plus a trailing whitespace.
        Modifier.AROUND_WHITESPACE -> TextRange(span.open, expandTrailing(chars, span.close + 1))
      }

    /**
     * All separator-bounded spans on the line containing [anchor]. Unlike quotes, separators are
     * not consumed when matched: every adjacent pair forms a span, so `a,b,c` yields `[a,b]` and
     * `[b,c]` sharing the middle separator.
     */
    private fun pairsOnLineOf(chars: CharSequence, anchor: Int): List<DelimiterSpan> {
      val lineStart = lineStartOf(chars, anchor)
      val lineEnd = lineEndOf(chars, anchor)
      val separators = (lineStart until lineEnd).filter { chars[it] == separator }
      return separators.zipWithNext { open, close -> DelimiterSpan(open, close) }
    }
  }

  /**
   * Locates an HTML/XML tag target. A target's [DelimiterSpan] holds the outer `<` of the opening
   * tag and the outer `>` of the matching closing tag; [toRange] re-derives the inner boundaries
   * from the text, so `it` is the content between the tags and `at` is the whole tag pair.
   */
  private object TagSource : TargetSource {

    override fun enclosing(chars: CharSequence, offset: Int, count: Int): DelimiterSpan? =
      containing(allTagPairs(chars), offset).getOrNull(count - 1)

    override fun growing(chars: CharSequence, start: Int, end: Int): DelimiterSpan? {
      val enclosingPairs = allTagPairs(chars)
        .filter { it.open <= start && it.close >= end - 1 }
        .sortedByDescending { it.open }
      // The smallest enclosing tag whose interior is not already exactly the current selection.
      return enclosingPairs.firstOrNull { pair ->
        val (innerStart, innerEnd) = innerOf(chars, pair)
        !(innerStart >= start && innerEnd <= end)
      } ?: enclosingPairs.lastOrNull()
    }

    override fun nearestForward(chars: CharSequence, from: Int): DelimiterSpan? =
      allTagPairs(chars).filter { it.open >= from }.minByOrNull { it.open }

    override fun nearestBackward(chars: CharSequence, from: Int): DelimiterSpan? =
      allTagPairs(chars).filter { it.close < from }.maxByOrNull { it.close }

    override fun seek(chars: CharSequence, offset: Int, lineStart: Int, lineEnd: Int): DelimiterSpan? =
      nearestForward(chars, offset) ?: nearestBackward(chars, offset)

    override fun toRange(chars: CharSequence, span: DelimiterSpan, modifier: Modifier): TextRange {
      val (innerStart, innerEnd) = innerOf(chars, span)
      return when (modifier) {
        Modifier.INNER -> TextRange(innerStart, innerEnd)
        Modifier.INNER_TRIMMED -> trimmedRange(chars, innerStart, innerEnd)
        Modifier.AROUND -> TextRange(span.open, span.close + 1)
        Modifier.AROUND_WHITESPACE -> TextRange(span.open, expandTrailing(chars, span.close + 1))
      }
    }

    /** Tag pairs ordered innermost-first that enclose [offset]. */
    private fun containing(pairs: List<DelimiterSpan>, offset: Int): List<DelimiterSpan> =
      pairs.filter { offset in it.open..it.close }.sortedByDescending { it.open }

    /** The [innerStart, innerEnd) content range between the opening and closing tags. */
    private fun innerOf(chars: CharSequence, span: DelimiterSpan): Pair<Int, Int> {
      val openTagEnd = chars.indexOf('>', span.open)
      val closeTagStart = chars.lastIndexOf('<', span.close)
      return (openTagEnd + 1) to closeTagStart
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

      return source.toRange(chars, pair, modifier)
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

    private val SEPARATORS = listOf(
      ',', '.', ';', ':', '+', '-', '=', '~', '_', '*', '#', '/', '|', '\\', '&', '$',
    )

    private val SINGLE_QUOTE = QuoteSource(listOf('\''))
    private val DOUBLE_QUOTE = QuoteSource(listOf('"'))
    private val BACK_TICK = QuoteSource(listOf('`'))
    private val ANY_QUOTE = QuoteSource(listOf('\'', '"', '`'))

    private val TRIGGERS: Map<Char, TargetSource> = mapOf(
      '(' to PAREN, ')' to PAREN,
      '{' to CURLY, '}' to CURLY, 'B' to CURLY,
      '[' to BRACKET, ']' to BRACKET,
      '<' to ANGLE, '>' to ANGLE,
      'b' to ANY_BLOCK,
      '\'' to SINGLE_QUOTE, '"' to DOUBLE_QUOTE, '`' to BACK_TICK,
      'q' to ANY_QUOTE,
      't' to TagSource,
    ) + SEPARATORS.associateWith { SeparatorSource(it) }

    private data class OpenTag(val start: Int, val name: String)

    /** All matched tag pairs in the buffer, each as the outer `<` and outer `>` offsets. */
    private fun allTagPairs(chars: CharSequence): List<DelimiterSpan> {
      val openTags = ArrayDeque<OpenTag>()
      val pairs = ArrayList<DelimiterSpan>()
      var i = 0
      while (i < chars.length) {
        if (chars[i] != '<') {
          i++
          continue
        }
        val closeAngle = chars.indexOf('>', i + 1)
        if (closeAngle < 0) break
        val body = chars.subSequence(i + 1, closeAngle).toString()
        when {
          isClosingTag(body) -> closeTag(openTags, tagName(body.substring(1)), closeAngle)?.let { pairs.add(it) }
          isSelfClosingTag(body) -> {} // no pair
          isOpeningTag(body) -> openTags.addLast(OpenTag(i, tagName(body)))
        }
        i = closeAngle + 1
      }
      return pairs
    }

    private fun isClosingTag(body: String): Boolean = body.startsWith("/")
    private fun isSelfClosingTag(body: String): Boolean = body.endsWith("/")
    private fun isOpeningTag(body: String): Boolean = body.isNotEmpty() && body[0].isLetter()

    // Tag names are matched case-insensitively, like Vim's `it`/`at`.
    private fun tagName(body: String): String =
      body.trim().takeWhile { !it.isWhitespace() && it != '/' }.lowercase()

    /**
     * Matches the closing tag at [closeAngle] against the innermost open tag of the same [name],
     * discarding any unclosed tags nested inside it. Returns the completed pair, or null if no open
     * tag matches.
     */
    private fun closeTag(openTags: ArrayDeque<OpenTag>, name: String, closeAngle: Int): DelimiterSpan? {
      val matchIndex = openTags.indexOfLast { it.name == name }
      if (matchIndex < 0) return null
      val opening = openTags[matchIndex]
      while (openTags.size > matchIndex) openTags.removeLast()
      return DelimiterSpan(opening.start, closeAngle)
    }

    private fun lineStartOf(chars: CharSequence, offset: Int): Int {
      var i = offset.coerceAtMost(chars.length)
      while (i > 0 && chars[i - 1] != '\n') i--
      return i
    }

    private fun lineEndOf(chars: CharSequence, offset: Int): Int {
      var i = offset
      while (i < chars.length && chars[i] != '\n') i++
      return i
    }

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

    private fun applyBalancedModifier(chars: CharSequence, span: DelimiterSpan, modifier: Modifier): TextRange {
      val openIdx = span.open
      val closeIdx = span.close
      return when (modifier) {
        Modifier.AROUND -> TextRange(openIdx, closeIdx + 1)
        Modifier.INNER -> TextRange(openIdx + 1, closeIdx)
        Modifier.INNER_TRIMMED -> trimmedRange(chars, openIdx + 1, closeIdx)
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

    /** [start, end) with any leading/trailing inline whitespace removed. */
    private fun trimmedRange(chars: CharSequence, start: Int, end: Int): TextRange {
      var s = start
      var e = end
      while (s < e && chars[s].isInlineWhitespace()) s++
      while (e > s && chars[e - 1].isInlineWhitespace()) e--
      return TextRange(s, e)
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
