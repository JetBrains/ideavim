/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import kotlinx.serialization.Serializable

@Serializable
data class KspExtensionBean(val extensionName: String, val functionName: String, val className: String)

/**
 * Represents an IdeaVim extension with its metadata.
 *
 * It contains all the necessary information to identify and load an extension,
 * including its name, associated plugin, function name, and class path.
 */
@Serializable
data class ExtensionBean(
  /**
   * The unique name of the extension.
   */
  val extensionName: String,

  /**
   * The ID of the plugin that provides this extension.
   */
  val pluginId: String,

  /**
   * The name of the function that implements the extension's functionality.
   */
  val functionName: String,

  /**
   * The fully qualified class path where the extension is implemented.
   */
  val className: String,
)

fun KspExtensionBean.toExtensionBean(pluginId: String): ExtensionBean {
  return ExtensionBean(extensionName, pluginId, functionName, className)
}