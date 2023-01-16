/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.TabService
import com.maddyhome.idea.vim.group.VimWindowGroup
import com.maddyhome.idea.vim.helper.VimCommandLineHelper
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.macro.VimMacro
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.undo.VimUndoRedo
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.yank.VimYankGroup
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval

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

  // [UNFINISHED] !! in progress
  val optionService: OptionService
  // [FINISHED] Can't be fully moved to vim-engine.
  val nativeActionManager: NativeActionManager
  // [FINISHED] Can't be fully moved to vim-engine.
  val keyGroup: VimKeyGroup
  // [FINISHED] Only state left in the IJ && some IJ specifics
  val markService: VimMarkService

  val jumpService: VimJumpService

  // [FINISHED] Only IJ staff left
  val visualMotionGroup: VimVisualMotionGroup
  // [FINISHED] Class moved to vim-engine, but it's attached to Editor using IJ things
  fun commandStateFor(editor: VimEditor): VimStateMachine
  // [FINISHED] Class moved to vim-engine, but it's attached to Editor using IJ things
  /**
   * COMPATIBILITY-LAYER: Added new method with Any
   * Please see: https://jb.gg/zo8n0r
   */
  fun commandStateFor(editor: Any): VimStateMachine
  // !! in progress
  val engineEditorHelper: EngineEditorHelper
  // [FINISHED] Only IJ staff
  val editorGroup: VimEditorGroup
  // [FINISHED] Fully moved to vim-engine. Should we remove it from injector?
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
  val scroll: VimScrollGroup
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

  val systemInfoService: SystemInfoService
  val vimStorageService: VimStorageService

  /**
   * Please use vimLogger() function
   */
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger
}

lateinit var injector: VimInjector

/**
 * Gets an API for consuming only global options
 *
 * This function is intended to retrieve global options, not global values for options that are local to buffer, local
 * to window or global-local - for that, use [OptionService.getOptionValue]. Typical option access should use
 * [VimInjector.options] and pass in a [VimEditor] for context. This will return local or global values as
 * appropriate.
 */
fun VimInjector.globalOptions() = this.optionService.getValueAccessor(null)

/**
 * Gets an API for consuming all options
 *
 * This is the preferred means of accessing options as it will return the effective value for the current window or
 * buffer context
 * If an editor isn't available to the calling code, the [globalOptions] function can be used to access global
 * options. It should not be used to access options that are local to buffer, local to window or global-local.
 */
fun VimInjector.options(editor: VimEditor) = this.optionService.getValueAccessor(editor)
