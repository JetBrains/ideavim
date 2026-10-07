/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtensionBase
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener

@NlsSafe
private const val FIND_UNDER = "<Plug>(VM-Find-Under)"

@NlsSafe
private const val FIND_SUBWORD_UNDER = "<Plug>(VM-Find-Subword-Under)"

@NlsSafe
private const val SELECT_ALL = "<Plug>(VM-Select-All)"

@NlsSafe
private const val VISUAL_ALL = "<Plug>(VM-Visual-All)"

@NlsSafe
private const val SKIP_REGION = "<Plug>(VM-Skip-Region)"

@NlsSafe
private const val REMOVE_REGION = "<Plug>(VM-Remove-Region)"

/**
 * When set to `0`, only the `<C-n>` mappings are created, the same as `g:VM_default_mappings` in vim-visual-multi.
 */
@NlsSafe
private const val DEFAULT_MAPPINGS = "VM_default_mappings"

/**
 * A dictionary of mapping name to key, the same as `g:VM_maps` in vim-visual-multi. E.g.
 * `let g:VM_maps['Find Under'] = '<C-d>'`. An empty string disables the mapping, unknown names are ignored.
 */
@NlsSafe
private const val MAPS = "VM_maps"

/**
 * The default `g:VM_leader` of vim-visual-multi
 */
@NlsSafe
private const val LEADER = "<Bslash><Bslash>"

/**
 * A permanent mapping of vim-visual-multi, i.e. one that is available before a multi-cursor session starts
 *
 * @param name The name used in `g:VM_maps`
 * @param alwaysMapped Whether the default key is mapped even if `g:VM_default_mappings` is `0`
 */
private class PermanentMapping(
  val name: String,
  val modes: Set<MappingMode>,
  val plug: String,
  val defaultKeys: String,
  val alwaysMapped: Boolean = false,
)

private val permanentMappings = listOf(
  PermanentMapping("Find Under", MappingMode.N, FIND_UNDER, "<C-n>", alwaysMapped = true),
  PermanentMapping("Find Subword Under", MappingMode.X, FIND_SUBWORD_UNDER, "<C-n>", alwaysMapped = true),
  PermanentMapping("Select All", MappingMode.N, SELECT_ALL, "${LEADER}A"),
  PermanentMapping("Visual All", MappingMode.X, VISUAL_ALL, "${LEADER}A"),
)

/**
 * The keys a permanent mapping is mapped to
 *
 * @param fromUser Whether the keys come from `g:VM_maps`. Such keys are mapped even if the user has already mapped
 *   something else to the `<Plug>` mapping, the same as vim-visual-multi does.
 */
private data class MappingKeys(val keys: String, val fromUser: Boolean)

/**
 * Emulation of vim-visual-multi, built on the same native carets as [multiple-cursors][com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtension].
 *
 * See https://github.com/mg979/vim-visual-multi
 */
internal class VimVisualMultiExtension : VimMultipleCursorsExtensionBase() {

  /**
   * The keys of the permanent mappings, as they were registered last time. `null` if no mappings are registered.
   */
  private var registeredKeys: Map<String, MappingKeys>? = null

  /**
   * Reloads the mappings every time an editor gets focus, so the changed `g:VM_maps` is applied without restarting the
   * IDE. E.g. after `:let g:VM_maps['Find Under'] = '<C-d>'` or reloading the ideavimrc file.
   */
  internal val focusListener = PropertyChangeListener { event ->
    if (event.newValue is EditorComponentImpl) {
      reloadMappings()
    }
  }

  override fun getName() = "visual-multi"

  override fun init() {
    registerMappings(readMappingKeys())
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    registeredKeys = null
    super.dispose()
  }

  /**
   * Re-reads `g:VM_maps` and `g:VM_default_mappings`, and registers the mappings again if they have changed
   */
  internal fun reloadMappings() {
    if (registeredKeys == null) return

    val keys = readMappingKeys()
    if (keys == registeredKeys) return

    injector.keyGroup.removeKeyMapping(owner)
    registerMappings(keys)
  }

  private fun registerMappings(keys: Map<String, MappingKeys>) {
    putPlugMapping(MappingMode.N, FIND_UNDER, NextOccurrenceHandler())
    putPlugMapping(MappingMode.X, FIND_SUBWORD_UNDER, NextOccurrenceHandler(whole = false))
    putPlugMapping(MappingMode.N, SELECT_ALL, AllOccurrencesHandler())
    putPlugMapping(MappingMode.X, VISUAL_ALL, AllOccurrencesHandler(whole = false))
    putPlugMapping(MappingMode.X, SKIP_REGION, SkipOccurrenceHandler())
    putPlugMapping(MappingMode.X, REMOVE_REGION, RemoveOccurrenceHandler())

    for (mapping in permanentMappings) {
      val mappingKeys = keys[mapping.name] ?: continue
      if (mappingKeys.fromUser) {
        val parser = injector.parser
        putKeyMapping(mapping.modes, parser.parseKeys(mappingKeys.keys), owner, parser.parseKeys(mapping.plug), true)
      } else {
        putDefaultMapping(mapping.modes, mappingKeys.keys, mapping.plug)
      }
    }
    registeredKeys = keys
  }

  /**
   * Returns the keys of each permanent mapping by name, taking `g:VM_maps` and `g:VM_default_mappings` into account.
   * Disabled mappings are not included.
   */
  private fun readMappingKeys(): Map<String, MappingKeys> {
    val userMaps = VimPlugin.getVariableService().getGlobalVariableValue(MAPS) as? VimDictionary
    val defaultMappings = isFlagEnabled(DEFAULT_MAPPINGS)

    return buildMap {
      for (mapping in permanentMappings) {
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
}
