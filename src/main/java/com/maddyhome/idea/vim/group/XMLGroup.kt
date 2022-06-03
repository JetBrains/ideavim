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

package com.maddyhome.idea.vim.group

import org.apache.commons.codec.binary.Base64
import org.jdom.Element

class XMLGroup {
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