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

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

// this class will be deleted in release 1.11
class OldAndNewOptionTest : VimTestCase() {

  fun `test toggle option`() {
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu", null))
    OptionsManager.relativenumber.set()
    TestCase.assertTrue(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu", null))
    OptionsManager.relativenumber.reset()
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu", null))
  }

  fun `test toggle option 2`() {
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu", null))
    VimPlugin.getOptionService().setOption(OptionService.Scope.GLOBAL, "rnu", null)
    TestCase.assertTrue(OptionsManager.relativenumber.isSet)
    VimPlugin.getOptionService().unsetOption(OptionService.Scope.GLOBAL, "rnu", null)
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu", null))
  }

  fun `test number option`() {
    TestCase.assertEquals("1", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "sj", null).asString())
    OptionsManager.scrolljump.set(10)
    TestCase.assertEquals("10", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "sj", null).asString())
    OptionsManager.scrolljump.resetDefault()
    TestCase.assertEquals("1", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "sj", null).asString())
  }

  fun `test number option 2`() {
    TestCase.assertEquals("1", OptionsManager.scrolljump.value)
    VimPlugin.getOptionService().setOptionValue(OptionService.Scope.GLOBAL, "sj", VimInt(10), null)
    TestCase.assertEquals("10", OptionsManager.scrolljump.value)
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "sj", null)
    TestCase.assertEquals("1", OptionsManager.scrolljump.value)
  }

  fun `test string option`() {
    TestCase.assertEquals("all", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "ideawrite", null).asString())
    OptionsManager.ideawrite.set("file")
    TestCase.assertEquals("file", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "ideawrite", null).asString())
    OptionsManager.ideawrite.resetDefault()
    TestCase.assertEquals("all", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "ideawrite", null).asString())
  }

  fun `test string option 2`() {
    TestCase.assertEquals("all", OptionsManager.ideawrite.value)
    VimPlugin.getOptionService().setOptionValue(OptionService.Scope.GLOBAL, "ideawrite", VimString("file"), null)
    TestCase.assertEquals("file", OptionsManager.ideawrite.value)
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "ideawrite", null)
    TestCase.assertEquals("all", OptionsManager.ideawrite.value)
  }

  fun `test list option`() {
    TestCase.assertEquals("'100,<50,s10,h", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.append("k")
    TestCase.assertEquals("'100,<50,s10,h,k", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.prepend("j")
    TestCase.assertEquals("j,'100,<50,s10,h,k", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.remove("s10")
    TestCase.assertEquals("j,'100,<50,h,k", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.remove("k")
    TestCase.assertEquals("j,'100,<50,h", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.remove("j")
    TestCase.assertEquals("'100,<50,h", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
    OptionsManager.viminfo.resetDefault()
    TestCase.assertEquals("'100,<50,s10,h", VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "vi", null).asString())
  }

  fun `test list option 2`() {
    TestCase.assertEquals("'100,<50,s10,h", OptionsManager.viminfo.value)
    OptionServiceImpl.appendValue(OptionService.Scope.GLOBAL, "vi", "k", null, "vi")
    TestCase.assertEquals("'100,<50,s10,h,k", OptionsManager.viminfo.value)
    OptionServiceImpl.prependValue(OptionService.Scope.GLOBAL, "vi", "j", null, "vi")
    TestCase.assertEquals("j,'100,<50,s10,h,k", OptionsManager.viminfo.value)
    OptionServiceImpl.removeValue(OptionService.Scope.GLOBAL, "vi", "s10", null, "vi")
    TestCase.assertEquals("j,'100,<50,h,k", OptionsManager.viminfo.value)
    OptionServiceImpl.removeValue(OptionService.Scope.GLOBAL, "vi", "k", null, "vi")
    TestCase.assertEquals("j,'100,<50,h", OptionsManager.viminfo.value)
    OptionServiceImpl.removeValue(OptionService.Scope.GLOBAL, "vi", "j", null, "vi")
    TestCase.assertEquals("'100,<50,h", OptionsManager.viminfo.value)
    OptionServiceImpl.resetDefault(OptionService.Scope.GLOBAL, "vi", null)
    TestCase.assertEquals("'100,<50,s10,h", OptionsManager.viminfo.value)
  }
}
