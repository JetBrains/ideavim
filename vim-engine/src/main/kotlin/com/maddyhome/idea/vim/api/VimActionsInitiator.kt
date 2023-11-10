/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import org.jetbrains.annotations.ApiStatus

@Deprecated(message = "Replace it with LazyVimCommand")
@ApiStatus.ScheduledForRemoval(inVersion = "2.9.0")
public interface VimActionsInitiator {
  public fun getInstance(): EditorActionHandlerBase
}
