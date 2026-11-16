/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.tag

object TagStackPlace {
  /**
   * The place IdeaVim runs the IDE's Go to Declaration from when `:tag` redoes a tag jump, see
   * [com.maddyhome.idea.vim.api.VimActionExecutor.executeAction]
   *
   * IDE navigation to a definition pushes onto the tag stack, but a redo only walks up a stack that already holds the
   * jump. The listener that pushes skips invocations from this place.
   */
  const val TAG_STACK_PLACE: String = "TAG_STACK_PLACE"
}
