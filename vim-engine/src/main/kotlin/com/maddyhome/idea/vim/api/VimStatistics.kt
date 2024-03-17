/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimStatistics {
  public fun logTrackedAction(actionId: String)
  public fun logCopiedAction(actionId: String)
  public fun setIfIfUsed(value: Boolean)
  public fun setIfFunctionCallUsed(value: Boolean)
  public fun setIfFunctionDeclarationUsed(value: Boolean)
  public fun setIfLoopUsed(value: Boolean)
  public fun setIfMapExprUsed(value: Boolean)
  public fun addExtensionEnabledWithPlug(extension: String)
  public fun addSourcedFile(path: String)
}
