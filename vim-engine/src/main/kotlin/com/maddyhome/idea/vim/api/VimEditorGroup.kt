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

  // TODO find a better place for methods below. Maybe make CaretVisualAttributesHelper abstract?
  public fun updateCaretsVisualAttributes(editor: VimEditor)
  public fun updateCaretsVisualPosition(editor: VimEditor)
}
