/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.diff

import com.intellij.diff.impl.CacheDiffRequestChainProcessor
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * The files a diff tab shows one at a time, and switching it to one of them
 *
 * A diff tab can show the changes of many files - all the files of a "Show Diff", or a preview that follows the
 * selection in the Commit tool window. Each kind of tab keeps its files its own way.
 */
interface DiffTabFiles {
  fun canShow(path: String): Boolean

  /** Starts showing the diff of [path]. The tab shows it once the diff is loaded. Returns false if it cannot show it */
  fun show(path: String): Boolean

  companion object {
    fun of(processor: DiffRequestProcessor): DiffTabFiles? {
      if (processor is CacheDiffRequestChainProcessor) return ChainFiles(processor)
      return DiffTabFilesProvider.EP_NAME.extensionList.firstNotNullOfOrNull { it.filesOf(processor) }
    }
  }
}

/**
 * Knows the files of the diff tabs that live outside the platform core, such as the previews of version control
 *
 * This extension point is available only for the IdeaVim internally
 */
interface DiffTabFilesProvider {
  /** The files of a tab shown by [processor], or null if this provider does not know such tabs */
  fun filesOf(processor: DiffRequestProcessor): DiffTabFiles?

  companion object {
    internal val EP_NAME = ExtensionPointName.create<DiffTabFilesProvider>("IdeaVIM.internal.diffTabFiles")
  }
}

/**
 * The files of a "Show Diff" tab
 *
 * They are matched by the names of their diffs. A diff of a change in version control is named after the path of the
 * file, and other diffs give us nothing better.
 */
private class ChainFiles(private val processor: CacheDiffRequestChainProcessor) : DiffTabFiles {
  override fun canShow(path: String): Boolean = indexOf(path) != null

  override fun show(path: String): Boolean {
    val index = indexOf(path) ?: return false
    processor.setCurrentRequest(index)
    return true
  }

  private fun indexOf(path: String): Int? {
    return processor.requestChain.requests.indexOfFirst { it.name == path }.takeIf { it >= 0 }
  }
}
