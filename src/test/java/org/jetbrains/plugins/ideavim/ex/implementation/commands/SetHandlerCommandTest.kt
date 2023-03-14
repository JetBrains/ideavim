/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.commands.SetHandlerCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SetHandlerCommandTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test parse a mappings`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "a:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test i mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "i:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test n mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "n:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test n-v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "n-v:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "v:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test x mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "x:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test s mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "s:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test i-n-v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "i-n-v:ide")!!
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.select)
    kotlin.test.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test broken mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "l:ide")
    kotlin.test.assertNull(newOwner)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test broken mapping 2`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerCommand.updateOwner(owner, "hau2h2:ide")
    kotlin.test.assertNull(newOwner)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test to notation`() {
    var owner = ShortcutOwnerInfo.allPerModeVim
    owner = owner.copy(normal = ShortcutOwner.IDE)
    kotlin.test.assertEquals("n:ide i-v:vim", owner.toNotation())
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test to notation 2`() {
    var owner = ShortcutOwnerInfo.allPerModeVim
    owner = owner.copy(normal = ShortcutOwner.IDE, insert = ShortcutOwner.IDE)
    kotlin.test.assertEquals("n-i:ide v:vim", owner.toNotation())
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test to notation 3`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    kotlin.test.assertEquals("a:vim", owner.toNotation())
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test to notation 4`() {
    var owner = ShortcutOwnerInfo.allPerModeVim
    owner = owner.copy(
      normal = ShortcutOwner.IDE,
      insert = ShortcutOwner.IDE,
      visual = ShortcutOwner.IDE,
      select = ShortcutOwner.IDE,
    )
    kotlin.test.assertEquals("a:ide", owner.toNotation())
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test executing command`() {
    configureByText("")
    kotlin.test.assertTrue(VimPlugin.getKey().savedShortcutConflicts.isEmpty())
    typeText(commandToKeys("sethandler <C-C> a:ide"))
    kotlin.test.assertTrue(VimPlugin.getKey().savedShortcutConflicts.isNotEmpty())
    val key = VimPlugin.getKey().savedShortcutConflicts.entries.single().key
    val owner = VimPlugin.getKey().savedShortcutConflicts.entries.single().value
    kotlin.test.assertEquals("<C-C>", injector.parser.toKeyNotation(key))
    kotlin.test.assertEquals(ShortcutOwnerInfo.allPerModeIde, owner)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test executing command 1`() {
    configureByText("")
    kotlin.test.assertTrue(VimPlugin.getKey().savedShortcutConflicts.isEmpty())
    typeText(commandToKeys("sethandler <C-C> a:ide "))
    kotlin.test.assertTrue(VimPlugin.getKey().savedShortcutConflicts.isNotEmpty())
    val key = VimPlugin.getKey().savedShortcutConflicts.entries.single().key
    val owner = VimPlugin.getKey().savedShortcutConflicts.entries.single().value
    kotlin.test.assertEquals("<C-C>", injector.parser.toKeyNotation(key))
    kotlin.test.assertEquals(ShortcutOwnerInfo.allPerModeIde, owner)
  }
}
