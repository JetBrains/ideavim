/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import org.apache.commons.codec.binary.Base64
import org.jdom.Element

@Service
class XMLGroup {
  companion object {
    @JvmStatic
    fun getInstance(): XMLGroup = ApplicationManager.getApplication().getService(XMLGroup::class.java)
  }
  /**
   * Set the text of an XML element, safely encode it if needed.
   */
  fun setSafeXmlText(element: Element, text: String): Element {
    element.setAttribute("encoding", "base64")
    val encoded = String(Base64.encodeBase64(text.toByteArray()))
    element.text = encoded
    return element
  }

  /**
   * Get the (potentially safely encoded) text of an XML element.
   */
  fun getSafeXmlText(element: Element): String? {
    val text = element.text
    val encoding = element.getAttributeValue("encoding")
    if (encoding == null) {
      return text
    } else if (encoding == "base64") {
      return String(Base64.decodeBase64(text.toByteArray()))
    }
    return null
  }
}
