package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.TabService
import com.maddyhome.idea.vim.group.VimWindowGroup
import com.maddyhome.idea.vim.helper.VimCommandLineHelper
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.macro.VimMacro
import com.maddyhome.idea.vim.mark.VimMarkGroup
import com.maddyhome.idea.vim.options.OptionService
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.undo.VimUndoRedo
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.yank.VimYankGroup

interface VimInjector {
  // [FINISHED] Fully moved to vim-engine. Should we remove it from injector?
  val parser: VimStringParser
  // [FINISHED] Can't be fully moved to vim-engine
  val messages: VimMessages
  // [FINISHED] Fully moved to vim-engine. Only state left in the IJ
  // Let's keep the state saver as is until we'll figure out how to implement this in fleet.
  val registerGroup: VimRegisterGroup
  val registerGroupIfCreated: VimRegisterGroup?
  // [FINISHED] Can't be fully moved to vim-engine.
  // Lots of interaction with EX panel. Let's refactor it when figure out how it works in fleet.
  val processGroup: VimProcessGroup
  // [FINISHED] Can't be fully moved to vim-engine.
  // A lot of interaction with IJ.
  val application: VimApplication
  // [FINISHED] Can't be fully moved to vim-engine.
  // Getting contextes. Need to clarify how it works in fleet before refactoring.
  val executionContextManager: ExecutionContextManager
  // [FINISHED] Fully moved to vim-engine except one method that iterates with IJ.
  // Need to check how it would work in fleet before moving this method.
  val digraphGroup: VimDigraphGroup
  // [FINISHED] Fully moved to vim-engine. Should we remove it from injector?
  val vimMachine: VimMachine
  // [FINISHED] Can't be fully moved to vim-engine.
  val enabler: VimEnabler

  // TODO We should somehow state that [OptionServiceImpl] can be used from any implementation
  // [UNFINISHED] !! in progress
  val optionService: OptionService
  // [FINISHED] Can't be fully moved to vim-engine.
  val nativeActionManager: NativeActionManager
  // !! in progress
  val keyGroup: VimKeyGroup
  // Only state left in the IJ && some IJ specifics
  val markGroup: VimMarkGroup
  // !! in progress
  val visualMotionGroup: VimVisualMotionGroup
  // !! in progress
  fun commandStateFor(editor: VimEditor): CommandState
  // !! in progress
  val engineEditorHelper: EngineEditorHelper
  // !! in progress
  val editorGroup: VimEditorGroup
  // Fully moved to vim-engine. Should we remove it from injector?
  val commandGroup: VimCommandGroup
  // !! in progress
  val changeGroup: VimChangeGroup
  // Can't be fully moved to vim-engine.
  val actionExecutor: VimActionExecutor
  // Can't be fully moved to vim-engine.
  val exEntryPanel: ExEntryPanel
  // Can't be fully moved to vim-engine.
  val exOutputPanel: VimExOutputPanelService
  // Can't be fully moved to vim-engine.
  val clipboardManager: VimClipboardManager
  // Only state left in the IJ
  val historyGroup: VimHistory
  // !! in progress
  val extensionRegistrator: VimExtensionRegistrator
  // Can't be fully moved to vim-engine.
  val tabService: TabService
  // !! in progress
  val regexpService: VimRegexpService

  // !! in progress
  val searchHelper: VimSearchHelper
  // !! in progress
  val motion: VimMotionGroup
  // Can't be fully moved to vim-engine.
  val lookupManager: VimLookupManager
  // Can't be fully moved to vim-engine.
  val templateManager: VimTemplateManager
  // !! in progress
  val searchGroup: VimSearchGroup
  // Can't be fully moved to vim-engine.
  val statisticsService: VimStatistics
  // !! in progress
  val put: VimPut
  // Can't be fully moved to vim-engine.
  val window: VimWindowGroup
  // !! in progress
  val yank: VimYankGroup
  // !! in progress
  val file: VimFile
  // !! in progress
  val macro: VimMacro
  // !! in progress
  val undo: VimUndoRedo
  // !! in progress
  val commandLineHelper: VimCommandLineHelper

  // Can't be fully moved to vim-engine.
  val vimscriptExecutor: VimscriptExecutor
  // Can't be fully moved to vim-engine.
  val vimscriptParser: VimscriptParser
  // !! in progress
  val variableService: VariableService
  // !! in progress
  val functionService: VimscriptFunctionService
  // Can't be fully moved to vim-engine.
  val vimrcFileState: VimrcFileState

  /**
   * Please use vimLogger() function
   */
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger
}

lateinit var injector: VimInjector
