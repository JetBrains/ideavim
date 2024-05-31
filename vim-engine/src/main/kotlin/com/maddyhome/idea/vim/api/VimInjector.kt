/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.VimListenersNotifier
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.TabService
import com.maddyhome.idea.vim.group.VimWindowGroup
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.macro.VimMacro
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.undo.VimUndoRedo
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.yank.VimYankGroup

public interface VimInjector {
  /**
   * The window used when we need a window but there are no editor windows available.
   *
   * This is primarily used to capture state for local options, either at startup, or when all editor windows are
   * closed.
   *
   * Vim always has at least one buffer and window. During startup, Vim will evaluate the appropriate `vimrc` files, and
   * any local or global-local options are set against this initial buffer and window. IdeaVim does not always have an
   * open buffer or window, so we create a hidden window, with a private buffer that can be used when evaluating the
   * `~/.ideavimrc` file, and updated with the last set local options of the current window. This window (and buffer) is
   * then used to initialise the local options of the first window that is subsequently opened or initialised.
   */
  public val fallbackWindow: VimEditor

  // [FINISHED] Fully moved to vim-engine. Should we remove it from injector?
  public val parser: VimStringParser

  // [FINISHED] Can't be fully moved to vim-engine
  public val messages: VimMessages

  // [FINISHED] Fully moved to vim-engine. Only state left in the IJ
  // Let's keep the state saver as is until we'll figure out how to implement this in fleet.
  public val registerGroup: VimRegisterGroup
  public val registerGroupIfCreated: VimRegisterGroup?

  // [FINISHED] Can't be fully moved to vim-engine.
  // Lots of interaction with EX panel. Let's refactor it when figure out how it works in fleet.
  public val processGroup: VimProcessGroup

  // [FINISHED] Can't be fully moved to vim-engine.
  // A lot of interaction with IJ.
  public val application: VimApplication

  // [FINISHED] Can't be fully moved to vim-engine.
  // Getting contextes. Need to clarify how it works in fleet before refactoring.
  public val executionContextManager: ExecutionContextManager

  // [FINISHED] Fully moved to vim-engine except one method that iterates with IJ.
  // Need to check how it would work in fleet before moving this method.
  public val digraphGroup: VimDigraphGroup

  // [FINISHED] Can't be fully moved to vim-engine.
  public val enabler: VimEnabler

  // [FINISHED] Fully moved to vim-engine. IJ service implementation adds additional IJ specific options
  // (These could be moved out of the implementation and into initialisation code)
  public val optionGroup: VimOptionGroup

  // [FINISHED] Can't be fully moved to vim-engine.
  public val nativeActionManager: NativeActionManager

  // [FINISHED] Can't be fully moved to vim-engine.
  public val keyGroup: VimKeyGroup

  // [FINISHED] Only state left in the IJ && some IJ specifics
  public val markService: VimMarkService

  public val jumpService: VimJumpService

  // [FINISHED] Only IJ staff left
  public val visualMotionGroup: VimVisualMotionGroup

  // [FINISHED] Class moved to vim-engine, but it's attached to Editor using IJ things
  public fun commandStateFor(editor: VimEditor): VimStateMachine
  // [FINISHED] Class moved to vim-engine, but it's attached to Editor using IJ things
  /**
   * COMPATIBILITY-LAYER: Added new method with Any
   * Please see: https://jb.gg/zo8n0r
   */
  public fun commandStateFor(editor: Any): VimStateMachine

  // !! in progress
  public val engineEditorHelper: EngineEditorHelper

  // [FINISHED] Only IJ staff
  public val editorGroup: VimEditorGroup

  // [FINISHED] Fully moved to vim-engine. Should we remove it from injector?
  public val commandGroup: VimCommandGroup

  // !! in progress
  public val changeGroup: VimChangeGroup

  // Can't be fully moved to vim-engine.
  public val actionExecutor: VimActionExecutor

  // Can't be fully moved to vim-engine.
  public val exOutputPanel: VimExOutputPanelService

  // Can't be fully moved to vim-engine.
  public val clipboardManager: VimClipboardManager

  // Only state left in the IJ
  public val historyGroup: VimHistory

  // !! in progress
  public val extensionRegistrator: VimExtensionRegistrator

  // Can't be fully moved to vim-engine.
  public val tabService: TabService

  // !! in progress
  public val regexpService: VimRegexpService

  // !! in progress
  public val searchHelper: VimSearchHelper

  // !! in progress
  public val motion: VimMotionGroup
  public val scroll: VimScrollGroup

  // Can't be fully moved to vim-engine.
  public val lookupManager: VimLookupManager

  // Can't be fully moved to vim-engine.
  public val templateManager: VimTemplateManager

  // !! in progress
  public val searchGroup: VimSearchGroup

  // Can't be fully moved to vim-engine.
  public val statisticsService: VimStatistics

  // !! in progress
  public val put: VimPut

  // Can't be fully moved to vim-engine.
  public val window: VimWindowGroup

  // !! in progress
  public val yank: VimYankGroup

  // !! in progress
  public val file: VimFile

  // !! in progress
  public val macro: VimMacro

  // !! in progress
  public val undo: VimUndoRedo

  public val psiService: VimPsiService

  // Can't be fully moved to vim-engine.
  public val vimscriptExecutor: VimscriptExecutor

  // Can't be fully moved to vim-engine.
  public val vimscriptParser: VimscriptParser

  // !! in progress
  public val variableService: VariableService

  public val commandLine: VimCommandLineService

  // !! in progress
  public val functionService: VimscriptFunctionService

  // Can't be fully moved to vim-engine.
  public val vimrcFileState: VimrcFileState

  public val systemInfoService: SystemInfoService
  public val vimStorageService: VimStorageService

  /**
   * Please use vimLogger() function
   */
  public fun <T : Any> getLogger(clazz: Class<T>): VimLogger
  
  public val listenersNotifier: VimListenersNotifier

  public val redrawService: VimRedrawService
}

public lateinit var injector: VimInjector

/**
 * Gets an API for consuming only global options
 *
 * This function is intended to retrieve global options, not global public values for options that are local to buffer, local
 * to window or global-local - for that, use [OptionService.getOptionpublic value]. Typical option access should use
 * [VimInjector.options] and pass in a [VimEditor] for context. This will return local or global public values as
 * appropriate.
 */
public fun VimInjector.globalOptions(): GlobalOptions = this.optionGroup.getGlobalOptions()

/**
 * Gets an API for consuming all options
 *
 * This is the preferred means of accessing options as it will return the effective public value for the current window or
 * buffer context
 * If an editor isn't available to the calling code, the [globalOptions] function can be used to access global
 * options. It should not be used to access options that are local to buffer, local to window or global-local.
 */
public fun VimInjector.options(editor: VimEditor): EffectiveOptions = this.optionGroup.getEffectiveOptions(editor)
