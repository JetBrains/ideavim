/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.fold

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

/**
 * Manages fold state and fold level for editors.
 *
 * This object provides utilities to get and set the fold level (foldlevel) for an editor window,
 * as well as convenience methods to open or close all folds. The fold level determines which
 * folds are visible: folds with depth less than the fold level are expanded, while deeper folds
 * are collapsed.
 */
object FoldState {

  /**
   * Gets the current fold level for the editor window.
   *
   * Returns the fold level stored for this editor window, or initializes it to the maximum
   * fold depth if not yet set. The fold level determines which folds are expanded: folds
   * with depth < foldlevel are expanded, others are collapsed.
   *
   * @param editor the editor to get the fold level for
   * @return the current fold level
   */
  fun getFoldLevel(editor: VimEditor): Int {
    return injector.optionGroup.getOptionValue(Options.foldlevel, OptionAccessScope.EFFECTIVE(editor)).value
  }

  /**
   * Sets the fold level for the editor window.
   *
   * Updates the fold level and applies it to all folds in the editor. Folds with depth
   * less than the specified level will be expanded, while deeper folds will be collapsed.
   * The level is coerced to be between 0 and maxDepth + 1.
   *
   * @param editor the editor to set the fold level for
   * @param level the fold level to set (0 = all folds closed, maxDepth + 1 = all folds open)
   */
  fun setFoldLevel(editor: VimEditor, level: Int) {
    injector.optionGroup.setOptionValue(Options.foldlevel, OptionAccessScope.EFFECTIVE(editor), VimInt(level))
  }

  /**
   * Closes all folds in the editor.
   *
   * Sets the fold level to 0, which collapses all folds regardless of their depth.
   *
   * @param editor the editor to close all folds in
   */
  fun closeAllFolds(editor: VimEditor) {
    setFoldLevel(editor, 0)
  }

  /**
   * Opens all folds in the editor.
   *
   * Sets the fold level to maxDepth + 1, which expands all folds regardless of their depth.
   *
   * @param editor the editor to open all folds in
   */
  fun openAllFolds(editor: VimEditor) {
    val maxDepth = editor.getMaxFoldDepth()
    setFoldLevel(editor, maxDepth + 1)
  }
}
