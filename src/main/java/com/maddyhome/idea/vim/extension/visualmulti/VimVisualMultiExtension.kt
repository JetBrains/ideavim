/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.getMappingInfo
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtensionBase
import javax.swing.KeyStroke

/**
 * Emulation of vim-visual-multi
 *
 * See https://github.com/mg979/vim-visual-multi
 */
internal class VimVisualMultiExtension : VimMultipleCursorsExtensionBase() {

  /**
   * `null` while the extension is disabled
   */
  private var registeredKeys: Map<String, MappingKeys>? = null

  /**
   * Applies the changed `g:VM_maps` without restarting the IDE, e.g. after reloading the ideavimrc. NERDTree does the
   * same.
   */
  internal val focusListener = EditorFocusListener(::reloadMappings)

  override fun getName() = "visual-multi"

  override fun init() {
    registerMappings(readMappingKeys())
    focusListener.install()
    injector.listenersNotifier.modeChangeListeners.add(VmSession.EndOnModeChange)
  }

  override fun dispose() {
    focusListener.uninstall()
    injector.listenersNotifier.modeChangeListeners.remove(VmSession.EndOnModeChange)
    injector.editorGroup.getEditors().forEach(VmSession::end)
    registeredKeys = null
    super.dispose()
  }

  internal fun reloadMappings() {
    if (registeredKeys == null) return

    val keys = readMappingKeys()
    if (keys == registeredKeys) return

    injector.keyGroup.removeKeyMapping(owner)
    registerMappings(keys)
  }

  private fun registerMappings(keys: Map<String, MappingKeys>) {
    registerPlugMappings()
    registerPermanentKeys(keys)
    registerSessionKeys(keys)
    registeredKeys = keys
  }

  private fun registerPlugMappings() {
    for (mapping in getPermanentMappings()) {
      putPlugMapping(mapping.modes, mapping.plug, StartSessionHandler(mapping.createHandler()))
    }
    for (mapping in getSessionMappings()) {
      putPlugMapping(mapping.modes, mapping.plug, mapping.createHandler())
    }
  }

  private fun registerPermanentKeys(keys: Map<String, MappingKeys>) {
    for (mapping in getPermanentMappings()) {
      val mappingKeys = keys[mapping.name] ?: continue
      if (mappingKeys.fromUser) {
        putMapping(mapping.modes, mappingKeys.keys, mapping.plug)
      } else {
        putDefaultMapping(mapping.modes, mappingKeys.keys, mapping.plug)
      }
    }
  }

  /**
   * The keys are mapped to the handler rather than to the `<Plug>` mapping, so the handler knows what to do outside the
   * session.
   *
   * vim-visual-multi doesn't override existing buffer mappings. We don't override the mappings of the user either.
   */
  private fun registerSessionKeys(keys: Map<String, MappingKeys>) {
    for (mapping in getSessionMappings()) {
      val keysText = keys[mapping.name]?.keys ?: continue
      val fromKeys = injector.parser.parseKeys(keysText)
      for (mode in mapping.modes) {
        val outsideSession = outsideSessionHandler(keys, keysText, mode, fromKeys) ?: continue
        val handler = SessionOnlyHandler(mapping.createHandler(), outsideSession)
        putExtensionHandlerMapping(setOf(mode), fromKeys, owner, handler, false)
      }
    }
  }

  /**
   * Usually the keys keep their Vim meaning outside the session. When a permanent mapping uses the same keys, e.g.
   * `\\a` for Visual Add and Align, it is the permanent mapping. Returns `null` for the keys mapped by the user.
   */
  private fun outsideSessionHandler(
    keys: Map<String, MappingKeys>,
    keysText: String,
    mode: MappingMode,
    fromKeys: List<KeyStroke>,
  ): ExtensionHandler? {
    val permanent = getPermanentMappings().firstOrNull { mode in it.modes && keys[it.name]?.keys == keysText }
    return when {
      permanent != null -> StartSessionHandler(permanent.createHandler())
      injector.keyGroup.getMappingInfo(fromKeys, mode) == null -> NativeKeysHandler(fromKeys)
      else -> null
    }
  }
}
