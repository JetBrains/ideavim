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
      MappingMode.X, plugVisualStarSearchKeys, owner, VisualStarSearchMappingHandler(Direction.FORWARDS), false
    )
    putExtensionHandlerMapping(
      MappingMode.X, plugVisualHashSearchKeys, owner, VisualStarSearchMappingHandler(Direction.BACKWARDS), false
    )

    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("*"), owner, plugVisualStarSearchKeys, true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("#"), owner, plugVisualHashSearchKeys, true)
  }

  private class VisualStarSearchMappingHandler(val direction: Direction) : ExtensionHandler {

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val selection = editor.collectSelections()?.first() ?: return
      val pattern = editor.getText(selection.value.toVimTextRange())
      val offsetAndMotion = injector.searchGroup.processSearchCommand(
        editor, makePattern(pattern), selection.value.vimStart, operatorArguments.count1, direction
      )
      editor.exitVisualMode()
      if (offsetAndMotion == null) return
      editor.primaryCaret().moveToOffset(offsetAndMotion.first)
    }

    /**
     * Builds a "very nomagic" pattern, so that the selected text is searched for literally
     *
     * The pattern is passed to a search command, which treats an unescaped delimiter as the start of a search offset,
     * so the delimiter has to be escaped. A backslash is not enough, because it stays in the pattern, and `\?` means
     * "zero or one" (Vim removes the backslash while parsing the command, IdeaVim does not). The delimiter is
     * therefore replaced with its decimal character code. That code is greedy, so any digits following the delimiter
     * are replaced the same way, to keep them out of the number.
     */
    private fun makePattern(text: String): String {
      val delimiter = if (direction == Direction.FORWARDS) '/' else '?'
      val pattern = StringBuilder("\\V")
      var escapeDigits = false
      for (char in text) {
        when {
          char == delimiter -> {
            pattern.append("\\%d").append(delimiter.code)
            escapeDigits = true
          }

          escapeDigits && char in '0'..'9' -> pattern.append("\\%d").append(char.code)

          // In "very nomagic" mode, only backslash has special meaning
          char == '\\' -> {
            pattern.append("\\\\")
            escapeDigits = false
          }

          else -> {
            pattern.append(char)
            escapeDigits = false
          }
        }
      }
      return pattern.toString()
    }
  }
}
