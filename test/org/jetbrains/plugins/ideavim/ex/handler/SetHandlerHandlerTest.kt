/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.ex.handler.SetHandlerHandler
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class SetHandlerHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test parse a mappings`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "a:ide")!!
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test i mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "i:ide")!!
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test n mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "n:ide")!!
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test n-v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "n-v:ide")!!
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "v:ide")!!
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test x mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "x:ide")!!
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test s mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "s:ide")!!
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.VIM, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test i-n-v mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "i-n-v:ide")!!
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.insert)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.normal)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.select)
    TestCase.assertEquals(ShortcutOwner.IDE, newOwner.visual)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test broken mapping`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "l:ide")
    TestCase.assertNull(newOwner)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test broken mapping 2`() {
    val owner = ShortcutOwnerInfo.allPerModeVim
    val newOwner = SetHandlerHandler.updateOwner(owner, "hau2h2:ide")
    TestCase.assertNull(newOwner)
  }
}