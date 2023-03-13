/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.VimStateMachine

public interface VimVisualMotionGroup {
  public val exclusiveSelection: Boolean
  public val selectionAdj: Int

  /**
   * This function toggles visual mode.
   *
   * If visual mode is disabled, enable it
   * If visual mode is enabled, but [subMode] differs, update visual according to new [subMode]
   * If visual mode is enabled with the same [subMode], disable it
   */
  public fun toggleVisual(editor: VimEditor, count: Int, rawCount: Int, subMode: VimStateMachine.SubMode): Boolean
  public fun enterSelectMode(editor: VimEditor, subMode: VimStateMachine.SubMode): Boolean

  /**
   * Enters visual mode based on current editor state.
   * If [subMode] is null, subMode will be detected automatically
   *
   * it:
   * - Updates command state
   * - Updates [vimSelectionStart] property
   * - Updates caret colors
   * - Updates care shape
   *
   * - DOES NOT change selection
   * - DOES NOT move caret
   * - DOES NOT check if carets actually have any selection
   */
  public fun enterVisualMode(editor: VimEditor, subMode: VimStateMachine.SubMode? = null): Boolean
  public fun autodetectVisualSubmode(editor: VimEditor): VimStateMachine.SubMode
}
