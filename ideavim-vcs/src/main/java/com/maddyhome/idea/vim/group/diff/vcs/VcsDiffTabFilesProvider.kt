/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.diff.vcs

import com.intellij.diff.impl.CacheDiffRequestChainProcessor
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.ChangeViewDiffRequestProcessor
import com.intellij.openapi.vcs.changes.ui.PresentableChange
import com.maddyhome.idea.vim.group.diff.DiffTabFiles
import com.maddyhome.idea.vim.group.diff.DiffTabFilesProvider
import java.lang.reflect.Method

internal class VcsDiffTabFilesProvider : DiffTabFilesProvider {
  override fun filesOf(processor: DiffRequestProcessor): DiffTabFiles? = when (processor) {
    is ChangeViewDiffRequestProcessor -> ChangesViewFiles(processor)
    is CacheDiffRequestChainProcessor -> ChangesChainFiles.of(processor)
    else -> null
  }
}

/**
 * The files of a "Show Diff" tab of changes in version control, matched by the paths the changes know
 */
private class ChangesChainFiles(private val processor: CacheDiffRequestChainProcessor) : DiffTabFiles {
  override fun canShow(path: String): Boolean = indexOf(path) != null

  override fun show(path: String): Boolean {
    val index = indexOf(path) ?: return false
    processor.setCurrentRequest(index)
    return true
  }

  private fun indexOf(path: String): Int? {
    val changes = processor.requestChain.requests
    return changes.indexOfFirst { (it as? PresentableChange)?.filePath?.path == path }.takeIf { it >= 0 }
  }

  companion object {
    /** The files of a chain of changes, or null for a chain of other diffs */
    fun of(processor: CacheDiffRequestChainProcessor): ChangesChainFiles? {
      val isChainOfChanges = processor.requestChain.requests.any { it is PresentableChange }
      return if (isChainOfChanges) ChangesChainFiles(processor) else null
    }
  }
}

/**
 * The files of a diff preview that follows the selection in a tree of changes, such as the one of the Commit tool window
 *
 * Switching the file also selects it in the tree, so the preview keeps it when the tree refreshes. Only 2026.3 and later
 * can do that from outside the tree, with `showChange(FilePath)` (IJPL-248190). On older versions only the preview
 * switches, and it goes back to the file selected in the tree on the next refresh.
 */
private class ChangesViewFiles(private val processor: ChangeViewDiffRequestProcessor) : DiffTabFiles {
  override fun canShow(path: String): Boolean = changeOf(path) != null

  override fun show(path: String): Boolean {
    val change = changeOf(path) ?: return false
    if (!showAndSelectInTree(change.filePath)) {
      processor.currentChange = change
    }
    return true
  }

  /** Calls `showChange(FilePath)` of 2026.3 and later. Returns false on older versions, which do not have it */
  private fun showAndSelectInTree(path: FilePath): Boolean {
    val showChange = showChangeMethods.get(processor.javaClass) ?: return false
    return try {
      showChange.invoke(processor, path) == true
    } catch (_: ReflectiveOperationException) {
      false
    }
  }

  private fun changeOf(path: String): ChangeViewDiffRequestProcessor.Wrapper? {
    return processor.iterateAllChanges().firstOrNull { it.filePath.path == path }
  }
}

/**
 * `showChange(FilePath)` of each class of processor, looked up once per class
 *
 * TODO: call showChange directly once 2026.3 is the minimum version. IdeaVim is compiled against an older platform
 */
private val showChangeMethods = object : ClassValue<Method?>() {
  override fun computeValue(type: Class<*>): Method? {
    return try {
      // The lookup can find an override declared in a class of the processor that is not public
      type.getMethod("showChange", FilePath::class.java).apply { trySetAccessible() }
    } catch (_: NoSuchMethodException) {
      null
    }
  }
}
