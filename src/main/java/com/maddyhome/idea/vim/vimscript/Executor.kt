/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript

import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.api.VimScriptExecutorBase
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar
import java.io.File

@Service
internal class Executor : VimScriptExecutorBase() {
  override fun enableDelayedExtensions() {
    VimExtensionRegistrar.enableDelayedExtensions()
  }

  override fun ensureFileIsSaved(file: File) {
    val documentManager = FileDocumentManager.getInstance()

    VirtualFileManager.getInstance().findFileByNioPath(file.toPath())
      ?.let(documentManager::getCachedDocument)
      ?.takeIf(documentManager::isDocumentUnsaved)
      ?.let(documentManager::saveDocumentAsIs)
  }
}
