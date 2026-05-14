/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class HistoryCommandTest : VimTestCase("\n") {
  @Test
  fun `test history lists current cmd history by default`() {
    assertCommandOutput(
      "history",
      """
        |      #  cmd history
        |>     1  history
      """.trimMargin()
    )
  }

  @Test
  fun `test history lists all entries in cmd history by default`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd",
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
        |     10  echo 10
        |>    11  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test his lists current cmd history by default`() {
    assertCommandOutput(
      "his",
      """
        |      #  cmd history
        |>     1  his
      """.trimMargin()
    )
  }

  @Test
  fun `test history with 'history' option set to 0 shows nothing`() {
    enterCommand("set history=0")
    enterCommand("history")
    assertPluginError(false)
    assertPluginErrorMessage("'history' option is zero")
  }

  @Test
  fun `test history with bang reports error`() {
    enterCommand("history!")
    assertPluginError(true)
    assertPluginErrorMessage("E477: No ! allowed")
  }

  @Test
  fun `test history with unknown symbol raises error`() {
    enterCommand("history !")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: !")
  }

  @VimBehaviorDiffers(description = "Vim does not eat the 'a' for the 'all' command")
  @Test
  fun `test history with unknown name reports error`() {
    enterCommand("history asdf")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: sdf")
  }

  @Test
  fun `test history adds indicator to current entry`() {
    // It doesn't seem possible to change the current history entry while not editing the command line. (Vim's command
    // line window `q:` can interact with it, but also seems limited to while the command line is active). Essentially,
    // when the `:history` command runs, the command line has just been completed, so a new entry has been saved, and
    // the current entry has been reset. The search history is obviously not active either, so the last search command
    // line would have saved a new entry and reset the current entry too. Therefore, the `:history` command always seems
    // to use the last history entry as the current entry.
    // Furthermore, the current entry can never actually be the last entry in this scenario. The current entry is
    // supposed to be used to navigate _from_ when moving through history. So pressing `<S-Up>` on an empty command line
    // will navigate to the last history entry. If the current entry was already the last entry, we would select the
    // second to last, which is clearly wrong.
    repeat(5) { i -> enterSearch("foo${i + 1}") }
    repeat(5) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history all",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |>     6  history all
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
    assertCommandOutput(
      "history : 1,5",
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
    assertCommandOutput(
      "history 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin()
    )
  }

  @Test
  fun `test history with no name and two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin()
    )
  }

  @Test
  fun `test history with colon and empty cmd history lists just invoked command`() {
    assertCommandOutput(
      "history :",
      """
        |      #  cmd history
        |>     1  history :
      """.trimMargin()
    )
  }

  @Test
  fun `test history with colon and first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history : 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin()
    )
  }

  @Test
  fun `test history with colon and no space before first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history :3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin()
    )
  }

  @Test
  fun `test history with colon and two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history : 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd lists self in history`() {
    assertCommandOutput(
      "history cmd",
      """
        |      #  cmd history
        |>     1  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test abbreviated history cmd lists cmd history`() {
    assertCommandOutput(
      "history c",
      """
        |      #  cmd history
        |>     1  history c
      """.trimMargin()
    )
    assertCommandOutput(
      "history cm",
      """
        |      #  cmd history
        |      1  history c
        |>     2  history cm
      """.trimMargin()
    )
    assertCommandOutput(
      "history cmd",
      """
        |      #  cmd history
        |      1  history c
        |      2  history cm
        |>     3  history cmd
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd 3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with no space before first number lists single entry from command history`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd3",
      """
        |      #  cmd history
        |      3  echo 3
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with two numbers lists command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd 3, 6",
      """
        |      #  cmd history
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with two numbers incorrectly ordered lists nothing from command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput("history cmd 6,3", "      #  cmd history")
  }

  @Test
  fun `test history cmd does not include duplicate entries`() {
    enterCommand("echo 1")  // #1
    enterCommand("echo 2")  // #2
    enterCommand("echo 3")  // #3
    enterCommand("echo 1")  // Removes #1 and adds as #4
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |      2  echo 2
      |      3  echo 3
      |      4  echo 1
      |>     5  history cmd
    """.trimMargin())
  }

  @Test
  fun `test history cmd does not include empty commands`() {
    enterCommand("")
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |>     1  history cmd
    """.trimMargin())
  }

  @Test
  fun `test history cmd includes cancelled commands`() {
    enterCommand("echo 1")
    enterCommand("echo 2")
    typeText(":echo 'cancelled'<Esc>")  // Cancelled!
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |      1  echo 1
      |      2  echo 2
      |      3  echo 'cancelled'
      |>     4  history cmd
    """.trimMargin())
  }

  @Test
  fun `test history cmd does not include empty cancelled commands`() {
    typeText(":<Esc>")  // Cancelled!
    assertCommandOutput("history cmd", """
      |      #  cmd history
      |>     1  history cmd
    """.trimMargin())
  }

  @Test
  fun `test history cmd with number that is no longer used outputs no entries`() {
    enterCommand("echo 1")  // #1
    enterCommand("echo 2")  // #2
    enterCommand("echo 3")  // #3
    enterCommand("echo 1")  // Removes #1 and adds as #4
    assertCommandOutput("history cmd 1", "      #  cmd history")
  }

  @Test
  fun `test history cmd with range starting from number that is no longer used`() {
    enterCommand("echo 1")  // #1
    enterCommand("echo 2")  // #2
    enterCommand("echo 3")  // #3
    enterCommand("echo 1")  // Removes #1 and adds as #4
    assertCommandOutput(
      "history cmd 1,4",
      """
        |      #  cmd history
        |      2  echo 2
        |      3  echo 3
        |      4  echo 1
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd -1 shows last entry`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd -1",
      """
        |      #  cmd history
        |>    11  history cmd -1
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with negative number shows list of entries relative to last entry`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd -4,-1",
      """
        |      #  cmd history
        |      8  echo 8
        |      9  echo 9
        |     10  echo 10
        |>    11  history cmd -4,-1
      """.trimMargin()
    )
  }

  @Test
  fun `test history cmd with two negative numbers incorrectly ordered lists nothing from command history range`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput("history cmd -1,-4", "      #  cmd history")
  }

  @Test
  fun `test history with positive start number and negative last number`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd 4,-3",
      """
        |      #  cmd history
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
        |      9  echo 9
      """.trimMargin()
    )
  }

  @Test
  fun `test history with negative start number and positive last number`() {
    repeat(10) { i -> enterCommand("echo ${i + 1}") }
    assertCommandOutput(
      "history cmd -8,8",
      """
        |      #  cmd history
        |      4  echo 4
        |      5  echo 5
        |      6  echo 6
        |      7  echo 7
        |      8  echo 8
      """.trimMargin()
    )
  }

  @VimBehaviorDiffers(description = "Vim parses the ',' and reports 'foo' as the trailing characters")
  @Test
  fun `test history cmd with one number and trailing characters reports error`() {
    enterCommand("history cmd 3, foo")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: , foo")
  }

  @Test
  fun `test history cmd with two numbers and trailing characters reports error`() {
    enterCommand("history cmd 3, 6, foo")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: , foo")
  }

  @VimBehaviorDiffers(description = "Vim reports 'cmdfoo' as the trailing characters")
  @Test
  fun `test history cmd with trailing characters reports error`() {
    enterCommand("history cmdfoo")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: foo")
  }

  @Test
  fun `test history cmd with trailing characters reports error 2`() {
    enterCommand("history cmd foo")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: foo")
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
    assertCommandOutput(
      "history search",
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
      """.trimMargin()
    )
  }


  @Test
  fun `test history search with first number lists single entry from search history`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    assertCommandOutput(
      "history search 3",
      """
        |      #  search history
        |      3  foo3
      """.trimMargin()
    )
  }

  @Test
  fun `test history search with no space before first number lists single entry from search history`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    assertCommandOutput(
      "history search3",
      """
        |      #  search history
        |      3  foo3
      """.trimMargin()
    )
  }

  @Test
  fun `test history search with two numbers lists search history range`() {
    repeat(10) { i -> enterSearch("foo${i + 1}") }
    assertCommandOutput(
      "history search 3, 6",
      """
        |      #  search history
        |      3  foo3
        |      4  foo4
        |      5  foo5
        |      6  foo6
      """.trimMargin()
    )
  }

  @Test
  fun `test history search does not include duplicate entries`() {
    enterSearch("foo 1")  // #1
    enterSearch("foo 2")  // #2
    enterSearch("foo 3")  // #3
    enterSearch("foo 1")  // Removes #1 and adds as #4
    assertCommandOutput("history search", """
      |      #  search history
      |      2  foo 2
      |      3  foo 3
      |>     4  foo 1
    """.trimMargin())
  }

  @Test
  fun `test history search does not include empty entries`() {
    enterSearch("foo 1")
    enterSearch("foo 2")
    enterSearch("") // Search for last entry, but does not move #2 to #3
    assertCommandOutput("history search", """
      |      #  search history
      |      1  foo 1
      |>     2  foo 2
    """.trimMargin())
  }

  @Test
  fun `test history search includes cancelled commands`() {
    enterSearch("foo 1")
    enterSearch("foo 2")
    typeText("/foo 'cancelled'<Esc>")  // Cancelled!
    assertCommandOutput("history search", """
      |      #  search history
      |      1  foo 1
      |      2  foo 2
      |>     3  foo 'cancelled'
    """.trimMargin())
  }

  @Test
  fun `test history search does not include empty cancelled commands`() {
    typeText("/<Esc>")  // Cancelled!
    assertCommandOutput("history search", """
      |      #  search history
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
    assertCommandOutput(
      "history all",
      """
        |      #  cmd history
        |>     1  history all
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
    assertCommandOutput(
      "history all",
      """
        |      #  cmd history
        |      1  echo 1
        |      2  echo 2
        |      3  echo 3
        |      4  echo 4
        |      5  echo 5
        |>     6  history all
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
    assertCommandOutput(
      "history all 3",
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
    assertCommandOutput(
      "history all 2,4",
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
