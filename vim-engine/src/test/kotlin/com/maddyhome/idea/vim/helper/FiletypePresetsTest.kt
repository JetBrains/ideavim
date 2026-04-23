/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class FiletypePresetsTest {

  @Test
  fun `java preset matches Vim C-family default`() {
    assertEquals(
      "sO:* -,mO:*  ,exO:*/,s1:/*,mb:*,ex:*/,:///,://",
      FiletypePresets.presetFor("java"),
    )
  }

  @Test
  fun `python preset uses hash and bullet`() {
    assertEquals("b:#,fb:-", FiletypePresets.presetFor("python"))
  }

  @Test
  fun `sql preset uses line and block comments`() {
    assertEquals("s1:/*,mb:*,ex:*/,:--,://", FiletypePresets.presetFor("sql"))
  }

  @Test
  fun `unknown filetype returns null`() {
    assertNull(FiletypePresets.presetFor("cobol"))
  }

  @Test
  fun `lookup is case-insensitive`() {
    assertEquals(FiletypePresets.presetFor("java"), FiletypePresets.presetFor("JAVA"))
    assertEquals(FiletypePresets.presetFor("python"), FiletypePresets.presetFor("Python"))
  }

  @Test
  fun `every preset parses to a non-empty leader list`() {
    assertAll(
      FiletypePresets.allPresets().map { (filetype, preset) ->
        Executable {
          val parsed = CommentLeaderParser.parse(preset)
          assertTrue(parsed.isNotEmpty(), "$filetype: '$preset' parsed to empty list")
        }
      },
    )
  }

  @Test
  fun `rust preset includes rustdoc markers`() {
    val rust = FiletypePresets.presetFor("rust")
    assertNotNull(rust)
    assertTrue("://!" in rust!!, "rustdoc inner marker missing")
    assertTrue(":///" in rust, "rustdoc outer marker missing")
  }

  @Test
  fun `markdown preset uses nested blockquote flag`() {
    assertEquals("fb:*,fb:-,fb:+,n:>", FiletypePresets.presetFor("markdown"))
  }
}
