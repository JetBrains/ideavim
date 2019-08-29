package com.maddyhome.idea.vim.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute

class VimActionBean : AbstractExtensionPointBean() {
  @Attribute("id")
  lateinit var id: String

  @Attribute("actionClass")
  lateinit var actionClass: String

  @Attribute("text")
  lateinit var text: String

  val action: VimCommandActionBase by lazy {
    instantiate<VimCommandActionBase>(actionClass, ApplicationManager.getApplication().picoContainer).also {
      it.templatePresentation.text = text
      ActionManager.getInstance().registerAction(id, it)
    }
  }
}
