/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.help

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenerateHelpTest {
  @Test
  fun `plain keys are the tag`() {
    assertEquals("dd", keyToTag("dd"))
    assertEquals("<Left>", keyToTag("<Left>"))
    assertEquals("<C-Left>", keyToTag("<C-Left>"))
  }

  @Test
  fun `control keys use Vim's notation`() {
    assertEquals("CTRL-W", keyToTag("<C-W>"))
    assertEquals("CTRL-W", keyToTag("<c-w>"))
    assertEquals("CTRL-W_h", keyToTag("<C-W>h"))
    assertEquals("CTRL-W_CTRL-H", keyToTag("<C-W><C-H>"))
    assertEquals("g_CTRL-G", keyToTag("g<C-G>"))
    assertEquals("CTRL-\\_CTRL-N", keyToTag("<C-\\><C-N>"))
  }

  @Test
  fun `characters that delimit tags are spelled out`() {
    assertEquals("star", keyToTag("*"))
    assertEquals("gstar", keyToTag("g*"))
    assertEquals("bar", keyToTag("|"))
  }

  @Test
  fun `ex command tags are the abbreviation and the full name`() {
    assertEquals(listOf(":h", ":help"), exCommandTags("h[elp]"))
    assertEquals(listOf(":IdeaPlug"), exCommandTags("IdeaPlug"))
    assertEquals(listOf(":&"), exCommandTags("&"))
  }

  @Test
  fun `commands are placed into the sections of their modes`() {
    val help = renderHelp(
      listOf(
        HelpData(
          exCommands = listOf(ExCommandHelp(listOf("h[elp]"), "Open the help.")),
          commands = listOf(
            CommandHelp(listOf("w"), "NXO", lookup = false, description = "Move a word forward."),
            CommandHelp(listOf("d"), "X", lookup = false, description = "Delete the selection."),
            CommandHelp(listOf("<C-W>"), "IC", lookup = false, description = "Delete a word before the cursor."),
          ),
        )
      )
    )

    assertTrue(help.contains("*:h* *:help*\n:h[elp]                 Open the help.\n"), help)
    assertTrue(help.contains("*w*\nw                       Move a word forward.\n"), help)
    assertTrue(help.contains("*v_d*\nd                       Delete the selection.\n"), help)
    assertTrue(help.contains("*i_CTRL-W*\n"), help)
    assertTrue(help.contains("*c_CTRL-W*\n"), help)
    // A command that is in Normal mode isn't repeated in the Visual mode section
    assertTrue(!help.contains("*v_w*"), help)
  }

  @Test
  fun `a tag claimed twice leads to the first command`() {
    val help = renderHelp(
      listOf(
        HelpData(
          exCommands = emptyList(),
          commands = listOf(
            CommandHelp(listOf("<C-Y>"), "I", lookup = true, description = "Accept the completion."),
            CommandHelp(listOf("<C-Y>"), "I", lookup = false, description = "Insert the character above."),
          ),
        )
      )
    )

    assertEquals(1, Regex("""\*i_CTRL-Y\*""").findAll(help).count(), help)
    assertTrue(help.indexOf("Insert the character above.") < help.indexOf("Accept the completion."), help)
    assertTrue(help.contains("*i_CTRL-Y*\n<C-Y>                   Insert the character above."), help)
  }

  @Test
  fun `long descriptions are wrapped`() {
    val description = "word ".repeat(40).trim() + "."
    val help = renderHelp(
      listOf(HelpData(emptyList(), listOf(CommandHelp(listOf("x"), "N", lookup = false, description = description))))
    )

    assertTrue(help.lines().all { it.length <= TEXT_WIDTH }, help)
    assertTrue(help.contains("\n                        word word"), help)
  }

  @Test
  fun `parses the output of HelpProcessor`() {
    val json = """
      {
        "exCommands": [ { "names": ["h[elp]"], "description": "Open the help.", "className": "HelpCommand" } ],
        "commands": [
          { "keys": ["x"], "modes": "N", "lookup": false, "description": "Delete.", "className": "DeleteAction" }
        ]
      }
    """.trimIndent()

    assertEquals(
      HelpData(
        listOf(ExCommandHelp(listOf("h[elp]"), "Open the help.")),
        listOf(CommandHelp(listOf("x"), "N", lookup = false, description = "Delete.")),
      ),
      parseHelpData(json),
    )
  }
}
