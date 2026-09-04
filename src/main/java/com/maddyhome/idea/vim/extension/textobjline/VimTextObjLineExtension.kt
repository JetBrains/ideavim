/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjline

import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.scopes.TextObjectRange
import com.maddyhome.idea.vim.extension.VimExtension

/**
 * Port of vim-textobj-line:
 * https://github.com/kana/vim-textobj-line
 *
 * vim-textobj-line provides two text objects for the current line:
 * - `al` targets all characters in the current line, without the end of line character. Same as `0v$h`.
 * - `il` is similar to `al`, but without the leading and trailing whitespace. Same as `^vg_`.
 *
 * Both text objects select nothing on an empty line, and `il` also selects nothing on a line that
 * consists of whitespace only, because there is no text to select in the current line.
 *
 * See also the reference manual for more details:
 * https://github.com/kana/vim-textobj-line/blob/master/doc/textobj-line.txt
 */
internal class VimTextObjLineExtension : VimExtension {

  override fun getName(): String = "textobj-line"

  override fun init(initApi: VimInitApi) {
    val skipDefaults = initApi.getVariable<Boolean>("g:textobj_line_no_default_key_mappings") ?: false

    initApi.textObjects {
      register("al", registerDefaultMapping = !skipDefaults, preserveSelectionAnchor = false) { _ ->
        val line = editor { read { withPrimaryCaret { line } } }
        if (line.start == line.end) null else TextObjectRange.CharacterWise(line.start, line.end)
      }

      register("il", registerDefaultMapping = !skipDefaults, preserveSelectionAnchor = false) { _ ->
        val line = editor { read { withPrimaryCaret { line } } }
        // Line.text includes the end of line character, which is never part of the text object
        val content = line.text.substring(0, line.end - line.start)

        val firstNonBlank = content.indexOfFirst { !it.isWhitespace() }
        if (firstNonBlank == -1) return@register null
        val lastNonBlank = content.indexOfLast { !it.isWhitespace() }

        TextObjectRange.CharacterWise(line.start + firstNonBlank, line.start + lastNonBlank + 1)
      }
    }
  }
}
