/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.common.TextRange

/**
 * No-op highlight [VimSearchGroupBase] for the backend in split mode.
 * The backend has no open editors — search highlighting is meaningless.
 * Extends [VimSearchGroupBase] to inherit real search/substitute text-processing logic.
 */
class BackendSearchGroup : VimSearchGroupBase() {
  override fun highlightSearchLines(editor: VimEditor, startLine: Int, endLine: Int) {}
  override fun updateSearchHighlights(force: Boolean) {}
  override fun resetIncsearchHighlights() {}
  override fun addSubstitutionConfirmationHighlight(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
  ): SearchHighlight = object : SearchHighlight() {
    override fun remove() {}
  }

  override fun setShouldShowSearchHighlights() {}
  override fun clearSearchHighlight() {}
  override fun isSomeTextHighlighted(): Boolean = false
  override fun getCurrentIncsearchResultRange(editor: VimEditor): TextRange? = null
}
