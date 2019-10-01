package org.jetbrains.plugins.ideavim.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
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
}

class TestExtension : VimNonDisposableExtension() {
  override fun getName(): String = "TestExtension"

  override fun initOnce() {
    putExtensionHandlerMapping(MappingMode.O, parseKeys("<Plug>TestExtension"), Move(), false)

    putKeyMapping(MappingMode.O, parseKeys("I"), parseKeys("<Plug>TestExtension"), true)
  }

  private class Move : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.caretModel.allCarets.forEach { it.moveToOffset(it.offset + 5) }
    }
  }
}
