/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimEditor

/**
 * Represents the scope in which an option is set.
 *
 * Maps closely to `:set`, `:setlocal` and `:setglobal`.
 */
sealed class OptionAccessScope {

  /**
   * Explicitly get or set the global value of an option
   *
   * This is equivalent to `:setglobal`. For local or global-local options, this will always get or set the "global"
   * value of the option without affecting or reading the local value. Note that for local-to-window options, the
   * "global" value is a per-window copy, and not a truly global value. See [OptionDeclaredScope.LOCAL_TO_WINDOW] for
   * more details.
   *
   * @param editor  The editor to access global option values from. This can only be `null` for global and global-local
   * options.
   */
  class GLOBAL(val editor: VimEditor?) : OptionAccessScope()

  /**
   * Explicitly get or set the local value of the option, relative to the given editor
   *
   * This is equivalent to `:setlocal`. For global values, this will get or set the global value of the option. For
   * local options, this will get or set the local value of the option. For global-local, this will also get or set the
   * local value of the option, which might be a sentinel value representing "unset". For numbers and booleans (which
   * are represented as numbers), this will be -1. For strings, this will be an empty string.
   */
  class LOCAL(val editor: VimEditor) : OptionAccessScope()

  /**
   * Get or set the effective value of the option for the given editor
   *
   * This is equivalent to `:set`, and will get or set the value of the option used by the given editor. For global
   * options, this is always the global value. For local options, this will get the local value, and will set both the
   * local and "global" value. For global-local values, this will get the local value if set, and the global value if
   * not. Similarly, it will set the local value if it's already set, else it will set the global value.
   *
   * Note that for local-to-window options, the "global" option is a per-window copy of the option. See
   * [OptionDeclaredScope.LOCAL_TO_WINDOW] for more details.
   */
  class EFFECTIVE(val editor: VimEditor) : OptionAccessScope()
}
