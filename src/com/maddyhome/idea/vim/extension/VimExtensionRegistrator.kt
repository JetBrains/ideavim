package com.maddyhome.idea.vim.extension

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.ToggleOption

object VimExtensionRegistrar {

  private val registeredExtensions = mutableSetOf<String>()

  fun registerExtensions() {
    // TODO: [VERSION UPDATE] since 191 use
    //  ExtensionPoint.addExtensionPointListener(ExtensionPointListener<T>, boolean, Disposable)
    VimExtension.EP_NAME.getPoint(null).addExtensionPointListener(object : ExtensionPointListener<VimExtension> {
      override fun extensionAdded(extension: VimExtension, pluginDescriptor: PluginDescriptor) {
        synchronized(VimExtensionRegistrar) {
          registerExtension(extension)
        }
      }
    })
  }

  private fun registerExtension(extension: VimExtension) {
    val name = extension.name
    if (name in registeredExtensions) return
    registeredExtensions += name
    val option = ToggleOption(name, name, false)
    option.addOptionChangeListener {
      for (extensionInListener in VimExtension.EP_NAME.extensionList) {
        if (name == extensionInListener.name) {
          if (OptionsManager.isSet(name)) {
            extensionInListener.init()
            logger.info("IdeaVim extension '$name' initialized")
          } else {
            extensionInListener.dispose()
          }
        }
      }
    }
    OptionsManager.addOption(option)
  }

  private val logger = Logger.getInstance(VimExtensionRegistrar::class.java)
}
