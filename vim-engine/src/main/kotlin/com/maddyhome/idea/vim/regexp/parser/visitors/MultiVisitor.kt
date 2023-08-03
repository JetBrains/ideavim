/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.visitors

import com.maddyhome.idea.vim.regexp.nfa.MultiDelimiter
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParserBaseVisitor

internal class MultiVisitor : RegexParserBaseVisitor<Pair<MultiDelimiter.IntMultiDelimiter, MultiDelimiter>>() {

  override fun visitZeroOrMore(ctx: RegexParser.ZeroOrMoreContext): Pair<MultiDelimiter.IntMultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(0), MultiDelimiter.InfiniteMultiDelimiter)
  }

  override fun visitOneOrMore(ctx: RegexParser.OneOrMoreContext): Pair<MultiDelimiter.IntMultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(1), MultiDelimiter.InfiniteMultiDelimiter)
  }

  override fun visitZeroOrOne(ctx: RegexParser.ZeroOrOneContext?): Pair<MultiDelimiter.IntMultiDelimiter, MultiDelimiter> {
    return Pair(MultiDelimiter.IntMultiDelimiter(0), MultiDelimiter.IntMultiDelimiter(1))
  }

  override fun visitRange(ctx: RegexParser.RangeContext): Pair<MultiDelimiter.IntMultiDelimiter, MultiDelimiter> {
    val lowerDelimiter = if (ctx.lower_bound == null) MultiDelimiter.IntMultiDelimiter(0) else MultiDelimiter.IntMultiDelimiter(ctx.lower_bound.text.toInt())
    val upperDelimiter = if (ctx.COMMA() != null) if (ctx.upper_bound == null) MultiDelimiter.InfiniteMultiDelimiter else MultiDelimiter.IntMultiDelimiter(ctx.upper_bound.text.toInt())
    else if (ctx.lower_bound == null) MultiDelimiter.InfiniteMultiDelimiter else lowerDelimiter
    return Pair(lowerDelimiter, upperDelimiter)
  }
}