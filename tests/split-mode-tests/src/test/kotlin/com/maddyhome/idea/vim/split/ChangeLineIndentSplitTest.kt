/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

/**
 * `cc` and `S` reindent the line they empty, as Vim's 'cindent' does. The indent is worked out on the backend, which
 * owns the formatter, while the line itself is emptied on the frontend - so these tests pin down that the backend sees
 * the emptied line by the time it is asked, and that the indent it answers with lands in the frontend editor.
 */
class ChangeLineIndentSplitTest : IdeaVimStarterTestBase() {

  /**
   * Line 3 sits between the members of the class and line 5 inside the method body. Both are given as arguments,
   * because an indent on a line of its own is trailing white space, which an editor may strip from a raw string.
   */
  private fun javaFile(name: String, fieldGap: String = "", bodyGap: String = "") = createFile(
    "src/$name.java",
    listOf("public class $name {", "    int a = 1;", fieldGap, "    void f() {", bodyGap, "    }", "}")
      .joinToString("\n") + "\n",
  )

  @Test
  fun `cc on empty line in method body indents to the statements`() {
    openFile(javaFile("ChangeLine1"))
    goToLine(5)
    typeVimAndEscape("ccy")

    assertEditorContains("{\n        y\n    }", "`cc` should indent line 5 to the method body")
  }

  @Test
  fun `cc on empty line in class body indents to the members`() {
    openFile(javaFile("ChangeLine2"))
    goToLine(3)
    typeVimAndEscape("ccy")

    assertEditorContains(";\n    y\n    void", "`cc` should indent line 3 to the class members")
  }

  @Test
  fun `S on blank line with wrong indent fixes the indent`() {
    openFile(javaFile("ChangeLine3", bodyGap = "  "))
    goToLine(5)
    typeVimAndEscape("Sy")

    assertEditorContains("{\n        y\n    }", "`S` should replace the wrong indent of line 5")
  }

  @Test
  fun `cc and immediate escape leaves an empty line`() {
    openFile(javaFile("ChangeLine4", bodyGap = "  "))
    goToLine(5)
    typeVimAndEscape("cc")

    assertEditorContains("{\n\n    }", "Leaving Insert mode right away should remove the indent `cc` added")
  }

  @Test
  fun `repeating cc indents every line by its own context`() {
    openFile(javaFile("ChangeLine6"))
    goToLine(3)
    typeVimAndEscape("ccy")

    assertEditorContains(";\n    y\n    void", "`cc` should indent line 3 to the class members")

    goToLine(5)
    typeVim(".")

    assertEditorContains("{\n        y\n    }", "`.` should indent line 5 to the method body")
  }
}
