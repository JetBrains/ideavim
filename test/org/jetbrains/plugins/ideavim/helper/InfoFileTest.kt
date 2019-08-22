package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandActionBase
import org.jetbrains.plugins.ideavim.VimTestCase
import java.io.File

/**
 * @author Alex Plate
 */
class InfoFileTest : VimTestCase() {
  fun `test info`() {
    val text = File("src/com/maddyhome/idea/vim/package-info.java").readText()

    val notPresentedActions = mutableListOf<String>()
    forEachAction { action ->
      val actionName = action.javaClass.name
      if ("{@link $actionName}" !in text) {
        notPresentedActions += actionName
      }
    }
    assertTrue(notPresentedActions.joinToString(prefix = "Not presented actions in info file: \n", separator = "\n"), notPresentedActions.isEmpty())
  }

  private inline fun forEachAction(supply: (action: VimCommandActionBase) -> Unit) {
    val manager = ActionManagerEx.getInstanceEx()
    for (actionId in manager.getPluginActions(VimPlugin.getPluginId())) {
      val action = manager.getAction(actionId)
      if (action is VimCommandActionBase) supply(action)
    }
  }
}