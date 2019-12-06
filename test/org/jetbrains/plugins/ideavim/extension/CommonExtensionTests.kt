package org.jetbrains.plugins.ideavim.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.mode
import org.jetbrains.plugins.ideavim.VimTestCase

class OpMappingTest : VimTestCase() {
  private var initialized = false

  override fun setUp() {
    super.setUp()
    if (!initialized) {
      initialized = true
      TestExtension().init()
    }
  }

  fun `test simple delete`() {
    doTest(parseKeys("dI"),
      "${c}I found it in a legendary land",
      "${c}nd it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test simple delete backwards`() {
    doTest(parseKeys("dP"),
      "I found ${c}it in a legendary land",
      "I f${c}it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test delete emulate inclusive`() {
    doTest(parseKeys("dU"),
      "${c}I found it in a legendary land",
      "${c}d it in a legendary land",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test linewise delete`() {
    doTest(parseKeys("dO"),
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
}

private class TestExtension : VimNonDisposableExtension() {
  override fun getName(): String = "TestExtension"

  override fun initOnce() {
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionEmulateInclusive"), MoveEmulateInclusive(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionBackwardsCharacter"), MoveBackwards(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionCharacter"), Move(), false)
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtensionLinewise"), MoveLinewise(), false)

    putKeyMapping(MappingMode.O, parseKeys("U"), parseKeys("<Plug>TestExtensionEmulateInclusive"), true)
    putKeyMapping(MappingMode.O, parseKeys("P"), parseKeys("<Plug>TestExtensionBackwardsCharacter"), true)
    putKeyMapping(MappingMode.O, parseKeys("I"), parseKeys("<Plug>TestExtensionCharacter"), true)
    putKeyMapping(MappingMode.O, parseKeys("O"), parseKeys("<Plug>TestExtensionLinewise"), true)
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
}
