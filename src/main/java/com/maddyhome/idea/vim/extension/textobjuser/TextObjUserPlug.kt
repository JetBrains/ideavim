/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * Builds the interface `<Plug>` name for an operation, mirroring vim-textobj-user's `s:interface_mapping_name`:
 * `<Plug>(textobj-{plugin}-{object}-{suffix})`, where the leading `move`/`select` word of the spec name is dropped
 * (so `select-a` → `a`, `move-n` → `n`, bare `select` → empty). Repeated dashes are collapsed and a trailing dash is
 * trimmed, which also handles an object named `-`.
 */
internal fun plugName(plugin: String, obj: String, specName: String): String {
  val suffix = specName.replaceFirst(Regex("^(move|select)"), "")
  return "<Plug>(textobj-$plugin-$obj-$suffix)"
    .replace(Regex("-+"), "-")
    .replace(Regex("-(?=\\)$)"), "")
}

/**
 * Maps every user [keys] to the `<Plug>` interface name [plug], leaving any key the user has already bound untouched.
 *
 * The "already bound" check is keyed on the left-hand side (the user's key), not the target: several keys may map to
 * the same `<Plug>` name (e.g. both `ad` and `id` select a date), and `textobj#user#map` adds further keys to a name
 * that is already a mapping target.
 */
internal fun mapUserKeys(owner: MappingOwner, keys: List<String>, plug: String, modes: Set<MappingMode>) {
  val plugKeys = injector.parser.parseKeys(plug)
  for (key in keys) {
    val fromKeys = injector.parser.parseKeys(key)
    val freeModes = modes.filterTo(mutableSetOf()) { injector.keyGroup.getKeyMapping(it)[fromKeys] == null }
    if (freeModes.isNotEmpty()) {
      putKeyMapping(freeModes, fromKeys, owner, plugKeys, true)
    }
  }
}

/**
 * Flattens a vim-textobj-user field that accepts either a single string or a list of strings (`pattern`, `select`, ...)
 * into a list. Returns an empty list when the field is absent.
 */
internal fun VimDataType?.toStringList(): List<String> = when (this) {
  is VimString -> listOf(value)
  is VimList -> values.map { (it as VimString).value }
  else -> emptyList()
}
