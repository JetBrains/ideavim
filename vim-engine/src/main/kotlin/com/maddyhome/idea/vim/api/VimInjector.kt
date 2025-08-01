/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.VimListenersNotifier
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.group.TabService
import com.maddyhome.idea.vim.group.VimWindowGroup
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.macro.VimMacro
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.thinapi.VimHighlightingService
import com.maddyhome.idea.vim.thinapi.VimPluginService
import com.maddyhome.idea.vim.undo.VimUndoRedo
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.yank.VimYankGroup
import org.jetbrains.annotations.ApiStatus

interface VimInjector {
  val vimState: VimStateMachine

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
  val fallbackWindow: VimEditor

  val parser: VimStringParser

  val messages: VimMessages

  val registerGroup: VimRegisterGroup
  val registerGroupIfCreated: VimRegisterGroup?

  val processGroup: VimProcessGroup

  val application: VimApplication

  val executionContextManager: ExecutionContextManager

  val digraphGroup: VimDigraphGroup

  val enabler: VimEnabler

  val optionGroup: VimOptionGroup

  val nativeActionManager: NativeActionManager

  val keyGroup: VimKeyGroup

  val markService: VimMarkService

  val jumpService: VimJumpService

  val visualMotionGroup: VimVisualMotionGroup

  val engineEditorHelper: EngineEditorHelper

  val editorGroup: VimEditorGroup

  val commandGroup: VimCommandGroup

  val changeGroup: VimChangeGroup

  val actionExecutor: VimActionExecutor

  val clipboardManager: VimClipboardManager

  val historyGroup: VimHistory

  val extensionRegistrator: VimExtensionRegistrator

  val tabService: TabService

  val regexpService: VimRegexpService

  val searchHelper: VimSearchHelper

  val motion: VimMotionGroup
  val scroll: VimScrollGroup

  val lookupManager: VimLookupManager

  val templateManager: VimTemplateManager

  val searchGroup: VimSearchGroup

  val statisticsService: VimStatistics

  val put: VimPut

  val window: VimWindowGroup

  val yank: VimYankGroup

  val file: VimFile

  val macro: VimMacro

  val undo: VimUndoRedo

  val psiService: VimPsiService

  val vimscriptExecutor: VimscriptExecutor

  val vimscriptParser: VimscriptParser

  val variableService: VariableService

  val modalInput: VimModalInputService
  val commandLine: VimCommandLineService
  val outputPanel: VimOutputPanelService

  val functionService: VimscriptFunctionService

  val vimrcFileState: VimrcFileState

  val systemInfoService: SystemInfoService
  val vimStorageService: VimStorageService

  /**
   * Please use vimLogger() function
   */
  fun <T : Any> getLogger(clazz: Class<T>): VimLogger

  val listenersNotifier: VimListenersNotifier

  val redrawService: VimRedrawService

  val pluginService: VimPluginService

  val highlightingService: VimHighlightingService
}

lateinit var injector: VimInjector

@ApiStatus.Internal
fun isInjectorInitialized(): Boolean {
  return ::injector.isInitialized
}

/**
 * Gets an API for consuming only global options
 *
 * This function is intended to retrieve global options, not global public values for options that are local to buffer, local
 * to window or global-local - for that, use [OptionService.getOptionpublic value]. Typical option access should use
 * [VimInjector.options] and pass in a [VimEditor] for context. This will return local or global public values as
 * appropriate.
 */
fun VimInjector.globalOptions(): GlobalOptions = this.optionGroup.getGlobalOptions()

/**
 * Gets an API for consuming all options
 *
 * This is the preferred means of accessing options as it will return the effective public value for the current window or
 * buffer context
 * If an editor isn't available to the calling code, the [globalOptions] function can be used to access global
 * options. It should not be used to access options that are local to buffer, local to window or global-local.
 */
fun VimInjector.options(editor: VimEditor): EffectiveOptions = this.optionGroup.getEffectiveOptions(editor)
