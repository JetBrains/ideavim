/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class OperatorActionTest : VimTestCase() {
  @Test
  fun `test operator action throws error if operatorfunc is empty`() {
    doTest("g@w", "lorem ipsum", "lorem ipsum")
    assertPluginErrorMessageContains("E774: 'operatorfunc' is empty")
  }

  @Test
  fun `test operator action throws error if operatorfunc is name of unknown function`() {
    doTest("g@w", "lorem ipsum", "lorem ipsum") {
      enterCommand("set operatorfunc=Foo")
    }
    assertPluginErrorMessageContains("E117: Unknown function: Foo")
  }

  @Test
  fun `test operator action with function name`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "xxxxx ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal `[v`]rx"
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx :set opfunc=Redact<CR>g@")
    }
  }

  @Test
  fun `test operator action with character wise motion`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "charlorem ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal i" . a:type
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx :set opfunc=Redact<CR>g@")
    }
  }

  @Test
  fun `test operator action with linewise motion`() {
    doTest(
      "Vgx",
      "lorem ipsum dolor sit amet",
      "linelorem ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal i" . a:type
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx <Esc>:set opfunc=Redact<CR>gvg@")
    }
  }

  @Test
  fun `test operator action with blockwise motion`() {
    doTest(
      "<C-V>gx",
      "lorem ipsum dolor sit amet",
      "blocklorem ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal i" . a:type
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx <Esc>:set opfunc=Redact<CR>gvg@")
    }
  }

  @Test
  fun `test operator action with function`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "xxxxx ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal `[v`]rx"
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx :set opfunc=function('Redact')<CR>g@")
    }
  }

  @Test
  fun `test operator action throws error with unknown function`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "lorem ipsum dolor sit amet"
    ) {
      enterCommand("noremap gx :set opfunc=function('Foo')<CR>g@")
    }
    assertPluginErrorMessageContains("E117: Unknown function: Foo")
  }

  @Test
  fun `test operator function with funcref`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "xxxxx ipsum dolor sit amet"
    ) {
      executeVimscript(
        """function! Redact(type)
        |  execute "normal `[v`]rx"
        |endfunction
      """.trimMargin()
      )
      enterCommand("noremap gx :set opfunc=funcref('Redact')<CR>g@")
    }
  }

  @Test
  fun `test operator action throws error with unknown function ref`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "lorem ipsum dolor sit amet"
    ) {
      enterCommand("noremap gx :set opfunc=funcref('Foo')<CR>g@")
    }
    assertPluginErrorMessageContains("E117: Unknown function: Foo")
  }

  @Test
  @Disabled(":set does not correctly parse the quotes in the lambda syntax")
  // The parser is treating the second double-quote char as a comment. The argument to the command is parsed as:
  // opfunc={ arg -> execute "`[v`]rx
  // The map command is properly handled - the `<CR>g@` is correctly understood, and the full lambda is passed to the
  // parser, but the parser does not fully handle the text
  fun `test operator function with lambda`() {
    doTest(
      "gxe",
      "lorem ipsum dolor sit amet",
      "lorem ipsum dolor sit amet"
    ) {
      enterCommand("noremap gx :set opfunc={ arg -> execute \"`[v`]rx\" }<CR>g@")
    }
  }
}
