/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AbbrevCommandTest : VimTestCase() {

  @AfterEach
  fun tearDownAbbrev() {
    injector.abbreviationGroup.removeAllAbbreviations()
  }

  @Test
  fun `iabbrev stores insert-mode abbreviation`() {
    configureByText("\n")
    enterCommand("iabbrev foo bar")
    assertPluginError(false)

    val entry = injector.abbreviationGroup.getAbbreviation("foo", MappingMode.INSERT)
    assertNotNull(entry)
    assertEquals("bar", entry.rhs)
  }

  @Test
  fun `iabbrev accepts full-id lhs of only keyword chars`() {
    configureByText("\n")
    enterCommand("iabbrev my_var2 placeholder")
    assertPluginError(false)
    assertNotNull(injector.abbreviationGroup.getAbbreviation("my_var2", MappingMode.INSERT))
  }

  @Test
  fun `iabbrev accepts end-id lhs whose preceding chars are non-keyword`() {
    configureByText("\n")
    enterCommand("iabbrev #i include")
    assertPluginError(false)
    assertNotNull(injector.abbreviationGroup.getAbbreviation("#i", MappingMode.INSERT))
  }

  @Test
  fun `iabbrev accepts non-id lhs ending in non-keyword char`() {
    configureByText("\n")
    enterCommand("iabbrev def# define")
    assertPluginError(false)
    assertNotNull(injector.abbreviationGroup.getAbbreviation("def#", MappingMode.INSERT))
  }

  @Test
  fun `iabbrev rejects mixed lhs ending in keyword with non-keyword in middle`() {
    configureByText("\n")
    enterCommand("iabbrev f#i value")
    assertPluginError(true)
    assertEquals(null, injector.abbreviationGroup.getAbbreviation("f#i", MappingMode.INSERT))
  }

  @Test
  fun `iabbrev expands lhs on whitespace trigger in insert mode`() {
    configureByText("${c}\n")
    enterCommand("iabbrev foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }
}
