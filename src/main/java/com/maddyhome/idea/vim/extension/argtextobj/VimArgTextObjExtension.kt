/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.argtextobj

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.TextObjectRange
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.annotations.Nls
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * The pairs of brackets that delimit different types of argument lists.
 */
private class BracketPairs(openBrackets: String, closeBrackets: String) {
  // NOTE: brackets must match by the position, and ordered by rank (highest to lowest).
  private val openBrackets: String
  private val closeBrackets: String

  class ParseException(message: String) : Exception(message)

  private enum class ParseState {
    OPEN,
    COLON,
    CLOSE,
    COMMA,
  }

  init {
    assert(openBrackets.length == closeBrackets.length)
    this.openBrackets = openBrackets
    this.closeBrackets = closeBrackets
  }

  fun getBracketPrio(ch: Char): Int {
    return max(openBrackets.indexOf(ch), closeBrackets.indexOf(ch))
  }

  fun matchingBracket(ch: Char): Char {
    var idx = closeBrackets.indexOf(ch)
    if (idx != -1) {
      return openBrackets[idx]
    } else {
      assert(isOpenBracket(ch.code))
      idx = openBrackets.indexOf(ch)
      return closeBrackets[idx]
    }
  }

  fun isCloseBracket(ch: Int): Boolean {
    return closeBrackets.indexOf(ch.toChar()) != -1
  }

  fun isOpenBracket(ch: Int): Boolean {
    return openBrackets.indexOf(ch.toChar()) != -1
  }

  companion object {
    /**
     * Constructs @ref BracketPair from a string of bracket pairs with the same syntax
     * as VIM's @c matchpairs option: "(:),{:},[:]"
     *
     * @param bracketPairs comma-separated list of colon-separated bracket pairs.
     * @throws ParseException if a syntax error is detected.
     */
    @Throws(ParseException::class)
    fun fromBracketPairList(bracketPairs: String): BracketPairs {
      val openBrackets = StringBuilder()
      val closeBrackets = StringBuilder()
      var state = ParseState.OPEN
      for (ch in bracketPairs.toCharArray()) {
        when (state) {
          ParseState.OPEN -> {
            openBrackets.append(ch)
            state = ParseState.COLON
          }

          ParseState.COLON -> if (ch == ':') {
            state = ParseState.CLOSE
          } else {
            throw ParseException("expecting ':', but got '$ch' instead")
          }

          ParseState.CLOSE -> {
            val lastOpenBracket = openBrackets[openBrackets.length - 1]
            if (lastOpenBracket == ch) {
              throw ParseException("open and close brackets must be different")
            }
            closeBrackets.append(ch)
            state = ParseState.COMMA
          }

          ParseState.COMMA -> if (ch == ',') {
            state = ParseState.OPEN
          } else {
            throw ParseException("expecting ',', but got '$ch' instead")
          }
        }
      }
      if (state != ParseState.COMMA) {
        throw ParseException("list of pairs is incomplete")
      }
      return BracketPairs(openBrackets.toString(), closeBrackets.toString())
    }
  }
}

class VimArgTextObjExtension : VimExtension {
  override fun getName(): String = "argtextobj"

  override fun init() {
    val api = api()
    api.textObjects {
      register("ia", preserveSelectionAnchor = false) { count ->
        findArgumentRange(isInner = true, count)
      }
      register("aa", preserveSelectionAnchor = false) { count ->
        findArgumentRange(isInner = false, count)
      }
    }
  }
}

/**
 * Helper class to find argument boundaries starting at the specified
 * position
 */
