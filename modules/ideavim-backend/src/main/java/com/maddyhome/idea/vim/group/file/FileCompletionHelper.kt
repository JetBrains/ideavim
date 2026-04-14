/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlin.io.path.Path

/**
 * Resolves a user-typed path prefix into a list of matching file/directory names
 * for command-line completion. Directories are suffixed with `/`.
 */
internal object FileCompletionHelper {

  fun listMatchingFiles(pathPrefix: String, basePath: String?): List<String> {
    val (parentDir, namePrefix) = resolveParentAndPrefix(pathPrefix, basePath)
    if (parentDir == null || !parentDir.isDirectory) return emptyList()

    return filterAndFormat(parentDir, namePrefix, pathPrefix)
  }

  private fun filterAndFormat(parentDir: VirtualFile, namePrefix: String, pathPrefix: String): List<String> {
    val dirPrefix = pathPrefix.substringBeforeLast('/', "")

    return parentDir.children
      .filter { it.name.startsWith(namePrefix, ignoreCase = true) }
      .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
      .map { formatChild(it, dirPrefix) }
  }

  private fun formatChild(child: VirtualFile, dirPrefix: String): String {
    val name = if (child.isDirectory) child.name + "/" else child.name
    if (dirPrefix.isEmpty()) return name
    return "$dirPrefix/$name"
  }

  private fun resolveParentAndPrefix(pathPrefix: String, basePath: String?): Pair<VirtualFile?, String> {
    if (pathPrefix.isEmpty()) return resolveProjectRoot(basePath)
    if (pathPrefix.startsWith("~/") || pathPrefix.startsWith("~\\")) return resolveHomePath(pathPrefix)
    if (Path(pathPrefix).isAbsolute) return resolveAbsolutePath(pathPrefix)
    return resolveRelativePath(pathPrefix, basePath)
  }

  private fun resolveProjectRoot(basePath: String?): Pair<VirtualFile?, String> {
    val dir = basePath?.let { LocalFileSystem.getInstance().findFileByNioFile(Path(it)) }
    return dir to ""
  }

  private fun resolveHomePath(pathPrefix: String): Pair<VirtualFile?, String> {
    val home = System.getProperty("user.home")
    val relativePath = pathPrefix.substring(2)
    return splitDirAndPrefix(relativePath) { dirPath ->
      LocalFileSystem.getInstance().findFileByNioFile(Path(home, dirPath))
    } ?: (LocalFileSystem.getInstance().findFileByNioFile(Path(home)) to relativePath)
  }

  private fun resolveAbsolutePath(pathPrefix: String): Pair<VirtualFile?, String> {
    return splitDirAndPrefix(pathPrefix) { dirPath ->
      LocalFileSystem.getInstance().findFileByNioFile(Path(dirPath.ifEmpty { "/" }))
    } ?: (null to "")
  }

  private fun resolveRelativePath(pathPrefix: String, basePath: String?): Pair<VirtualFile?, String> {
    val baseDir = basePath?.let { LocalFileSystem.getInstance().findFileByNioFile(Path(it)) }
    return splitDirAndPrefix(pathPrefix) { dirPath ->
      baseDir?.findFileByRelativePath(dirPath)
    } ?: (baseDir to pathPrefix)
  }

  private fun splitDirAndPrefix(
    path: String,
    resolveDir: (String) -> VirtualFile?,
  ): Pair<VirtualFile?, String>? {
    val lastSlash = path.lastIndexOf('/')
    if (lastSlash < 0) return null

    val dirPath = path.substring(0, lastSlash)
    val prefix = path.substring(lastSlash + 1)
    return resolveDir(dirPath) to prefix
  }
}
