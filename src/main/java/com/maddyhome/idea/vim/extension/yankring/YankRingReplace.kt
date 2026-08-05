/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.api.injector

internal const val YR_REPLACE = "YRReplace"

/**
 * Which way round the ring `<C-P>` and `<C-N>` walk.
 *
 * [step] is expressed against [YankRing.entries], which is newest first, so stepping to an older
 * entry means moving *up* an index. YankRing passes the opposite signs to `:YRReplace` (`'-1', P`
 * for previous and `'1', p` for next) because its history list is stored oldest first.
 */
internal enum class Direction(val step: Int) {
  PREVIOUS(1),
  NEXT(-1),
}

/**
 * Replaces the text inserted by the last paste with another entry from the ring.
 *
 * These keys are `k` and `j` in plain Vim, but the mapping deliberately takes them over completely:
 * replacing sometimes has to decline - there is nothing pasted, the buffer has been edited, or the
 * caret has left the pasted line - and moving the caret in those cases looks like the plugin
 * silently did the wrong thing. `s:YRReplace` refuses the same way, with the same message.
 */
internal suspend fun VimApi.replaceLastPaste(direction: Direction) {
  cycleLastPaste(direction.step * typedCount())
}

/**
 * `:YRReplace {offset} {pastecommand}` - the entry point the plugin's own key mappings use:
 *
 * ```vim
 * nnoremap <silent> <C-P> :<C-U>YRReplace '-1', P<CR>
 * ```
 *
 * The second argument is **accepted and ignored**. YankRing re-pastes with whatever the mapping
 * passed rather than with the key the user originally pasted with, because after its `normal! u`
 * the caret happens to sit where `P` reproduces a `p`. We put the caret back deliberately instead,
 * and repeat the paste command that was actually used, which is both simpler and correct for
 * mappings the plugin never anticipated. The argument stays in the signature so that the
 * documented mappings keep working.
 */
internal suspend fun VimApi.yankRingReplace(commandText: String) {
  val offset = offsetArgumentOf(commandText)
  if (offset == null) {
    reportError(MESSAGE_MISSING_OFFSET)
    return
  }

  cycleLastPaste(-offset)
}

/**
 * The offset argument counts against YankRing's oldest-first history, the opposite of
 * [Direction.step].
 *
 * `<f-args>` hands `s:YRPaste` the raw `'-1',` and it digs the number out with
 * `matchstr(a:nextvalue, '-\?\d\+')`, so quotes and the trailing comma have to be tolerated here
 * too - anyone copying that mapping out of the plugin's docs will pass them.
 */
private fun offsetArgumentOf(commandText: String): Int? =
  OFFSET.find(commandText.removePrefix(YR_REPLACE))?.value?.toIntOrNull()

private suspend fun VimApi.cycleLastPaste(steps: Int) {
  val entries = YankRing.entries()
  val pending = pendingPaste()

  if (pending == null || entries.isEmpty()) {
    reportError(MESSAGE_PASTE_FIRST)
    return
  }

  // Walking off either end wraps around, so that holding <C-P> keeps cycling.
  val nextIndex = Math.floorMod(pending.ringIndex + steps, entries.size)

  undoAndRepaste(entries[nextIndex], pending.paste)
  rememberPaste(pending.paste, nextIndex)
}

/**
 * Undoes the previous paste and pastes [entry] over it, exactly as `s:YRReplace` does. This is not
 * just imitation: re-pasting applies the entry's own character/line-wise semantics, and undoing
 * first keeps the undo stack holding a single paste however long the user keeps cycling, so one
 * `u` still returns to the state before the paste.
 */
private suspend fun VimApi.undoAndRepaste(entry: YankRingEntry, paste: Paste) {
  withUnnamedRegister(RegisterContents(entry.text, entry.type)) {
    // The count that selected the entry is still pending in the key handler, and `normal` feeds keys
    // back through that same state - leaving it would apply the count to the undo and re-enter this
    // mapping. YankRing clears it with the `:<C-U>` in its own mapping; <Esc> is our equivalent.
    normal("<Esc>u")

    restoreCaretFor(paste)

    normal("${paste.count}${paste.command}")
  }
}

/**
 * Puts the caret back where the paste started from, because `p` / `P` insert relative to it and
 * undo does not reliably restore it - without this a replacement can land inside a neighbouring
 * word.
 */
private suspend fun VimApi.restoreCaretFor(paste: Paste) {
  if (paste.fromVisual) {
    // The selection the paste consumed is gone, but its marks survive, so `gv` brings it back -
    // the same trick `s:YRReplace` uses for a paste made from visual mode.
    normal("gv")
    return
  }

  // Line and column rather than a character offset, because an empty final line sits at an offset
  // that neither `go` nor the caret API will accept.
  normal("${paste.startedFrom.line + 1}G")
  normal("0")
  if (paste.startedFrom.column > 0) normal("${paste.startedFrom.column}l")
}

private fun reportError(message: String) {
  injector.messages.showStatusBarMessage(null, message)
  injector.messages.indicateError()
}

/** Matches the offset the way `matchstr(a:nextvalue, '-\?\d\+')` does, quotes and commas and all. */
private val OFFSET = Regex("-?\\d+")

/** Word for word what `s:YRWarningMsg` reports in the same situation. */
private const val MESSAGE_PASTE_FIRST = "YR: You must paste text first, before you can replace"

/**
 * Not a message the plugin has: `s:YRPaste` simply blows up on a missing argument, because its
 * command declares `-nargs=*` but the function does not treat the argument as optional.
 */
private const val MESSAGE_MISSING_OFFSET = "YR: YRReplace needs an offset, for example :YRReplace -1 P"
