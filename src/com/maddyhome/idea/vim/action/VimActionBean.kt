package com.maddyhome.idea.vim.action

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase

class VimActionBean : AbstractExtensionPointBean() {
  @Attribute("id")
  lateinit var id: String

  @Attribute("actionClass")
  lateinit var actionClass: String

  @Attribute("text")
  lateinit var text: String

  val action: EditorActionHandlerBase by lazy {
    instantiate<EditorActionHandlerBase>(actionClass, ApplicationManager.getApplication().picoContainer).also {
      it.text = text
      it.id = id
    }
  }
}
