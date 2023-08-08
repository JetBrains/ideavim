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

internal class CollectionElementVisitor : RegexParserBaseVisitor<CollectionElement>() {

  override fun visitSingleColElem(ctx: RegexParser.SingleColElemContext): CollectionElement {
    return CollectionElement.SingleCharacter(cleanLiteralChar(ctx.text))
  }

  override fun visitRangeColElem(ctx: RegexParser.RangeColElemContext): CollectionElement {
    return CollectionElement.CharacterRange(cleanLiteralChar(ctx.start.text), cleanLiteralChar(ctx.end.text))
  }

  private fun cleanLiteralChar(str : String) : Char {
    return if (str.length == 2 && str[0] == '\\') str[1]
    else str[0]
  }
}

internal sealed class CollectionElement {
  data class SingleCharacter(val char: Char) : CollectionElement()
  data class CharacterRange(val start: Char, val end: Char) : CollectionElement()
}