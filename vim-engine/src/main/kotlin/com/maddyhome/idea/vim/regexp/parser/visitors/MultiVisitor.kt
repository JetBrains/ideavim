/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParserBaseVisitor
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

internal class MultiVisitor : RegexParserBaseVisitor<MultiDelimiter>() {

  override fun visitZeroOrMore(ctx: RegexParser.ZeroOrMoreContext): MultiDelimiter {
    return MultiDelimiter(MultiBoundary.IntMultiBoundary(0), MultiBoundary.InfiniteMultiBoundary, true)
  }

  override fun visitOneOrMore(ctx: RegexParser.OneOrMoreContext): MultiDelimiter {
    return MultiDelimiter(MultiBoundary.IntMultiBoundary(1), MultiBoundary.InfiniteMultiBoundary, true)
  }

  override fun visitZeroOrOne(ctx: RegexParser.ZeroOrOneContext?): MultiDelimiter {
    return MultiDelimiter(MultiBoundary.IntMultiBoundary(0), MultiBoundary.IntMultiBoundary(1), true)
  }

  override fun visitRangeGreedy(ctx: RegexParser.RangeGreedyContext): MultiDelimiter {
    return visitRange(ctx.lower_bound, ctx.upper_bound, ctx.COMMA(), true)
  }

  override fun visitRangeLazy(ctx: RegexParser.RangeLazyContext): MultiDelimiter {
    return visitRange(ctx.lower_bound, ctx.upper_bound, ctx.COMMA(), false)
  }

  private fun visitRange(lowerBoundToken: Token?, upperBoundToken: Token?, comma: TerminalNode?, isGreedy: Boolean): MultiDelimiter {
    val lowerDelimiter = if (lowerBoundToken == null) MultiBoundary.IntMultiBoundary(0) else MultiBoundary.IntMultiBoundary(lowerBoundToken.text.toInt())
    val upperDelimiter = if (comma != null) if (upperBoundToken == null) MultiBoundary.InfiniteMultiBoundary else MultiBoundary.IntMultiBoundary(upperBoundToken.text.toInt())
    else if (lowerBoundToken == null) MultiBoundary.InfiniteMultiBoundary else lowerDelimiter
    return MultiDelimiter(lowerDelimiter, upperDelimiter, isGreedy)
  }
}

/**
 * Delimits the number of times that a multi should
 * make a certain atom repeat itself
 */
internal data class MultiDelimiter(
  val lowerBoundary: MultiBoundary.IntMultiBoundary,
  val upperBoundary: MultiBoundary,
  val isGreedy: Boolean
)

internal sealed class MultiBoundary {
  /**
   * Represents an integer boundary
   *
   * @param i The boundary of the multi
   */
  data class IntMultiBoundary(val i: Int) : MultiBoundary()

  /**
   * Represents an infinite boundary
   */
  object InfiniteMultiBoundary : MultiBoundary()
}