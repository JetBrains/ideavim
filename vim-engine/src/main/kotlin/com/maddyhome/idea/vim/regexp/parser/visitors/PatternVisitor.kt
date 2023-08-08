/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.regexp.nfa.MultiDelimiter
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.nfa.matcher.CharacterMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.CollectionRange
import com.maddyhome.idea.vim.regexp.nfa.matcher.CursorMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.DotMatcher
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
    for (i in 0 until range.first.i)
      prefixNFA.concatenate(visit(ctx.atom()))

    var suffixNFA = NFA.fromSingleState()
    if (range.second is MultiDelimiter.InfiniteMultiDelimiter) suffixNFA = visit(ctx.atom()).closure()
    else {
      for (i in range.first.i until (range.second as MultiDelimiter.IntMultiDelimiter).i) {
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