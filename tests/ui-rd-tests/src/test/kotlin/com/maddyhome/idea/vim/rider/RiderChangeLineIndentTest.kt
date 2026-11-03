/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * The indent of the line emptied by `cc`/`S`. In Rider, C# indentation is worked out by the ReSharper backend
 */
class RiderChangeLineIndentTest : IdeaVimRiderTestBase() {

  @AfterEach
  fun resetMode() {
    esc()
  }

  // Control for the tests below: `o` gets its indent from the IDE's Enter handler
  @Test
  fun `o on the line above an empty line indents the new line`() {
    setText(TEXT, caretAt = "static void Main")
    typeVim("j")

    typeVim("ox")

    assertText(TEXT.replace("{\n\n", "{\n        x\n\n"))
  }

  // VIM-2524
  @Test
  fun `cc on an empty line indents it`() {
    setText(TEXT, caretAt = "\n\n        Console")
    typeVim("j")

    typeVim("ccx")

    assertText(TEXT.replace("{\n\n", "{\n        x\n"))
  }

  // VIM-2524
  @Test
  fun `cc on a line with a wrong indent reindents it`() {
    val text = TEXT.replace("{\n\n", "{\n  \n")
    setText(text, caretAt = "\n  \n")
    typeVim("j")

    typeVim("ccx")

    assertText(TEXT.replace("{\n\n", "{\n        x\n"))
  }

  // VIM-2524
  @Test
  fun `S on an empty line indents it`() {
    setText(TEXT, caretAt = "\n\n        Console")
    typeVim("j")

    typeVim("Sx")

    assertText(TEXT.replace("{\n\n", "{\n        x\n"))
  }

  companion object {
    // The reporter's example
    private val TEXT = """
      |namespace ConsoleApp;
      |
      |class Program
      |{
      |    static void Main(string[] args)
      |    {
      |
      |        Console.WriteLine("Hello, World!");
      |    }
      |}
    """.trimMargin()
  }
}
