/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Implementation of path expansion for file paths in commands like `:source`.
 *
 * Non-existent environment variables expand to empty string (matching Vim's behavior in `:source` and expressions).
 *
 * This implementation focuses on Unix-style environment variables.
 * Windows %VAR% syntax is not supported as it's not part of Vim's expand-env.
 */
class VimPathExpansionImpl : VimPathExpansion {

  companion object {
    // Regex for matching $VAR or ${VAR} patterns, accounting for escaped \$
    // Matches: optional backslash, dollar sign, optional braces, variable name
    private val ENV_VAR_REGEX = Regex("""(\\?)\$(?:([A-Za-z_][A-Za-z0-9_]*)|(?:\{([A-Za-z_][A-Za-z0-9_]*)\}))""")
  }

  override fun expandPath(path: String): String {
    var expanded = path

    // First, expand tilde (~) if at the start
    expanded = expandTilde(expanded)

    // Then, expand environment variables
    expanded = expandEnvironmentVariables(expanded)

    return expanded
  }

  /**
   * Expands tilde at the start of the path to the user's home directory.
   *
   * Supports:
   * - `~` alone → home directory
   * - `~/path` → home directory + /path
   * - `~\path` → home directory + \path (Windows-style)
   *
   * Does NOT support (yet):
   * - `~username` → other user's home directory (requires system user lookup)
   * - `~+` → current working directory (Vim-specific)
   * - `~.file` or other non-separator chars after tilde (stays literal per Vim)
   *
   * Tilde is only expanded when:
   * 1. It's at the start of the string
   * 2. It's followed by nothing, `/`, or `\`
   *
   * This matches Vim's behavior for the most common use cases.
   */
  private fun expandTilde(path: String): String {
    if (path == "~" || path.startsWith("~/") || path.startsWith("~\\")) {
      val home = System.getProperty("user.home")
      if (home != null) {
        return home + path.substring(1)
      }
    }
    return path
  }

  /**
   * Expands environment variables in the string.
   * Supports both $VAR and ${VAR} syntax.
   * Escaped variables \$VAR are converted to literal $VAR.
   * Non-existent variables expand to empty string.
   */
  private fun expandEnvironmentVariables(text: String): String {
    return ENV_VAR_REGEX.replace(text) { matchResult ->
      val escaped = matchResult.groupValues[1] // Backslash before $, if any
      val varName =
        matchResult.groupValues[2].ifEmpty { matchResult.groupValues[3] } // Variable name from $VAR or ${VAR}

      if (escaped.isNotEmpty()) {
        // Escaped $ - remove backslash and keep literal $VAR
        matchResult.value.substring(1) // Remove the backslash
      } else {
        // Not escaped - expand to value or empty string if doesn't exist
        System.getenv(varName) ?: ""
      }
    }
  }
}
