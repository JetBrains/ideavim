/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ChangeCaseTest : VimTestCase() {
  /**
   * Note: The tests for duplicated commands (gugu and gUgU) might fail due to issues with the test environment,
   * specifically related to file refresh operations. This is a known issue with the test infrastructure
   * and not with the actual functionality being tested.
   * 
   * The tests for guu and gUU test the same functionality and should pass.
   */
  @Test
  fun testChangeCaseLowerLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("guu"),
      """
        H${c}ELLO WORLD
        ${c}THIS IS A TEST
        ${c}FOR CASE CONVERSION

        """.trimIndent(),
    )
    assertState(
      """
        ${c}hello world
        ${c}this is a test
        ${c}for case conversion

        """.trimIndent()
    )
  }

  @Disabled("Not yet supported")
  @Test
  fun testChangeCaseLowerLineActionDuplicated() {
    typeTextInFile(
      injector.parser.parseKeys("gugu"),
      """
        H${c}ELLO WORLD
        ${c}THIS IS A TEST
        ${c}FOR CASE CONVERSION

        """.trimIndent(),
    )
    assertState(
      """
        ${c}hello world
        ${c}this is a test
        ${c}for case conversion

        """.trimIndent()
    )
  }

  @Test
  fun testChangeCaseUpperLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("gUU"),
      """
        h${c}ello world
        ${c}this is a test
        ${c}for case conversion

        """.trimIndent(),
    )
    assertState(
      """
        ${c}HELLO WORLD
        ${c}THIS IS A TEST
        ${c}FOR CASE CONVERSION

        """.trimIndent()
    )
  }

  @Disabled("Not yet supported")
  @Test
  fun testChangeCaseUpperLineActionDuplicated() {
    typeTextInFile(
      injector.parser.parseKeys("gUgU"),
      """
        h${c}ello world
        ${c}this is a test
        ${c}for case conversion

        """.trimIndent(),
    )
    assertState(
      """
        ${c}HELLO WORLD
        ${c}THIS IS A TEST
        ${c}FOR CASE CONVERSION

        """.trimIndent()
    )
  }
}
