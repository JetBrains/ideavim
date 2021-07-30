package com.maddyhome.idea.vim.vimscript.model.functions

import com.intellij.openapi.diagnostic.logger
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

class FunctionBeanClass : BaseKeyedLazyInstance<FunctionHandler>() {

  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("name")
  var name: String? = null

  override fun getImplementationClassName(): String? = implementation

  fun register() {
    if (this.pluginDescriptor.pluginId != VimPlugin.getPluginId()) {
      logger<FunctionHandler>().error("IdeaVim doesn't accept contributions to `vimActions` extension points. Please create a plugin using `VimExtension`. Plugin to blame: ${this.pluginDescriptor.pluginId}")
      return
    }
    FunctionStorage.addHandler(this)
  }
}
