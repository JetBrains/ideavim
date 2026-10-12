/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing

/**
 * Port of clever-f.vim (https://github.com/rhysd/clever-f.vim).
 *
 * Extends `f`, `F`, `t` and `T` so that pressing the same key again repeats the last search, making `;` and `,`
 * unnecessary. A search is repeated only if the cursor is still where the previous search left it; any other cursor
 * movement, text change or entering Insert mode starts a new search. Unlike the builtin motions, searches cross line
 * boundaries by default. The characters the search can jump to on the cursor line are highlighted.
 */
internal class CleverFExtension : VimExtension {
  private val session = CleverFSession()
  private val finalizer = SearchFinalizer(session)

  override fun getName(): String = "clever-f"

  override fun init() {
    finalizer.attach()
    registerMappings(SearchExecutor(session))
  }

  override fun dispose() {
    finalizer.detach()
    session.reset()
    super.dispose()
  }

  private fun registerMappings(executor: SearchExecutor) {
    for (map in FIND_KEYS) {
      val plugName = "<Plug>(clever-f-$map)"
      mapPlug(plugName, FindHandler(map, session, executor))
      if (!CleverFSettings.notOverwritesStandardMappings) mapKeyToPlug(map.toString(), plugName)
    }
    mapPlug("<Plug>(clever-f-repeat-forward)", RepeatHandler(back = false, session, executor))
    mapPlug("<Plug>(clever-f-repeat-back)", RepeatHandler(back = true, session, executor))
    mapPlug("<Plug>(clever-f-reset)", ResetHandler(session))
  }

  private fun mapPlug(plugName: String, handler: ExtensionHandler) {
    putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys(plugName), owner, handler, false)
  }

  private fun mapKeyToPlug(key: String, plugName: String) {
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys(key),
      owner,
      injector.parser.parseKeys(plugName),
      true,
    )
  }

  private companion object {
    val FIND_KEYS = listOf('f', 'F', 't', 'T')
  }
}
