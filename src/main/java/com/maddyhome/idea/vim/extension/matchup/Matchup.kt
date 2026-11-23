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
import java.util.EnumSet

/**
 * Port of matchup.vim (https://github.com/chrisbra/matchit)
 */
internal class Matchup : VimExtension {
  private val matchParen = MatchParen()

  override fun getName(): String = "matchup"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchupMotion)"),
      owner,
      MatchHandler(false, isMatchup = true),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchupMotion)"),
      owner,
      MatchHandler(false, isMatchup = true),
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
      MatchHandler(true, isMatchup = true),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(ReverseMatchupMotion)"),
      owner,
      MatchHandler(true, isMatchup = true),
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
    VimExtensionFacade.putExtensionHandlerMapping(
      EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
      injector.parser.parseKeys("<Plug>(matchup-Z%)"),
      owner,
      BlockMotionHandler(BlockMotion.INSIDE_PREVIOUS),
      false
    )

    putBlockTextObject("i%", isInner = true)
    putBlockTextObject("a%", isInner = false)

    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("<Plug>(matchup-hi-surround)"),
      owner,
      HighlightSurroundingHandler(matchParen),
      false
    )
    VimExtensionFacade.addCommand("NoMatchParen", MatchParenCommandHandler(matchParen, isEnabled = false))
    VimExtensionFacade.addCommand("DoMatchParen", MatchParenCommandHandler(matchParen, isEnabled = true))
    matchParen.attach()
  }

  override fun dispose() {
    matchParen.detach()
    super.dispose()
  }

  private fun putBlockTextObject(keys: String, isInner: Boolean) {
    val plug = injector.parser.parseKeys("<Plug>(matchup-$keys)")
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.XO, plug, owner, BlockTextObjectHandler(isInner), false)
    putKeyMappingIfMissing(MappingMode.XO, injector.parser.parseKeys(keys), owner, plug, true)
  }

  private fun putBlockMotion(keys: String, motion: BlockMotion) {
    val plug = injector.parser.parseKeys("<Plug>(matchup-$keys)")
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, plug, owner, BlockMotionHandler(motion), false)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys(keys), owner, plug, true)
  }
}
