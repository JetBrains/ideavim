/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getMappingInfo
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtensionBase
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.userData
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener
import javax.swing.KeyStroke

@NlsSafe
private const val FIND_UNDER = "<Plug>(VM-Find-Under)"

@NlsSafe
private const val FIND_SUBWORD_UNDER = "<Plug>(VM-Find-Subword-Under)"

@NlsSafe
private const val SELECT_ALL = "<Plug>(VM-Select-All)"

@NlsSafe
private const val VISUAL_ALL = "<Plug>(VM-Visual-All)"

@NlsSafe
private const val FIND_NEXT = "<Plug>(VM-Find-Next)"

@NlsSafe
private const val SKIP_REGION = "<Plug>(VM-Skip-Region)"

@NlsSafe
private const val REMOVE_REGION = "<Plug>(VM-Remove-Region)"

@NlsSafe
private const val EXIT = "<Plug>(VM-Exit)"

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
 * How occurrences are matched, the same as `g:VM_case_setting` in vim-visual-multi: `'smart'`, `'sensitive'` or
 * `'ignore'`. Any other value, including the default empty string, follows 'ignorecase' and 'smartcase'.
 */
@NlsSafe
private const val CASE_SETTING = "VM_case_setting"

/**
 * The default `g:VM_leader` of vim-visual-multi
 */
@NlsSafe
private const val LEADER = "<Bslash><Bslash>"

/**
 * A mapping of vim-visual-multi that can be configured with `g:VM_maps`
 *
 * @param name The name used in `g:VM_maps`
 * @param alwaysMapped Whether the default key is mapped even if `g:VM_default_mappings` is `0`
 * @param createHandler Creates the handler of the [plug] mapping
 */
private class VmMapping(
  val name: String,
  val modes: Set<MappingMode>,
  val plug: String,
  val defaultKeys: String,
  val alwaysMapped: Boolean = false,
  val createHandler: VimMultipleCursorsExtensionBase.() -> ExtensionHandler,
)

/**
 * The mappings that are available before a multi-cursor session starts. They start the session.
 */
private val permanentMappings = listOf(
  VmMapping("Find Under", MappingMode.N, FIND_UNDER, "<C-n>", alwaysMapped = true) { NextOccurrenceHandler() },
  VmMapping("Find Subword Under", MappingMode.X, FIND_SUBWORD_UNDER, "<C-n>", alwaysMapped = true) {
    NextOccurrenceHandler(whole = false)
  },
  VmMapping("Select All", MappingMode.N, SELECT_ALL, "${LEADER}A") { AllOccurrencesHandler() },
  VmMapping("Visual All", MappingMode.X, VISUAL_ALL, "${LEADER}A") { AllOccurrencesHandler(whole = false) },
)

/**
 * The mappings that are only active inside a multi-cursor session (see [isSessionActive]). Outside the session, the
 * keys keep their Vim meaning.
 *
 * vim-visual-multi maps them regardless of `g:VM_default_mappings`. They are Normal mode mappings there, because
 * vim-visual-multi stays in Normal mode and draws the selections itself. The selections are native, so for us they are
 * Visual mode mappings.
 */
private val sessionMappings = listOf(
  VmMapping("Find Next", MappingMode.X, FIND_NEXT, "n", alwaysMapped = true) { NextOccurrenceHandler(whole = false) },
  VmMapping("Skip Region", MappingMode.X, SKIP_REGION, "q", alwaysMapped = true) { SkipOccurrenceHandler() },
  VmMapping("Remove Region", MappingMode.X, REMOVE_REGION, "Q", alwaysMapped = true) {
    VimMultipleCursorsExtensionBase.RemoveOccurrenceHandler()
  },
  // Only in Visual mode for now. In Normal mode, <Esc> is partly handled by the IDE, which we shouldn't break
  VmMapping("Exit", MappingMode.X, EXIT, "<Esc>", alwaysMapped = true) { ExitHandler() },
)

/**
 * Whether a multi-cursor session is active in the editor, the same as `b:visual_multi` in vim-visual-multi.
 *
 * The session is started by the [permanentMappings], and ended by Exit or when the last region is removed.
 */
private var Editor.vimVisualMultiSession: Boolean? by userData()

/**
 * vim-visual-multi keeps its own regions, while ours are the native carets and their selections. A session without
 * them doesn't make sense, e.g. when the IDE has removed the secondary carets, so the session is checked here too.
 */
private fun isSessionActive(editor: VimEditor): Boolean =
  editor.ij.vimVisualMultiSession == true && (editor.inVisualMode || editor.carets().size > 1)

/**
 * Starts the session if the command has created a region. Note that [VimEditor.carets] doesn't count the carets of
 * Visual block mode, which is not a session.
 */
private fun startSessionIfRegionsCreated(editor: VimEditor) {
  if (editor.inVisualMode || editor.carets().size > 1) {
    editor.ij.vimVisualMultiSession = true
  }
}

private fun endSession(editor: VimEditor) {
  editor.ij.vimVisualMultiSession = null
}

