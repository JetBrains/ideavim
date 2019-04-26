package com.maddyhome.idea.vim

import com.intellij.configurationStore.APP_CONFIG
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.VimPlugin.STATE_VERSION
import org.jdom.Element

/**
 * @author Alex Plate
 */

@State(name = "VimLocalSettings",
        storages = [Storage("$APP_CONFIG$/vim_local_settings.xml", roamingType = RoamingType.DISABLED)])
class VimLocalConfig : PersistentStateComponent<Element> {
    override fun getState(): Element {
        val element = Element("ideavim-local")

        val state = Element("state")
        state.setAttribute("version", Integer.toString(STATE_VERSION))
        element.addContent(state)

        VimPlugin.getMark().saveData(element)
        VimPlugin.getRegister().saveData(element)
        VimPlugin.getSearch().saveData(element)
        VimPlugin.getHistory().saveData(element)
        return element
    }

    override fun loadState(state: Element) {
        VimPlugin.getMark().readData(state)
        VimPlugin.getRegister().readData(state)
        VimPlugin.getSearch().readData(state)
        VimPlugin.getHistory().readData(state)
    }
}