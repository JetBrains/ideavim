/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.key.MappingOwner.Plugin.Companion.remove
import com.maddyhome.idea.vim.option.Option
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.OptionsManager.addOption
import com.maddyhome.idea.vim.option.OptionsManager.isSet
import com.maddyhome.idea.vim.option.OptionsManager.removeOption
import com.maddyhome.idea.vim.option.ToggleOption

object VimExtensionRegistrar {
  private val registeredExtensions: MutableSet<String> = HashSet()
  private val extensionAliases = HashMap<String, String>()
  private var extensionRegistered = false
  private val logger = logger<VimExtensionRegistrar>()

  @JvmStatic
  fun registerExtensions() {
    if (extensionRegistered) return
    extensionRegistered = true
    // [VERSION UPDATE] 202+
    @Suppress("DEPRECATION")
    VimExtension.EP_NAME.getPoint(null).addExtensionPointListener(object : ExtensionPointListener<VimExtension> {
      override fun extensionAdded(extension: VimExtension, pluginDescriptor: PluginDescriptor) {
        registerExtension(extension)
      }

      override fun extensionRemoved(extension: VimExtension, pluginDescriptor: PluginDescriptor) {
        unregisterExtension(extension)
      }
    }, true, VimPlugin.getInstance())
  }

  @Synchronized
  private fun registerExtension(extension: VimExtension) {
    val name = extension.name
    if (name in registeredExtensions) return

    registeredExtensions.add(name)
    registerAliases(name)
    val option = ToggleOption(name, name, false)
    option.addOptionChangeListener { _, _ ->
      for (extensionInListener in VimExtension.EP_NAME.extensionList) {
        if (name != extensionInListener.name) continue
        if (isSet(name)) {
          extensionInListener.init()
          logger.info("IdeaVim extension '$name' initialized")
        } else {
          extensionInListener.dispose()
        }
      }
    }
    addOption(option)
  }

  @Synchronized
  private fun unregisterExtension(extension: VimExtension) {
    val name = extension.name
    if (name !in registeredExtensions) return
    registeredExtensions.remove(name)
    removeAliases(extension)
    extension.dispose()
    removeOption(name)
    remove(name)
    logger.info("IdeaVim extension '$name' disposed")
  }

  fun getToggleByAlias(alias: String): ToggleOption? {
    val name = extensionAliases[alias] ?: return null
    return OptionsManager.getOption(name) as ToggleOption?
  }

  private fun registerAliases(name: String) {
    val extension = VimExtension.EP_NAME.findFirstSafe { it.name == name } ?: return
    extension.aliases.forEach { alias -> extensionAliases[alias] = name }
  }

  private fun removeAliases(extension: VimExtension) {
    extension.aliases.forEach { extensionAliases.remove(it) }
  }
}
