/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.key.MappingOwner.Plugin.Companion.remove
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.statistic.PluginState
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.options.ToggleOption

object VimExtensionRegistrar {
  internal val registeredExtensions: MutableSet<String> = HashSet()
  internal val extensionAliases = HashMap<String, String>()
  private var extensionRegistered = false
  private val logger = logger<VimExtensionRegistrar>()

  private val delayedExtensionEnabling = mutableListOf<ExtensionBeanClass>()

  @JvmStatic
  fun registerExtensions() {
    if (extensionRegistered) return
    extensionRegistered = true

    VimExtension.EP_NAME.extensions.forEach(this::registerExtension)

    VimExtension.EP_NAME.point.addExtensionPointListener(
      object : ExtensionPointListener<ExtensionBeanClass> {
        override fun extensionAdded(extension: ExtensionBeanClass, pluginDescriptor: PluginDescriptor) {
          registerExtension(extension)
        }

        override fun extensionRemoved(extension: ExtensionBeanClass, pluginDescriptor: PluginDescriptor) {
          unregisterExtension(extension)
        }
      },
      false, VimPlugin.getInstance()
    )
  }

  @Synchronized
  private fun registerExtension(extensionBean: ExtensionBeanClass) {
    val name = extensionBean.name ?: extensionBean.instance.name
    if (name in registeredExtensions) return

    registeredExtensions.add(name)
    registerAliases(extensionBean)
    OptionsManager.addOption(com.maddyhome.idea.vim.option.ToggleOption(name, getAbbrev(name), false))
    VimPlugin.getOptionServiceImpl().addOption(ToggleOption(name, getAbbrev(name), false))
    VimPlugin.getOptionService().addListener(
      name,
      object : OptionChangeListener<VimDataType> {
        override fun processGlobalValueChange(oldValue: VimDataType?) {
          if (VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, name)) {
            initExtension(extensionBean, name)
            PluginState.enabledExtensions.add(name)
          } else {
            extensionBean.instance.dispose()
          }
        }
      }
    )
  }

  private fun getAbbrev(name: String): String {
    return if (name == "NERDTree") "nerdtree" else name
  }

  private fun initExtension(extensionBean: ExtensionBeanClass, name: String) {
    if (injector.vimscriptExecutor.executingVimscript) {
      delayedExtensionEnabling += extensionBean
    } else {
      extensionBean.instance.init()
      logger.info("IdeaVim extension '$name' initialized")
    }
  }

  @JvmStatic
  fun enableDelayedExtensions() {
    delayedExtensionEnabling.forEach {
      it.instance.init()
      logger.info("IdeaVim extension '${it.name}' initialized")
    }
    delayedExtensionEnabling.clear()
  }

  @Synchronized
  private fun unregisterExtension(extension: ExtensionBeanClass) {
    val name = extension.name ?: extension.instance.name
    if (name !in registeredExtensions) return
    registeredExtensions.remove(name)
    removeAliases(extension)
    extension.instance.dispose()
    VimPlugin.getOptionService().removeOption(name)
    OptionsManager.removeOption(name)
    remove(name)
    logger.info("IdeaVim extension '$name' disposed")
  }

  fun setOptionByPluginAlias(alias: String): Boolean {
    val name = extensionAliases[alias] ?: return false
    try {
      VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, name)
    } catch (e: ExException) {
      return false
    }
    return true
  }

  private fun registerAliases(extension: ExtensionBeanClass) {
    extension.aliases
      ?.mapNotNull { it.name }
      ?.forEach { alias -> extensionAliases[alias] = extension.name ?: extension.instance.name }
  }

  private fun removeAliases(extension: ExtensionBeanClass) {
    extension.aliases?.mapNotNull { it.name }?.forEach { extensionAliases.remove(it) }
  }
}
