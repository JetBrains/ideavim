package org.jetbrains.plugins.ideavim.helper

import com.maddyhome.idea.vim.RegisterActions.VIM_ACTIONS_EP
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
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

  private inline fun forEachAction(supply: (action: EditorActionHandlerBase) -> Unit) {
    VIM_ACTIONS_EP.extensions.map { it.action }.forEach { supply(it) }
  }
}
