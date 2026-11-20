/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchup

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.matchit.MatchHandler

/**
 * Port of matchup.vim (https://github.com/chrisbra/matchit)
 */
internal class Matchup : VimExtension {

  override fun getName(): String = "matchup"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchupMotion)"),
      owner,
      MatchHandler(false),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchupMotion)"),
      owner,
      MatchHandler(false),
      false
    )
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys("%"),
      owner,
      injector.parser.parseKeys("<Plug>(MatchupMotion)"),
      true
    )

    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(ReverseMatchupMotion)"),
      owner,
      MatchHandler(true),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(ReverseMatchupMotion)"),
      owner,
      MatchHandler(true),
      false
    )
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys("g%"),
      owner,
      injector.parser.parseKeys("<Plug>(ReverseMatchupMotion)"),
      true
    )

    putBlockMotion("[%", BlockMotion.START)
    putBlockMotion("]%", BlockMotion.END)
    putBlockMotion("z%", BlockMotion.INSIDE)
  }

  private fun putBlockMotion(keys: String, motion: BlockMotion) {
    val plug = injector.parser.parseKeys("<Plug>(matchup-$keys)")
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, plug, owner, BlockMotionHandler(motion), false)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys(keys), owner, plug, true)
  }
}
