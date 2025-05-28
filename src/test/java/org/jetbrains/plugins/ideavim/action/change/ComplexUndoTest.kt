/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for complex undo scenarios involving motion and change operations
 * These tests verify that undo correctly restores both text and cursor position
 */
class ComplexUndoTest : VimTestCase() {
  @Test
  fun `test undo change inside parentheses with cursor movement`() {
    // This is the example from the user's request
    configureByText("a${c}bc(xxx)def")
    typeText("ci(")
    typeText("yyy")
    typeText("<Esc>")
    assertState("abc(yy${c}y)def")
    typeText("u")
    assertState("a${c}bc(xxx)def")
  }

  @Test
  fun `test undo change inside parentheses with cursor movement with oldundo`() {
    // This is the example from the user's request
    configureByText("a${c}bc(xxx)def")
    try {
      enterCommand("set oldundo")
      typeText("ci(")
      typeText("yyy")
      typeText("<Esc>")
      assertState("abc(yy${c}y)def")
      typeText("u")
      assertState("abc(yyy${c})def")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo delete inside brackets with cursor movement`() {
    configureByText("fo${c}o[bar]baz")
    typeText("di[")
    assertState("foo[${c}]baz")
    typeText("u")
    assertState("fo${c}o[bar]baz")
  }

  @Test
  fun `test undo delete inside brackets with cursor movement with oldundo`() {
    configureByText("fo${c}o[bar]baz")
    try {
      enterCommand("set oldundo")
      typeText("di[")
      assertState("foo[${c}]baz")
      typeText("u")
      assertState("fo${c}o[bar]baz")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change inside quotes with cursor movement`() {
    configureByText("Say ${c}hello \"world\" today")
    typeText("ci\"")
    typeText("universe")
    typeText("<Esc>")
    assertState("Say hello \"univers${c}e\" today")
    typeText("u")
    assertState("Say ${c}hello \"world\" today")
  }

  @Test
  fun `test undo change inside quotes with cursor movement with oldundo`() {
    configureByText("Say ${c}hello \"world\" today")
    try {
      enterCommand("set oldundo")
      typeText("ci\"")
      typeText("universe")
      typeText("<Esc>")
      assertState("Say hello \"univers${c}e\" today")
      typeText("u")
      assertState("Say hello \"universe${c}\" today")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo delete word with cursor at different position`() {
    configureByText("The qu${c}ick brown fox")
    typeText("daw")  // Delete a word (including surrounding spaces)
    assertState("The ${c}brown fox")
    typeText("u")
    assertState("The qu${c}ick brown fox")
  }

  @Test
  fun `test undo delete word with cursor at different position with oldundo`() {
    configureByText("The qu${c}ick brown fox")
    try {
      enterCommand("set oldundo")
      typeText("daw")  // Delete a word (including surrounding spaces)
      assertState("The ${c}brown fox")
      typeText("u")
      assertState("The qu${c}ick brown fox")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change paragraph with cursor movement`() {
    configureByText(
      """
      First paragraph.
      
      Sec${c}ond paragraph
      with multiple lines.
      
      Third paragraph.
    """.trimIndent()
    )
    typeText("cip")
    typeText("New content")
    typeText("<Esc>")
    assertState(
      """
      First paragraph.
      
      New conten${c}t
      
      Third paragraph.
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      First paragraph.
      
      Sec${c}ond paragraph
      with multiple lines.
      
      Third paragraph.
    """.trimIndent()
    )
  }

  @Test
  fun `test undo change paragraph with cursor movement with oldundo`() {
    configureByText(
      """
      First paragraph.
      
      Sec${c}ond paragraph
      with multiple lines.
      
      Third paragraph.
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("cip")
      typeText("New content")
      typeText("<Esc>")
      assertState(
        """
      First paragraph.
      
      New conten${c}t
      
      Third paragraph.
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      First paragraph.
      
      New content${c}
      
      Third paragraph.
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo delete to search result`() {
    configureByText("abc${c}defghijklmnop")
    typeText("d/jkl<CR>")  // Delete to search result
    assertState("abc${c}jklmnop")
    typeText("u")
    assertState("abc${c}defghijklmnop")
  }

  @Test
  fun `test undo delete to search result with oldundo`() {
    configureByText("abc${c}defghijklmnop")
    try {
      enterCommand("set oldundo")
      typeText("d/jkl<CR>")  // Delete to search result
      assertState("abc${c}jklmnop")
      typeText("u")
      assertState("abc${c}defghijklmnop")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change to mark with cursor movement`() {
    configureByText(
      """
      Li${c}ne 1
      Line 2
      Line 3
      Line 4
    """.trimIndent()
    )
    typeText("ma")  // Set mark 'a'
    typeText("2j")   // Move down 2 lines
    assertState(
      """
      Line 1
      Line 2
      Li${c}ne 3
      Line 4
    """.trimIndent()
    )
    typeText("c'a")  // Change to mark 'a'
    typeText("Changed")
    typeText("<Esc>")
    assertState(
      """
      Change${c}d
      Line 4
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      Line 1
      Line 2
      Li${c}ne 3
      Line 4
    """.trimIndent()
    )
  }

  @Test
  fun `test undo change to mark with cursor movement with oldundo`() {
    configureByText(
      """
      Li${c}ne 1
      Line 2
      Line 3
      Line 4
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("ma")  // Set mark 'a'
      typeText("2j")   // Move down 2 lines
      assertState(
        """
      Line 1
      Line 2
      Li${c}ne 3
      Line 4
    """.trimIndent()
      )
      typeText("c'a")  // Change to mark 'a'
      typeText("Changed")
      typeText("<Esc>")
      assertState(
        """
      Change${c}d
      Line 4
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      Changed${c}
      Line 4
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo substitute with cursor movement`() {
    configureByText("${c}Hello world hello")
    typeText(":s/hello/goodbye/gi<CR>")  // Substitute with flags
    assertState("${c}goodbye world goodbye")
    typeText("u")
    assertState("${c}Hello world hello")
  }

  @Test
  fun `test undo substitute with cursor movement with oldundo`() {
    configureByText("${c}Hello world hello")
    try {
      enterCommand("set oldundo")
      typeText(":s/hello/goodbye/gi<CR>")  // Substitute with flags
      assertState("${c}goodbye world goodbye")
      typeText("u")
      assertState("${c}Hello world hello")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo multiple operations in sequence`() {
    configureByText("${c}abc def ghi")

    // First operation: delete word
    typeText("dw")
    assertState("${c}def ghi")

    // Second operation: change word
    typeText("cw")
    typeText("xyz")
    typeText("<Esc>")
    assertState("xy${c}z ghi")

    // Third operation: append at end
    typeText("A")
    typeText(" jkl")
    typeText("<Esc>")
    assertState("xyz ghi jk${c}l")

    // Undo all operations
    typeText("u")
    assertState("xyz ghi${c}")

    typeText("u")
    assertState("${c}def ghi")

    typeText("u")
    assertState("${c}abc def ghi")
  }

  @Test
  fun `test undo multiple operations in sequence with oldundo`() {
    configureByText("${c}abc def ghi")
    try {
      enterCommand("set oldundo")

      // First operation: delete word
      typeText("dw")
      assertState("${c}def ghi")

      // Second operation: change word
      typeText("cw")
      typeText("xyz")
      typeText("<Esc>")
      assertState("xy${c}z ghi")

      // Third operation: append at end
      typeText("A")
      typeText(" jkl")
      typeText("<Esc>")
      assertState("xyz ghi jk${c}l")

      // Undo all operations
      typeText("u")
      assertState("xyz ghi jkl${c}")

      typeText("u")
      assertState("xyz ghi${c}")

      typeText("u")
      assertState("xyz${c} ghi")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo with text objects and counts`() {
    configureByText("function(${c}arg1, arg2, arg3)")
    typeText("d2f,")  // Delete to 2nd comma
    assertState("function(${c} arg3)")
    typeText("u")
    assertState("function(${c}arg1, arg2, arg3)")
  }

  @Test
  fun `test undo with text objects and counts with oldundo`() {
    configureByText("function(${c}arg1, arg2, arg3)")
    try {
      enterCommand("set oldundo")
      typeText("d2f,")  // Delete to 2nd comma
      assertState("function(${c} arg3)")
      typeText("u")
      assertState("function(${c}arg1, arg2, arg3)")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo inner word at end of word`() {
    configureByText("The quic${c}k brown fox")
    typeText("diw")
    assertState("The ${c} brown fox")
    typeText("u")
    assertState("The quic${c}k brown fox")
  }

  @Test
  fun `test undo inner word at end of word with oldundo`() {
    configureByText("The quic${c}k brown fox")
    try {
      enterCommand("set oldundo")
      typeText("diw")
      assertState("The ${c} brown fox")
      typeText("u")
      assertState("The quic${c}k brown fox")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo change with register and motion`() {
    configureByText("${c}Hello world")
    typeText("\"aciw")  // Change inner word into register 'a'
    typeText("Goodbye")
    typeText("<Esc>")
    assertState("Goodby${c}e world")
    typeText("u")
    assertState("${c}Hello world")
  }

  @Test
  fun `test undo change with register and motion with oldundo`() {
    configureByText("${c}Hello world")
    try {
      enterCommand("set oldundo")
      typeText("\"aciw")  // Change inner word into register 'a'
      typeText("Goodbye")
      typeText("<Esc>")
      assertState("Goodby${c}e world")
      typeText("u")
      assertState("Goodbye${c} world")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo join with indentation handling`() {
    configureByText(
      """
      if (condition) {
          ${c}doSomething();
          doMore();
      }
    """.trimIndent()
    )
    typeText("J")
    assertState(
      """
      if (condition) {
          doSomething();${c} doMore();
      }
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      if (condition) {
          ${c}doSomething();
          doMore();
      }
    """.trimIndent()
    )
  }

  @Test
  fun `test undo join with indentation handling with oldundo`() {
    configureByText(
      """
      if (condition) {
          ${c}doSomething();
          doMore();
      }
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("J")
      assertState(
        """
      if (condition) {
          doSomething();${c} doMore();
      }
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      if (condition) {
          ${c}doSomething();
          doMore();
      }
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo replace mode changes`() {
    configureByText("${c}Hello world")
    typeText("R")
    typeText("Goodbye")
    typeText("<Esc>")
    assertState("Goodby${c}eorld")
    typeText("u")
    assertState("${c}Hello world")
  }

  @Test
  fun `test undo replace mode changes with oldundo`() {
    configureByText("${c}Hello world")
    try {
      enterCommand("set oldundo")
      typeText("R")
      typeText("Goodbye")
      typeText("<Esc>")
      assertState("Goodby${c}eorld")
      typeText("u")
      assertState("Goodbye${c}orld")
    } finally {
      enterCommand("set nooldundo")
    }
  }
}