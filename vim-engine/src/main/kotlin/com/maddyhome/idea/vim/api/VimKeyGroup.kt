/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.KeyMapping
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import javax.swing.KeyStroke

interface VimKeyGroup {
  @Suppress("DEPRECATION")
  @Deprecated("Use getBuiltinCommandTrie", ReplaceWith("getBuiltinCommandsTrie(mappingMode)"))
  fun getKeyRoot(mappingMode: MappingMode): com.maddyhome.idea.vim.key.CommandPartNode<LazyVimCommand>

  fun getBuiltinCommandsTrie(mappingMode: MappingMode): KeyStrokeTrie<LazyVimCommand>
  fun getActions(editor: VimEditor, keyStroke: KeyStroke): List<NativeAction>
  fun getKeymapConflicts(keyStroke: KeyStroke): List<NativeAction>

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  )

  fun removeKeyMapping(owner: MappingOwner)
  fun removeKeyMapping(modes: Set<MappingMode>)
  fun removeKeyMapping(modes: Set<MappingMode>, keys: List<KeyStroke>)
  fun getKeyMapping(mode: MappingMode): KeyMapping
  fun updateShortcutKeysRegistration()
  fun unregisterCommandActions()
  fun resetKeyMappings()

  /**
   * Returns true if there exists a mapping to the given left-hand side keystrokes
   *
   * Note that the Vim function `hasmapto()` can accept a set of modes, and checks if any mapping _contains_ the given
   * left-hand side mapping, rather than is a direct map. (It also handles abbreviations)
   */
  fun hasmapto(mode: MappingMode, toKeys: List<KeyStroke>): Boolean

  val shortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>
  val savedShortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>

  fun getChar(editor: VimEditor): Char?

  /** Registers a command action and its shortcut keys. */
  fun registerCommandAction(command: LazyVimCommand) {}

  /** Registers a shortcut that is handled directly by KeyHandler, rather than by an action. */
  fun registerShortcutWithoutAction(keyStroke: KeyStroke, owner: MappingOwner) {}
}

fun VimKeyGroup.getMappingInfo(keys: List<KeyStroke>, mode: MappingMode) = getKeyMapping(mode)[keys]

/**
 * Retrieve the first mapping that is an exact match for the given LHS keystrokes in the given modes
 *
 * This function is essentially equivalent to the Vim function `maparg()`.
 *
 * Typically, this function will be called with a single mode, in which case there will be a single result, or null.
 * However, for `maparg()` support, it can also be called with [MappingMode.NVO], which might have the same mapping in
 * all modes, or a different mapping in any one of the modes. This function, like `maparg()`, will return an arbitrary
 * (undocumented) mapping in this scenario.
 */
fun VimKeyGroup.getFirstMappingInfoMatch(name: List<KeyStroke>, mode: Set<MappingMode>) =
  mode.map { getMappingInfo(name, it) }.firstNotNullOfOrNull { it }

/**
 * Retrieve the first mapping that is a prefix for the given LHS keystrokes, or has those keystrokes as a prefix, in any
 * of the given modes.
 *
 * This function is essentially equivalent to the Vim function `mapcheck()`.
 *
 * There can be multiple mappings that match, but like `mapcheck()`, this function will return an arbitrary
 * (undocumented) mapping.
 */
fun VimKeyGroup.getFirstMappingInfoPrefix(name: List<KeyStroke>, mode: Set<MappingMode>) =
  mode.map { getKeyMapping(it) }.flatMap { it.getAll(name) }.map { it.mappingInfo }.firstNotNullOfOrNull { it }

data class MappingInfoWithMode(val mappingInfo: MappingInfo, val modes: Set<MappingMode>)

private typealias KeyStrokes = List<KeyStroke>
private typealias MappingModes = Set<MappingMode>

/**
 * Get all mappings for a given prefix, together with the modes the mapping is defined for
 *
 * Some map commands set a mapping for more than one mode (e.g. `:map` sets for Normal, Visual, Select, and
 * Op-pending). Vim treats this as a single mapping, and when listing all maps only lists it once, with the
 * appropriate mode indicator(s) in the first column (NVO is represented with a space char). If the lhs mapping is
 * changed or cleared for one of the modes, the existing mapping for the other modes is still a single map, just for the
 * changed set of modes. Vim displays a different indicator (e.g. `:map ...` followed by `:nunmap ...` will list
 * the remaining mapping with an "ov" indicator, and `:map ...` followed by `:sunmap ...` will use "nox").
 *
 * Other functions such as [getMappingInfo] return a single mapping for a given mode, typically so the mapping can be
 * expanded.
 *
 * If [prefix] is empty, all mappings are returned, otherwise mappings that are a prefix of the given keystroke sequence
 * or use the keystroke sequence as a prefix are returned.
 */
fun VimKeyGroup.getAllMappingInfoWithMode(prefix: List<KeyStroke>, modes: Set<MappingMode>): List<MappingInfoWithMode> {
  /**
   * Get the actual modes used by a given mapping
   *
   * The mapping can be made across multiple modes (e.g. `:map` applies to NVO). When a mapping is removed or changed
   * from one mode, the existing mapping remains, with a different set of modes. This function returns the actual modes
   * that a given mapping is currently defined for.
   */
  fun getModesForMapping(keys: KeyStrokes, originalModes: MappingModes): MappingModes {
    if (originalModes.size == 1) return originalModes
    val actualModes = noneOfEnum<MappingMode>()
    originalModes.forEach {
      val mappingInfo = getMappingInfo(keys, it)
      if (mappingInfo != null && mappingInfo.originalModes == originalModes) {
        actualModes.add(it)
      }
    }
    return actualModes
  }

  val results = mutableListOf<MappingInfoWithMode>()
  val fromKeysPool = mutableListOf<KeyStroke>()
  val multiModeMappings = mutableMapOf<KeyStrokes, MutableSet<MappingModes>>()

  modes.forEach { mode ->
    val mapping = getKeyMapping(mode)

    // Vim includes mappings for each key in the prefix, where appropriate. That is, it doesn't just all mappings that
    // are descendants of the prefix, but includes the mappings for each key in the prefix as well.
    // E.g. `foo` will include mappings for `f` and `fo`, as well as any mappings that are descendants of `foo`.
    val iterator = mapping.getAll(prefix, true).iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      val mappingInfo = entry.mappingInfo

      val originalModes = mappingInfo.originalModes
      if (originalModes.size == 1) {
        results.add(MappingInfoWithMode(mappingInfo, originalModes))
      }
      else {
        entry.collectPath(fromKeysPool)
        when (multiModeMappings[fromKeysPool]?.contains(originalModes)) {
          null, false -> {
            multiModeMappings.computeIfAbsent(fromKeysPool) { mutableSetOf() }.add(originalModes)
            val modes = getModesForMapping(fromKeysPool, originalModes)
            results.add(MappingInfoWithMode(mappingInfo, modes))
          }

          else -> {}
        }
      }
    }
  }
  results.sortWith(Comparator.comparing { it.mappingInfo })
  return results
}
