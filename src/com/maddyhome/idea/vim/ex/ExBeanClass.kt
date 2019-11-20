package com.maddyhome.idea.vim.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute

class ExBeanClass : AbstractExtensionPointBean() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("names")
  var names: String? = null

  val handler: CommandHandler by lazy {
    // FIXME. [VERSION UPDATE] change to instantiateClass for 193+
    @Suppress("DEPRECATION")
    this.instantiate<CommandHandler>(
      implementation ?: "", ApplicationManager.getApplication().picoContainer)
  }

  fun register() {
    CommandParser.getInstance().addHandler(this)
  }
}

interface ComplicatedNameExCommand {
  val names: Array<CommandName>
}
