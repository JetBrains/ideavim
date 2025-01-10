/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.parser.generated.RegexParser
import com.maddyhome.idea.vim.parser.generated.RegexParserBaseVisitor
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AfterColumnCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AfterColumnMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AfterLineCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AfterLineMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AfterMarkMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AtColumnCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AtColumnMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AtLineCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AtLineMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.AtMarkMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BackreferenceMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BeforeColumnCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BeforeColumnMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BeforeLineCursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BeforeLineMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BeforeMarkMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.CharacterMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.CollectionMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.CollectionRange
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.CursorMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.EditorAwarePredicateMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.EndOfFileMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.EndOfLineMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.EndOfWordMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.PredicateMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.StartOfFileMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.StartOfLineMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.StartOfWordMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.VisualAreaMatcher

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
   */
  private val groupNumbers: HashMap<RegexParser.GroupingCaptureContext, Int> = HashMap()

  /**
   * Determines whether the visited tree contains
   */
  internal var hasUpperCase: Boolean = false

  override fun visitPattern(ctx: RegexParser.PatternContext): NFA {
    hasUpperCase = false
    groupCount = 0
    groupNumbers.clear()
    groupCount++
    val subNfa = visit(ctx.sub_pattern())
    subNfa.capture(0, false)
    return subNfa
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
    return nfaStart.concatenate(if (ctx.concats.isNotEmpty()) visit(ctx.concats.last()) else NFA.fromSingleState())
      .concatenate(nfaEnd)
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
      is Multi.AssertionMulti -> return visit(ctx.atom()).assert(
        shouldConsume = false,
        isPositive = multi.isPositive,
        isAhead = multi.isAhead,
        limit = multi.limit
      )
    }
  }

  private fun buildQuantifiedNFA(atom: RegexParser.AtomContext, range: Multi.RangeMulti): NFA {
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
    if (!range.isGreedy) prefixNFA.startState.hasLazyMulti = true
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
    val base = { editor: VimEditor, char: Char -> KeywordOptionHelper.isKeyword(editor, char) }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(EditorAwarePredicateMatcher { editor, char -> char == '\n' || base(editor, char) })
    else
      NFA.fromMatcher(EditorAwarePredicateMatcher { editor, char -> base(editor, char) })
  }

  override fun visitKeywordNotDigit(ctx: RegexParser.KeywordNotDigitContext): NFA {
    val base = { editor: VimEditor, char: Char -> !char.isDigit() && KeywordOptionHelper.isKeyword(editor, char) }
    return if (ctx.text.contains('_'))
      NFA.fromMatcher(EditorAwarePredicateMatcher { editor, char -> char == '\n' || base(editor, char) })
    else
      NFA.fromMatcher(EditorAwarePredicateMatcher { editor, char -> base(editor, char) })
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
    return NFA.fromMatcher(
      CollectionMatcher(
        setOf(' ', '\t'),
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
  }

  override fun visitNotWhitespace(ctx: RegexParser.NotWhitespaceContext): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        setOf(' ', '\t'),
        isNegated = true,
        includesEOL = ctx.text.contains('_'),
        forceNoIgnoreCase = true
      )
    )
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

  private fun visitCollection(
    collectionElements: List<RegexParser.Collection_elemContext>,
    isNegated: Boolean,
    includesEOL: Boolean,
  ): NFA {
    val individualChars: HashSet<Char> = HashSet()
    val range: ArrayList<CollectionRange> = ArrayList()
    val charClasses: ArrayList<(Char) -> Boolean> = ArrayList()
    val collectionElementVisitor = CollectionElementVisitor()
    var containsEOL = false

    for (elem in collectionElements) {
      val result = collectionElementVisitor.visit(elem)
      containsEOL = containsEOL || result.second
      val element = result.first
      when (element) {
        is CollectionElement.SingleCharacter -> {
          hasUpperCase = hasUpperCase || element.char.isUpperCase()
          individualChars.add(element.char)
        }

        is CollectionElement.CharacterRange -> range.add(CollectionRange(element.start, element.end))
        is CollectionElement.CharacterClassExpression -> charClasses.add(element.predicate)
      }
    }

    /**
     * If the collection is empty, match literally with '[]', or '[^]' if negated
     */
    if (individualChars.isEmpty() && range.isEmpty() && charClasses.isEmpty())
      return if (isNegated) NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher('^')))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))
      else NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))

    return NFA.fromMatcher(
      CollectionMatcher(
        individualChars,
        range,
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

  override fun visitDecimalCode(ctx: RegexParser.DecimalCodeContext): NFA {
    return NFA.fromMatcher(
      CharacterMatcher(
        Char((if (ctx.text[0] == '\\') ctx.text.substring(3) else ctx.text.substring(2)).toInt())
      )
    )
  }

  override fun visitOctalCode(ctx: RegexParser.OctalCodeContext): NFA {
    val code = (if (ctx.text[0] == '\\') ctx.text.substring(3) else ctx.text.substring(2)).toInt(8)

    /**
     * An octal code can only go up to 0o377. But the parser still allows codes like 0o400. For these cases, the actual
     * code should be 0o40, and that is followed by a literal '0'
     */
    return if (code > "377".toInt(8)) {
      NFA.fromMatcher(
        CharacterMatcher(
          Char((if (ctx.text[0] == '\\') ctx.text.substring(3) else ctx.text.substring(2)).dropLast(1).toInt(8))
        )
      ).concatenate(
        NFA.fromMatcher(
          CharacterMatcher(ctx.text.last())
        )
      )
    } else {
      NFA.fromMatcher(
        CharacterMatcher(
          Char(code)
        )
      )
    }
  }

  override fun visitHexCode(ctx: RegexParser.HexCodeContext): NFA {
    return NFA.fromMatcher(
      CharacterMatcher(
        Char((if (ctx.text[0] == '\\') ctx.text.substring(3) else ctx.text.substring(2)).toInt(16))
      )
    )
  }

  override fun visitLine(ctx: RegexParser.LineContext): NFA {
    return NFA.fromMatcher(
      AtLineMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 2 else 1, ctx.text.length - 1).toInt())
    )
  }

  override fun visitBeforeLine(ctx: RegexParser.BeforeLineContext): NFA {
    return NFA.fromMatcher(
      BeforeLineMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 3 else 2, ctx.text.length - 1).toInt())
    )
  }

  override fun visitAfterLine(ctx: RegexParser.AfterLineContext): NFA {
    return NFA.fromMatcher(
      AfterLineMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 3 else 2, ctx.text.length - 1).toInt())
    )
  }

  override fun visitColumn(ctx: RegexParser.ColumnContext): NFA {
    return NFA.fromMatcher(
      AtColumnMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 2 else 1, ctx.text.length - 1).toInt())
    )
  }

  override fun visitBeforeColumn(ctx: RegexParser.BeforeColumnContext): NFA {
    return NFA.fromMatcher(
      BeforeColumnMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 3 else 2, ctx.text.length - 1).toInt())
    )
  }

  override fun visitAfterColumn(ctx: RegexParser.AfterColumnContext): NFA {
    return NFA.fromMatcher(
      AfterColumnMatcher(ctx.text.substring(if (ctx.text[0] == '\\') 3 else 2, ctx.text.length - 1).toInt())
    )
  }

  override fun visitLineCursor(ctx: RegexParser.LineCursorContext?): NFA {
    return NFA.fromMatcher(AtLineCursorMatcher())
  }

  override fun visitBeforeLineCursor(ctx: RegexParser.BeforeLineCursorContext?): NFA {
    return NFA.fromMatcher(BeforeLineCursorMatcher())
  }

  override fun visitAfterLineCursor(ctx: RegexParser.AfterLineCursorContext?): NFA {
    return NFA.fromMatcher(AfterLineCursorMatcher())
  }

  override fun visitColumnCursor(ctx: RegexParser.ColumnCursorContext?): NFA {
    return NFA.fromMatcher(AtColumnCursorMatcher())
  }

  override fun visitBeforeColumnCursor(ctx: RegexParser.BeforeColumnCursorContext?): NFA {
    return NFA.fromMatcher(BeforeColumnCursorMatcher())
  }

  override fun visitAfterColumnCursor(ctx: RegexParser.AfterColumnCursorContext?): NFA {
    return NFA.fromMatcher(AfterColumnCursorMatcher())
  }

  override fun visitOptionallyMatched(ctx: RegexParser.OptionallyMatchedContext): NFA {
    if (ctx.atoms.isEmpty()) {
      return NFA.fromSingleState()
    } // TODO: Throw E70 error

    val nfa = NFA.fromSingleState()
    for (atom in ctx.atoms) nfa.concatenate(visit(atom).optional(true))
    return nfa
  }

  override fun visitVisual(ctx: RegexParser.VisualContext?): NFA {
    return NFA.fromMatcher(VisualAreaMatcher())
  }

  override fun visitMark(ctx: RegexParser.MarkContext): NFA {
    return NFA.fromMatcher(
      AtMarkMatcher(ctx.text[if (ctx.text[0] == '\\') 3 else 2])
    )
  }

  override fun visitBeforeMark(ctx: RegexParser.BeforeMarkContext): NFA {
    return NFA.fromMatcher(
      BeforeMarkMatcher(ctx.text[if (ctx.text[0] == '\\') 4 else 3])
    )
  }

  override fun visitAfterMark(ctx: RegexParser.AfterMarkContext): NFA {
    return NFA.fromMatcher(
      AfterMarkMatcher(ctx.text[if (ctx.text[0] == '\\') 4 else 3])
    )
  }

  private fun cleanLiteralChar(str: String): Char {
    return if (str.length == 2 && str[0] == '\\') {
      hasUpperCase = hasUpperCase || str[1].isUpperCase()
      str[1]
    } else {
      hasUpperCase = hasUpperCase || str[0].isUpperCase()
      str[0]
    }
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
