/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.stringFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class EscapeFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test escape windows path with spaces`() {
    assertCommandOutput("""echo escape('c:\program files\vim', ' \')""", """c:\\program\ files\\vim""")
  }

  @Test
  fun `test escape multiple special characters`() {
    assertCommandOutput("""echo escape('special chars: #$^', '#$^')""", """special chars: \#\$\^""")
  }

  @Test
  fun `test escape when no escaping needed`() {
    assertCommandOutput("""echo escape('no escaping needed', 'xyz')""", "no escaping needed")
  }

  @Test
  fun `test escape empty strings`() {
    assertCommandOutput("""echo escape('', '')""", "")
  }

  @Test
  fun `test escape consecutive special characters`() {
    assertCommandOutput("""echo escape('$$$$', '$')""", """\$\$\$\$""")
  }

  @Test
  fun `test escape with double backslashes`() {
    assertCommandOutput("""echo escape('test\\here', '\\')""", """test\\\\here""")
  }

  @Test
  fun `test escape with unicode characters`() {
    assertCommandOutput("""echo escape('Hello 👋 #world', '#')""", """Hello 👋 \#world""")
    assertCommandOutput("""echo escape('🎉$🎊$', '$')""", """🎉\$🎊\$""")
  }
}
