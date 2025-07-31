/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.IdeaPlug

class IjPluginListener : DynamicPluginListener {
  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    super.pluginLoaded(pluginDescriptor)
    if (!pluginDescriptor.isIdeaVimExtension()) return

    // 1) Scan plugin jar
    val extensions = IjPluginExtensionsScanner.instance().scanPluginJar(pluginDescriptor) ?: return
    injector.jsonExtensionProvider.addExtensions(extensions)

    val includedExtensions = extensions.associateBy { it.extensionName }

    // 2) Enable plugins that are enabled with IdeaPlug in the ideavimrc file
    IdeaPlug.Companion.EnabledExtensions.enabledExtensions.forEach { extensionName ->
      includedExtensions[extensionName]?.let { injector.extensionLoader.enableExtension(it) }
    }
  }

  override fun pluginUnloaded(
    pluginDescriptor: IdeaPluginDescriptor,
    isUpdate: Boolean,
  ) {
    super.pluginUnloaded(pluginDescriptor, isUpdate)
    if (!pluginDescriptor.isIdeaVimExtension()) return

    val pluginId = pluginDescriptor.pluginId.idString
    val extensions = injector.jsonExtensionProvider.getExtensionsForPlugin(pluginId)

    // 1) disable all extensions
    extensions.forEach { extension ->
      injector.extensionLoader.disableExtension(extension.extensionName)
    }

    // 2) remove them from json
    injector.jsonExtensionProvider.removeExtensionForPlugin(pluginId)
  }
}