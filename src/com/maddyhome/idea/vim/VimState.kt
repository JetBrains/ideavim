package com.maddyhome.idea.vim

import org.jdom.Element

/**
 * @author Alex Plate
 */
class VimState {
  var isIdeaJoinNotified = false

  fun readData(element: Element) {
    val notifications: Element? = element.getChild("notifications")

    notifications?.getChild("idea-join")?.getAttributeValue("enabled")?.let {
      isIdeaJoinNotified = it.toBoolean()
    }
  }

  fun saveData(element: Element) {
    val notifications = Element("notifications")
    element.addContent(notifications)

    val ideaJoin = Element("idea-join")
    ideaJoin.setAttribute("enabled", isIdeaJoinNotified.toString())
    notifications.addContent(ideaJoin)
  }
}