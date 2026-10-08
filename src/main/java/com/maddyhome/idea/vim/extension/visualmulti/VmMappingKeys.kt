/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.extension.multiplecursors.isGlobalFlagEnabled
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * When `0`, only the mappings with [VmMapping.alwaysMapped] are created
 */
@NlsSafe
private const val DEFAULT_MAPPINGS = "VM_default_mappings"

/**
 * Mapping names to keys, e.g. `let g:VM_maps['Find Under'] = '<C-d>'`. An empty string disables the mapping, and
 * unknown names are ignored.
 */
@NlsSafe
private const val MAPS = "VM_maps"

/**
 * @param fromUser Whether the keys come from `g:VM_maps`. vim-visual-multi maps them even if the user has mapped other
 *   keys to the `<Plug>` mapping.
 */
internal data class MappingKeys(val keys: String, val fromUser: Boolean)

/**
 * Returns the keys of the enabled mappings by name
 */
internal fun readMappingKeys(): Map<String, MappingKeys> {
  val userMaps = VimPlugin.getVariableService().getGlobalVariableValue(MAPS) as? VimDictionary
  val defaultMappings = isGlobalFlagEnabled(DEFAULT_MAPPINGS)

  return buildMap {
    for (mapping in getPermanentMappings() + getSessionMappings()) {
      val userKeys = userMaps?.get(mapping.name) as? VimString
      val keys = when {
        userKeys != null -> MappingKeys(userKeys.value, fromUser = true)
        mapping.alwaysMapped || defaultMappings -> MappingKeys(mapping.defaultKeys, fromUser = false)
        else -> null
      }
      if (keys != null && keys.keys.isNotEmpty()) {
        put(mapping.name, keys)
      }
    }
  }
}
