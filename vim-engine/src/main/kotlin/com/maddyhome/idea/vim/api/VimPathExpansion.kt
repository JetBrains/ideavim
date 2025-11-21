/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Service for expanding environment variables and special characters in file paths.
 *
 * Used for commands like `:source` where file paths need environment variable expansion.
 * Non-existent environment variables expand to empty string (matching Vim's behavior in `:source` and expressions).
 *
 * For more details, see `doc/posts/environment-variable-expansion-in-file-commands.md`.
 *
 * See `:help filename-modifiers` for tilde expansion.
 */
interface VimPathExpansion {
  /**
   * Expands environment variables and tilde for use in file paths.
   *
   * Behavior for non-existent variables: **expands to empty string**
   *
   * Examples:
   * - `:source $HOME/.vimrc` → expands `$HOME`
   * - `:source $NONEXISTENT/file` → becomes `/file` (empty string)
   *
   * Supports:
   * - Tilde expansion: `~` and `~/` expand to user's home directory
   * - Environment variables: `$VAR` and `${VAR}` expand to their values or empty
   * - Escaped dollar signs: `\$VAR` becomes literal `$VAR`
   *
   * @param path The string to expand
   * @return The expanded string
   */
  fun expandPath(path: String): String
}
