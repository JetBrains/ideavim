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

package org.jetbrains.plugins.ideavim.group

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.VimTestCase

class KeyGroupTest : VimTestCase() {
  private val owner = MappingOwner.Plugin.get("KeyGroupTest")

  fun `test remove key mapping`() {
    val keyGroup = VimPlugin.getKey()
    val keys = parseKeys("<C-S-B>")

    configureByText("I ${c}found it in a legendary land")
    typeText(keys)
    myFixture.checkResult("I ${c}found it in a legendary land")

    keyGroup.putKeyMapping(MappingMode.N, keys, owner, parseKeys("h"), false)
    typeText(keys)
    myFixture.checkResult("I${c} found it in a legendary land")

    keyGroup.removeKeyMapping(owner)
    typeText(keys)
    myFixture.checkResult("I${c} found it in a legendary land")
  }

  fun `test remove and add key mapping`() {
    val keyGroup = VimPlugin.getKey()
    val keys = parseKeys("<C-S-B>")

    configureByText("I ${c}found it in a legendary land")
    typeText(keys)
    myFixture.checkResult("I ${c}found it in a legendary land")

    keyGroup.putKeyMapping(MappingMode.N, keys, owner, parseKeys("h"), false)
    typeText(keys)
    myFixture.checkResult("I${c} found it in a legendary land")

    repeat(10) {
      keyGroup.removeKeyMapping(owner)
      keyGroup.putKeyMapping(MappingMode.N, keys, owner, parseKeys("h"), false)
    }
    typeText(keys)
    myFixture.checkResult("${c}I found it in a legendary land")
  }
}