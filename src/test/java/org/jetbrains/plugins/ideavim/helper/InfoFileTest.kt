/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

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
    val text = File("src/main/java/com/maddyhome/idea/vim/package-info.java").readText()

    val notPresentedActions = mutableListOf<String>()
    forEachAction { action ->
      val actionName = action.javaClass.name
      if ("{@link $actionName}" !in text) {
        notPresentedActions += actionName
      }
    }
    assertTrue(
      notPresentedActions.joinToString(prefix = "Not presented actions in info file: \n", separator = "\n"),
      notPresentedActions.isEmpty(),
    )
  }

  private inline fun forEachAction(supply: (action: EditorActionHandlerBase) -> Unit) {
    VIM_ACTIONS_EP.extensions.map { it.instance }.forEach { supply(it) }
  }
}
