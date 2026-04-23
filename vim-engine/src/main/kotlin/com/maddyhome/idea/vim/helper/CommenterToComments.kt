/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

/**
 * Vim-style `'comments'` markers derived from IntelliJ's `com.intellij.lang.Commenter`.
 *
 * Plugin-side callers populate [linePrefix], [blockPrefix], [blockSuffix] from the
 * `Commenter` API and hand the result to [CommenterToComments.derive].
 */
data class CommenterMarkers(
  val linePrefix: String?,
  val blockPrefix: String?,
  val blockSuffix: String?,
)

/**
 * Derives a Vim-style `'comments'` value from [CommenterMarkers] — the fallback when
 * [FiletypePresets] has no entry for the buffer's filetype.
 *
 * Lossy by design: the output omits Vim-specific flags (notably `b:` BLANK_REQUIRED
 * and per-language bullet markers), because the IntelliJ `Commenter` API does not
 * expose them. Use a [FiletypePresets] entry for fidelity on supported languages.
 */
object CommenterToComments {
  fun derive(markers: CommenterMarkers): String? {
    val line = markers.linePrefix?.trim()?.takeIf { it.isNotEmpty() }
    val blockPrefix = markers.blockPrefix?.takeIf { it.isNotEmpty() }
    val blockSuffix = markers.blockSuffix?.takeIf { it.isNotEmpty() }

    val entries = mutableListOf<String>()
    // Three-piece entries first: Vim parses left-to-right and matches continuation
    // lines against `mb:` before the line leader.
    if (blockPrefix != null && blockSuffix != null) {
      entries += blockEntries(blockPrefix, blockSuffix)
    }
    if (line != null) entries += ":$line"

    return entries.joinToString(",").ifEmpty { null }
  }

  private fun blockEntries(prefix: String, suffix: String): List<String> {
    // Middle marker is the last char of the prefix when the block is symmetric
    // (e.g., /* ... */, (* ... *)). For asymmetric block forms like <!-- -->
    // or --[[ ]], fall back to a space — matches Vim's html.vim convention and
    // degrades safely on unknown shapes.
    val symmetric = prefix.length >= 2 && prefix.first() == suffix.last()
    val middle: Char = if (symmetric) prefix.last() else ' '
    return listOf("s1:$prefix", "mb:$middle", "ex:$suffix")
  }
}
