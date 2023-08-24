/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.nfa.matcher.BackreferenceMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CharacterMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionRange
import com.maddyhome.idea.vim.regexp.nfa.matcher.CursorMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.EndOfFileMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.EndOfLineMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.EndOfWordMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.PredicateMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.StartOfFileMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.StartOfLineMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.StartOfWordMatcher
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParserBaseVisitor

/**
 * A tree visitor for converting a parsed Vim pattern into an internal
 * NFA, that is then used to then find matches in an editor.
 * This is a singleton.
 */
internal object PatternVisitor : RegexParserBaseVisitor<NFA>() {

  /**
   * Tracks the number of capture groups visited
   */
  private var groupCount: Int = 0

  /**
   * Maps tree nodes representing capture groups to their respective group number
   *
   */
  private val groupNumbers: HashMap<RegexParser.GroupingCaptureContext, Int> = HashMap()

  override fun visitPattern(ctx: RegexParser.PatternContext): NFA {
    groupCount = 0
    groupNumbers.clear()
    val groupNumber = groupCount
    groupCount++
    val subnfa = visit(ctx.sub_pattern())
    subnfa.capture(groupNumber, false)
    return NFA.fromMatcher(DotMatcher(true)).closure(false).concatenate(subnfa)
  }
  override fun visitSub_pattern(ctx: RegexParser.Sub_patternContext): NFA {
    return ctx.branches.map { visitBranch(it) }.union()
  }

  override fun visitBranch(ctx: RegexParser.BranchContext): NFA {
    val nfaStart = if (ctx.CARET() != null) NFA.fromMatcher(StartOfLineMatcher()) else NFA.fromSingleState()
    val nfaEnd = if (ctx.DOLLAR() != null) NFA.fromMatcher(EndOfLineMatcher()) else NFA.fromSingleState()

    for (concat in ctx.concats.dropLast(1)) {
      val subNFA = visit(concat)
      subNFA.assert(shouldConsume = false, isPositive = true, isAhead = true)
      nfaStart.concatenate(subNFA)
    }
    return nfaStart.concatenate(visit(ctx.concats.last())).concatenate(nfaEnd)
  }

  override fun visitConcat(ctx: RegexParser.ConcatContext): NFA {
    return ctx.pieces.map { visitPiece(it) }.concatenate()
  }

  override fun visitPiece(ctx: RegexParser.PieceContext): NFA {
    if (ctx.multi() == null) return visit(ctx.atom())

    val multi = MultiVisitor().visit(ctx.multi())

    return when (multi) {
      is Multi.RangeMulti -> buildQuantifiedNFA(ctx.atom(), multi)
      is Multi.AtomicMulti -> return visit(ctx.atom()).assert(shouldConsume = true, isPositive = true, isAhead = true)
      is Multi.AssertionMulti -> return visit(ctx.atom()).assert(shouldConsume = false, isPositive = multi.isPositive, isAhead = multi.isAhead)
    }
  }

  private fun buildQuantifiedNFA(atom: RegexParser.AtomContext, range: Multi.RangeMulti) : NFA {
    val prefixNFA = NFA.fromSingleState()
    for (i in 0 until range.lowerBoundary.i)
      prefixNFA.concatenate(visit(atom))

    var suffixNFA = NFA.fromSingleState()
    if (range.upperBoundary is RangeBoundary.InfiniteRangeBoundary) suffixNFA = visit(atom).closure(range.isGreedy)
    else {
      for (i in range.lowerBoundary.i until (range.upperBoundary as RangeBoundary.IntRangeBoundary).i) {
        suffixNFA.concatenate(visit(atom))
        suffixNFA.optional(range.isGreedy)
      }
    }

    prefixNFA.concatenate(suffixNFA)
    if (atom is RegexParser.GroupingCaptureContext)
      groupNumbers[atom]?.let { prefixNFA.capture(it, false) }
    return prefixNFA
  }

  override fun visitGroupingCapture(ctx: RegexParser.GroupingCaptureContext): NFA {
    val groupNumber = groupNumbers[ctx] ?: groupCount.also { groupNumbers[ctx] = it; groupCount++ }

    val nfa = if (ctx.sub_pattern() == null) NFA.fromSingleState() else visit(ctx.sub_pattern())
    nfa.capture(groupNumber)

    return nfa
  }

