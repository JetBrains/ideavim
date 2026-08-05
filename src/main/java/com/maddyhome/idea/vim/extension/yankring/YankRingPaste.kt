/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.models.TextType
import com.intellij.vim.api.scopes.editor.ReadScope
import com.maddyhome.idea.vim.api.injector

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
 * Runs a real paste and records where it put its text, so that it can be replaced afterwards.
 */
internal suspend fun VimApi.pasteAndRemember(pasteCommand: String) {
  val count = count1()
  val registerPrefix = requestedRegisterPrefix()
  val positionBefore = caretPosition()

  normal("$registerPrefix$count$pasteCommand")

  val pasted = editor { read { pastedText() } }
  if (pasted == null) {
    LastPaste.clear()
    return
  }
  rememberPaste(pasteCommand, YankRing.cycleStartIndex(pasted), positionBefore)
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
  replaceLastPaste(direction.step * count1())
}

/**
 * `:YRReplace {offset} {pastecommand}` - the entry point the plugin's own key mappings use:
 *
 * ```vim
 * nnoremap <silent> <C-P> :<C-U>YRReplace '-1', P<CR>
 * ```
 *
 * The offset counts against YankRing's oldest-first history, the opposite of [Direction.step].
 * `<f-args>` hands `s:YRPaste` the raw `'-1',` and it digs the number out with
 * `matchstr(a:nextvalue, '-\?\d\+')`, so quotes and the trailing comma have to be tolerated here
 * too - anyone copying that mapping out of the plugin's docs will pass them.
 *
 * The second argument is **accepted and ignored**. YankRing re-pastes with whatever the mapping
 * passed rather than with the key the user originally pasted with, because after its `normal! u`
 * the caret happens to sit where `P` reproduces a `p`. We put the caret back deliberately instead,
 * and repeat the paste command that was actually used, which is both simpler and correct for
 * mappings the plugin never anticipated. The argument stays in the signature so that the
 * documented mappings keep working.
 */
internal suspend fun VimApi.yankRingReplace(commandText: String) {
  val offset = OFFSET.find(commandText.removePrefix(YR_REPLACE))?.value?.toIntOrNull()
  if (offset == null) {
    injector.messages.showStatusBarMessage(null, MESSAGE_MISSING_OFFSET)
    injector.messages.indicateError()
    return
  }

  replaceLastPaste(-offset)
}

private suspend fun VimApi.replaceLastPaste(steps: Int) {
  val entries = YankRing.entries()
  val pending = pendingPaste()

  if (pending == null || entries.isEmpty()) {
    injector.messages.showStatusBarMessage(null, MESSAGE_PASTE_FIRST)
    injector.messages.indicateError()
    return
  }

  // Walking off either end wraps around, so that holding <C-P> keeps cycling.
  val nextIndex = Math.floorMod(pending.ringIndex + steps, entries.size)

  undoAndRepaste(entries[nextIndex], pending.pasteCommand, pending.positionBeforePaste)
  rememberPaste(pending.pasteCommand, nextIndex, pending.positionBeforePaste)
}

/**
 * Undoes the previous paste and pastes [entry] over it, exactly as `s:YRReplace` does. This is not
 * just imitation: re-pasting applies the entry's own character/line-wise semantics, and undoing
 * first keeps the undo stack holding a single paste however long the user keeps cycling, so one
 * `u` still returns to the state before the paste.
 */
private suspend fun VimApi.undoAndRepaste(
  entry: YankRingEntry,
  pasteCommand: String,
  positionBeforePaste: LastPaste.Position,
) {
  val saved = readUnnamedRegister()
  writeUnnamedRegister(RegisterContents(entry.text, entry.type))

  // The count that selected the entry is still pending in the key handler, and `normal` feeds keys
  // back through that same state - leaving it would apply the count to the undo and re-enter this
  // mapping. YankRing clears it with the `:<C-U>` in its own mapping; <Esc> is our equivalent.
  normal("<Esc>u")

  // Put the caret back where the original paste started from. Undo does not reliably restore it,
  // and `p` / `P` insert relative to the caret, so without this a replacement can land inside a
  // neighbouring word. Line and column rather than a character offset, because an empty final line
  // sits at an offset that neither `go` nor the caret API will accept.
  normal("${positionBeforePaste.line + 1}G")
  normal("0")
  if (positionBeforePaste.column > 0) normal("${positionBeforePaste.column}l")

  normal(pasteCommand)

  saved?.let { writeUnnamedRegister(it) }
}

/**
 * The text the last paste inserted, or null when it inserted nothing we can find again.
 */
private fun ReadScope.pastedText(): String? {
  val marks = withPrimaryCaret { changeMarks } as? Range.Simple ?: return null
  return text.substring(marks.start, marks.end.coerceAtMost(text.length))
}

/**
 * The keys that put the register the user asked for back in front of the paste.
 *
 * The paste keys are mapped, so the register is no longer part of the key sequence by the time we
 * get here - it has to be read back out of `v:register`.
 */
private fun VimApi.requestedRegisterPrefix(): String {
  val register = getVariable<String>("v:register").orEmpty()
  if (register.isEmpty() || register == UNNAMED_REGISTER.toString()) return ""
  return "\"$register"
}

private fun VimApi.count1(): Int = getVariable<Int>("v:count1") ?: 1

private data class RegisterContents(val text: String, val type: TextType)

private suspend fun VimApi.readUnnamedRegister(): RegisterContents? =
  editor {
    read {
      withPrimaryCaret {
        val text = getReg(UNNAMED_REGISTER) ?: return@withPrimaryCaret null
        RegisterContents(text, getRegType(UNNAMED_REGISTER) ?: TextType.CHARACTER_WISE)
      }
    }
  }

private suspend fun VimApi.writeUnnamedRegister(contents: RegisterContents) {
  editor { read { withPrimaryCaret { setReg(UNNAMED_REGISTER, contents.text, contents.type) } } }
}

private const val UNNAMED_REGISTER = '"'

internal const val YR_REPLACE = "YRReplace"

/** Matches the offset the way `matchstr(a:nextvalue, '-\?\d\+')` does, quotes and commas and all. */
private val OFFSET = Regex("-?\\d+")

/** Word for word what `s:YRWarningMsg` reports in the same situation. */
private const val MESSAGE_PASTE_FIRST = "YR: You must paste text first, before you can replace"

/**
 * Not a message the plugin has: `s:YRPaste` simply blows up on a missing argument, because its
 * command declares `-nargs=*` but the function does not treat the argument as optional.
 */
private const val MESSAGE_MISSING_OFFSET = "YR: YRReplace needs an offset, for example :YRReplace -1 P"
