/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.paragraphmotion

import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import javax.swing.KeyStroke

internal class ParagraphMotion : VimExtension {
  override val name: String = "vim-paragraph-motion"

  override suspend fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ParagraphNextMotion)"), mappingOwner, ParagraphMotionHandler(1), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ParagraphPrevMotion)"), mappingOwner, ParagraphMotionHandler(-1), false)

    putKeyMappingIfMissingFromAndToKeys(MappingMode.NXO, injector.parser.parseKeys("}"), mappingOwner, injector.parser.parseKeys("<Plug>(ParagraphNextMotion)"), true)
    putKeyMappingIfMissingFromAndToKeys(MappingMode.NXO, injector.parser.parseKeys("{"), mappingOwner, injector.parser.parseKeys("<Plug>(ParagraphPrevMotion)"), true)
  }

  private class ParagraphMotionHandler(private val count: Int) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      editor.ij.vimForEachCaret { caret ->
        val motion = moveCaretToNextParagraph(editor, caret, count)
        if (motion != null) {
          caret.vim.moveToOffset(motion)
        }
      }
    }

    fun moveCaretToNextParagraph(editor: VimEditor, caret: Caret, count: Int): Int? {
      return injector.searchHelper.findNextParagraph(editor, caret.vim, count, true)
        ?.let { editor.normalizeOffset(it, true) }
    }
  }

  // For VIM-3306
  @Suppress("SameParameterValue")
  private fun putKeyMappingIfMissingFromAndToKeys(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  ) {
    val filteredModes = modes.filterNotTo(HashSet()) { VimPlugin.getKey().getKeyMapping(it).getLayer(fromKeys) != null }
    putKeyMappingIfMissing(filteredModes, fromKeys, pluginOwner, toKeys, recursive)
  }
}
