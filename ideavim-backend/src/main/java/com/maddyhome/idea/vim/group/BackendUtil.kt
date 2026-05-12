/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Finds a [VirtualFile] by path, trying local filesystem first, then jar for library sources.
 * When [protocol] is provided, tries that filesystem first before falling back.
 */
internal fun findVirtualFile(filePath: String, protocol: String? = null): VirtualFile? {
  if (protocol != null) {
    VirtualFileManager.getInstance().getFileSystem(protocol)?.findFileByPath(filePath)?.let { return it }
  }
  LocalFileSystem.getInstance().findFileByPath(filePath)?.let { return it }
  return VirtualFileManager.getInstance().getFileSystem("jar")?.findFileByPath(filePath)
}

