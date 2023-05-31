/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.intellij.openapi.diagnostic.logger
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector

@Deprecated("Moved to annotation approach and lazy initialization")
internal class FunctionBeanClass : BaseKeyedLazyInstance<FunctionHandler>() {

  @Attribute("implementation")
  lateinit var implementation: String

  @Attribute("name")
  lateinit var name: String

  override fun getImplementationClassName(): String = implementation

  fun register() {
    if (this.pluginDescriptor.pluginId != VimPlugin.getPluginId()) {
      logger<FunctionHandler>().error("IdeaVim doesn't accept contributions to `vimActions` extension points. Please create a plugin using `VimExtension`. Plugin to blame: ${this.pluginDescriptor.pluginId}")
      return
    }
    injector.functionService.addOldHandler(this)
  }
}
