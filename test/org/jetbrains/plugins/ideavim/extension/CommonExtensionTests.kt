/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class OpMappingTest : VimTestCase() {
  private var initialized = false

  private val extension = TestExtension()

  override fun setUp() {
    super.setUp()
    if (!initialized) {
      initialized = true
      @Suppress("DEPRECATION") // [VERSION UPDATE] 202+
      VimExtension.EP_NAME.getPoint(null).registerExtension(extension)
      enableExtensions("TestExtension")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test simple delete`() {
    doTest("dI",
      "${c}I found it in a legendary land",
      "${c}nd it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test simple delete backwards`() {
    doTest("dP",
      "I found ${c}it in a legendary land",
      "I f${c}it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test delete emulate inclusive`() {
    doTest("dU",
      "${c}I found it in a legendary land",
      "${c}d it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test linewise delete`() {
    doTest("dO",
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                ${c}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test disable extension via set`() {
    configureByText("${c}I found it in a legendary land")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    enterCommand("set noTestExtension")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    enterCommand("set TestExtension")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I ${c}found it in a legendary land")
  }

  fun `test disable extension as extension point`() {
    configureByText("${c}I found it in a legendary land")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    @Suppress("DEPRECATION") // [VERSION UPDATE] 202+
    VimExtension.EP_NAME.getPoint(null).unregisterExtension(TestExtension::class.java)
    assertEmpty(VimPlugin.getKey().getKeyMappingByOwner(extension.owner))
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    @Suppress("DEPRECATION") // [VERSION UPDATE] 202+
    VimExtension.EP_NAME.getPoint(null).registerExtension(extension)
    assertEmpty(VimPlugin.getKey().getKeyMappingByOwner(extension.owner))
    enableExtensions("TestExtension")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I ${c}found it in a legendary land")
  }

  fun `test disable disposed extension`() {
    configureByText("${c}I found it in a legendary land")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    enterCommand("set noTestExtension")
    @Suppress("DEPRECATION") // [VERSION UPDATE] 202+
    VimExtension.EP_NAME.getPoint(null).unregisterExtension(TestExtension::class.java)
    typeText(parseKeys("Q"))
    myFixture.checkResult("I${c} found it in a legendary land")

    @Suppress("DEPRECATION") // [VERSION UPDATE] 202+
    VimExtension.EP_NAME.getPoint(null).registerExtension(extension)
    enableExtensions("TestExtension")
    typeText(parseKeys("Q"))
    myFixture.checkResult("I ${c}found it in a legendary land")
  }
}

private class TestExtension : VimExtension {
  override fun getName(): String = "TestExtension"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionEmulateInclusive"), owner, MoveEmulateInclusive(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionBackwardsCharacter"), owner, MoveBackwards(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionCharacter"), owner, Move(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionLinewise"), owner, MoveLinewise(), false)
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>TestMotion"), owner, MoveLinewiseInNormal(), false)

    putKeyMapping(MappingMode.O, parseKeys("U"), owner, parseKeys("<Plug>TestExtensionEmulateInclusive"), true)
    putKeyMapping(MappingMode.O, parseKeys("P"), owner, parseKeys("<Plug>TestExtensionBackwardsCharacter"), true)
    putKeyMapping(MappingMode.O, parseKeys("I"), owner, parseKeys("<Plug>TestExtensionCharacter"), true)
    putKeyMapping(MappingMode.O, parseKeys("O"), owner, parseKeys("<Plug>TestExtensionLinewise"), true)
    putKeyMapping(MappingMode.N, parseKeys("Q"), owner, parseKeys("<Plug>TestMotion"), true)
  }

  private class MoveEmulateInclusive : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      VimPlugin.getVisualMotion().enterVisualMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
      val caret = editor.caretModel.currentCaret
      val newOffset = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, 5, editor.mode.isEndAllowed)
      MotionGroup.moveCaret(editor, caret, newOffset)
    }
  }

  private class MoveBackwards : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.caretModel.allCarets.forEach { it.moveToOffset(it.offset - 5) }
    }
  }

  private class Move : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.caretModel.allCarets.forEach { it.moveToOffset(it.offset + 5) }
    }
  }

  private class MoveLinewise : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      VimPlugin.getVisualMotion().enterVisualMode(editor, CommandState.SubMode.VISUAL_LINE)
      val caret = editor.caretModel.currentCaret
      val newOffset = VimPlugin.getMotion().moveCaretVertical(editor, caret, 1)
      MotionGroup.moveCaret(editor, caret, newOffset)
    }
  }

  private class MoveLinewiseInNormal : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val caret = editor.caretModel.currentCaret
      val newOffset = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, 1, true)
      MotionGroup.moveCaret(editor, caret, newOffset)
    }
  }
}
