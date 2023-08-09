/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.nfa.matcher.CharacterMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionRange
import com.maddyhome.idea.vim.regexp.nfa.matcher.CursorMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.PredicateMatcher
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParserBaseVisitor

internal class PatternVisitor : RegexParserBaseVisitor<NFA>() {

  private var groupCount: Int = 0

  override fun visitPattern(ctx: RegexParser.PatternContext): NFA {
    val groupNumber = groupCount
    groupCount++
    val nfa = visit(ctx.sub_pattern())
    nfa.capture(groupNumber)
    return nfa
  }
  override fun visitSub_pattern(ctx: RegexParser.Sub_patternContext): NFA {
    return ctx.branches.map { visitBranch(it) }.union()
  }

  override fun visitConcat(ctx: RegexParser.ConcatContext): NFA {
    return ctx.pieces.map { visitPiece(it) }.concatenate()
  }

  override fun visitPiece(ctx: RegexParser.PieceContext): NFA {
    if (ctx.multi() == null) return visit(ctx.atom())

    val multiVisitor = MultiVisitor()
    val range = multiVisitor.visit(ctx.multi())

    val prefixNFA = NFA.fromSingleState()
    for (i in 0 until range.lowerBoundary.i)
      prefixNFA.concatenate(visit(ctx.atom()))

    var suffixNFA = NFA.fromSingleState()
    if (range.upperBoundary is MultiBoundary.InfiniteMultiBoundary) suffixNFA = visit(ctx.atom()).closure()
    else {
      for (i in range.lowerBoundary.i until (range.upperBoundary as MultiBoundary.IntMultiBoundary).i) {
        suffixNFA.concatenate(visit(ctx.atom()))
        suffixNFA.optional()
      }
    }

    return prefixNFA.concatenate(suffixNFA)
  }

  override fun visitGroupingCapture(ctx: RegexParser.GroupingCaptureContext): NFA {
    val groupNumber = groupCount
    groupCount++
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

  override fun visitIdentifier(ctx: RegexParser.IdentifierContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> char.isJavaIdentifierPart() }
    )
  }

  override fun visitIdentifierNotDigit(ctx: RegexParser.IdentifierNotDigitContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> !char.isDigit() && char.isJavaIdentifierPart() }
    )
  }

  override fun visitKeyword(ctx: RegexParser.KeywordContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> char.isLetterOrDigit() || char == '_' }
    )
  }

  override fun visitKeywordNotDigit(ctx: RegexParser.KeywordNotDigitContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> char.isLetter() || char == '_' }
    )
  }

  override fun visitFilename(ctx: RegexParser.FilenameContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> char.isLetterOrDigit() || "_/.-+,#$%~=".contains(char) }
    )
  }

  override fun visitFilenameNotDigit(ctx: RegexParser.FilenameNotDigitContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> char.isLetter() || "_/.-+,#$%~=".contains(char) }
    )
  }

  override fun visitPrintable(ctx: RegexParser.PrintableContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> !char.isISOControl() }
    )
  }

  override fun visitPrintableNotDigit(ctx: RegexParser.PrintableNotDigitContext?): NFA {
    return NFA.fromMatcher(
      PredicateMatcher { char -> !char.isDigit() && !char.isISOControl() }
    )
  }

  override fun visitWhitespace(ctx: RegexParser.WhitespaceContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        listOf(' ', '\t')
      )
    )
  }

  override fun visitNotWhitespace(ctx: RegexParser.NotWhitespaceContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        listOf(' ', '\t'),
        isNegated = true
      )
    )
  }

  override fun visitDigit(ctx: RegexParser.DigitContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '9'))
      )
    )
  }

  override fun visitNotDigit(ctx: RegexParser.NotDigitContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '9')),
        isNegated = true
      )
    )
  }

  override fun visitHex(ctx: RegexParser.HexContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'F'),
          CollectionRange('a', 'f'),
        )
      )
    )
  }

  override fun visitNotHex(ctx: RegexParser.NotHexContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'F'),
          CollectionRange('a', 'f'),
        ),
        isNegated = true
      )
    )
  }

  override fun visitOctal(ctx: RegexParser.OctalContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '7'))
      )
    )
  }

  override fun visitNotOctal(ctx: RegexParser.NotOctalContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('0', '7')),
        isNegated = true
      )
    )
  }

  override fun visitWordchar(ctx: RegexParser.WordcharContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = listOf('_'),
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        )
      )
    )
  }

  override fun visitNotwordchar(ctx: RegexParser.NotwordcharContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = listOf('_'),
        ranges = listOf(
          CollectionRange('0', '9'),
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true
      )
    )
  }

  override fun visitHeadofword(ctx: RegexParser.HeadofwordContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = listOf('_'),
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        )
      )
    )
  }

  override fun visitNotHeadOfWord(ctx: RegexParser.NotHeadOfWordContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        chars = listOf('_'),
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true
      )
    )
  }

  override fun visitAlpha(ctx: RegexParser.AlphaContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        )
      )
    )
  }

  override fun visitNotAlpha(ctx: RegexParser.NotAlphaContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(
          CollectionRange('A', 'Z'),
          CollectionRange('a', 'z'),
        ),
        isNegated = true
      )
    )
  }

  override fun visitLcase(ctx: RegexParser.LcaseContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('a', 'z'))
      )
    )
  }

  override fun visitNotLcase(ctx: RegexParser.NotLcaseContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('a', 'z')),
        isNegated = true
      )
    )
  }

  override fun visitUcase(ctx: RegexParser.UcaseContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('A', 'Z'))
      )
    )
  }

  override fun visitNotUcase(ctx: RegexParser.NotUcaseContext?): NFA {
    return NFA.fromMatcher(
      CollectionMatcher(
        ranges = listOf(CollectionRange('A', 'Z')),
        isNegated = true
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

  override fun visitNL(ctx: RegexParser.NLContext?): NFA {
    return NFA.fromMatcher(
      CharacterMatcher('\n')
    )
  }

  override fun visitCollectionPos(ctx: RegexParser.CollectionPosContext): NFA {
    return visitCollection(ctx.collection_elems, false)
  }

  override fun visitCollectionNeg(ctx: RegexParser.CollectionNegContext): NFA {
    return visitCollection(ctx.collection_elems, true)
  }

  private fun visitCollection(collectionElements: List<RegexParser.Collection_elemContext>, isNegated: Boolean) : NFA {
    val individualChars: ArrayList<Char> = ArrayList()
    val ranges: ArrayList<CollectionRange> = ArrayList()
    val collectionElementVisitor = CollectionElementVisitor()

    for (elem in collectionElements) {
      val element = collectionElementVisitor.visit(elem)
      when (element) {
        is CollectionElement.SingleCharacter -> individualChars.add(element.char)
        is CollectionElement.CharacterRange -> ranges.add(CollectionRange(element.start, element.end))
      }
    }

    /**
     * If the collection is empty, match literally with '[]', or '[^]' if negated
     */
    if (individualChars.isEmpty() && ranges.isEmpty())
      return if (isNegated) NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher('^')))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))
      else NFA.fromMatcher(CharacterMatcher('['))
        .concatenate(NFA.fromMatcher(CharacterMatcher(']')))

    return NFA.fromMatcher(
      CollectionMatcher(
        individualChars,
        ranges,
        isNegated
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