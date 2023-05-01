/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimEditorGroup {
  public fun notifyIdeaJoin(editor: VimEditor)
  public fun localEditors(): Collection<VimEditor>
  public fun localEditors(buffer: VimDocument): Collection<VimEditor>
}
