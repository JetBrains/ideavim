package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.mark.VimMarkGroup
import com.maddyhome.idea.vim.options.OptionService
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup

interface VimInjector {
  val parser: VimStringParser
  val messages: VimMessages
  val registerGroup: VimRegisterGroup
  val registerGroupIfCreated: VimRegisterGroup?
  val processGroup: VimProcessGroup
  val application: VimApplication
  val executionContextManager: ExecutionContextManager
  val digraphGroup: VimDigraphGroup
  val vimMachine: VimMachine
  val enabler: VimEnabler

  // TODO We should somehow state that [OptionServiceImpl] can be used from any implementation
  val optionService: OptionService
  val nativeActionManager: NativeActionManager
  val keyGroup: VimKeyGroup
  val markGroup: VimMarkGroup
  val visualMotionGroup: VimVisualMotionGroup
  fun commandStateFor(editor: VimEditor): CommandState
  val engineEditorHelper: EngineEditorHelper
  val changeGroup: VimChangeGroup
  val actionExecutor: VimActionExecutor
  val exEntryPanel: ExEntryPanel
  val clipboardManager: VimClipboardManager

  val searchHelper: VimSearchHelper
  val motion: VimMotionGroup
  val lookupManager: VimLookupManager
  val templateManager: VimTemplateManager
  val searchGroup: VimSearchGroup
  val put: VimPut

  /**
   * Please use vimLogger() function
   */
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger
}

lateinit var injector: VimInjector
