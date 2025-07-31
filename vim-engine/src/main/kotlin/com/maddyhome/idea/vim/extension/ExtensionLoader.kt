/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

/**
 * The ExtensionLoader handles loading, enabling, and disabling extensions in the IdeaVim.
 */
interface ExtensionLoader {
  /**
   * Returns a collection of all currently enabled extensions.
   * 
   * @return A collection of [ExtensionBean] objects representing all enabled extensions.
   */
  fun getEnabledExtensions(): Collection<ExtensionBean>

  /**
   * Enables the specified extension by calling its initialization method.
   *
   * @param extension The [ExtensionBean] representing the extension to be enabled.
   */
  fun enableExtension(extension: ExtensionBean)

  /**
   * Disables the specified extension and cleans up its registered resources (mappings, listeners etc.).
   *
   * @param name The name of the extension to be disabled.
   */
  fun disableExtension(name: String)
}
