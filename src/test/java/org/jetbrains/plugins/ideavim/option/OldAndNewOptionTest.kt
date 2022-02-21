/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

// this class will be deleted in release 1.11
class OldAndNewOptionTest : VimTestCase() {

  fun `test toggle option`() {
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
    OptionsManager.relativenumber.set()
    TestCase.assertTrue(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
    OptionsManager.relativenumber.reset()
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
  }

  fun `test toggle option 2`() {
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.relativenumberName)
    TestCase.assertTrue(OptionsManager.relativenumber.isSet)
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.relativenumberName)
    TestCase.assertFalse(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
  }

  fun `test number option`() {
    TestCase.assertEquals("1", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "sj").asString())
    OptionsManager.scrolljump.set(10)
    TestCase.assertEquals("10", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "sj").asString())
    OptionsManager.scrolljump.resetDefault()
    TestCase.assertEquals("1", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "sj").asString())
  }

  fun `test number option 2`() {
    TestCase.assertEquals("1", OptionsManager.scrolljump.value)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, "sj", VimInt(10))
    TestCase.assertEquals("10", OptionsManager.scrolljump.value)
    VimPlugin.getOptionService().resetDefault(OptionScope.GLOBAL, "sj")
    TestCase.assertEquals("1", OptionsManager.scrolljump.value)
  }

  fun `test string option`() {
    TestCase.assertEquals("all", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.ideawriteName).asString())
    OptionsManager.ideawrite.set("file")
    TestCase.assertEquals("file", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.ideawriteName).asString())
    OptionsManager.ideawrite.resetDefault()
    TestCase.assertEquals("all", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.ideawriteName).asString())
  }

  fun `test string option 2`() {
    TestCase.assertEquals("all", OptionsManager.ideawrite.value)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.ideawriteName, VimString("file"))
    TestCase.assertEquals("file", OptionsManager.ideawrite.value)
    VimPlugin.getOptionService().resetDefault(OptionScope.GLOBAL, OptionConstants.ideawriteName)
    TestCase.assertEquals("all", OptionsManager.ideawrite.value)
  }

  fun `test list option`() {
    TestCase.assertEquals("'100,<50,s10,h", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.append("k")
    TestCase.assertEquals("'100,<50,s10,h,k", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.prepend("j")
    TestCase.assertEquals("j,'100,<50,s10,h,k", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.remove("s10")
    TestCase.assertEquals("j,'100,<50,h,k", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.remove("k")
    TestCase.assertEquals("j,'100,<50,h", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.remove("j")
    TestCase.assertEquals("'100,<50,h", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
    OptionsManager.viminfo.resetDefault()
    TestCase.assertEquals("'100,<50,s10,h", VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, "vi").asString())
  }

  fun `test list option 2`() {
    TestCase.assertEquals("'100,<50,s10,h", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().appendValue(OptionScope.GLOBAL, "vi", "k", "vi")
    TestCase.assertEquals("'100,<50,s10,h,k", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().prependValue(OptionScope.GLOBAL, "vi", "j", "vi")
    TestCase.assertEquals("j,'100,<50,s10,h,k", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().removeValue(OptionScope.GLOBAL, "vi", "s10", "vi")
    TestCase.assertEquals("j,'100,<50,h,k", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().removeValue(OptionScope.GLOBAL, "vi", "k", "vi")
    TestCase.assertEquals("j,'100,<50,h", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().removeValue(OptionScope.GLOBAL, "vi", "j", "vi")
    TestCase.assertEquals("'100,<50,h", OptionsManager.viminfo.value)
    VimPlugin.getOptionService().resetDefault(OptionScope.GLOBAL, "vi")
    TestCase.assertEquals("'100,<50,s10,h", OptionsManager.viminfo.value)
  }
}