private class ArgBoundsFinder(
  private val text: CharSequence,
  private val api: VimApi,
  private val brackets: BracketPairs,
) {
  var leftBound: Int = Int.MAX_VALUE
    private set
  var rightBound: Int = Int.MIN_VALUE
    private set
  private var leftBracket = 0
  private var rightBracket = 0

  @Nls
  private var error: @Nls String? = null

  // Line info methods that call VimApi directly
  private fun getLineNumber(offset: Int) = api.editor { read { getLine(offset).number } }
  private fun getLineStartOffset(lineNo: Int) = api.editor { read { getLineStartOffset(lineNo) } }
  private fun getLineEndOffset(lineNo: Int) = api.editor { read { getLineEndOffset(lineNo, true) } }
  private fun getLineCount() = api.editor { read { lineCount } }

  /**
   * Finds left and right boundaries of an argument at the specified
   * position. If successful @ref getLeftBound() will point to the left
   * argument delimiter and @ref getRightBound() will point to the right
   * argument delimiter. Use @ref adjustForInner or @ref adjustForOuter to
   * fix the boundaries based on the type of text object.
   *
   * @param position starting position.
   */
  @Throws(IllegalStateException::class)
  fun findBoundsAt(position: Int): Boolean {
    if (text.isEmpty()) {
      error = "empty document"
      return false
    }
    leftBound = min(position, leftBound)
    rightBound = max(position, rightBound)
    this.outOfQuotedText
    if (rightBound == leftBound) {
      if (brackets.isCloseBracket(getCharAt(rightBound).code)) {
        --leftBound
      } else {
        ++rightBound
      }
    }
    var nextLeft = leftBound
    var nextRight = rightBound
    val leftLimit = leftLimit(position)
    val rightLimit = rightLimit(position)
    //
    // Try to extend the bounds until one of the bounds is a comma.
    // This handles cases like: fun(a, (30 + <cursor>x) * 20, c)
    //
    var bothBrackets: Boolean
    do {
      leftBracket = nextLeft
      rightBracket = nextRight
      if (!findOuterBrackets(leftLimit, rightLimit)) {
        error = "not inside argument list"
        return false
      }
      leftBound = nextLeft
      findLeftBound()
      nextLeft = leftBound - 1
      rightBound = nextRight
      findRightBound()
      nextRight = rightBound + 1
      //
      // If reached text boundaries
      //
      if (nextLeft < leftLimit || nextRight > rightLimit) {
        error = "not an argument"
        return false
      }
      bothBrackets = getCharAt(leftBound) != ',' && getCharAt(rightBound) != ','
      val nonEmptyArg = (rightBound - leftBound) > 1
      if (bothBrackets && nonEmptyArg && this.isIdentPreceding) {
        // Looking at a pair of brackets preceded by an
        // identifier -- single argument function call.
        break
      }
    } while (leftBound > leftLimit && rightBound < rightLimit && bothBrackets)
    return true
  }

  /**
   * Skip left delimiter character and any following whitespace.
   */
  fun adjustForInner() {
    ++leftBound
    while (leftBound < rightBound && Character.isWhitespace(getCharAt(leftBound))) {
      ++leftBound
    }
  }

  /**
   * Exclude left bound character for the first argument, include the
   * right bound character and any following whitespace.
   */
  fun adjustForOuter() {
    if (getCharAt(leftBound) != ',') {
      ++leftBound
      extendTillNext()
    }
  }

  /**
   * Extend the right bound to the beginning of the next argument (if any).
   */
  fun extendTillNext() {
    if (rightBound + 1 < rightBracket && getCharAt(rightBound) == ',') {
      ++rightBound
      while (rightBound + 1 < rightBracket && Character.isWhitespace(getCharAt(rightBound))) {
        ++rightBound
      }
    }
  }

  val isIdentPreceding: Boolean
    get() {
      var i = leftBound - 1
      val idEnd = i
      while (i >= 0 && Character.isJavaIdentifierPart(getCharAt(i))) {
        --i
      }
      return (idEnd - i) > 0 && Character.isJavaIdentifierStart(getCharAt(i + 1))
    }

  val outOfQuotedText: Unit
    /**
     * Detects if current position is inside a quoted string and adjusts
     * left and right bounds to the boundaries of the string.
     *
     * NOTE: Does not support line continuations for quoted string ('\' at the end of line).
     */
    get() {
      // TODO this method should use IdeaVim methods to determine if the current position is in the string
      val lineNo = getLineNumber(leftBound)
      val lineStartOffset = getLineStartOffset(lineNo)
      val lineEndOffset = getLineEndOffset(lineNo)
      var i = lineStartOffset
      while (i <= leftBound) {
        if (isQuote(i)) {
          val endOfQuotedText = skipQuotedTextForward(i, lineEndOffset)
          if (endOfQuotedText >= leftBound) {
            leftBound = i - 1
            rightBound = endOfQuotedText + 1
            break
          } else {
            i = endOfQuotedText
          }
        }
        ++i
      }
    }

  private fun findRightBound() {
    while (rightBound < rightBracket) {
      val ch = getCharAt(rightBound)
      if (ch == ',') {
        break
      }
      if (brackets.isOpenBracket(ch.code)) {
        rightBound = skipSexp(rightBound, rightBracket, SexpDirection.forward(brackets))
      } else {
        if (isQuoteChar(ch.code)) {
          rightBound = skipQuotedTextForward(rightBound, rightBracket)
        }
        ++rightBound
      }
    }
  }

  private fun findLeftBound() {
    while (leftBound > leftBracket) {
      val ch = getCharAt(leftBound)
      if (ch == ',') {
        break
      }
      if (brackets.isCloseBracket(ch.code)) {
        leftBound = skipSexp(leftBound, leftBracket, SexpDirection.backward(brackets))
      } else {
        if (isQuoteChar(ch.code)) {
          leftBound = skipQuotedTextBackward(leftBound, leftBracket)
        }
        --leftBound
      }
    }
  }

  private fun isQuote(i: Int): Boolean {
    return QUOTES.indexOf(getCharAt(i)) != -1
  }

  private fun getCharAt(logicalOffset: Int): Char {
    require(logicalOffset >= 0 && logicalOffset < text.length) {
      "Offset $logicalOffset out of bounds [0, ${text.length})"
    }
    return text[logicalOffset]
  }

  private fun skipQuotedTextForward(start: Int, end: Int): Int {
    require(start < end) { "start ($start) must be less than end ($end)" }
    val quoteChar = getCharAt(start)
    var backSlash = false
    var i = start + 1

    while (i <= end) {
      val ch = getCharAt(i)
      if (ch == quoteChar && !backSlash) {
        // Found a matching quote, and it's not escaped.
        break
      } else {
        backSlash = ch == '\\' && !backSlash
      }
      ++i
    }
    return i
  }

  private fun skipQuotedTextBackward(start: Int, end: Int): Int {
    require(start > end) { "start ($start) must be greater than end ($end)" }
    val quoteChar = getCharAt(start)
    var i = start - 1

    while (i > end) {
      val ch = getCharAt(i)
      val prevChar = getCharAt(i - 1)
      // NOTE: doesn't handle cases like \\"str", but they make no
      //       sense anyway.
      if (ch == quoteChar && prevChar != '\\') {
        // Found a matching quote, and it's not escaped.
        break
      }
      --i
    }
    return i
  }

  private fun leftLimit(pos: Int): Int {
    val offsetLimit = max(pos - MAX_SEARCH_OFFSET, 0)
    val lineNo = getLineNumber(pos)
    val lineOffsetLimit = getLineStartOffset(max(0, lineNo - MAX_SEARCH_LINES))
    return max(offsetLimit, lineOffsetLimit)
  }

  private fun rightLimit(pos: Int): Int {
    val offsetLimit = min(pos + MAX_SEARCH_OFFSET, text.length)
    val lineNo = getLineNumber(pos)
    val lineOffsetLimit = getLineEndOffset(min(getLineCount() - 1, lineNo + MAX_SEARCH_LINES))
    return min(offsetLimit, lineOffsetLimit)
  }

  fun errorMessage(): String? {
    return error
  }

  /**
   * Interface to parametrise S-expression traversal direction.
   */
  abstract class SexpDirection {
    abstract fun delta(): Int

    abstract fun isOpenBracket(ch: Char): Boolean

    abstract fun isCloseBracket(ch: Char): Boolean

    abstract fun skipQuotedText(pos: Int, end: Int, self: ArgBoundsFinder): Int

    companion object {
      fun forward(brackets: BracketPairs): SexpDirection {
        return object : SexpDirection() {
          override fun delta(): Int {
            return 1
          }

          override fun isOpenBracket(ch: Char): Boolean {
            return brackets.isOpenBracket(ch.code)
          }

          override fun isCloseBracket(ch: Char): Boolean {
            return brackets.isCloseBracket(ch.code)
          }

          override fun skipQuotedText(pos: Int, end: Int, self: ArgBoundsFinder): Int {
            return self.skipQuotedTextForward(pos, end)
          }
        }
      }

      fun backward(brackets: BracketPairs): SexpDirection {
        return object : SexpDirection() {
          override fun delta(): Int {
            return -1
          }

          override fun isOpenBracket(ch: Char): Boolean {
            return brackets.isCloseBracket(ch.code)
          }

          override fun isCloseBracket(ch: Char): Boolean {
            return brackets.isOpenBracket(ch.code)
          }

          override fun skipQuotedText(pos: Int, end: Int, self: ArgBoundsFinder): Int {
            return self.skipQuotedTextBackward(pos, end)
          }
        }
      }
    }
  }

  /**
   * Skip over an S-expression considering priorities when unbalanced.
   *
   * @param start position of the starting bracket.
   * @param end   maximum position
   * @param dir   direction instance
   * @return position after the S-expression, or the next to the start position if
   * unbalanced.
   */
  private fun skipSexp(start: Int, end: Int, dir: SexpDirection): Int {
    val lastChar = getCharAt(start)
    assert(dir.isOpenBracket(lastChar))
    val bracketStack: Deque<Char?> = ArrayDeque<Char?>()
    bracketStack.push(lastChar)
    var i = start + dir.delta()
    while (!bracketStack.isEmpty() && i != end) {
      val ch = getCharAt(i)
      if (dir.isOpenBracket(ch)) {
        bracketStack.push(ch)
      } else {
        if (dir.isCloseBracket(ch)) {
          if (bracketStack.getLast() == brackets.matchingBracket(ch)) {
            bracketStack.pop()
          } else {
            if (brackets.getBracketPrio(ch) < brackets.getBracketPrio(bracketStack.getLast()!!)) {
              // (<...) ->  (...)
              bracketStack.pop()
              // Retry the same character again for cases like (...<<...).
              continue
            } else {                        // Unbalanced brackets -- check ranking.
              // Ignore lower-priority closing brackets.
              // (...> ->  (....
            }
          }
        } else {
          if (isQuoteChar(ch.code)) {
            i = dir.skipQuotedText(i, end, this)
          }
        }
      }
      i += dir.delta()
    }
    if (bracketStack.isEmpty()) {
      return i
    } else {
      return start + dir.delta()
    }
  }

  /**
   * Find a pair of brackets surrounding (leftBracket..rightBracket) block.
   *
   * @param start minimum position to look for
   * @param end   maximum position
   * @return true if found
   */
  private fun findOuterBrackets(start: Int, end: Int): Boolean {
    val foundPrev = findPrevOpenBracket(start)
    val foundNext = if (foundPrev) findNextCloseBracket(end) else false
    var hasNewBracket = foundPrev && foundNext
    while (hasNewBracket) {
      val leftPrio = brackets.getBracketPrio(getCharAt(leftBracket))
      val rightPrio = brackets.getBracketPrio(getCharAt(rightBracket))
      if (leftPrio == rightPrio) {
        // matching brackets
        return true
      } else {
        if (leftPrio < rightPrio) {
          if (rightBracket + 1 < end) {
            ++rightBracket
            hasNewBracket = findNextCloseBracket(end)
          } else {
            hasNewBracket = false
          }
        } else {
          if (leftBracket > 1) {
            --leftBracket
            hasNewBracket = findPrevOpenBracket(start)
          } else {
            hasNewBracket = false
          }
        }
      }
    }
    return false
  }

  /**
   * Finds unmatched open bracket starting at @a leftBracket.
   *
   * @param start minimum position.
   * @return true if found
   */
  private fun findPrevOpenBracket(start: Int): Boolean {
    var ch: Char
    while (!brackets.isOpenBracket(getCharAt(leftBracket).also { ch = it }.code)) {
      if (brackets.isCloseBracket(ch.code)) {
        leftBracket = skipSexp(leftBracket, start, SexpDirection.backward(brackets))
      } else {
        if (isQuoteChar(ch.code)) {
          leftBracket = skipQuotedTextBackward(leftBracket, start)
        } else {
          if (leftBracket == start) {
            return false
          }
        }
        --leftBracket
      }
    }
    return true
  }

  /**
   * Finds unmatched close bracket starting at @a rightBracket.
   *
   * @param end maximum position.
   * @return true if found
   */
  private fun findNextCloseBracket(end: Int): Boolean {
    var ch: Char
    while (!brackets.isCloseBracket(getCharAt(rightBracket).also { ch = it }.code)) {
      if (brackets.isOpenBracket(ch.code)) {
        rightBracket = skipSexp(rightBracket, end, SexpDirection.forward(brackets))
      } else {
        if (isQuoteChar(ch.code)) {
          rightBracket = skipQuotedTextForward(rightBracket, end)
        }
        ++rightBracket
      }
      if (rightBracket >= end) {
        return false
      }
    }
    return true
  }

  companion object {
    private const val QUOTES = "\"'"

    private const val MAX_SEARCH_LINES = 10
    private val MAX_SEARCH_OFFSET: Int = MAX_SEARCH_LINES * 80

    private fun isQuoteChar(ch: Int): Boolean {
      return QUOTES.indexOf(ch.toChar()) != -1
    }
  }
}

