/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class InsertFilenameUnderCaretActionTest : VimExTestCase() {
  private fun setWindowsOption() {
    enterCommand("set isfname=@,48-57,/,\\,.,-,_,+,,,#,$,%,{,},[,],:,@-@,!,~,=")
  }

  private fun setUnixOption() {
    enterCommand("set isfname=@,48-57,/,.,-,_,+,,,#,$,%,~,=")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":set <C-R>")
    assertRenderedExText("set \"")
    typeText("<C-A>")
    assertRenderedExText("set /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-R`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":set <C-R><C-R>")
    assertRenderedExText("set \"")
    typeText("<C-F>")
    assertRenderedExText("set /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test render quote prompt after c_CTRL-R_CTRL-O`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":set <C-R><C-O>")
    assertRenderedExText("set \"")
    typeText("<C-F>")
    assertRenderedExText("set /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret literally with c_CTRL-R_CTRL-R`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret literally with c_CTRL-R_CTRL-O`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-O><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret in Windows format`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur C:\Users\${c}JasonIsaacs\Documents\hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setWindowsOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit C:\\Users\\JasonIsaacs\\Documents\\hello.txt")
  }

  @Test
  fun `test insert filename under caret at start of document`() {
    configureByText("""
      |/Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret at end of document`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret at start of line`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |/Users/${c}JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret at end of line`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/${c}JasonIsaacs/Documents/hello.txt
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename under caret on simple word`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur he${c}llo.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit hello.txt")
  }

  @Test
  fun `test insert filename following caret`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur $c  /Users/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertExText("edit /Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test insert filename on empty file causes error`() {
    configureByText(c)
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertPluginError(true)
    assertPluginErrorMessage("E446: No file name under cursor")
  }

  @Test
  fun `test insert filename on empty line causes error`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |${c}
      |consectetur adipiscing elit
    """.trimMargin())
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertPluginError(true)
    assertPluginErrorMessage("E446: No file name under cursor")
  }

  @Test
  fun `test insert filename on trailing whitespace causes error`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users/JasonIsaacs/Documents/hello.txt  ${c}....
      |sed in orci mauris.
    """.trimMargin().dotToSpace())  // Breaks hello.txt, but that's ok here
    setUnixOption()
    typeText(":edit <C-R><C-F>")
    assertPluginError(true)
    assertPluginErrorMessage("E446: No file name under cursor")
  }

  @Test
  fun `test insert filename under caret does not try to match prefix`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |consectetur /Users${c}/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin())
    setUnixOption()
    typeText(":edit /Users<C-R><C-F>")
    assertExText("edit /Users/Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test inserts filename at offset of end of incsearch range`() {
    configureByText(
      """
      |Lorem ip${c}sum dolor sit amet
      |consectetur /Users/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/tur \\/U<C-R><C-F>")
    assertExText("tur \\/U/Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test inserts filename literally at offset of end of incsearch range`() {
    configureByText(
      """
      |Lorem ip${c}sum dolor sit amet
      |consectetur /Users/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/tur \\/U<C-R><C-R><C-F>")
    assertExText("tur \\/U/Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test inserts filename following offset of end of incsearch range`() {
    configureByText(
      """
      |Lorem ip${c}sum dolor sit amet
      |consectetur /Users/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/tur<C-R><C-F>")
    assertExText("tur/Users/JasonIsaacs/Documents/hello.txt")
  }

  @Test
  fun `test inserts filename following offset of end of incsearch range across multiple lines`() {
    configureByText(
      """
      |Lorem ip${c}sum dolor sit amet
      |consectetur /Users/JasonIsaacs/Documents/hello.txt adipiscing elit
      |sed in orci mauris.
    """.trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/amet\\n.*tur<C-R><C-F>")
    assertExText("amet\\n.*tur/Users/JasonIsaacs/Documents/hello.txt")
  }
}
