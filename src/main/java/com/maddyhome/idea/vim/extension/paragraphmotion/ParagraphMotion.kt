/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.paragraphmotion

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.scopes.nmapPluginAction
import com.intellij.vim.api.scopes.omapPluginAction
import com.intellij.vim.api.scopes.xmapPluginAction
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api

internal class ParagraphMotion : VimExtension {
  override fun getName(): String = "vim-paragraph-motion"

  override fun init() {
    val api = api()

    api.mappings {
      nmapPluginAction("}", "<Plug>(ParagraphNextMotion)", keepDefaultMapping = true) {
        moveParagraph(1)
      }
      nmapPluginAction("{", "<Plug>(ParagraphPrevMotion)", keepDefaultMapping = true) {
        moveParagraph(-1)
      }
      xmapPluginAction("}", "<Plug>(ParagraphNextMotion)", keepDefaultMapping = true) {
        moveParagraph(1)
      }
      xmapPluginAction("{", "<Plug>(ParagraphPrevMotion)", keepDefaultMapping = true) {
        moveParagraph(-1)
      }
      omapPluginAction("}", "<Plug>(ParagraphNextMotion)", keepDefaultMapping = true) {
        moveParagraph(1)
      }
      omapPluginAction("{", "<Plug>(ParagraphPrevMotion)", keepDefaultMapping = true) {
        moveParagraph(-1)
      }
    }
  }
}

internal fun VimApi.moveParagraph(direction: Int) {
  val count = getVariable<Int>("v:count1") ?: 1
  val actualCount = count * direction

  editor {
    change {
      forEachCaret {
        val newOffset = getNextParagraphBoundOffset(actualCount, includeWhitespaceLines = true)
        if (newOffset != null) {
          updateCaret(offset = newOffset)
        }
      }
    }
  }
}
