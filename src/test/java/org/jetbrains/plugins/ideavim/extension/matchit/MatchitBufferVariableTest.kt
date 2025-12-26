/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchit

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchitBufferVariableTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  fun `test simple custom pattern with b match_words`() {
    configureByText(
      """
        ${c}#region Test
        Some content here
        #endregion
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion'")
    typeText("%")
    assertState(
      """
        #region Test
        Some content here
        ${c}#endregion
      """.trimIndent()
    )
  }

  @Test
  fun `test custom pattern reverse motion`() {
    configureByText(
      """
        #region Test
        Some content here
        ${c}#endregion
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion'")
    typeText("%")
    assertState(
      """
        ${c}#region Test
        Some content here
        #endregion
      """.trimIndent()
    )
  }

  @Test
  fun `test multiple custom patterns`() {
    configureByText(
      """
        ${c}BEGIN transaction
        Some SQL here
        END
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion,BEGIN:END'")
    typeText("%")
    assertState(
      """
        BEGIN transaction
        Some SQL here
        ${c}END
      """.trimIndent()
    )
  }

  @Test
  fun `test nested custom patterns`() {
    configureByText(
      """
        #region Outer
        ${c}#region Inner
        Content
        #endregion
        #endregion
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion'")
    typeText("%")
    assertState(
      """
        #region Outer
        #region Inner
        Content
        ${c}#endregion
        #endregion
      """.trimIndent()
    )
  }

  @Test
  fun `test custom pattern in operator pending mode`() {
    configureByText(
      """
        ${c}#region Test
        Some content here
        #endregion
        Next line
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion'")
    typeText("d%")
    assertState(
      """
        ${c}
        Next line
      """.trimIndent()
    )
  }

  @Test
  fun `test custom pattern with middle keyword`() {
    configureByText(
      """
        ${c}start
        first part
        middle
        second part
        end
      """.trimIndent()
    )
    enterCommand("let b:match_words = 'start:middle:end'")
    typeText("%")
    assertState(
      """
        start
        first part
        ${c}middle
        second part
        end
      """.trimIndent()
    )
  }

  @Test
  fun `test custom pattern does not affect default pairs`() {
    configureByText(
      """
        $c(test)
      """.trimIndent()
    )
    enterCommand("let b:match_words = '#region:#endregion'")
    typeText("%")
    assertState(
      """
        (test$c)
      """.trimIndent()
    )
  }

}
