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

internal class MultiVisitor : RegexParserBaseVisitor<Multi>() {

  override fun visitZeroOrMore(ctx: RegexParser.ZeroOrMoreContext): Multi {
    return Multi.RangeMulti(RangeBoundary.IntRangeBoundary(0), RangeBoundary.InfiniteRangeBoundary, true)
  }

  override fun visitOneOrMore(ctx: RegexParser.OneOrMoreContext): Multi {
    return Multi.RangeMulti(RangeBoundary.IntRangeBoundary(1), RangeBoundary.InfiniteRangeBoundary, true)
  }

  override fun visitZeroOrOne(ctx: RegexParser.ZeroOrOneContext?): Multi {
    return Multi.RangeMulti(RangeBoundary.IntRangeBoundary(0), RangeBoundary.IntRangeBoundary(1), true)
  }

  override fun visitRangeGreedy(ctx: RegexParser.RangeGreedyContext): Multi {
    return visitRange(ctx.lower_bound, ctx.upper_bound, ctx.COMMA(), true)
  }

  override fun visitRangeLazy(ctx: RegexParser.RangeLazyContext): Multi {
    return visitRange(ctx.lower_bound, ctx.upper_bound, ctx.COMMA(), false)
  }

  private fun visitRange(lowerBoundToken: Token?, upperBoundToken: Token?, comma: TerminalNode?, isGreedy: Boolean): Multi {
    val lowerDelimiter = if (lowerBoundToken == null) RangeBoundary.IntRangeBoundary(0) else RangeBoundary.IntRangeBoundary(lowerBoundToken.text.toInt())
    val upperDelimiter = if (comma != null) if (upperBoundToken == null) RangeBoundary.InfiniteRangeBoundary else RangeBoundary.IntRangeBoundary(upperBoundToken.text.toInt())
    else if (lowerBoundToken == null) RangeBoundary.InfiniteRangeBoundary else lowerDelimiter
    return Multi.RangeMulti(lowerDelimiter, upperDelimiter, isGreedy)
  }

  override fun visitAtomic(ctx: RegexParser.AtomicContext?): Multi {
    return Multi.AtomicMulti
  }
}

internal sealed class Multi {

  /**
   * Delimits the number of times that a multi should
   * make a certain atom repeat itself
   */
  internal data class RangeMulti(
    val lowerBoundary: RangeBoundary.IntRangeBoundary,
    val upperBoundary: RangeBoundary,
    val isGreedy: Boolean
    ) : Multi()

  /**
   * Used to represent an atomic group.
   */
  object AtomicMulti : Multi()
}

internal sealed class RangeBoundary {
  /**
   * Represents an integer boundary
   *
   * @param i The boundary of the multi
   */
  data class IntRangeBoundary(val i: Int) : RangeBoundary()

  /**
   * Represents an infinite boundary
   */
  object InfiniteRangeBoundary : RangeBoundary()
}