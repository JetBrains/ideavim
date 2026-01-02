/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi

/**
 * Represents the range of a text object selection.
 *
 * Unlike [com.intellij.vim.api.models.Range], this type is specifically for text object definitions
 * and encodes the visual selection type (character-wise or line-wise).
 */
sealed interface TextObjectRange {
  /**
   * A character-wise text object range.
   *
   * When selected in visual mode, this will use character-wise selection.
   * Example: `iw` (inner word) uses character-wise selection.
   *
   * @param start The start offset (inclusive)
   * @param end The end offset (exclusive)
   */
  data class CharacterWise(val start: Int, val end: Int) : TextObjectRange

  /**
   * A line-wise text object range.
   *
   * When selected in visual mode, this will switch to line-wise selection.
   * Example: `ip` (inner paragraph) uses line-wise selection.
   *
   * @param startLine The start line (0-based, inclusive)
   * @param endLine The end line (0-based, inclusive)
   */
  data class LineWise(val startLine: Int, val endLine: Int) : TextObjectRange
}

/**
 * Scope for registering custom text objects.
 *
 * Text objects are selections that can be used with operators (like `d`, `c`, `y`)
 * or in visual mode. Examples include `iw` (inner word), `ap` (a paragraph), etc.
 *
 * Example usage:
 * ```kotlin
 * api.textObjects {
 *     register("ae") { count ->
 *         TextObjectRange.CharacterWise(0, editor { read { textLength.toInt() } })
 *     }
 * }
 * ```
 */
@VimApiDsl
interface TextObjectScope {
  /**
   * Registers a text object.
   *
   * This creates a `<Plug>(pluginname-keys)` mapping for the text object,
   * allowing users to remap it. If [registerDefaultMapping] is true, it also
   * maps the [keys] to the `<Plug>` mapping.
   *
   * Example:
   * ```kotlin
   * // Creates <Plug>(textobj-entire-ae) and maps "ae" to it
   * register("ae") { count ->
   *     TextObjectRange.CharacterWise(0, editor { read { textLength.toInt() } })
   * }
   *
   * // Only creates <Plug>(textobj-entire-ip), user must map manually
   * register("ip", registerDefaultMapping = false) { count ->
   *     findParagraphRange(count)
   * }
   * ```
   *
   * @param keys Key sequence (e.g., "ae", "ip"). Also used as suffix for `<Plug>` name.
   * @param registerDefaultMapping If true (default), maps [keys] to `<Plug>(pluginname-keys)`.
   *                               If false, only creates the `<Plug>` mapping.
   * @param rangeProvider Function that returns the [TextObjectRange] for this text object,
   *                      or null if no valid range is found at the current position.
   *                      The function receives the count (e.g., `2iw` passes count=2).
   */
  fun register(
    keys: String,
    registerDefaultMapping: Boolean = true,
    rangeProvider: VimApi.(count: Int) -> TextObjectRange?,
  )
}
