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

package com.maddyhome.idea.vim.extension.paragraphmotion

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

class ParagraphMotion : VimExtension {
  override fun getName(): String = "vim-paragraph-motion"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(ParagraphNextMotion)"), owner, ParagraphMotionHandler(1), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(ParagraphPrevMotion)"), owner, ParagraphMotionHandler(-1), false)

    putKeyMappingIfMissing(MappingMode.NXO, parseKeys("}"), owner, parseKeys("<Plug>(ParagraphNextMotion)"), true)
    putKeyMappingIfMissing(MappingMode.NXO, parseKeys("{"), owner, parseKeys("<Plug>(ParagraphPrevMotion)"), true)
  }

  private class ParagraphMotionHandler(private val count: Int) : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.caretModel.runForEachCaret { caret ->
        val motion = moveCaretToNextParagraph(editor, caret, count)
        if (motion >= 0) {
          MotionGroup.moveCaret(editor, caret, motion)
        }
      }
    }

    fun moveCaretToNextParagraph(editor: Editor, caret: Caret, count: Int): Int {
      var res = SearchHelper.findNextParagraph(editor, caret, count, true)
      res = if (res >= 0) {
        EditorHelper.normalizeOffset(editor, res, true)
      } else {
        -1
      }
      return res
    }
  }
}
