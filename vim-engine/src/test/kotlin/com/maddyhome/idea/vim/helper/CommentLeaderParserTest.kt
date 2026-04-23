/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CommentLeaderParserTest {

  @Test
  fun `empty string returns empty list`() {
    assertEquals(emptyList<CommentLeader>(), CommentLeaderParser.parse(""))
  }

  @Test
  fun `parses single leader with no flags`() {
    assertEquals(
      listOf(CommentLeader(text = "//")),
      CommentLeaderParser.parse("://"),
    )
  }

  @Test
  fun `parses b flag as BLANK_REQUIRED`() {
    assertEquals(
      listOf(CommentLeader(text = "#", flags = setOf(CommentLeader.Flag.BLANK_REQUIRED))),
      CommentLeaderParser.parse("b:#"),
    )
  }

  @Test
  fun `comma separates multiple entries`() {
    assertEquals(
      listOf(
        CommentLeader(text = "//"),
        CommentLeader(text = "#", flags = setOf(CommentLeader.Flag.BLANK_REQUIRED)),
      ),
      CommentLeaderParser.parse("://,b:#"),
    )
  }

  @Test
  fun `parses fb as NO_CONTINUATION and BLANK_REQUIRED`() {
    assertEquals(
      listOf(
        CommentLeader(
          text = "-",
          flags = setOf(CommentLeader.Flag.NO_CONTINUATION, CommentLeader.Flag.BLANK_REQUIRED),
        ),
      ),
      CommentLeaderParser.parse("fb:-"),
    )
  }

  @Test
  fun `parses s flag as START with numeric offset`() {
    assertEquals(
      listOf(CommentLeader(text = "/*", flags = setOf(CommentLeader.Flag.START), offset = 1)),
      CommentLeaderParser.parse("s1:/*"),
    )
  }

  @Test
  fun `parses s flag with negative offset`() {
    assertEquals(
      listOf(CommentLeader(text = "/*", flags = setOf(CommentLeader.Flag.START), offset = -1)),
      CommentLeaderParser.parse("s-1:/*"),
    )
  }

  @Test
  fun `ignores digit before flag when computing offset`() {
    assertEquals(
      listOf(CommentLeader(text = "/*", flags = setOf(CommentLeader.Flag.START), offset = 0)),
      CommentLeaderParser.parse("1s:/*"),
    )
  }

  @Test
  fun `parses m flag as MIDDLE`() {
    assertEquals(
      listOf(CommentLeader(text = "*", flags = setOf(CommentLeader.Flag.MIDDLE, CommentLeader.Flag.BLANK_REQUIRED))),
      CommentLeaderParser.parse("mb:*"),
    )
  }

  @Test
  fun `parses e flag as END`() {
    assertEquals(
      listOf(CommentLeader(text = "*/", flags = setOf(CommentLeader.Flag.END))),
      CommentLeaderParser.parse("e:*/"),
    )
  }

  @Test
  fun `parses x flag as END_SHORTCUT`() {
    assertEquals(
      listOf(
        CommentLeader(
          text = "*/",
          flags = setOf(CommentLeader.Flag.END, CommentLeader.Flag.END_SHORTCUT),
        ),
      ),
      CommentLeaderParser.parse("ex:*/"),
    )
  }

  @Test
  fun `parses n flag as NESTED`() {
    assertEquals(
      listOf(CommentLeader(text = ">", flags = setOf(CommentLeader.Flag.NESTED))),
      CommentLeaderParser.parse("n:>"),
    )
  }

  @Test
  fun `parses l flag as LEFT_ALIGN`() {
    assertEquals(
      listOf(CommentLeader(text = "*/", flags = setOf(CommentLeader.Flag.LEFT_ALIGN))),
      CommentLeaderParser.parse("l:*/"),
    )
  }

  @Test
  fun `parses r flag as RIGHT_ALIGN`() {
    assertEquals(
      listOf(CommentLeader(text = "*", flags = setOf(CommentLeader.Flag.RIGHT_ALIGN))),
      CommentLeaderParser.parse("r:*"),
    )
  }

  @Test
  fun `parses O flag as NO_OPEN_BELOW`() {
    assertEquals(
      listOf(
        CommentLeader(
          text = "* -",
          flags = setOf(CommentLeader.Flag.START, CommentLeader.Flag.NO_OPEN_BELOW),
        ),
      ),
      CommentLeaderParser.parse("sO:* -"),
    )
  }

  @Test
  fun `parses Vim default comments value into nine entries`() {
    val vimDefault = "s1:/*,mb:*,ex:*/,://,b:#,:%,:XCOMM,n:>,fb:-"
    val result = CommentLeaderParser.parse(vimDefault)
    assertEquals(9, result.size)
    assertEquals("/*", result[0].text)
    assertEquals(setOf(CommentLeader.Flag.START), result[0].flags)
    assertEquals(1, result[0].offset)
    assertEquals("//", result[3].text)
    assertEquals(emptySet<CommentLeader.Flag>(), result[3].flags)
    assertEquals("XCOMM", result[6].text)
    assertEquals("-", result[8].text)
  }

  @Test
  fun `parses realistic Java ftplugin value`() {
    val java = "sO:* -,mO:*  ,exO:*/,s1:/*,mb:*,ex:*/,://"
    val result = CommentLeaderParser.parse(java)
    assertEquals(7, result.size)
    assertEquals(
      CommentLeader(
        text = "* -",
        flags = setOf(CommentLeader.Flag.START, CommentLeader.Flag.NO_OPEN_BELOW),
      ),
      result[0],
    )
    assertEquals(
      CommentLeader(
        text = "*  ",
        flags = setOf(CommentLeader.Flag.MIDDLE, CommentLeader.Flag.NO_OPEN_BELOW),
      ),
      result[1],
    )
    assertEquals(
      CommentLeader(
        text = "*/",
        flags = setOf(
          CommentLeader.Flag.END,
          CommentLeader.Flag.END_SHORTCUT,
          CommentLeader.Flag.NO_OPEN_BELOW,
        ),
      ),
      result[2],
    )
    assertEquals(CommentLeader(text = "//"), result[6])
  }

  @Test
  fun `backslash escapes a comma inside the text`() {
    assertEquals(
      listOf(CommentLeader(text = ",foo")),
      CommentLeaderParser.parse(":\\,foo"),
    )
  }

  @Test
  fun `backslash escapes a backslash`() {
    assertEquals(
      listOf(CommentLeader(text = "foo\\bar")),
      CommentLeaderParser.parse(":foo\\\\bar"),
    )
  }

  @Test
  fun `trailing backslash at end of input is dropped`() {
    assertEquals(
      listOf(CommentLeader(text = "//")),
      CommentLeaderParser.parse("://\\"),
    )
  }

  @Test
  fun `entry without colon is silently skipped`() {
    assertEquals(
      listOf(CommentLeader(text = "//")),
      CommentLeaderParser.parse("garbage,://,more-garbage"),
    )
  }
}
