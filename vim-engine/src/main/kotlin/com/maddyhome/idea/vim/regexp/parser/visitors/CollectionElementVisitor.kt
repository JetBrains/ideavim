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

internal class CollectionElementVisitor : RegexParserBaseVisitor<Pair<CollectionElement, Boolean>>() {

  override fun visitSingleColElem(ctx: RegexParser.SingleColElemContext): Pair<CollectionElement, Boolean> {
    val elem = cleanLiteralChar(ctx.text)
    return Pair(CollectionElement.SingleCharacter(elem.first), elem.second)
  }

  override fun visitRangeColElem(ctx: RegexParser.RangeColElemContext): Pair<CollectionElement, Boolean> {
    val rangeStart = cleanLiteralChar(ctx.start.text)
    val rangeEnd = cleanLiteralChar(ctx.end.text)
    val includesEOL = rangeStart.second || rangeEnd.second
    return Pair(CollectionElement.CharacterRange(rangeStart.first, rangeEnd.first), includesEOL)
  }

  private fun cleanLiteralChar(str: String) : Pair<Char, Boolean> {
    return  if (str.length > 2 && str[0] == '\\' && str[1] == 'd') Pair(Char(str.substring(2).toInt()), false)
    else if (str.length > 2 && str[0] == '\\' && str[1] == 'o') Pair(Char(str.substring(2).toInt(8)), false)
    else if (str.length > 2 && str[0] == '\\' && str[1] == 'x') Pair(Char(str.substring(2).toInt(16)), false)
    else if (str.length > 2 && str[0] == '\\' && str[1] == 'u') Pair(Char(str.substring(2).toInt(16)), false)
    else if (str.length > 2 && str[0] == '\\' && str[1] == 'U') Pair(Char(str.substring(2).toInt(16)), false)
    else if (str.length == 2 && str[0] == '\\' && str[1] == 'e') Pair('', false)
    else if (str.length == 2 && str[0] == '\\' && str[1] == 't') Pair('\t', false)
    else if (str.length == 2 && str[0] == '\\' && str[1] == 'r') Pair('\r', false)
    else if (str.length == 2 && str[0] == '\\' && str[1] == 'b') Pair('\b', false)
    else if (str.length == 2 && str[0] == '\\' && str[1] == 'n') Pair('\n', true)
    else if (str.length == 2 && str[0] == '\\') Pair(str[1], false)
    else Pair(str[0], false)
  }
}

internal sealed class CollectionElement {
  data class SingleCharacter(val char: Char) : CollectionElement()
  data class CharacterRange(val start: Char, val end: Char) : CollectionElement()
}