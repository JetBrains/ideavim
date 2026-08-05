/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.scopes.nmapPluginAction
import com.intellij.vim.api.scopes.xmapPluginAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension

internal const val PLUGIN_NAME: String = "yankring"

/**
 * A port of YankRing.vim (VIM-301): a history of yanks, deletes and changes, plus the ability to
 * cycle the text of the last paste through that history with `<C-P>` / `<C-N>`.
 *
 * Enabled with `set yankring` in `~/.ideavimrc`.
 *
 * See VIM-301-yankring-roadmap.md for the behaviours still to fill in, in order.
 */
internal class YankRingExtension : VimExtension {

  override fun getName(): String = PLUGIN_NAME

  override fun init(initApi: VimInitApi) {
    // Registered straight on the notifier rather than through `listeners { onRegisterStore { ... } }`,
    // because `VimApi.listeners` is currently commented out and the scope is unreachable. Move this
    // over once the listeners scope is enabled again.
    injector.listenersNotifier.registerListeners.add(YankRingRecorder)

    initApi.commands {
      register(YR_SHOW) { _, _, _ -> showYankRing() }
      register(YR_CLEAR) { _, _, _ -> YankRing.clear() }
      register(YR_REPLACE) { commandText, _, _ -> yankRingReplace(commandText) }
    }

    initApi.mappings {
      nmapPluginAction(PREVIOUS_KEY, REPLACE_PREVIOUS, keepDefaultMapping = true) {
        replaceLastPaste(Direction.PREVIOUS)
      }
      nmapPluginAction(NEXT_KEY, REPLACE_NEXT, keepDefaultMapping = true) {
        replaceLastPaste(Direction.NEXT)
      }
      PASTE_KEYS.forEach { (key, plugSuffix) ->
        nmapPluginAction(key, "<Plug>YankRingPasteNormal$plugSuffix", keepDefaultMapping = true) {
          pasteAndRemember(key)
        }
        xmapPluginAction(key, "<Plug>YankRingPasteVisual$plugSuffix", keepDefaultMapping = true) {
          pasteAndRemember(key, fromVisual = true)
        }
      }
    }
  }

  /**
   * Called on `set noyankring`. The default only drops the mappings, so everything else the
   * extension registered has to be undone here - commands included, or `:YRShow` keeps working
   * after the user has turned the plugin off.
   *
   * The ring itself survives. Disabling is not the same as forgetting a session's yank history, and
   * the plugin's own runtime switch (`g:yankring_enabled`, E3) does not clear it either.
   */
  override fun dispose() {
    injector.listenersNotifier.registerListeners.remove(YankRingRecorder)
    injector.keyGroup.removeKeyMapping(owner)
    COMMANDS.forEach(injector.commandGroup::removeAlias)
    LastPaste.clear()
  }
}

internal const val YR_SHOW = "YRShow"
internal const val YR_CLEAR = "YRClear"

private val COMMANDS = listOf(YR_SHOW, YR_CLEAR, YR_REPLACE)

private const val PREVIOUS_KEY = "<C-P>"
private const val NEXT_KEY = "<C-N>"
private const val REPLACE_PREVIOUS = "<Plug>YankRingReplacePrevious"
private const val REPLACE_NEXT = "<Plug>YankRingReplaceNext"

// A paste is what makes <C-P> meaningful, so the ring has to see every one of them; anything not
// listed here is invisible to it and <C-P> afterwards reports there is nothing to replace.
//
// `s:YRMapsCreate` maps `p` and `P` in normal and visual mode (`g:yankring_paste_n_akey` / `_bkey`
// and their `_v_` twins) plus `gp` and `gP`, the latter two only when `g:yankring_paste_using_g`
// is set - which we do not honour yet, so ours are unconditional. The bracket variants are **our
// addition**: the plugin leaves them unmapped, which means it silently cannot cycle after them.
//
// Each key is paired with the way it is spelled out in its `<Plug>` name, which cannot contain
// punctuation.
private val PASTE_KEYS: Map<String, String> = linkedMapOf(
  "p" to "After",
  "P" to "Before",
  "gp" to "GAfter",
  "gP" to "GBefore",
  "]p" to "BracketCloseAfter",
  "[p" to "BracketAfter",
  "]P" to "BracketCloseBefore",
  "[P" to "BracketBefore",
)
