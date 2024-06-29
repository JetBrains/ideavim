/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.Extension
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.ui.ModalEntry
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

/**
 * Vim API facade that defines functions similar to the built-in functions and statements of the original Vim.
 *
 * See :help eval.
 *
 * @author vlan
 */
object VimExtensionFacade {

  private val LOG = logger<VimExtensionFacade>()

  /** The 'map' command for mapping keys to handlers defined in extensions. */
  @JvmStatic
  fun putExtensionHandlerMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    pluginOwner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  ) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, extensionHandler, recursive)
  }


  @JvmStatic
  @Deprecated("Use VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, extensionHandler, recursive)",
    ReplaceWith(
      "VimPlugin.getKey().putKeyMapping(modes, fromKeys, pluginOwner, extensionHandler, recursive)",
      "com.maddyhome.idea.vim.VimPlugin"
    )
  )
  fun putExtensionHandlerMapping(
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
  fun putKeyMapping(
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
  fun putKeyMappingIfMissing(
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
  fun addCommand(
    name: String,
    handler: CommandAliasHandler,
  ) {
    addCommand(name, 0, 0, handler)
  }

  /**
   * Equivalent to calling 'command' to set up a user-defined command or alias
   */
  @JvmStatic
  fun addCommand(
    name: String,
    minimumNumberOfArguments: Int,
    maximumNumberOfArguments: Int,
    handler: CommandAliasHandler,
  ) {
    VimPlugin.getCommand()
      .setAlias(name, CommandAlias.Call(minimumNumberOfArguments, maximumNumberOfArguments, name, handler))
  }

  /**
   * Runs normal mode commands similar to ':normal! {commands}'.
   * Mappings doesn't work with this function
   *
   * XXX: Currently it doesn't make the editor enter the normal mode, it doesn't recover from incomplete commands, it
   * leaves the editor in the insert mode if it's been activated.
   */
  @JvmStatic
  fun executeNormalWithoutMapping(keys: List<KeyStroke>, editor: Editor) {
    val context = injector.executionContextManager.getEditorExecutionContext(editor.vim)
    val keyHandler = KeyHandler.getInstance()
    keys.forEach { keyHandler.handleKey(editor.vim, it, context, false, false, keyHandler.keyHandlerState) }
  }

  /** Returns a single key stroke from the user input similar to 'getchar()'. */
  @JvmStatic
  fun inputKeyStroke(editor: Editor): KeyStroke {
    if (editor.vim.inRepeatMode) {
      val input = Extension.consumeKeystroke()
      LOG.trace("inputKeyStroke: dot repeat in progress. Input: $input")
      return input ?: error("Not enough keystrokes saved: ${Extension.lastExtensionHandler}")
    }

    val key: KeyStroke? = if (ApplicationManager.getApplication().isUnitTestMode) {
      LOG.trace("Unit test mode is active")
      val mappingStack = KeyHandler.getInstance().keyStack
      mappingStack.feedSomeStroke() ?: TestInputModel.getInstance(editor).nextKeyStroke()?.also {
        if (injector.registerGroup.isRecording) {
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
  fun inputString(editor: Editor, context: DataContext, prompt: String, finishOn: Char?): String {
    return injector.commandLine.inputString(editor.vim, context.vim, prompt, finishOn) ?: ""
  }

  /** Get the current contents of the given register similar to 'getreg()'. */
  @JvmStatic
  fun getRegister(register: Char): List<KeyStroke>? {
    val reg = VimPlugin.getRegister().getRegister(register) ?: return null
    return reg.keys
  }

  @JvmStatic
  fun getRegisterForCaret(register: Char, caret: VimCaret): List<KeyStroke>? {
    val reg = caret.registerStorage.getRegister(register) ?: return null
    return reg.keys
  }

  /** Set the current contents of the given register */
  @JvmStatic
  fun setRegister(register: Char, keys: List<KeyStroke?>?) {
    VimPlugin.getRegister().setKeys(register, keys?.filterNotNull() ?: emptyList())
  }

  /** Set the current contents of the given register */
  @JvmStatic
  fun setRegisterForCaret(register: Char, caret: ImmutableVimCaret, keys: List<KeyStroke?>?) {
    caret.registerStorage.setKeys(register, keys?.filterNotNull() ?: emptyList())
  }

  /** Set the current contents of the given register */
  @JvmStatic
  fun setRegister(register: Char, keys: List<KeyStroke?>?, type: SelectionType) {
    VimPlugin.getRegister().setKeys(register, keys?.filterNotNull() ?: emptyList(), type)
  }

  @JvmStatic
  fun exportScriptFunction(
    scope: Scope?,
    name: String,
    args: List<String>,
    defaultArgs: List<Pair<String, Expression>>,
    hasOptionalArguments: Boolean,
    flags: EnumSet<FunctionFlag>,
    function: ScriptFunction
  ) {
    var functionDeclaration: FunctionDeclaration? = null
    val body = listOf(object : Executable {
      // This context is set to the function declaration during initialisation and then set to the function execution
      // context during execution
      override lateinit var vimContext: VimLContext
      override var rangeInScript: TextRange = TextRange(0, 0)

      override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
        return function.execute(editor, context, functionDeclaration!!.functionVariables)
      }
    })
    functionDeclaration = FunctionDeclaration(
      scope,
      name,
      args,
      defaultArgs,
      body,
      replaceExisting = true,
      flags,
      hasOptionalArguments
    )
    functionDeclaration.rangeInScript = TextRange(0, 0)
    body.forEach { it.vimContext = functionDeclaration }
    injector.functionService.storeFunction(functionDeclaration)
  }
}

fun VimExtensionFacade.exportOperatorFunction(name: String, function: OperatorFunction) {
  exportScriptFunction(null, name, listOf("type"), emptyList(), false, noneOfEnum()) {
    editor, context, args ->

    val type = args["type"]?.asString()
    val selectionType = when (type) {
      "line" -> SelectionType.LINE_WISE
      "block" -> SelectionType.BLOCK_WISE
      "char" -> SelectionType.CHARACTER_WISE
      else -> return@exportScriptFunction ExecutionResult.Error
    }

    if (function.apply(editor, context, selectionType)) {
      ExecutionResult.Success
    }
    else {
      ExecutionResult.Error
    }
  }
}

fun interface ScriptFunction {
  fun execute(editor: VimEditor, context: ExecutionContext, args: Map<String, VimDataType>): ExecutionResult
}