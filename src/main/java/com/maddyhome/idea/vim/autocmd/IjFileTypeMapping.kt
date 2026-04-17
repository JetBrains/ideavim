/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.autocmd

import com.intellij.openapi.vfs.VirtualFile

/**
 * Maps IntelliJ's [com.intellij.openapi.fileTypes.FileType] name to a Vim-style filetype string
 * suitable for matching against a `FileType` autocmd pattern.
 *
 * Most Vim filetypes are just the lowercase form of the IntelliJ name (e.g. `JAVA` → `java`,
 * `Python` → `python`). A small override table covers the common cases where the conventional
 * Vim name differs from IntelliJ's, so users can write `autocmd FileType python ...` and have
 * it work out of the box.
 */
object IjFileTypeMapping {

  private val overrides: Map<String, String> = mapOf(
    "PLAIN_TEXT" to "text",
    "C++" to "cpp",
    "C#" to "cs",
    "ObjectiveC" to "objc",
    "Shell Script" to "sh",
    "JavaScript" to "javascript",
    "TypeScript" to "typescript",
    "Vue.js" to "vue",
    "Handlebars/Mustache" to "handlebars",
    "CMakeLists.txt" to "cmake",
  )

  fun toVimFileType(virtualFile: VirtualFile?): String? {
    val name = virtualFile?.fileType?.name ?: return null
    return overrides[name] ?: name.lowercase()
  }
}
