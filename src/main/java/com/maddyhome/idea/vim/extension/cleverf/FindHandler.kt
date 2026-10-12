/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler

/**
 * Handles `<Plug>(clever-f-{map})`. Repeats the search of the current mode if the cursor is still where it left it,
 * in the direction implied by [map]. Otherwise asks for a character and starts a new search.
 */
internal class FindHandler(
  private val map: Char,
  private val session: CleverFSession,
  private val executor: SearchExecutor,
) : ExtensionHandler {
  private val charReader = CharReader(session)

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val search = repeatedSearch(editor) ?: newSearch(editor) ?: return
    executor.execute(editor, search, operatorArguments.count1)
  }

  /** While a macro runs, clever-f always asks for a character, so a recorded `fa` replays as `fa` */
  private fun repeatedSearch(editor: VimEditor): CharSearch? {
    if (injector.macro.isExecutingMacro) return null
    val state = session.stateFor(editor.mode)?.takeIf { it.canRepeat(editor) } ?: return null
    return state.repeatSearch(back = map.isUpperCase())
  }

  private fun newSearch(editor: VimEditor): CharSearch? {
    val char = charReader.read(editor) ?: return null
    return session.startSearch(editor, map, char).firstSearch()
  }
}
