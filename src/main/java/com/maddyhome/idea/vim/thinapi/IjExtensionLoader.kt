/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.extension.ExtensionBean
import com.maddyhome.idea.vim.extension.ExtensionLoader
import com.maddyhome.idea.vim.extension.LazyVimExtension
import com.maddyhome.idea.vim.key.MappingOwner


/**
 * Extension function that converts an [ExtensionBean] to a [LazyVimExtension].
 *
 * @return A [LazyVimExtension] instance that can be used to invoke the extension's initialization method,
 *         or null if the plugin's ClassLoader cannot be found.
 */
internal fun ExtensionBean.toLazyExtension(): LazyVimExtension? {
  val classLoader = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.classLoader ?: return null
  return LazyVimExtension(extensionName, className, functionName, classLoader)
}

@Service(Service.Level.APP)
class IjExtensionLoader : ExtensionLoader {
  /**
   * Registry of all currently enabled extensions, mapped by their names.
   */
  private val enabledExtensions: MutableMap<String, ExtensionBean> = mutableMapOf()

  /**
   * Creates a VimApi for the specified extension.
   *
   * The VimApi provides extension-specific context for mappings and listeners,
   * allowing the extension to register its functionality in an isolated manner.
   *
   * @param name The name of the extension for which to create a scope.
   * @return A [VimApiImpl] instance configured for the specified extension.
   */
  private fun createVimApi(name: String): VimApiImpl {
    val mappingOwner = MappingOwner.Plugin.Companion.get(name)
    val listenerOwner = ListenerOwner.Plugin.Companion.get(name)
    return VimApiImpl(listenerOwner, mappingOwner)
  }

  /**
   * Returns a collection of all currently enabled extensions.
   *
   * @return A collection of [ExtensionBean] objects representing all enabled extensions.
   */
  override fun getEnabledExtensions(): Collection<ExtensionBean> {
    return enabledExtensions.values
  }

  /**
   * Enables the specified extension by calling its initialization method.
   *
   * @param extension The [ExtensionBean] representing the extension to be enabled.
   */
  override fun enableExtension(extension: ExtensionBean) {
    val name = extension.extensionName

    // extension is already enabled
    if (name in enabledExtensions) return

    // add name to enabled extensions
    enabledExtensions[name] = extension

    // call init method
    extension.toLazyExtension()?.instance?.invoke(createVimApi(name))
  }

  /**
   * Disables the specified extension and cleans up its registered resources (mappings, listeners etc.).
   *
   * @param name The name of the extension to be disabled.
   */
  override fun disableExtension(name: String) {
    if (name !in enabledExtensions) return
    enabledExtensions.remove(name)

    // unload mappings
    injector.keyGroup.removeKeyMapping(MappingOwner.Plugin.get(name))

    // unload all listeners
    injector.listenersNotifier.unloadListeners(ListenerOwner.Plugin.get(name))
  }
}
