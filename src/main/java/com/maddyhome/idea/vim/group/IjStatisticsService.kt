/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimStatistics
import com.maddyhome.idea.vim.statistic.ActionTracker
import com.maddyhome.idea.vim.statistic.VimscriptState

class IjStatisticsService : VimStatistics {

  override fun logTrackedAction(actionId: String) {
    ActionTracker.logTrackedAction(actionId)
  }

  override fun logCopiedAction(actionId: String) {
    ActionTracker.logCopiedAction(actionId)
  }

  override fun setIfLoopUsed(value: Boolean) {
    VimscriptState.isLoopUsed = value
  }

  override fun setIfMapExprUsed(value: Boolean) {
    VimscriptState.isMapExprUsed = value
  }

  override fun setIfFunctionCallUsed(value: Boolean) {
    VimscriptState.isFunctionCallUsed = value
  }

  override fun setIfFunctionDeclarationUsed(value: Boolean) {
    VimscriptState.isFunctionDeclarationUsed = value
  }

  override fun setIfIfUsed(value: Boolean) {
    VimscriptState.isIfUsed = value
  }

  override fun addExtensionEnabledWithPlug(extension: String) {
    VimscriptState.extensionsEnabledWithPlug.add(extension)
  }

  override fun addSourcedFile(path: String) {
    VimscriptState.sourcedFiles.add(path)
  }
}
