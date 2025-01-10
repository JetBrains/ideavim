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

/**
 * A tree visitor for visiting nodes representing a collection.
 *
 * @see :help /collection
 */
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

  override fun visitAlnumClass(ctx: RegexParser.AlnumClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isLetterOrDigit() }, false)
  }

  override fun visitAlphaClass(ctx: RegexParser.AlphaClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isLetter() }, false)
  }

  override fun visitBlankClass(ctx: RegexParser.BlankClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { " \t".contains(it) }, false)
  }

  override fun visitCntrlClass(ctx: RegexParser.CntrlClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isISOControl() }, false)
  }

  override fun visitDigitClass(ctx: RegexParser.DigitClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isDigit() }, false)
  }

  override fun visitGraphClass(ctx: RegexParser.GraphClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it in '!'..'~' }, false)
  }

  override fun visitLowerClass(ctx: RegexParser.LowerClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isLowerCase() }, false)
  }

  override fun visitPrintClass(ctx: RegexParser.PrintClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { !it.isISOControl() }, false)
  }

  override fun visitPunctClass(ctx: RegexParser.PunctClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression {
      it in '!'..'/' ||
        it in ':'..'@' ||
        it in '['..'`' ||
        it in '{'..'~'
    }, false)
  }

  override fun visitSpaceClass(ctx: RegexParser.SpaceClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isWhitespace() }, false)
  }

  override fun visitUpperClass(ctx: RegexParser.UpperClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isUpperCase() }, false)
  }

  override fun visitXdigitClass(ctx: RegexParser.XdigitClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression {
      it in '0'..'9' ||
        it in 'a'..'f' ||
        it in 'A'..'F'
    }, false)
  }

  override fun visitReturnClass(ctx: RegexParser.ReturnClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it == '\r' }, false)
  }

  override fun visitTab(ctx: RegexParser.TabContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it == '\t' }, false)
  }

  override fun visitEsc(ctx: RegexParser.EscContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it == '' }, false)
  }

  override fun visitBackspaceClass(ctx: RegexParser.BackspaceClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it == '\b' }, false)
  }

  override fun visitIdentClass(ctx: RegexParser.IdentClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isJavaIdentifierPart() }, false)
  }

  override fun visitKeywordClass(ctx: RegexParser.KeywordClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isLetterOrDigit() || it == '_' }, false)
  }

  override fun visitFnameClass(ctx: RegexParser.FnameClassContext?): Pair<CollectionElement, Boolean> {
    return Pair(CollectionElement.CharacterClassExpression { it.isLetter() || "_/.-+,#$%~=".contains(it) }, false)
  }

  private fun cleanLiteralChar(str: String): Pair<Char, Boolean> {
    return if (str.length > 2 && str[0] == '\\' && str[1] == 'd') Pair(Char(str.substring(2).toInt()), false)
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

/**
 * Represents a single element in a collection. This element can be
 * a single character, a range of characters, or a character class
 * expression.
 */
internal sealed class CollectionElement {
  /**
   * Represents a single character collection element.
   *
   * @param char The character element.
   */
  data class SingleCharacter(val char: Char) : CollectionElement()

  /**
   * Represents a range of characters collection element.
   *
   * @param start The starting character of the range.
   * @param end   The ending character of the range.
   */
  data class CharacterRange(val start: Char, val end: Char) : CollectionElement()

  /**
   * Represents a character class expression element. e.g. [:digit:].
   *
   * @param predicate The condition that a character has to meet to belong in the character class.
   */
  data class CharacterClassExpression(val predicate: (Char) -> Boolean) : CollectionElement()
}