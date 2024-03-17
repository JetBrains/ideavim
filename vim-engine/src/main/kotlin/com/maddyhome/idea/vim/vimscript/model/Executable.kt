/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo
import org.jetbrains.annotations.ApiStatus.Internal

public interface Executable : VimLContext {

  public var vimContext: VimLContext

  /**
   * Text range of the executable in original script
   */
  public var rangeInScript: TextRange

  override fun getPreviousParentContext(): VimLContext {
    return vimContext
  }

  public fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult

  /**
   * Current implementation of parser deletes some substrings in the original string in order to reduce parsing time or skip lines with errors.
   * This method restores original text range in script according to all the deleted substrings.
   */
  @Internal
  public fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    val startOffset = deletionInfo.restoreOriginalOffset(rangeInScript.startOffset)
    val endOffset = deletionInfo.restoreOriginalOffset(rangeInScript.endOffset - 1) + 1
    rangeInScript = TextRange(startOffset, endOffset)
  }
}
