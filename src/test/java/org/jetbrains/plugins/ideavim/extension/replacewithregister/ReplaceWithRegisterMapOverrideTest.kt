/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.replacewithregister

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// Reproduction for VIM-4180: user's `nmap gr <nop>` in .ideavimrc is overridden by the
// ReplaceWithRegister extension's default mappings when they are applied after the vimrc
// execution as part of the delayed extension-init flow.
class ReplaceWithRegisterMapOverrideTest : VimTestCase() {

  @Test
  fun `user nmap gr nop is not overridden by plugin default`() {
    configureByText("hello")

    injector.vimscriptExecutor.executingVimscript = true
    injector.vimscriptExecutor.executingIdeaVimRcConfiguration = true
    executeVimscript(
      """
      set ReplaceWithRegister
      xmap s <Plug>ReplaceWithRegisterVisual
      nmap s <Plug>ReplaceWithRegisterOperator
      nmap ss <Plug>ReplaceWithRegisterLine
      nmap gr <nop>
      nmap grr <nop>
      vmap gr <nop>
      """.trimIndent(),
      skipHistory = false,
    )
    injector.vimscriptExecutor.executingIdeaVimRcConfiguration = false
    injector.vimscriptExecutor.executingVimscript = false

    val nop = injector.parser.parseKeys("<nop>")
    val grKeys = injector.parser.parseKeys("gr")
    val grrKeys = injector.parser.parseKeys("grr")

    val nGr = VimPlugin.getKey().getKeyMapping(MappingMode.NORMAL)[grKeys]
    val nGrr = VimPlugin.getKey().getKeyMapping(MappingMode.NORMAL)[grrKeys]
    val vGr = VimPlugin.getKey().getKeyMapping(MappingMode.VISUAL)[grKeys]

    assertEquals(nop, (nGr as? ToKeysMappingInfo)?.toKeys, "normal gr should map to <nop>")
    assertEquals(nop, (nGrr as? ToKeysMappingInfo)?.toKeys, "normal grr should map to <nop>")
    assertEquals(nop, (vGr as? ToKeysMappingInfo)?.toKeys, "visual gr should map to <nop>")
  }
}
