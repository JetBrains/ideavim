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

internal class MultiVisitor : RegexParserBaseVisitor<Pair<MultiDelimiter, MultiDelimiter>>() {

  override fun visitZeroOrMore(ctx: RegexParser.ZeroOrMoreContext): Pair<MultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(0), MultiDelimiter.InfiniteMultiDelimiter)
  }

  override fun visitOneOrMore(ctx: RegexParser.OneOrMoreContext): Pair<MultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(1), MultiDelimiter.InfiniteMultiDelimiter)
  }

  override fun visitZeroOrOne(ctx: RegexParser.ZeroOrOneContext?): Pair<MultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(0), MultiDelimiter.IntMultiDelimiter(1))
  }

  override fun visitRange(ctx: RegexParser.RangeContext): Pair<MultiDelimiter, MultiDelimiter> {
    val lowerDelimiter = if (ctx.lower_bound.text.isEmpty()) MultiDelimiter.IntMultiDelimiter(0) else MultiDelimiter.IntMultiDelimiter(ctx.lower_bound.text.toInt())
    val upperDelimiter = if (ctx.upper_bound.text.isEmpty()) MultiDelimiter.InfiniteMultiDelimiter else MultiDelimiter.IntMultiDelimiter(ctx.upper_bound.text.toInt())
    return Pair(lowerDelimiter, upperDelimiter)
  }
}