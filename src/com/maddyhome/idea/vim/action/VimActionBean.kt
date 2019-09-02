package com.maddyhome.idea.vim.action

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase

class VimActionBean : AbstractExtensionPointBean() {
  @Attribute("actionClass")
  lateinit var actionClass: String

  val action: EditorActionHandlerBase by lazy {
    instantiate<EditorActionHandlerBase>(actionClass, ApplicationManager.getApplication().picoContainer)
  }
}
