/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjentire

import com.intellij.vim.api.getVariable
import com.intellij.vim.api.scopes.TextObjectRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api

/**
 * Port of vim-textobj-entire:
 * https://github.com/kana/vim-textobj-entire
 *
 * vim-textobj-entire provides two text objects:
 * - `ae` targets the entire content of the current buffer.
 * - `ie` is similar to `ae`, but does not include leading and trailing empty lines.
 *
 * See also the reference manual for more details:
 * https://github.com/kana/vim-textobj-entire/blob/master/doc/textobj-entire.txt
 */
internal class VimTextObjEntireExtension : VimExtension {

  override fun getName(): String = "textobj-entire"

  override fun init() {
    val api = api()
    val skipDefaults = api.getVariable<Boolean>("g:textobj_entire_no_default_mappings") ?: false

    api.textObjects {
      register("ae", registerDefaultMapping = !skipDefaults) { _ ->
        TextObjectRange.CharacterWise(0, editor { read { textLength.toInt() } })
      }

      register("ie", registerDefaultMapping = !skipDefaults) { _ ->
        val content = editor { read { text.toString() } }
        var start = 0
        var end = content.length

        // Find first non-whitespace character
        for (i in content.indices) {
          if (!content[i].isWhitespace()) {
            start = i
            break
          }
        }

        // Find last non-whitespace character
        for (i in content.indices.reversed()) {
          if (!content[i].isWhitespace()) {
            end = i + 1
            break
          }
        }

        TextObjectRange.CharacterWise(start, end)
      }
    }
  }
}
