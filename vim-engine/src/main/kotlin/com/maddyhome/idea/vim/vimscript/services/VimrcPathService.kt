/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import java.nio.file.Path

/**
 * Stores the user-configured location of the ideavimrc file.
 *
 * When no path is configured, IdeaVim searches the default locations (see [VimRcService.findIdeaVimRc]).
 */
interface VimrcPathService {
  /**
   * The path exactly as the user entered it, before any expansion. An empty string means that no path is configured
   * and the default locations are used. Setting a blank value clears the configuration.
   */
  var vimrcPath: String

  /**
   * [vimrcPath] with `~`, environment variables and IDE path macros (such as `$APPLICATION_CONFIG_DIR$`) expanded,
   * or null when no path is configured. The file is not required to exist.
   */
  fun resolveVimrcPath(): Path? = resolvePath(vimrcPath)

  /**
   * Expands [path] the same way [resolveVimrcPath] expands the configured path. Returns null for a blank [path].
   */
  fun resolvePath(path: String): Path?
}