private object ArgTextObjUtil {
  val DEFAULT_BRACKET_PAIRS = BracketPairs("(", ")")

  fun bracketPairsVariable(): String? {
    val value: Any? = VimPlugin.getVariableService().getGlobalVariableValue("argtextobj_pairs")
    if (value is VimString) {
      return value.value
    }
    return null
  }
}

/**
 * Find argument range using the new VimApi.
 */
private fun VimApi.findArgumentRange(isInner: Boolean, count: Int): TextObjectRange? {
  var bracketPairs: BracketPairs = ArgTextObjUtil.DEFAULT_BRACKET_PAIRS
  val bracketPairsVar: String? = ArgTextObjUtil.bracketPairsVariable()
  if (bracketPairsVar != null) {
    try {
      bracketPairs = BracketPairs.fromBracketPairList(bracketPairsVar)
    } catch (parseException: BracketPairs.ParseException) {
      @VimNlsSafe val message =
        MessageHelper.message("argtextobj.error.invalid.value.of.g.argtextobj.pairs.0", parseException.message ?: "")
      VimPlugin.showMessage(message)
      VimPlugin.indicateError()
      return null
    }
  }

  val (text, caretOffset) = editor { read { text to withPrimaryCaret { offset } } }
  val finder = ArgBoundsFinder(text, this, bracketPairs)
  var pos = caretOffset

  for (i in 0..<count) {
    if (!finder.findBoundsAt(pos)) {
      VimPlugin.showMessage(finder.errorMessage())
      VimPlugin.indicateError()
      return null
    }
    if (i + 1 < count) {
      finder.extendTillNext()
    }

    pos = finder.rightBound
  }

  if (isInner) {
    finder.adjustForInner()
  } else {
    finder.adjustForOuter()
  }
  return TextObjectRange.CharacterWise(finder.leftBound, finder.rightBound)
}
