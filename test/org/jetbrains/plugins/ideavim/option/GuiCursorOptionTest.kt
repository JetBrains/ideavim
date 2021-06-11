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

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.option.GuiCursorMode
import com.maddyhome.idea.vim.option.GuiCursorOption
import com.maddyhome.idea.vim.option.GuiCursorOptionData
import com.maddyhome.idea.vim.option.GuiCursorType
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

class GuiCursorOptionTest: VimTestCase() {
  private lateinit var option: GuiCursorOption

  override fun setUp() {
    super.setUp()
    option = OptionsManager.guicursor
  }

  @Suppress("SpellCheckingInspection")
  fun `test parses default values`() {
    val values = option.values()
    assertEquals(6, values.size)

    assertEquals(enumSetOf(GuiCursorMode.NORMAL, GuiCursorMode.VISUAL, GuiCursorMode.CMD_LINE), values[0]!!.modes)
    assertEquals(GuiCursorType.BLOCK, values[0]!!.attributes.type)
    assertEquals("Cursor", values[0]!!.attributes.highlightGroup)
    assertEquals("lCursor", values[0]!!.attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.VISUAL_EXCLUSIVE), values[1]!!.modes)
    assertEquals(GuiCursorType.VER, values[1]!!.attributes.type)
    assertEquals(35, values[1]!!.attributes.thickness)
    assertEquals("Cursor", values[1]!!.attributes.highlightGroup)
    assertEquals("", values[1]!!.attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.OP_PENDING), values[2]!!.modes)
    assertEquals(GuiCursorType.HOR, values[2]!!.attributes.type)
    assertEquals(50, values[2]!!.attributes.thickness)
    assertEquals("Cursor", values[2]!!.attributes.highlightGroup)
    assertEquals("", values[2]!!.attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.INSERT, GuiCursorMode.CMD_LINE_INSERT), values[3]!!.modes)
    assertEquals(GuiCursorType.VER, values[3]!!.attributes.type)
    assertEquals(25, values[3]!!.attributes.thickness)
    assertEquals("Cursor", values[3]!!.attributes.highlightGroup)
    assertEquals("lCursor", values[3]!!.attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.REPLACE, GuiCursorMode.CMD_LINE_REPLACE), values[4]!!.modes)
    assertEquals(GuiCursorType.HOR, values[4]!!.attributes.type)
    assertEquals(20, values[4]!!.attributes.thickness)
    assertEquals("Cursor", values[4]!!.attributes.highlightGroup)
    assertEquals("lCursor", values[4]!!.attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.SHOW_MATCH), values[5]!!.modes)
    assertEquals(GuiCursorType.BLOCK, values[5]!!.attributes.type)
    assertEquals("Cursor", values[5]!!.attributes.highlightGroup)
    assertEquals("", values[5]!!.attributes.lmapHighlightGroup)
    assertEquals(3, values[5]!!.attributes.blinkModes.size)
    assertEquals("blinkwait175", values[5]!!.attributes.blinkModes[0])
    assertEquals("blinkoff150", values[5]!!.attributes.blinkModes[1])
    assertEquals("blinkon175", values[5]!!.attributes.blinkModes[2])
  }

  fun `test ignores set with missing colon`() {
    // E545: Missing colon: {value}
    assertThrows(ExException::class.java, "E545: Missing colon: whatever") { option.set("whatever") }
    assertEquals(GuiCursorOptionData.defaultValue, option.value)
  }

  fun `test ignores set with invalid mode`() {
    // E546: Illegal mode: {value}
    assertThrows(ExException::class.java, "E546: Illegal mode: foo:block-Cursor") { option.set("foo:block-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, option.value)
  }

  fun `test ignores set with invalid mode 2`() {
    // E546: Illegal mode: {value}
    assertThrows(ExException::class.java, "E546: Illegal mode: n-foo:block-Cursor") { option.set("n-foo:block-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, option.value)
  }

  fun `test ignores set with zero thickness`() {
    // E549: Illegal percentage
    assertThrows(ExException::class.java, "E549: Illegal percentage: n:ver0-Cursor") { option.set("n:ver0-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, option.value)
  }

  fun `test ignores set with invalid vertical cursor details`() {
    // E548: Digit expected: {value}
    assertThrows(ExException::class.java, "E548: Digit expected: n:ver-Cursor") { option.set("n:ver-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, option.value)
  }

  fun `test simple string means block caret and highlight group`() {
    option.set("n:MyHighlightGroup")
    val values = option.values()
    assertEquals(1, values.size)
    assertEquals(enumSetOf(GuiCursorMode.NORMAL), values[0]!!.modes)
    assertEquals(GuiCursorType.BLOCK, values[0]!!.attributes.type)
    assertEquals("MyHighlightGroup", values[0]!!.attributes.highlightGroup)
    assertEquals("", values[0]!!.attributes.lmapHighlightGroup)
  }
}
