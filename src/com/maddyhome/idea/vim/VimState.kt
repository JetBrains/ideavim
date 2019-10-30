package com.maddyhome.idea.vim

import org.jdom.Element
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author Alex Plate
 */
class VimState {
  var isIdeaJoinNotified by StateProperty("idea-join")
  var isIdeaPutNotified by StateProperty("idea-put")

  fun readData(element: Element) {
    val notifications = element.getChild("notifications")
    map.keys.forEach { name ->
      notifications?.getChild(name)?.getAttributeValue("enabled")?.let {
        map[name] = it.toBoolean()
      }
    }
  }

  fun saveData(element: Element) {
    val notifications = Element("notifications")
    element.addContent(notifications)

    map.forEach { (name, value) ->
      val child = Element(name)
      child.setAttribute("enabled", value.toString())
      notifications.addContent(child)
    }
  }
}

val map by lazy { mutableMapOf<String, Boolean>() }

private class StateProperty(val xmlName: String) : ReadWriteProperty<VimState, Boolean> {

  init {
    map[xmlName] = false
  }

  override fun getValue(thisRef: VimState, property: KProperty<*>): Boolean = map.getOrPut(xmlName) { false }

  override fun setValue(thisRef: VimState, property: KProperty<*>, value: Boolean) {
    map[xmlName] = value
  }
}
