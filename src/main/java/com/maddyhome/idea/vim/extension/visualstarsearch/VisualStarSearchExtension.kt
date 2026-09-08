/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.visualstarsearch

import com.intellij.vim.api.VimInitApi
import com.jetbrains.rd.util.first
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.helper.exitVisualMode

internal class VisualStarSearchExtension : VimExtension {

  override fun getName() = "visual-star-search"

  override fun init(initApi: VimInitApi) {
    val plugVisualStarSearchKeys = injector.parser.parseKeys("<Plug>VisualStarSearch")
    val plugVisualHashSearchKeys = injector.parser.parseKeys("<Plug>VisualHashSearch")

    putExtensionHandlerMapping(
      MappingMode.X,
      plugVisualStarSearchKeys,
      owner,
      VisualStarSearchMappingHandler(Direction.FORWARDS),
      false
    )
    putExtensionHandlerMapping(
      MappingMode.X,
      plugVisualHashSearchKeys,
      owner,
      VisualStarSearchMappingHandler(Direction.BACKWARDS),
      false
    )

    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("*"), owner, plugVisualStarSearchKeys, true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("#"), owner, plugVisualHashSearchKeys, true)
  }

  private class VisualStarSearchMappingHandler(val direction: Direction) : ExtensionHandler {

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val selection = editor.collectSelections()?.first() ?: return
      val pattern = editor.getText(selection.value.toVimTextRange())
      val position = injector.searchGroup.searchWord(
        makePattern(pattern),
        direction,
        editor,
        selection.value.toVimTextRange(),
        operatorArguments.count1
      )
      editor.exitVisualMode()
      editor.primaryCaret().moveToOffset(position)
    }

    private fun makePattern(text: String): String {
      val escapedText = text.replace("\\", "\\\\")
      return "\\V\\C$escapedText"
    }
  }
}
