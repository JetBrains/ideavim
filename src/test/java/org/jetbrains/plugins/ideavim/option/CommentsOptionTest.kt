/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class CommentsOptionTest : VimTestCase() {

  private val vimDefault = "s1:/*,mb:*,ex:*/,://,b:#,:%,:XCOMM,n:>,fb:-"

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun commentsValue(): String =
    injector.optionGroup.getOptionValue(
      Options.comments,
      OptionAccessScope.LOCAL(fixture.editor.vim),
    ).value

  @Test
  fun `comments option default value matches Vim`() {
    assertEquals(vimDefault, commentsValue())
  }

  @Test
  fun `set comments changes the value`() {
    enterCommand("set comments=://,b:#")
    assertEquals("://,b:#", commentsValue())
  }

  @Test
  fun `set comments& resets to default`() {
    val original = commentsValue()
    enterCommand("set comments=://")
    assertNotEquals(original, commentsValue())
    enterCommand("set comments&")
    assertEquals(original, commentsValue())
  }

  @Test
  fun `comments abbreviation com is accepted`() {
    enterCommand("set com=://")
    assertEquals("://", commentsValue())
  }

  @Test
  fun `set comments inspect displays name and value`() {
    assertCommandOutput("set comments?", "  comments=$vimDefault")
  }

  @Test
  fun `set comments+= appends a list entry`() {
    enterCommand("set comments=://")
    enterCommand("set comments+=b:#")
    assertEquals("://,b:#", commentsValue())
  }

  @Test
  fun `set comments-= removes a list entry`() {
    enterCommand("set comments=://,b:#")
    enterCommand("set comments-=://")
    assertEquals("b:#", commentsValue())
  }
}
