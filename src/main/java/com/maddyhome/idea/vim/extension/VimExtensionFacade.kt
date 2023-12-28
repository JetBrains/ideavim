/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.Extension
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.helper.CommandLineHelper
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.ui.ModalEntry
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Vim API facade that defines functions similar to the built-in functions and statements of the original Vim.
 *
 * See :help eval.
 *
 * @author vlan
 */
public object VimExtensionFacade {

  private val LOG = logger<VimExtensionFacade>()

  /** The 'map' command for mapping keys to handlers defined in extensions. */
  @JvmStatic
  public fun putExtensionHandlerMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  ) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, extensionHandler, recursive)
  }


  /**
   * COMPATIBILITY-LAYER: Additional method
   * Please see: https://jb.gg/zo8n0r
   */
  /** The 'map' command for mapping keys to handlers defined in extensions. */
  @JvmStatic
  public fun putExtensionHandlerMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    extensionHandler: VimExtensionHandler,
    recursive: Boolean,
  ) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, extensionHandler, recursive)
  }

  /** The 'map' command for mapping keys to other keys. */
  @JvmStatic
  public fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  ) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, toKeys, recursive)
  }

  /** The 'map' command for mapping keys to other keys if there is no other mapping to these keys */
  @JvmStatic
  public fun putKeyMappingIfMissing(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  ) {
    val filteredModes = modes.filterNotTo(HashSet()) { VimPlugin.getKey().hasmapto(it, toKeys) }
    VimPlugin.getKey().putKeyMapping(filteredModes, fromKeys, pluginOwner, toKeys, recursive)
  }

  /**
   * Equivalent to calling 'command' to set up a user-defined command or alias
   */
  public fun addCommand(
    name: String,
    handler: CommandAliasHandler,
  ) {
    addCommand(name, 0, 0, handler)
  }

  /**
   * Equivalent to calling 'command' to set up a user-defined command or alias
   */
  @JvmStatic
  public fun addCommand(
    name: String,
    minimumNumberOfArguments: Int,
    maximumNumberOfArguments: Int,
    handler: CommandAliasHandler,
  ) {
    VimPlugin.getCommand()
      .setAlias(name, CommandAlias.Call(minimumNumberOfArguments, maximumNumberOfArguments, name, handler))
  }

  /** Sets the value of 'operatorfunc' to be used as the operator function in 'g@'. */
  @JvmStatic
  public fun setOperatorFunction(function: OperatorFunction) {
    VimPlugin.getKey().operatorFunction = function
  }

  /**
   * Runs normal mode commands similar to ':normal! {commands}'.
   * Mappings doesn't work with this function
   *
   * XXX: Currently it doesn't make the editor enter the normal mode, it doesn't recover from incomplete commands, it
   * leaves the editor in the insert mode if it's been activated.
   */
  @JvmStatic
  public fun executeNormalWithoutMapping(keys: List<KeyStroke>, editor: Editor) {
    val context = injector.executionContextManager.onEditor(editor.vim)
    keys.forEach { KeyHandler.getInstance().handleKey(editor.vim, it, context, false, false) }
  }

  /** Returns a single key stroke from the user input similar to 'getchar()'. */
  @JvmStatic
  public fun inputKeyStroke(editor: Editor): KeyStroke {
    if (editor.vim.vimStateMachine.isDotRepeatInProgress) {
      val input = Extension.consumeKeystroke()
      LOG.trace("inputKeyStroke: dot repeat in progress. Input: $input")
      return input ?: error("Not enough keystrokes saved: ${Extension.lastExtensionHandler}")
    }

    val key: KeyStroke? = if (ApplicationManager.getApplication().isUnitTestMode) {
      LOG.trace("Unit test mode is active")
      val mappingStack = KeyHandler.getInstance().keyStack
      mappingStack.feedSomeStroke() ?: TestInputModel.getInstance(editor).nextKeyStroke()?.also {
        if (editor.vim.vimStateMachine.isRecording) {
          KeyHandler.getInstance().modalEntryKeys += it
        }
      }
    } else {
      LOG.trace("Getting char from the modal entry...")
      var ref: KeyStroke? = null
      ModalEntry.activate(editor.vim) { stroke: KeyStroke? ->
        ref = stroke
        false
      }
      LOG.trace("Got char $ref")
      ref
    }
    val result = key ?: KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE.toChar())
    Extension.addKeystroke(result)
    return result
  }

  /** Returns a string typed in the input box similar to 'input()'. */
  @JvmStatic
  public fun inputString(editor: Editor, prompt: String, finishOn: Char?): String {
    return service<CommandLineHelper>().inputString(editor.vim, prompt, finishOn) ?: ""
  }

  /** Get the current contents of the given register similar to 'getreg()'. */
  @JvmStatic
  public fun getRegister(register: Char): List<KeyStroke>? {
    val reg = VimPlugin.getRegister().getRegister(register) ?: return null
    return reg.keys
  }

  @JvmStatic
  public fun getRegisterForCaret(register: Char, caret: VimCaret): List<KeyStroke>? {
    val reg = caret.registerStorage.getRegister(register) ?: return null
    return reg.keys
  }

  /** Set the current contents of the given register */
  @JvmStatic
  public fun setRegister(register: Char, keys: List<KeyStroke?>?) {
    VimPlugin.getRegister().setKeys(register, keys?.filterNotNull() ?: emptyList())
  }

  /** Set the current contents of the given register */
  @JvmStatic
  public fun setRegisterForCaret(register: Char, caret: ImmutableVimCaret, keys: List<KeyStroke?>?) {
    caret.registerStorage.setKeys(register, keys?.filterNotNull() ?: emptyList())
  }

  /** Set the current contents of the given register */
  @JvmStatic
  public fun setRegister(register: Char, keys: List<KeyStroke?>?, type: SelectionType) {
    VimPlugin.getRegister().setKeys(register, keys?.filterNotNull() ?: emptyList(), type)
  }
}
