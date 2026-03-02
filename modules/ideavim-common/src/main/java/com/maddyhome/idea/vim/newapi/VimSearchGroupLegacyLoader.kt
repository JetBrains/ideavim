/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import org.jdom.Element

/**
 * Bridge interface for legacy state loading/saving of search group data.
 * Implemented by [IjVimSearchGroup] in the frontend module, used by [com.maddyhome.idea.vim.VimPlugin]
 * in common for backwards-compatible state migration.
 */
interface VimSearchGroupLegacyLoader {
  fun readData(element: Element)
  fun saveData(element: Element)
}
