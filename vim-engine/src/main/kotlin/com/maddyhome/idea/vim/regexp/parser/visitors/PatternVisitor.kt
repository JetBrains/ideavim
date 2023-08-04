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
    val nfa = visit(ctx.atom())
    if (ctx.multi() == null) return nfa

    val multiVisitor = MultiVisitor()
    val range = multiVisitor.visit(ctx.multi())

    return nfa.loop(range.first, range.second)
  }

  override fun visitGroupingCapture(ctx: RegexParser.GroupingCaptureContext): NFA {
    val groupNumber = groupCount
    groupCount++
    val nfa = if (ctx.sub_pattern() == null) NFA.fromEpsilon() else visit(ctx.sub_pattern())
    nfa.capture(groupNumber)
    return nfa
  }

  override fun visitGroupingNoCapture(ctx: RegexParser.GroupingNoCaptureContext): NFA {
    return if (ctx.sub_pattern() == null) NFA.fromEpsilon()
    else visit(ctx.sub_pattern())
  }

  override fun visitLiteralChar(ctx: RegexParser.LiteralCharContext): NFA {
    return NFA.fromMatcher(CharacterMatcher(cleanLiteralChar(ctx.text)))
  }

  private fun cleanLiteralChar(str : String) : Char {
    return if (str.length == 2 && str[0] == '\\') str[1]
    else str[0]
  }

  private fun List<NFA>.union(): NFA {
    return this.foldIndexed(null as NFA?) { index, acc, elem ->
      if (index == 0) elem
      else acc?.unify(elem) ?: elem
    } ?: NFA.fromEpsilon()
  }

  private fun List<NFA>.concatenate(): NFA {
    return this.foldIndexed(null as NFA?) { index, acc, elem ->
      if (index == 0) elem
      else acc?.concatenate(elem) ?: elem
    } ?: NFA.fromEpsilon()
  }
}