/**
 * Runs [handler], and starts the session if it has created a region
 */
private class StartSessionHandler(private val handler: ExtensionHandler) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    handler.execute(editor, context, operatorArguments)
    startSessionIfRegionsCreated(editor)
  }
}

/**
 * Exits the session, keeping only the cursor at the primary caret, the same as `<Esc>` in vim-visual-multi
 */
private class ExitHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    endSession(editor)
    editor.removeSecondaryCarets()
    editor.exitVisualMode()
  }
}

/**
 * Ends the session when the mode changes and there is at most one cursor left. E.g. after removing the last region,
 * leaving Visual mode with `v`, or after Insert mode with a single cursor.
 *
 * vim-visual-multi would keep the session with a single cursor in Normal mode until it is exited. Without its
 * highlighting, such a session would be invisible, and keys like `n` would unexpectedly behave differently.
 */
private object SessionModeListener : ModeChangeListener {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    if (editor.carets().size <= 1) {
      endSession(editor)
    }
  }
}

/**
 * Runs [handler] inside a multi-cursor session. Outside the session, the [keys] are executed without mappings, so they
 * keep their Vim meaning, e.g. `n` still extends the Visual selection to the next match.
 */
private class SessionHandler(private val handler: ExtensionHandler, private val keys: List<KeyStroke>) :
  ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    if (isSessionActive(editor)) {
      handler.execute(editor, context, operatorArguments)
    } else {
      val count = operatorArguments.count0
      val countKeys = if (count > 0) injector.parser.stringToKeys(count.toString()) else emptyList()
      executeNormalWithoutMapping(countKeys + keys, editor.ij)
    }
  }
}

/**
 * The keys a mapping is mapped to
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
   * The keys of the mappings, as they were registered last time. `null` if no mappings are registered.
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

  /**
   * vim-visual-multi sets 'ignorecase' and 'smartcase' according to `g:VM_case_setting` for the duration of the
   * session, and searches the occurrences with them. We don't change the options of the user, but resolve them the
   * same way here, and the search pattern then forces the case.
   */
  override fun ignoreCase(text: String): Boolean {
    val setting = VimPlugin.getVariableService().getGlobalVariableValue(CASE_SETTING) as? VimString
    val (ignoreCase, smartCase) = when (setting?.value?.lowercase()) {
      "smart" -> true to true
      "sensitive" -> false to false
      "ignore" -> true to false
      else -> injector.globalOptions().let { it.ignorecase to it.smartcase }
    }
    return ignoreCase && !(smartCase && text.any { it.isUpperCase() })
  }

  override fun init() {
    registerMappings(readMappingKeys())
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
    injector.listenersNotifier.modeChangeListeners.add(SessionModeListener)
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    injector.listenersNotifier.modeChangeListeners.remove(SessionModeListener)
    injector.editorGroup.getEditors().forEach(::endSession)
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
    for (mapping in permanentMappings) {
      putPlugMapping(mapping.modes, mapping.plug, StartSessionHandler(mapping.createHandler(this)))
    }
    for (mapping in sessionMappings) {
      putPlugMapping(mapping.modes, mapping.plug, mapping.createHandler(this))
    }

    for (mapping in permanentMappings) {
      val mappingKeys = keys[mapping.name] ?: continue
      if (mappingKeys.fromUser) {
        val parser = injector.parser
        putKeyMapping(mapping.modes, parser.parseKeys(mappingKeys.keys), owner, parser.parseKeys(mapping.plug), true)
      } else {
        putDefaultMapping(mapping.modes, mappingKeys.keys, mapping.plug)
      }
    }

    for (mapping in sessionMappings) {
      val mappingKeys = keys[mapping.name] ?: continue
      putSessionMapping(mapping, injector.parser.parseKeys(mappingKeys.keys))
    }
    registeredKeys = keys
  }

  /**
   * Maps [keys] directly to the handler, rather than to the `<Plug>` mapping, so the handler knows which keys to run
   * outside the session.
   *
   * vim-visual-multi doesn't override the existing buffer mappings with its session mappings. Similarly, we don't
   * override the existing mappings of the user: their mapping wins, and the session mapping is not available.
   */
  private fun putSessionMapping(mapping: VmMapping, keys: List<KeyStroke>) {
    val modes = mapping.modes.filterTo(mutableSetOf()) { injector.keyGroup.getMappingInfo(keys, it) == null }
    if (modes.isEmpty()) return
    putExtensionHandlerMapping(modes, keys, owner, SessionHandler(mapping.createHandler(this), keys), false)
  }

  /**
   * Returns the keys of each mapping by name, taking `g:VM_maps` and `g:VM_default_mappings` into account.
   * Disabled mappings are not included.
   */
  private fun readMappingKeys(): Map<String, MappingKeys> {
    val userMaps = VimPlugin.getVariableService().getGlobalVariableValue(MAPS) as? VimDictionary
    val defaultMappings = isFlagEnabled(DEFAULT_MAPPINGS)

    return buildMap {
      for (mapping in permanentMappings + sessionMappings) {
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
