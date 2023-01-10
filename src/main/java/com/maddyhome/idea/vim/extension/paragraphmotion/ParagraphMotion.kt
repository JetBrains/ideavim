/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.paragraphmotion

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

class ParagraphMotion : VimExtension {
  override fun getName(): String = "vim-paragraph-motion"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ParagraphNextMotion)"), owner, ParagraphMotionHandler(1), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ParagraphPrevMotion)"), owner, ParagraphMotionHandler(-1), false)

    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("}"), owner, injector.parser.parseKeys("<Plug>(ParagraphNextMotion)"), true)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("{"), owner, injector.parser.parseKeys("<Plug>(ParagraphPrevMotion)"), true)
  }

  private class ParagraphMotionHandler(private val count: Int) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext) {
      editor.ij.vimForEachCaret { caret ->
        val motion = moveCaretToNextParagraph(editor.ij, caret, count)
        if (motion >= 0) {
          caret.vim.moveToOffset(motion)
        }
      }
    }

    fun moveCaretToNextParagraph(editor: Editor, caret: Caret, count: Int): Int {
      var res = SearchHelper.findNextParagraph(editor, caret, count, true)
      res = if (res >= 0) {
        editor.vim.normalizeOffset(res, true)
      } else {
        -1
      }
      return res
    }
  }
}
