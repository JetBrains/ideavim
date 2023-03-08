/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.getKnownOption
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import org.jetbrains.plugins.ideavim.VimTestCase

class GuiCursorOptionTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    configureByText("\n")
  }

  private fun getGuiCursorEntries() =
    options().getStringListValues(OptionConstants.guicursor).map { GuiCursorOptionHelper.convertToken(it) }

  private fun assertHasDefaultValue() {
    val defaultValue = VimPlugin.getOptionGroup().getKnownOption(OptionConstants.guicursor).defaultValue.asString()
    assertEquals(defaultValue, options().getStringValue(OptionConstants.guicursor))
  }

  @Suppress("SpellCheckingInspection")
  fun `test parses default values`() {
    val values = getGuiCursorEntries()
    assertEquals(6, values.size)

    assertEquals(enumSetOf(GuiCursorMode.NORMAL, GuiCursorMode.VISUAL, GuiCursorMode.CMD_LINE), values[0].modes)
    assertEquals(GuiCursorType.BLOCK, values[0].type)
    assertEquals("Cursor", values[0].highlightGroup)
    assertEquals("lCursor", values[0].lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.VISUAL_EXCLUSIVE), values[1].modes)
    assertEquals(GuiCursorType.VER, values[1].type)
    assertEquals(35, values[1].thickness)
    assertEquals("Cursor", values[1].highlightGroup)
    assertEquals("", values[1].lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.OP_PENDING), values[2].modes)
    assertEquals(GuiCursorType.HOR, values[2].type)
    assertEquals(50, values[2].thickness)
    assertEquals("Cursor", values[2].highlightGroup)
    assertEquals("", values[2].lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.INSERT, GuiCursorMode.CMD_LINE_INSERT), values[3].modes)
    assertEquals(GuiCursorType.VER, values[3].type)
    assertEquals(25, values[3].thickness)
    assertEquals("Cursor", values[3].highlightGroup)
    assertEquals("lCursor", values[3].lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.REPLACE, GuiCursorMode.CMD_LINE_REPLACE), values[4].modes)
    assertEquals(GuiCursorType.HOR, values[4].type)
    assertEquals(20, values[4].thickness)
    assertEquals("Cursor", values[4].highlightGroup)
    assertEquals("lCursor", values[4].lmapHighlightGroup)

    assertEquals(enumSetOf(GuiCursorMode.SHOW_MATCH), values[5].modes)
    assertEquals(GuiCursorType.BLOCK, values[5].type)
    assertEquals("Cursor", values[5].highlightGroup)
    assertEquals("", values[5].lmapHighlightGroup)
    assertEquals(3, values[5].blinkModes.size)
    assertEquals("blinkwait175", values[5].blinkModes[0])
    assertEquals("blinkoff150", values[5].blinkModes[1])
    assertEquals("blinkon175", values[5].blinkModes[2])
  }

  fun `test ignores set with missing colon`() {
    enterCommand("set guicursor=whatever")
    assertPluginError(true)
    assertPluginErrorMessageContains("E545: Missing colon: whatever")
    assertHasDefaultValue()
  }

  fun `test ignores set with invalid mode`() {
    enterCommand("set guicursor=foo:block-Cursor")
    assertPluginError(true)
    assertPluginErrorMessageContains("E546: Illegal mode: foo:block-Cursor")
    assertHasDefaultValue()
  }

  fun `test ignores set with invalid mode 2`() {
    enterCommand("set guicursor=n-foo:block-Cursor")
    assertPluginError(true)
    assertPluginErrorMessageContains("E546: Illegal mode: n-foo:block-Cursor")
    assertHasDefaultValue()
  }

  fun `test ignores set with zero thickness`() {
    enterCommand("set guicursor=n:ver0-Cursor")
    assertPluginError(true)
    assertPluginErrorMessageContains("E549: Illegal percentage: n:ver0-Cursor")
    assertHasDefaultValue()
  }

  fun `test ignores set with invalid vertical cursor details`() {
    enterCommand("set guicursor=n:ver-Cursor")
    assertPluginError(true)
    assertPluginErrorMessageContains("E548: Digit expected: n:ver-Cursor")
    assertHasDefaultValue()
  }

  fun `test simple string means default caret and highlight group`() {
    enterCommand("set guicursor=n:MyHighlightGroup")
    val values = getGuiCursorEntries()
    assertEquals(1, values.size)
    assertEquals(enumSetOf(GuiCursorMode.NORMAL), values[0].modes)
    // null from convertToken and we'll give it a default value in getAttributes
    assertEquals(null, values[0].type)
    assertEquals("MyHighlightGroup", values[0].highlightGroup)
    assertEquals("", values[0].lmapHighlightGroup)
  }

  fun `test get effective values`() {
    enterCommand("set guicursor=n:hor20-Cursor,i:hor50,a:ver25,n:ver75")
    val attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(75, attributes.thickness)
    assertEquals("Cursor", attributes.highlightGroup)
  }

  fun `test get effective values 2`() {
    enterCommand("set guicursor=n:hor20-Cursor,i:hor50,a:ver25,n:ver75")
    val attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.INSERT)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(25, attributes.thickness)
  }

  fun `test get effective values on update`() {
    enterCommand("set guicursor=n:hor20-Cursor")
    var attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.HOR, attributes.type)
    assertEquals(20, attributes.thickness)
    assertEquals("Cursor", attributes.highlightGroup)
    enterCommand("set guicursor+=n:ver75-OtherCursor")
    attributes = GuiCursorOptionHelper.getAttributes(GuiCursorMode.NORMAL)
    assertEquals(GuiCursorType.VER, attributes.type)
    assertEquals(75, attributes.thickness)
    assertEquals("OtherCursor", attributes.highlightGroup)
  }
}