  override fun visitGroupingNoCapture(ctx: RegexParser.GroupingNoCaptureContext): NFA {
    return if (ctx.sub_pattern() == null) NFA.fromSingleState()
    else visit(ctx.sub_pattern())
  }

  override fun visitLiteralChar(ctx: RegexParser.LiteralCharContext): NFA {
    return NFA.fromMatcher(CharacterMatcher(cleanLiteralChar(ctx.text)))
  }

  override fun visitAnyChar(ctx: RegexParser.AnyCharContext?): NFA {
    return NFA.fromMatcher(DotMatcher(false))
  }

  override fun visitAnyCharNL(ctx: RegexParser.AnyCharNLContext?): NFA {
    return NFA.fromMatcher(DotMatcher(true))
  }

  override fun visitCursor(ctx: RegexParser.CursorContext?): NFA {
    return NFA.fromMatcher(CursorMatcher())
  }

  override fun visitIdentifier(ctx: RegexParser.IdentifierContext): NFA {
    val base = { char: Char -> char.isJavaIdentifierPart() }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitIdentifierNotDigit(ctx: RegexParser.IdentifierNotDigitContext): NFA {
    val base = { char: Char -> !char.isDigit() && char.isJavaIdentifierPart() }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitKeyword(ctx: RegexParser.KeywordContext): NFA {
    val base = { char: Char -> char.isLetterOrDigit() || char == '_' }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitKeywordNotDigit(ctx: RegexParser.KeywordNotDigitContext): NFA {
    val base = { char: Char -> char.isLetter() || char == '_' }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitFilename(ctx: RegexParser.FilenameContext): NFA {
    val base = { char: Char -> char.isLetterOrDigit() || "_/.-+,#$%~=".contains(char) }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitFilenameNotDigit(ctx: RegexParser.FilenameNotDigitContext): NFA {
    val base = { char: Char -> char.isLetter() || "_/.-+,#$%~=".contains(char) }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitPrintable(ctx: RegexParser.PrintableContext): NFA {
    val base = { char: Char -> !char.isISOControl() }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitPrintableNotDigit(ctx: RegexParser.PrintableNotDigitContext): NFA {
    val base = { char: Char -> !char.isDigit() && !char.isISOControl() }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(
        PredicateMatcher { char -> char == '\n' || base(char) }
      )
    else
      NFA.fromMatcher(
        PredicateMatcher { char -> base(char) }
      )
  }

  override fun visitWhitespace(ctx: RegexParser.WhitespaceContext): NFA {
    return NFA.fromMatcher(CollectionMatcher(
      setOf(' ', '\t'),
      includesEOL = ctx.text.contains('_'),
      forceNoIgnoreCase = true
    ))
  }

  override fun visitNotWhitespace(ctx: RegexParser.NotWhitespaceContext): NFA {
    return NFA.fromMatcher(CollectionMatcher(
      setOf(' ', '\t'),
      isNegated = true,
      includesEOL = ctx.text.contains('_'),
      forceNoIgnoreCase = true
    ))
  }

  override fun visitDigit(ctx: RegexParser.DigitContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '9')),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotDigit(ctx: RegexParser.NotDigitContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '9')),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitHex(ctx: RegexParser.HexContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'F'),
          CollectionRange('a', 'f'),
        ),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotHex(ctx: RegexParser.NotHexContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'F'),
          CollectionRange('a', 'f'),
        ),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitOctal(ctx: RegexParser.OctalContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '7')),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotOctal(ctx: RegexParser.NotOctalContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '7')),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitWordchar(ctx: RegexParser.WordcharContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = setOf('_'),
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotwordchar(ctx: RegexParser.NotwordcharContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = setOf('_'),
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitHeadofword(ctx: RegexParser.HeadofwordContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = setOf('_'),
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotHeadOfWord(ctx: RegexParser.NotHeadOfWordContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = setOf('_'),
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitAlpha(ctx: RegexParser.AlphaContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotAlpha(ctx: RegexParser.NotAlphaContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitLcase(ctx: RegexParser.LcaseContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('a', 'z')),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotLcase(ctx: RegexParser.NotLcaseContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('a', 'z')),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitUcase(ctx: RegexParser.UcaseContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('A', 'Z')),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotUcase(ctx: RegexParser.NotUcaseContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('A', 'Z')),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitEsc(ctx: RegexParser.EscContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('')
    )
  }

  override fun visitTab(ctx: RegexParser.TabContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('\t')
    )
  }

  override fun visitCR(ctx: RegexParser.CRContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('\r')
    )
  }

  override fun visitBS(ctx: RegexParser.BSContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('\b')
    )
  }

  override fun visitNL(ctx: RegexParser.NLContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('\n')
    )
  }

  override fun visitCollectionPos(ctx: RegexParser.CollectionPosContext): NFA {
    return visitCollection(ctx.collection_elems, false, ctx.COLLECTION_START().text.contains('_'))
  }

  override fun visitCollectionNeg(ctx: RegexParser.CollectionNegContext): NFA {
    return visitCollection(ctx.collection_elems, true, ctx.COLLECTION_START().text.contains('_'))
  }

  private fun visitCollection(collectionElements: List<RegexParser.Collection_elemContext>, isNegated: Boolean, includesEOL: Boolean) : NFA {
    val individualChars: HashSet<Char> = HashSet()
    val ranges: ArrayList<CollectionRange> = ArrayList()
    val charClasses: ArrayList<(Char) -> Boolean> = ArrayList()
    val collectionElementVisitor = CollectionElementVisitor()
    var containsEOL = false

    for (elem in collectionElements) {
      val result = collectionElementVisitor.visit(elem)
      containsEOL = containsEOL || result.second
      val element = result.first
      when (element) {
        is CollectionElement.SingleCharacter -> individualChars.add(element.char)
        is CollectionElement.CharacterRange -> ranges.add(CollectionRange(element.start, element.end))
        is CollectionElement.CharacterClassExpression -> charClasses.add(element.predicate)
      }
    }

    /**
     * If the collection is empty, match literally with '[]', or '[^]' if negated
     */
    if (individualChars.isEmpty() && ranges.isEmpty() && charClasses.isEmpty())
      return if (isNegated) NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher('^')))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))
      else NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))

    return NFA.fromMatcher(
      CollectionMatcher(
        individualChars,
        ranges,
        charClasses,
        isNegated,
        includesEOL || containsEOL
      )
    )
  }

  override fun visitStartMatch(ctx: RegexParser.StartMatchContext?): NFA {
    val nfa = NFA.fromSingleState()
    nfa.startMatch()
    return nfa
  }

  override fun visitEndMatch(ctx: RegexParser.EndMatchContext?): NFA {
    val nfa = NFA.fromSingleState()
    nfa.endMatch()
    return nfa
  }

  override fun visitBackreference(ctx: RegexParser.BackreferenceContext): NFA {
    return NFA.fromMatcher(
      BackreferenceMatcher(ctx.text[1].digitToInt())
    )
  }

  override fun visitStartOfFile(ctx: RegexParser.StartOfFileContext?): NFA {
    return NFA.fromMatcher(StartOfFileMatcher())
  }

  override fun visitEndOfFile(ctx: RegexParser.EndOfFileContext?): NFA {
    return NFA.fromMatcher(EndOfFileMatcher())
  }

  override fun visitStartOfLine(ctx: RegexParser.StartOfLineContext?): NFA {
    return NFA.fromMatcher(StartOfLineMatcher())
  }

  override fun visitEndOfLine(ctx: RegexParser.EndOfLineContext?): NFA {
    return NFA.fromMatcher(EndOfLineMatcher())
  }

  override fun visitStartOfWord(ctx: RegexParser.StartOfWordContext?): NFA {
    return NFA.fromMatcher(StartOfWordMatcher())
  }

  override fun visitEndOfWord(ctx: RegexParser.EndOfWordContext?): NFA {
    return NFA.fromMatcher(EndOfWordMatcher())
  }

  private fun cleanLiteralChar(str : String) : Char {
    return if (str.length == 2 && str[0] == '\\') str[1]
    else str[0]
  }

  private fun List<NFA>.union(): NFA {
    return this.foldIndexed(null as NFA?) { index, acc, elem ->
      if (index == 0) elem
      else acc?.unify(elem) ?: elem
    } ?: NFA.fromSingleState()
  }

  private fun List<NFA>.concatenate(): NFA {
    return this.foldIndexed(null as NFA?) { index, acc, elem ->
      if (index == 0) elem
      else acc?.concatenate(elem) ?: elem
    } ?: NFA.fromSingleState()
  }
}