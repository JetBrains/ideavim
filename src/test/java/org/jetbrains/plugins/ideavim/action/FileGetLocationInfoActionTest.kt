/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// For all of these tests, note that Vim might show a different total byte count - off by one. This is ok, and not worth
// adding a VimBehaviorDiffers annotation for.
// It's because Vim requires each line to end with a linefeed character (otherwise it's not a line!) and adds one to
// the last line. If the last line ends with a linefeed, that's just the end of the line. In this case, Vim does not
// draw an empty line after the last line (because there isn't one!). If we hit enter at the end of the last line, Vim
// adds a second linefeed, and there's now a new (empty) line at the end of the file, and the file ends with two
// linefeed characters.
// IntelliJ treats a linefeed at the end of the last line as a line feed, and draws an empty line. When we initialise
// a test with a trailing empty line, IntelliJ only creates one linefeed char, instead of the two that Vim creates.
// Maybe we should ensure that each file ends with a linefeed when initialising tests?
@Suppress("SpellCheckingInspection")
class FileGetLocationInfoActionTest : VimTestCase() {
  @Test
  fun `test get file info`() {
    val before = """
      |${c}Lorem Ipsum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 1 of 11; Line 1 of 6; Word 1 of 21; Byte 1 of 128", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with single word`() {
    configureByText("Lorem")
    typeText("g<C-G>")
    assertEquals("Col 1 of 5; Line 1 of 1; Word 1 of 1; Byte 1 of 5", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info of two separate words`() {
    configureByText("Lorem ipsum")
    typeText("g<C-G>")
    assertEquals("Col 1 of 11; Line 1 of 1; Word 1 of 2; Byte 1 of 11", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info of one WORD containing non-word characters`() {
    configureByText("Lorem,,,,,ipsum")
    typeText("g<C-G>")
    assertEquals("Col 1 of 15; Line 1 of 1; Word 1 of 1; Byte 1 of 15", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info of words on multiple lines`() {
    val before = """
      |Lorem ipsum dolor sit amet
      |cons${c}ectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 5 of 27; Line 2 of 2; Word 6 of 8; Byte 32 of 54", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info of words with trailing punctuation on multiple lines`() {
    val before = """
      |Lorem ipsum dolor sit amet,
      |cons${c}ectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 5 of 27; Line 2 of 2; Word 6 of 8; Byte 33 of 55", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info of words with empty lines`() {
    val before = """
      |Lorem ipsum dolor sit amet,
      |
      |
      |${c}
      |consectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 1 of 0; Line 4 of 5; Word 5 of 8; Byte 31 of 58", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info on empty line shows zero columns`() {
    val before = """
      |Lorem Ipsum
      |${c}
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
      |
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 1 of 0; Line 2 of 7; Word 2 of 21; Byte 13 of 129", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info in the middle`() {
    val before = """
      |Lorem Ipsum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur ${c}adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
      |
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 13 of 27; Line 4 of 7; Word 9 of 21; Byte 54 of 129", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info on the last line`() {
    val before = """
      |Lorem Ipsum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
      |$c
    """.trimMargin()
    configureByText(before)
    typeText("g<C-G>")
    assertEquals("Col 1 of 0; Line 7 of 7; Word 21 of 21; Byte 130 of 129", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with single word selected`() {
    val before = """
      |Lorem ${c}ipsum dolor sit amet
      |consectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("ve", "g<C-G>")
    assertEquals("Selected 1 of 2 Lines; 1 of 8 Words; 5 of 54 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with multiple words selected`() {
    val before = """
      |Lorem ${c}ipsum dolor sit amet
      |consectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("v2e", "g<C-G>")
    assertEquals("Selected 1 of 2 Lines; 2 of 8 Words; 11 of 54 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with single WORD selected`() {
    val before = """
      |Lorem ${c}ipsum,,,,dolor sit amet
      |consectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("vE", "g<C-G>")
    assertEquals("Selected 1 of 2 Lines; 1 of 7 Words; 14 of 57 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info across multiple selected lines`() {
    val before = """
      |Lorem Ipsum
      |
      |Lorem ${c}ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByText(before)
    typeText("v", "jj", "g<C-G>")
    assertEquals("Selected 3 of 6 Lines; 9 of 21 Words; 57 of 128 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with empty lines selected`() {
    val before = """
      |Lorem ${c}ipsum dolor sit amet,
      |
      |
      |
      |consectetur adipiscing elit
    """.trimMargin()
    configureByText(before)
    typeText("vG", "g<C-G>")
    assertEquals("Selected 5 of 5 Lines; 5 of 8 Words; 26 of 58 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with linewise selection`() {
    val before = """
      |Lorem Ipsum
      |
      |Lorem ${c}ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByText(before)
    typeText("V", "jj", "g<C-G>")
    assertEquals("Selected 3 of 6 Lines; 12 of 21 Words; 76 of 128 Bytes", VimPlugin.getMessage())
  }

  @Test
  fun `test get file info with blockwise selection`() {
    val before = """
      |Lorem Ipsum
      |
      |Lorem ${c}ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByText(before)
    typeText("<C-V>", "jjj", "llll", "g<C-G>")
    assertEquals("Selected 5 Cols; 4 of 6 Lines; 5 of 21 Words; 20 of 128 Bytes", VimPlugin.getMessage())
  }
}
