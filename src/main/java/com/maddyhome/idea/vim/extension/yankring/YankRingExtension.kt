/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.common.VimRegisterListener
import com.maddyhome.idea.vim.helper.EngineStringHelper.toPrintableCharacters
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.thinapi.toTextSelectionType

/**
 * A port of YankRing.vim (VIM-301): a history of yanks, deletes and changes, plus the ability to
 * cycle the text of the last paste through that history with `<C-P>` / `<C-N>`.
 *
 * See VIM-301-yankring-roadmap.md for the behaviours still to fill in, in order.
 */
internal const val PLUGIN_NAME: String = "YankRing"

@VimPlugin(name = PLUGIN_NAME)
fun VimInitApi.init() {
  // Registered straight on the notifier rather than through `listeners { onRegisterStore { ... } }`,
  // because `VimApi.listeners` is currently commented out and the scope is unreachable. Tagging the
  // listener with our plugin owner keeps `disableExtension` able to unload it. Move this over once
  // the listeners scope is enabled again.
  injector.listenersNotifier.registerListeners.add(YankRingRecorder)

  commands {
    register("YRShow") { _, _, _ ->
      showYankRing()
    }
    register("YRClear") { _, _, _ ->
      clearYankRing()
    }
  }
}

/**
 * Feeds every yank, delete and change into the ring.
 */
private object YankRingRecorder : VimRegisterListener {
  override val owner: ListenerOwner = ListenerOwner.Plugin.get(PLUGIN_NAME)

  override fun registerStored(
    register: Char,
    copiedText: VimCopiedText,
    type: SelectionType,
    isDelete: Boolean,
  ) {
    YankRing.record(copiedText.text, type.toTextSelectionType())
  }
}

/**
 * Renders the ring the way `s:YRShow` does when `g:yankring_window_use_separate` is off: a banner,
 * a column header, then one line per entry. The separate window comes later.
 */
internal suspend fun VimApi.showYankRing() {
  val lines = mutableListOf(BANNER, "$ELEM_HEADER  Content")
  YankRing.entries().forEachIndexed { index, entry ->
    lines += displayElement(index + 1, entry)
  }

  outputPanel {
    setText(lines.joinToString("\n"))
  }
}

/**
 * `:YRClear` - empties the ring.
 */
@Suppress("unused") // Receiver keeps every command handler in the extension consistent
internal suspend fun clearYankRing() {
  YankRing.clear()
}

/**
 * Formats one entry the way `s:YRDisplayElem` does: the element number left-aligned in a column as
 * wide as the `Elem` header, then the content with newlines shown as a literal `\n`.
 */
private fun displayElement(number: Int, entry: YankRingEntry): String {
  val content = toPrintableCharacters(injector.parser.stringToKeys(entry.text.replace("\n", "\\n")))
  return number.toString().padEnd(ELEM_HEADER.length + 2) + content
}

private const val BANNER = "--- YankRing ---"
private const val ELEM_HEADER = "Elem"
