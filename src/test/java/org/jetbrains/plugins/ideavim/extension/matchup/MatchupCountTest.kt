/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchup

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchupCountTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchup")
  }

  @Test
  fun `test count jump goes to the second word of the group`() {
    doTest(
      "2%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        els${c}e
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump goes to the closing word`() {
    doTest(
      "3%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        else
          c
        en${c}d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump cycles back to the opening word`() {
    doTest(
      "4%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump of six still cycles`() {
    doTest(
      "6%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        els${c}e
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump of seven goes to a percentage of the file`() {
    doTest(
      "7%",
      """
        ${c}if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      """
        if x
          ${c}line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump of fifty goes to a percentage of the file`() {
    doTest(
      "50%",
      """
        ${c}if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      """
        if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          ${c}line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test reverse count jump goes backwards twice`() {
    doTest(
      "2g%",
      """
        if x
          a
        elsif y
          b
        else
          c
        ${c}end
      """.trimIndent(),
      """
        if x
          a
        ${c}elsif y
          b
        else
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test reverse count jump cycles instead of going to a percentage`() {
    doTest(
      "9g%",
      """
        if x
          a
        elsif y
          b
        else
          c
        ${c}end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        ${c}else
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete with count jump deletes through the second word`() {
    doTest(
      "d2%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
          ${c}c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test visual count jump selects through the second word`() {
    doTest(
      "v2%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        ${s}if x
          a
        elsif y
          b
        els${c}e${se}
          c
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test count jump on brackets comes back`() {
    doTest(
      "2%",
      """
        foo${c}(a, b)
      """.trimIndent(),
      """
        foo${c}(a, b)
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test count jump goes to a percentage when the override is zero`() {
    doTest(
      "50%",
      """
        ${c}if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      """
        if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          ${c}line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      fileName = "ruby.rb",
      afterEditorInitialized = { enterCommand("let g:matchup_motion_override_Npercent = 0") },
    )
  }

  @Test
  fun `test small count jump goes to a percentage when the override is zero`() {
    doTest(
      "2%",
      """
        ${c}if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      """
        ${c}if x
          line2
          line3
          line4
          line5
          line6
          line7
          line8
          line9
          line10
          line11
          line12
          line13
          line14
          line15
          line16
          line17
          line18
          line19
        end
      """.trimIndent(),
      fileName = "ruby.rb",
      afterEditorInitialized = { enterCommand("let g:matchup_motion_override_Npercent = 0") },
    )
  }

  @Test
  fun `test big count jump cycles when the override is a hundred`() {
    doTest(
      "50%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        els${c}e
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
      afterEditorInitialized = { enterCommand("let g:matchup_motion_override_Npercent = 100") },
    )
  }

  @Test
  fun `test count jump of ten cycles when the override is eleven`() {
    doTest(
      "10%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
        if x
          a
        elsif y
          b
        els${c}e
          c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
      afterEditorInitialized = { enterCommand("let g:matchup_motion_override_Npercent = 11") },
    )
  }

  @Test
  fun `test count before the operator deletes through the second word`() {
    doTest(
      "2d%",
      """
        ${c}if x
          a
        elsif y
          b
        else
          c
        end
      """.trimIndent(),
      """
          ${c}c
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }
}
