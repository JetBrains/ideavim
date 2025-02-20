/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CaretVisualAttributesHelperTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default normal mode caret is block`() {
    configureByText("Lorem ipsum dolor sit amet,")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default insert mode caret is vertical bar`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test insert mode caret is reset after Escape`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("i", "<Esc>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default replace mode caret is underscore`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("R")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default op pending caret is thick underscore`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("d")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.5F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret is reset after op pending`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("d$")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default visual mode caret is block`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("ve")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test default exclusive visual mode caret is bar`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set selection=exclusive")
    typeText("ve")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.35F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test exclusive visual falls back to visual if not specified`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set guicursor=v:hor10-Cursor/lCursor")
    enterCommand("set selection=exclusive")
    typeText("ve")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.1F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test visual block hides secondary carets`() {
    configureByLines(5, "Lorem ipsum dolor sit amet,")
    typeText("w", "<C-V>2j5l")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
    fixture.editor.caretModel.allCarets.forEach {
      if (it != fixture.editor.caretModel.primaryCaret) {
        assertCaretVisualAttributes(it, CaretVisualAttributes.Shape.BAR, 0F)
      }
    }
  }

  @VimBehaviorDiffers(
    description = "IdeaVim treats Select mode as exclusive, rather than acting the same as Visual",
    shouldBeFixed = true
  )
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test select mode uses visual-exclusive mode caret`() {
    // TODO: Select mode should use the same caret as Visual, based on the 'selection' option
    // IdeaVim has implemented Select mode to always be exclusive, rather than based on the 'selection' option.
    // Therefore, we must always use the `ve` Visual-exclusive caret
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("v7l", "<C-G>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.35F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test replace character uses replace mode caret`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("r")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after replacing character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("r", "z")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after escaping replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("r", "<Esc>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after cancelling replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("r", "<Left>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test visual replace character uses replace mode caret`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("ve", "r")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.2F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after completing visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("ve", "r", "z")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after escaping visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("ve", "r", "<Esc>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset after cancelling visual replace character`() {
    configureByText("I ${c}found it in a legendary land")
    typeText("ve", "r", "<Left>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test nested visual mode in ide gets visual caret`() {
    configureByText("I ${c}found it in a legendary land")
    enterCommand("set keymodel=startsel,stopsel")
    typeText("i", "<S-Right><S-Right><S-Right>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset to insert after leaving nested visual mode`() {
    configureByText("I ${c}found it in a legendary land")
    enterCommand("set keymodel=startsel,stopsel")
    typeText("i", "<S-Right><S-Right><S-Right>", "<Right>")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret reset to insert after cancelling nested visual mode`() {
    configureByText("I ${c}found it in a legendary land")
    enterCommand("set keymodel=startsel,stopsel")
    typeText("i", "<S-Right><S-Right><S-Right>", "<Esc>")
    assertMode(Mode.INSERT)
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test changing guicursor option updates caret immediately`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set guicursor=n:hor22")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.22F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test changing guicursor option invalidates caches correctly`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
    typeText("<Esc>")
    enterCommand("set guicursor=i:hor22")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.22F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test caret uses last matching guicursor option`() {
    configureByText("Lorem ipsum dolor sit amet,")
    // This will give us three matching options for INSERT
    enterCommand("set guicursor+=i:ver25")
    enterCommand("set guicursor+=i:hor75")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.75F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test block used when caret shape is unspecified`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set guicursor=c:ver25")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0.0F)
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0.0F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test 'all' guicursor option`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set guicursor+=a:ver25")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test 'all' guicursor option without cursor shape does not affect existing shapes`() {
    configureByText("Lorem ipsum dolor sit amet,")
    enterCommand("set guicursor+=a:blinkwait200-blinkoff125-blinkon150-Cursor/lCursor")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test 'all' guicursor option can be overridden`() {
    configureByText("Lorem ipsum dolor sit amet,")
    // A specific entry added after "all" takes precedence
    enterCommand("set guicursor+=a:ver25")
    enterCommand("set guicursor+=i:hor75")
    typeText("i")
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.UNDERSCORE, 0.75F)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test block caret setting overrides guicursor`() {
    val originalValue = EditorSettingsExternalizable.getInstance().isBlockCursor
    EditorSettingsExternalizable.getInstance().isBlockCursor = true
    try {
      configureByText("Lorem ipsum dolor sit amet,")
      typeText("i")
      assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 1.0F)
    } finally {
      EditorSettingsExternalizable.getInstance().isBlockCursor = originalValue
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test reset caret shape when disable plugin`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("i")
    VimPlugin.setEnabled(false)
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.DEFAULT, 1.0f)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test reset caret shape when disable plugin 2`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("v2e")
    VimPlugin.setEnabled(false)
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.DEFAULT, 1.0f)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test reset caret shape when disable plugin 3`() {
    configureByText("Lorem ipsum dolor sit amet,")
    typeText("r")
    VimPlugin.setEnabled(false)
    assertCaretVisualAttributes(CaretVisualAttributes.Shape.DEFAULT, 1.0f)
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test adding new caret via IJ`() {
    ApplicationManager.getApplication().invokeAndWait {
      configureByText("${c}Lorem ipsum dolor sit amet,")
      fixture.editor.caretModel.addCaret(VisualPosition(0, 5))
      assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0f)
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test adding new caret below`() {
    configureByText(
      """
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      """.trimMargin(),
    )
    ApplicationManager.getApplication().invokeAndWait {
      injector.actionExecutor.executeAction(
        fixture.editor.vim,
        name = "EditorCloneCaretBelow",
        context = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim),
      )
      kotlin.test.assertEquals(2, fixture.editor.caretModel.caretCount)
      assertCaretVisualAttributes(CaretVisualAttributes.Shape.BLOCK, 0f)
    }
  }

  private fun assertCaretVisualAttributes(expectedShape: CaretVisualAttributes.Shape, expectedThickness: Float) {
    assertCaretVisualAttributes(fixture.editor.caretModel.primaryCaret, expectedShape, expectedThickness)
  }

  private fun assertCaretVisualAttributes(
    caret: Caret,
    expectedShape: CaretVisualAttributes.Shape,
    expectedThickness: Float,
  ) {
    kotlin.test.assertEquals(expectedShape, caret.visualAttributes.shape)
    kotlin.test.assertEquals(expectedThickness, caret.visualAttributes.thickness)
  }
}
