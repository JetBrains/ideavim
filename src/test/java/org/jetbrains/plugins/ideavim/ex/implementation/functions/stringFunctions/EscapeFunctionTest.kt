/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.stringFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class EscapeFunctionTest : VimTestCase() {
  @Test
  fun `test escape windows path with spaces`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('c:\program files\vim', ' \')"""))
    assertExOutput("""c:\\program\ files\\vim""")
  }

  @Test
  fun `test escape multiple special characters`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('special chars: #$^', '#$^')"""))
    assertExOutput("""special chars: \#\$\^""")
  }

  @Test
  fun `test escape when no escaping needed`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('no escaping needed', 'xyz')"""))
    assertExOutput("no escaping needed")
  }

  @Test
  fun `test escape empty strings`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('', '')"""))
    assertExOutput("")
  }

  @Test
  fun `test escape consecutive special characters`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('$$$$', '$')"""))
    assertExOutput("""\$\$\$\$""")
  }

  @Test
  fun `test escape with double backslashes`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('test\\here', '\\')"""))
    assertExOutput("""test\\\\here""")
  }

  @Test
  fun `test escape with unicode characters`() {
    configureByText("\n")
    typeText(commandToKeys("""echo escape('Hello 👋 #world', '#')"""))
    assertExOutput("""Hello 👋 \#world""")

    typeText(commandToKeys("""echo escape('🎉$🎊$', '$')"""))
    assertExOutput("""🎉\$🎊\$""")
  }
}
