/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimStatistics {
  fun logTrackedAction(actionId: String)
  fun logCopiedAction(actionId: String)
  fun setIfIfUsed(value: Boolean)
  fun setIfFunctionCallUsed(value: Boolean)
  fun setIfFunctionDeclarationUsed(value: Boolean)
  fun setIfLoopUsed(value: Boolean)
  fun setIfMapExprUsed(value: Boolean)
  fun addExtensionEnabledWithPlug(extension: String)
  fun addSourcedFile(path: String)
}
