/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * Runs vim's `/` (or `?`) over the variant alternation for [keys] and moves
 * the primary caret to the first match. Shared between `:S /pat/` and
 * `:Abolish -search pat`.
 */
internal fun runVariantSearch(editor: VimEditor, keys: Set<String>, direction: Direction) {
  if (keys.isEmpty()) return
  val pattern = buildVimAlternationPattern(keys)
  val caret = editor.primaryCaret()
  injector.searchGroup.processSearchCommand(editor, pattern, caret.offset, 1, direction)
    ?.let { (offset, _) -> caret.moveToOffset(offset) }
}

/**
 * Runs vim's `:substitute` over [dictionary] keys, using the dictionary itself
 * as the replacement lookup (via `g:abolish_last_dict` + `\=get(...)` — the
 * same trick tpope uses in `s:commands.substitute.process`).
 */
internal fun runVariantSubstitution(
  editor: VimEditor,
  context: ExecutionContext,
  dictionary: Map<String, String>,
  rangePrefix: String,
  flags: String,
) {
  if (dictionary.isEmpty()) return
  storeAsLastDict(dictionary)
  val pattern = buildVimAlternationPattern(dictionary.keys)
  val command = "${rangePrefix}s/$pattern/\\=get(g:abolish_last_dict, submatch(0), submatch(0))/$flags"
  injector.vimscriptExecutor.execute(command, editor, context, skipHistory = true, indicateErrors = true, null)
}

private fun storeAsLastDict(dictionary: Map<String, String>) {
  val entries = LinkedHashMap<VimString, VimDataType>()
  dictionary.forEach { (key, value) -> entries[VimString(key)] = VimString(value) }
  VimPlugin.getVariableService().storeGlobalVariable("abolish_last_dict", VimDictionary(entries))
}
