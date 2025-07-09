/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimExtensionRegistrator
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.statistic.PluginState
import kotlinx.coroutines.runBlocking

internal object VimExtensionRegistrar : VimExtensionRegistrator {
  internal val registeredExtensions: MutableSet<String> = HashSet()
  internal val extensionAliases = HashMap<String, String>()
  private var extensionRegistered = false
  private val logger = logger<VimExtensionRegistrar>()

  private val delayedExtensionEnabling = mutableListOf<ExtensionBeanClass>()

  private fun ExtensionBeanClass.getToggleOption(): ToggleOption {
    val name = name ?: instance.name
    return ToggleOption(name, OptionDeclaredScope.GLOBAL, getAbbrev(name), false)
  }

  private fun ToggleOption.isEnabled(): Boolean {
    return injector.optionGroup.getOptionValue(this, OptionAccessScope.GLOBAL(null)).asBoolean()
  }

  private fun enableExtension(extensionBean: ExtensionBeanClass, name: String) {
    initExtension(extensionBean, name)
    PluginState.Util.enabledExtensions.add(name)
  }

  private fun disableExtension(extensionBean: ExtensionBeanClass, name: String) {
    extensionBean.instance.dispose()
    PluginState.Util.enabledExtensions.remove(name)
  }

  /**
   * Calls dispose method on all currently enabled extensions.
   */
  @JvmStatic
  fun disableExtensions() {
    VimExtension.EP_NAME.extensions.filter { extensionBean ->
      extensionBean.getToggleOption().isEnabled()
    }.forEach { disableExtension(it, it.name ?: it.instance.name) }
  }

  /**
   * Registers all extensions if they haven't been registered yet, or enables previously enabled extensions.
   * This is called when the plugin is turned on.
   */
  @JvmStatic
  fun enableExtensions() {
    if (!extensionRegistered) {
      registerExtensions()
      extensionRegistered = true
      return
    }
    // call init method for all extensions that were enabled before
    initializeEnabledExtensions()
  }

  /**
   * Enables all extensions that have their toggle option enabled.
   * Calls the init() method on each enabled extension.
   */
  @JvmStatic
  private fun initializeEnabledExtensions() {
    VimExtension.EP_NAME.extensions.filter { extensionBean ->
      extensionBean.getToggleOption().isEnabled()
    }.forEach { enableExtension(it, it.name ?: it.instance.name) }
  }

  private fun registerExtensions() {
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
      false,
      VimPlugin.getInstance(),
    )
  }

  @Synchronized
  private fun registerExtension(extensionBean: ExtensionBeanClass) {
    val name = extensionBean.name ?: extensionBean.instance.name
    if (name == "sneak" && extensionBean.name == null) {
      // Filter out the old ideavim-sneak extension that used to be a separate plugin
      // https://github.com/Mishkun/ideavim-sneak
      return
    }
    if (name in registeredExtensions) return

    registeredExtensions.add(name)
    registerAliases(extensionBean)
    val option = extensionBean.getToggleOption()
    VimPlugin.getOptionGroup().addOption(option)
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(option) {
      if (option.isEnabled()) {
        enableExtension(extensionBean, name)
      } else {
        disableExtension(extensionBean, name)
      }
    }
  }

  private fun getAbbrev(name: String): String {
    return if (name == "NERDTree") "nerdtree" else name
  }

  private fun initExtension(extensionBean: ExtensionBeanClass, name: String) {
    if (injector.vimscriptExecutor.executingVimscript) {
      delayedExtensionEnabling += extensionBean
    } else {
      runBlocking {
        extensionBean.instance.init()
      }
      logger.info("IdeaVim extension '$name' initialized")
    }
  }

  /**
   * See the docs for [VimExtension.init]
   *
   * In IdeaVim we don't have a separate plugins folder to load it after .ideavimrc load. However, we can collect
   *   the list of plugins mentioned in the .ideavimrc and load them after .ideavimrc execution is finished.
   */
  @JvmStatic
  fun enableDelayedExtensions() {
    delayedExtensionEnabling.forEach {
      runBlocking {
        it.instance.init()
      }
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
    disableExtension(extension, name)
    VimPlugin.getOptionGroup().removeOption(name)
    MappingOwner.Plugin.Companion.remove(name)
    ListenerOwner.Plugin.Companion.remove(name)
    logger.info("IdeaVim extension '$name' disposed")
  }

  override fun setOptionByPluginAlias(alias: String): Boolean {
    val name = extensionAliases[alias] ?: return false
    val option = injector.optionGroup.getOption(name) as? ToggleOption ?: return false
    injector.optionGroup.setToggleOption(option, OptionAccessScope.GLOBAL(null))
    return true
  }

  override fun getExtensionNameByAlias(alias: String): String? {
    return extensionAliases[alias]
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