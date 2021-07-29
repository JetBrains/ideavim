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

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class CaretVisualAttributesHelperTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test default normal mode caret is block`() {
    configureByText("I found it in a legendary land")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test default insert mode caret is vertical bar`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("i"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test insert mode caret is reset after Escape`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("i", "<Esc>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test default replace mode caret is underscore`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("R"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test default op pending caret is thick underscore`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("d"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.5F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret is reset after op pending`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("d$"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test default visual mode caret is block`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("ve"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test visual block hides secondary carets`() {
    configureByLines(5, "I found it in a legendary land")
    typeText(parseKeys("w", "<C-V>2j5l"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
    myFixture.editor.caretModel.allCarets.forEach {
      if (it != myFixture.editor.caretModel.primaryCaret) {
        assertCaretVisualAttributes(it, CaretVisualAttributes.Shape.BAR, 0F)
      }
    }
  }

  @VimBehaviorDiffers(description = "Vim does not change the caret for select mode", shouldBeFixed = false)
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test select mode uses insert mode caret`() {
    // Vim doesn't have a different caret for SELECT, and doesn't have an option in guicursor to change SELECT mode
    configureByText("I found it in a legendary land")
    typeText(parseKeys("v7l", "<C-G>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test replace character uses replace mode caret`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("r"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after replacing character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("r", "z"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after escaping replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("r", "<Esc>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after cancelling replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("r", "<Left>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test visual replace character uses replace mode caret`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("ve", "r"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after completing visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("ve", "r", "z"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after escaping visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("ve", "r", "<Esc>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret reset after cancelling visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText(parseKeys("ve", "r", "<Left>"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test changing guicursor option updates caret immediately`() {
    configureByText("I found it in a legendary land")
    enterCommand("set guicursor=n:hor22")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.22F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test changing guicursor option invalidates caches correctly`() {
    configureByText("I found it in a legendary land")
    typeText(parseKeys("i"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
    typeText(parseKeys("<Esc>"))
    enterCommand("set guicursor=i:hor22")
    typeText(parseKeys("i"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.22F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test caret uses last matching guicursor option`() {
    configureByText("I found it in a legendary land")
    // This will give us three matching options for INSERT
    enterCommand("set guicursor+=i:ver25")
    enterCommand("set guicursor+=i:hor75")
    typeText(parseKeys("i"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.75F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test 'all' guicursor option`() {
    configureByText("I found it in a legendary land")
    enterCommand("set guicursor+=a:ver25")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test 'all' guicursor option can be overridden`() {
    configureByText("I found it in a legendary land")
    // A specific entry added after "all" takes precedence
    enterCommand("set guicursor+=a:ver25")
    enterCommand("set guicursor+=i:hor75")
    typeText(parseKeys("i"))
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.75F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test block caret setting overrides guicursor`() {
    val originalValue = EditorSettingsExternalizable.getInstance().isBlockCursor
    EditorSettingsExternalizable.getInstance().isBlockCursor = true
    try {
      configureByText("I found it in a legendary land")
      typeText(parseKeys("i"))
      assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 1.0F)
    }
    finally {
      EditorSettingsExternalizable.getInstance().isBlockCursor = originalValue
    }
  }

  private fun assertCaretVisualAttributes(expectedShape: CaretVisualAttributes.Shape, expectedThickness: Float) {
    assertCaretVisualAttributes(myFixture.editor.caretModel.primaryCaret, expectedShape, expectedThickness)
  }

  private fun assertCaretVisualAttributes(caret: Caret, expectedShape: CaretVisualAttributes.Shape, expectedThickness: Float) {
    val visualAttributes = caret.visualAttributes
    assertEquals(expectedShape, visualAttributes.shape)
    assertEquals(expectedThickness, visualAttributes.thickness)
  }
}
