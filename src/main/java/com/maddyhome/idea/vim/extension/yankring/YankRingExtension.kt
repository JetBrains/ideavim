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
      register("YRShow") { _, _, _ -> showYankRing() }
      register("YRClear") { _, _, _ -> YankRing.clear() }
    }

    initApi.mappings {
      nmapPluginAction(PREVIOUS_KEY, REPLACE_PREVIOUS, keepDefaultMapping = true) {
        replaceLastPaste(Direction.PREVIOUS)
      }
      nmapPluginAction(NEXT_KEY, REPLACE_NEXT, keepDefaultMapping = true) {
        replaceLastPaste(Direction.NEXT)
      }
      nmapPluginAction(PASTE_AFTER_KEY, PASTE_AFTER, keepDefaultMapping = true) {
        pasteAndRemember(PASTE_AFTER_KEY)
      }
      nmapPluginAction(PASTE_BEFORE_KEY, PASTE_BEFORE, keepDefaultMapping = true) {
        pasteAndRemember(PASTE_BEFORE_KEY)
      }
    }
  }

  override fun dispose() {
    injector.listenersNotifier.registerListeners.remove(YankRingRecorder)
    injector.keyGroup.removeKeyMapping(owner)
    LastPaste.clear()
  }
}

private const val PREVIOUS_KEY = "<C-P>"
private const val NEXT_KEY = "<C-N>"
private const val REPLACE_PREVIOUS = "<Plug>YankRingReplacePrevious"
private const val REPLACE_NEXT = "<Plug>YankRingReplaceNext"

// YankRing maps the paste keys too (`g:yankring_paste_n_akey` / `_bkey`), because a paste is what
// makes `<C-P>` meaningful - it has to know what was inserted and where.
private const val PASTE_AFTER_KEY = "p"
private const val PASTE_BEFORE_KEY = "P"
private const val PASTE_AFTER = "<Plug>YankRingPasteAfter"
private const val PASTE_BEFORE = "<Plug>YankRingPasteBefore"
