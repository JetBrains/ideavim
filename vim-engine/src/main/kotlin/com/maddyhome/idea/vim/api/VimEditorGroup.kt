/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimEditorGroup {
  fun notifyIdeaJoin(editor: VimEditor)
  fun localEditors(): Collection<VimEditor>
}
