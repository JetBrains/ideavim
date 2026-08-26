/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.tag

/**
 * The outcome of walking back down the tag stack with `:tag`, see "h :tag"
 *
 * Walking down means redoing a tag jump, and where that jump landed was never recorded. Sometimes we can tell anyway -
 * if another tag jump was made from there, its entry holds the position. Otherwise the tag has to be looked up again.
 */
sealed interface TagStackMove {
  /**
   * A later tag jump was made from where this one landed, so [landing] holds the position to move to
   */
  data class ToKnownPosition(val landing: TagStackEntry) : TagStackMove

  /**
   * Nothing was ever recorded past this jump, so [redone] tells us which tag to look up again
   *
   * This is a snapshot of the entry as it was before its position was rewritten, so it still points at the place the
   * tag was originally jumped from - which is where the IDE has to look to resolve it.
   */
  data class ToTag(val redone: TagStackEntry) : TagStackMove

  /**
   * There is nothing left to redo, `E556`
   */
  data object AtTop : TagStackMove
}
