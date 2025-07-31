/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

/**
 * Provides functionality for managing IdeaVim extensions through JSON serialization.
 */
interface JsonExtensionProvider {
  /**
   * Retrieves an extension by its name.
   *
   * @param name The name of the extension to find
   * @return The [ExtensionBean] with the specified name, or null if not found
   */
  fun getExtension(name: String): ExtensionBean?

  /**
   * Retrieves all registered extensions.
   *
   * @return A list of all [ExtensionBean] objects
   */
  fun getAllExtensions(): List<ExtensionBean>

  /**
   * Retrieves all bundled extensions.
   *
   * @return A list of all [ExtensionBean] objects
   */
  fun getBundledExtensions(): List<ExtensionBean>

  /**
   * Retrieves all extensions bundled within a specific IDE plugin.
   *
   * @param pluginId The ID of the plugin to filter extensions by
   * @return A list of [ExtensionBean] objects bundled within the specified plugin
   */
  fun getExtensionsForPlugin(pluginId: String): List<ExtensionBean>

  /**
   * Adds a single extension to the registry.
   *
   * @param extension The [ExtensionBean] to add
   */
  fun addExtension(extension: ExtensionBean)

  /**
   * Adds multiple extensions to the registry.
   *
   * @param extensions Collection of [ExtensionBean] objects to add
   */
  fun addExtensions(extensions: Collection<ExtensionBean>)

  /**
   * Removes all extensions associated with a specific plugin.
   *
   * @param pluginId The ID of the plugin whose extensions should be removed
   */
  fun removeExtensionForPlugin(pluginId: String)

  /**
   * Initializes the extensions' system.
   *
   * This method should be called during application startup to set up
   * the extension registry and load any bundled extensions.
   */
  fun init()
}