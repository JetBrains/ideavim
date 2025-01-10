/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.parser.generated.RegexParser
import com.maddyhome.idea.vim.parser.generated.RegexParserBaseVisitor
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * A tree visitor for visiting nodes representing a multi. It is used to identify
 * what type of multi is being visited.
 *
 * @see :help /multi
 */
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

  private fun visitRange(
    lowerBoundToken: Token?,
    upperBoundToken: Token?,
    comma: TerminalNode?,
    isGreedy: Boolean,
  ): Multi {
    val lowerDelimiter =
      if (lowerBoundToken == null) RangeBoundary.IntRangeBoundary(0) else RangeBoundary.IntRangeBoundary(lowerBoundToken.text.toInt())
    val upperDelimiter =
      if (comma != null) if (upperBoundToken == null) RangeBoundary.InfiniteRangeBoundary else RangeBoundary.IntRangeBoundary(
        upperBoundToken.text.toInt()
      )
      else if (lowerBoundToken == null) RangeBoundary.InfiniteRangeBoundary else lowerDelimiter
    return if (upperDelimiter is RangeBoundary.IntRangeBoundary && lowerDelimiter.i > upperDelimiter.i) Multi.RangeMulti(
      lowerDelimiter,
      upperDelimiter,
      isGreedy
    )
    else Multi.RangeMulti(lowerDelimiter, upperDelimiter, isGreedy)
  }

  override fun visitAtomic(ctx: RegexParser.AtomicContext?): Multi {
    return Multi.AtomicMulti
  }

  override fun visitPositiveLookahead(ctx: RegexParser.PositiveLookaheadContext?): Multi {
    return Multi.AssertionMulti(isPositive = true, isAhead = true)
  }

  override fun visitNegativeLookahead(ctx: RegexParser.NegativeLookaheadContext?): Multi {
    return Multi.AssertionMulti(isPositive = false, isAhead = true)
  }

  override fun visitPositiveLookbehind(ctx: RegexParser.PositiveLookbehindContext?): Multi {
    return Multi.AssertionMulti(isPositive = true, isAhead = false)
  }

  override fun visitNegativeLookbehind(ctx: RegexParser.NegativeLookbehindContext?): Multi {
    return Multi.AssertionMulti(isPositive = false, isAhead = false)
  }

  override fun visitPositiveLimitedLookbehind(ctx: RegexParser.PositiveLimitedLookbehindContext): Multi {
    val limit = (Regex("\\d+").find(ctx.text))?.value?.toInt() ?: run { 0 }
    return Multi.AssertionMulti(isPositive = true, isAhead = false, limit)
  }

  override fun visitNegativeLimitedLookbehind(ctx: RegexParser.NegativeLimitedLookbehindContext): Multi {
    val limit = (Regex("\\d+").find(ctx.text))?.value?.toInt() ?: run { 0 }
    return Multi.AssertionMulti(isPositive = false, isAhead = false, limit)
  }
}

/**
 * Represents a multi.
 *
 * @see :help multi
 */
internal sealed class Multi {

  /**
   * Delimits the number of times that a multi should
   * make a certain atom repeat itself
   *
   * @param lowerBoundary The minimum number of times that the atom can repeat itself.
   * @param upperBoundary The maximum number of times that the atom can repeat itself. This number can be infinite.
   * @param isGreedy Whether this multi is greedy. A greedy multi always consumes as much input
   * it can, while a non-greedy, or lazy multi, consumes the least amount of input
   * it can.
   */
  internal data class RangeMulti(
    val lowerBoundary: RangeBoundary.IntRangeBoundary,
    val upperBoundary: RangeBoundary,
    val isGreedy: Boolean,
  ) : Multi()

  /**
   * Used to represent an atomic atom. Atoms that are atomic, match
   * as if they were a whole pattern.
   *
   * @see :help /\@>
   */
  object AtomicMulti : Multi()

  /**
   * Used to represent an assertion multi. These
   * are also known as look-ahead and look-behind.
   * They can be positive, meaning that they must match,
   * or negative, meaning that they must not match.
   *
   * @param isPositive Whether the assertion is positive
   * @param isAhead    Whether it is a look-ahead
   */
  internal data class AssertionMulti(
    val isPositive: Boolean,
    val isAhead: Boolean,
    val limit: Int = 0,
  ) : Multi()
}

/**
 * Used to represent a boundary of a range multi
 */
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