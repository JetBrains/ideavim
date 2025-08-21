/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.ex.ExOutputModel
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@Suppress("SpellCheckingInspection")
class HistoryCommandTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test history lists empty cmd history by default`() {
    assertCommandOutput("history", "      #  cmd history")
  }

  @Test
  fun `test history lists all entries in cmd history by default`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
        |      9  echo 9
        |>    10  echo 10
      """.trimMargin())
  }

  @Test
  fun `test his lists empty cmd history by default`() {
    assertCommandOutput("his", "      #  cmd history")
  }

  @Test
  fun `test history with 'history' option set to 0 shows nothing`() {
    enterCommand("set history=0")
    enterCommand("history")
    assertNoExOutput()
    assertPluginError(false)
    assertPluginErrorMessageContains("'history' option is zero")
  }

  @Test
  fun `test history with bang reports error`() {
    enterCommand("history!")
    assertPluginError(true)
    assertPluginErrorMessageContains("E477: No ! allowed")
  }

  @Test
  fun `test history with unknown symbol raises error`() {
    enterCommand("history !")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: !")
  }

  @VimBehaviorDiffers(description = "Vim does not eat the 'a' for the 'all' command")
  @Test
  fun `test history with unknown name reports error`() {
    enterCommand("history asdf")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: sdf")
  }

  @Test
  fun `test history adds indicator to current entry`() {
    repeat(5) { i -> enterSearch("foo${i + 1}") }
    repeat(5) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history all",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |>     5  echo 5
        |      #  search history
        |      1  foo1
        |      2  foo2
        |      3  foo3
        |      4  foo4
        |>     5  foo5
        |      #  expr history
        |      #  input history
      """.trimMargin()
    )
  }

  @Test
  fun `test history does not show indicator if not including current entry`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history : 1,5",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
      """.trimMargin()
    )
  }

  @Test
  fun `test history with no name and first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin())
  }

  @Test
  fun `test history with no name and two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin())
  }

  @Test
  fun `test history with colon lists empty cmd history`() {
    assertCommandOutput("history :", "      #  cmd history")
  }

  @Test
  fun `test history with colon and first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history : 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin())
  }

  @Test
  fun `test history with colon and no space before first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history :3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin())
  }

  @Test
  fun `test history with colon and two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history : 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin())
  }

  @Test
  fun `test history cmd lists empty command history`() {
    assertCommandOutput("history cmd", "      #  cmd history")
  }

  // TODO: Record command before it's run
  @Disabled
  @Test
  fun `test history cmd lists current cmd in history`() {
    assertCommandOutput("history cmd",
      """
        |      #  cmd history
        |      1  history cmd
      """.trimMargin())
  }

  @Test
  fun `test abbreviated history cmd lists cmd history`() {
    assertCommandOutput("history c", "      #  cmd history")
    assertCommandOutput("history cm",
      """
        |      #  cmd history
        |>     1  history c
      """.trimMargin()
    )
    assertCommandOutput("history cmd",
      """
        |      #  cmd history
        |      1  history c
        |>     2  history cm
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin())
  }

  @Test
  fun `test history cmd with no space before first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin())
  }

  @Test
  fun `test history cmd with two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin())
  }

  @Test
  fun `test history cmd with two numbers incorrectly ordered lists nothing from command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd 6,3", "      #  cmd history")
  }

  @Test
  fun `test history cmd with number that is no longer used`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    // This will make "echo 1" the last used entry, remove it from position 1 and add it at position 11
    typeText(":<Up><Up><Up><Up><Up><Up><Up><Up><Up><Up><Esc>")
    assertCommandOutput("history cmd 1", "      #  cmd history")
  }

  @Test
  fun `test history cmd with range starting from number that is no longer used`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    // This will make "echo 1" the last used entry, remove it from position 1 and add it at position 11
    typeText(":<Up><Up><Up><Up><Up><Up><Up><Up><Up><Up><Esc>")
    assertCommandOutput("history cmd 1,10",
      """
        |      #  cmd history
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
        |      9  echo 9
        |     10  echo 10
      """.trimMargin())
  }

  @Test
  fun `test history cmd -1 shows last entry`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd -1",
      """
        |      #  cmd history
        |>    10  echo 10
      """.trimMargin())
  }

  @Test
  fun `test history cmd with negative number shows list of entries relative to last entry`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd -4,-1",
      """
        |      #  cmd history
        |      7  echo 7
        |      8  echo 8
        |      9  echo 9
        |>    10  echo 10
      """.trimMargin())
  }

  @Test
  fun `test history cmd with two negative numbers incorrectly ordered lists nothing from command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd -1,-4", "      #  cmd history")
  }

  @Test
  fun `test history with positive start number and negative last number`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd 4,-3",
      """
        |      #  cmd history
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
      """.trimMargin())
  }

  @Test
  fun `test history with negative start number and positive last number`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history cmd -8,8",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
      """.trimMargin())
  }

  @VimBehaviorDiffers(description = "Vim parses the ',' and reports 'foo' as the trailing characters")
  @Test
  fun `test history cmd with one number and trailing characters reports error`() {
    enterCommand("history cmd 3, foo")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: , foo")
  }

  @Test
  fun `test history cmd with two numbers and trailing characters reports error`() {
    enterCommand("history cmd 3, 6, foo")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: , foo")
  }

  @VimBehaviorDiffers(description = "Vim reports 'cmdfoo' as the trailing characters")
  @Test
  fun `test history cmd with trailing characters reports error`() {
    enterCommand("history cmdfoo")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: foo")
  }

  @Test
  fun `test history cmd with trailing characters reports error 2`() {
    enterCommand("history cmd foo")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: foo")
  }

  @Test
  fun `test history with slash lists empty search history`() {
    assertCommandOutput("history /", "      #  search history")
  }

  @Test
  fun `test history with question mark lists empty search history`() {
    assertCommandOutput("history ?", "      #  search history")
  }

  @Test
  fun `test abbreviated history search lists empty search history`() {
    assertCommandOutput("history s", "      #  search history")
    assertCommandOutput("history se", "      #  search history")
    assertCommandOutput("history sea", "      #  search history")
    assertCommandOutput("history sear", "      #  search history")
    assertCommandOutput("history searc", "      #  search history")
    assertCommandOutput("history search", "      #  search history")
  }

  @Test
  fun `test list search history`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    assertCommandOutput("history search",
      """
        |      #  search history
        |      1  foo1
        |      2  foo2
        |      3  foo3
        |      4  foo4
        |      5  foo5
        |      6  foo6
        |      7  foo7
        |      8  foo8
        |      9  foo9
        |>    10  foo10
      """.trimMargin())
  }


  @Test
  fun `test history search with first number lists single entry from saerch history`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history search 3",
      """
        |      #  search history
        |      3  foo3
      """.trimMargin())
  }

  @Test
  fun `test history search with no space before first number lists single entry from search history`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history search3",
      """
        |      #  search history
        |      3  foo3
      """.trimMargin())
  }

  @Test
  fun `test history search with two numbers lists search history range`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history search 3, 6",
      """
        |      #  search history
        |      3  foo3
        |      4  foo4
        |      5  foo5
        |      6  foo6
      """.trimMargin())
  }

  @Test
  fun `test history with equals sign lists empty expression history`() {
    assertCommandOutput("history =", "      #  expr history")
  }

  @Test
  fun `test abbreviated history expr lists empty expression history`() {
    assertCommandOutput("history e", "      #  expr history")
    assertCommandOutput("history ex", "      #  expr history")
    assertCommandOutput("history exp", "      #  expr history")
    assertCommandOutput("history expr", "      #  expr history")
  }

  @Test
  fun `test history with at sign lists empty input history`() {
    assertCommandOutput("history @", "      #  input history")
  }

  @Test
  fun `test abbreviated history input lists empty input history`() {
    assertCommandOutput("history i", "      #  input history")
    assertCommandOutput("history in", "      #  input history")
    assertCommandOutput("history inp", "      #  input history")
    assertCommandOutput("history inpu", "      #  input history")
    assertCommandOutput("history input", "      #  input history")
  }

  @Test
  fun `test history all lists empty histories`() {
    assertCommandOutput("history all",
      """
        |      #  cmd history
        |      #  search history
        |      #  expr history
        |      #  input history
      """.trimMargin()
    )
  }

  @Test
  fun `test history all includes history entries`() {
    repeat(5) { i -> enterSearch("foo${i + 1}") }
    repeat(5) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history all",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |>     5  echo 5
        |      #  search history
        |      1  foo1
        |      2  foo2
        |      3  foo3
        |      4  foo4
        |>     5  foo5
        |      #  expr history
        |      #  input history
      """.trimMargin()
    )
  }

  @Test
  fun `test history all applies first number to all history entries`() {
    repeat(5) { i -> enterSearch("foo${i + 1}") }
    repeat(5) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history all 3",
      """
        |      #  cmd history
        |      3  echo 3
        |      #  search history
        |      3  foo3
        |      #  expr history
        |      #  input history
      """.trimMargin()
    )
  }

  @Test
  fun `test history all applies range to all history entries`() {
    repeat(5) { i -> enterSearch("foo${i + 1}") }
    repeat(5) { i -> enterCommand("echo ${i + 1}") }
    ExOutputModel.getInstance(fixture.editor).clear()
    assertCommandOutput("history all 2,4",
      """
        |      #  cmd history
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      #  search history
        |      2  foo2
        |      3  foo3
        |      4  foo4
        |      #  expr history
        |      #  input history
      """.trimMargin()
    )
  }
}
