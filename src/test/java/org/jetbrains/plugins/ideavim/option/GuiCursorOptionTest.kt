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
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.option.GuiCursorOptionData
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.vimscript.model.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.vimscript.model.options.helpers.GuiCursorType
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl
import org.jetbrains.plugins.ideavim.VimTestCase

class GuiCursorOptionTest : VimTestCase() {

  private fun getOptionValue(): String {
    return (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "guicursor", null) as VimString).value
  }

  private fun setValue(value: String) {
    return (VimPlugin.getOptionService().setOptionValue(OptionService.Scope.GLOBAL, "guicursor", VimString(value), null))
  }

  @Suppress("SpellCheckingInspection")
  fun `test parses default values`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    val values = getOptionValue().split(",").map { GuiCursorOptionHelper.convertToken(it) }
    assertEquals(6, values.size)

    assertEquals(enumSetOf(GuiCursorMode.NORMAL, GuiCursorMode.VISUAL, GuiCursorMode.CMD_LINE), values[0].modes)
    assertEquals(GuiCursorType.BLOCK, values[0].attributes.type)
    assertEquals("Cursor", values[0].attributes.highlightGroup)
    assertEquals("lCursor", values[0].attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.VISUAL_EXCLUSIVE), values[1].modes)
    assertEquals(GuiCursorType.VER, values[1].attributes.type)
    assertEquals(35, values[1].attributes.thickness)
    assertEquals("Cursor", values[1].attributes.highlightGroup)
    assertEquals("", values[1].attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.OP_PENDING), values[2].modes)
    assertEquals(GuiCursorType.HOR, values[2].attributes.type)
    assertEquals(50, values[2].attributes.thickness)
    assertEquals("Cursor", values[2].attributes.highlightGroup)
    assertEquals("", values[2].attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.INSERT, GuiCursorMode.CMD_LINE_INSERT), values[3].modes)
    assertEquals(GuiCursorType.VER, values[3].attributes.type)
    assertEquals(25, values[3].attributes.thickness)
    assertEquals("Cursor", values[3].attributes.highlightGroup)
    assertEquals("lCursor", values[3].attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.REPLACE, GuiCursorMode.CMD_LINE_REPLACE), values[4].modes)
    assertEquals(GuiCursorType.HOR, values[4].attributes.type)
    assertEquals(20, values[4].attributes.thickness)
    assertEquals("Cursor", values[4].attributes.highlightGroup)
    assertEquals("lCursor", values[4].attributes.lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.SHOW_MATCH), values[5].modes)
    assertEquals(GuiCursorType.BLOCK, values[5].attributes.type)
    assertEquals("Cursor", values[5].attributes.highlightGroup)
    assertEquals("", values[5].attributes.lmapHighlightGroup)
    assertEquals(3, values[5].attributes.blinkModes.size)
    assertEquals("blinkwait175", values[5].attributes.blinkModes[0])
    assertEquals("blinkoff150", values[5].attributes.blinkModes[1])
    assertEquals("blinkon175", values[5].attributes.blinkModes[2])
  }

  fun `test ignores set with missing colon`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    // E545: Missing colon: {value}
    assertExException("E545: Missing colon: whatever") { setValue("whatever") }
    assertEquals(GuiCursorOptionData.defaultValue, getOptionValue())
  }

  fun `test ignores set with invalid mode`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    // E546: Illegal mode: {value}
    assertExException("E546: Illegal mode: foo:block-Cursor") { setValue("foo:block-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, getOptionValue())
  }

  fun `test ignores set with invalid mode 2`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    // E546: Illegal mode: {value}
    assertExException("E546: Illegal mode: n-foo:block-Cursor") { setValue("n-foo:block-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, getOptionValue())
  }

  fun `test ignores set with zero thickness`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    // E549: Illegal percentage
    assertExException("E549: Illegal percentage: n:ver0-Cursor") { setValue("n:ver0-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, getOptionValue())
  }

  fun `test ignores set with invalid vertical cursor details`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    // E548: Digit expected: {value}
    assertExException("E548: Digit expected: n:ver-Cursor") { setValue("n:ver-Cursor") }
    assertEquals(GuiCursorOptionData.defaultValue, getOptionValue())
  }

  fun `test simple string means block caret and highlight group`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    setValue("n:MyHighlightGroup")
    val values = getOptionValue().split(",").map { GuiCursorOptionHelper.convertToken(it) }
    assertEquals(1, values.size)
    assertEquals(enumSetOf(GuiCursorMode.NORMAL), values[0].modes)
    assertEquals(GuiCursorType.BLOCK, values[0].attributes.type)
    assertEquals("MyHighlightGroup", values[0].attributes.highlightGroup)
    assertEquals("", values[0].attributes.lmapHighlightGroup)
  }

  fun `test get effective values`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    setValue("n:hor20-Cursor,i:hor50,a:ver25,n:ver75")
    val attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(75, attributes.thickness)
    assertEquals("Cursor", attributes.highlightGroup)
  }

  fun `test get effective values 2`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    setValue("n:hor20-Cursor,i:hor50,a:ver25,n:ver75")
    val attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.INSERT)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(25, attributes.thickness)
  }

  fun `test get effective values on update`() {
    VimPlugin.getOptionService().resetDefault(OptionService.Scope.GLOBAL, "guicursor", null)
    setValue("n:hor20-Cursor")
    var attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.HOR, attributes.type)
    assertEquals(20, attributes.thickness)
    assertEquals("Cursor", attributes.highlightGroup)
    OptionServiceImpl.appendValue(OptionService.Scope.GLOBAL, "guicursor", "n:ver75-OtherCursor", null, "guicursor")
    attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(75, attributes.thickness)
    assertEquals("OtherCursor", attributes.highlightGroup)
  }
}
