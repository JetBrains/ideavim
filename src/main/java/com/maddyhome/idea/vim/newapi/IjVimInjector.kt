/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.api.EngineEditorHelper
import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.LocalOptionInitialisationScenario
import com.maddyhome.idea.vim.api.NativeActionManager
import com.maddyhome.idea.vim.api.SystemInfoService
import com.maddyhome.idea.vim.api.VimActionExecutor
import com.maddyhome.idea.vim.api.VimApplication
import com.maddyhome.idea.vim.api.VimChangeGroup
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.api.VimCommandGroup
import com.maddyhome.idea.vim.api.VimCommandLineService
import com.maddyhome.idea.vim.api.VimDigraphGroup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorGroup
import com.maddyhome.idea.vim.api.VimEnabler
import com.maddyhome.idea.vim.api.VimExOutputPanel
import com.maddyhome.idea.vim.api.VimExOutputPanelService
import com.maddyhome.idea.vim.api.VimExtensionRegistrator
import com.maddyhome.idea.vim.api.VimFile
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.VimInjectorBase
import com.maddyhome.idea.vim.api.VimJumpService
import com.maddyhome.idea.vim.api.VimKeyGroup
import com.maddyhome.idea.vim.api.VimLookupManager
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.VimMessages
import com.maddyhome.idea.vim.api.VimMotionGroup
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.VimProcessGroup
import com.maddyhome.idea.vim.api.VimPsiService
import com.maddyhome.idea.vim.api.VimRedrawService
import com.maddyhome.idea.vim.api.VimRegexServiceBase
import com.maddyhome.idea.vim.api.VimRegexpService
import com.maddyhome.idea.vim.api.VimScrollGroup
import com.maddyhome.idea.vim.api.VimSearchGroup
import com.maddyhome.idea.vim.api.VimSearchHelper
import com.maddyhome.idea.vim.api.VimStatistics
import com.maddyhome.idea.vim.api.VimStorageService
import com.maddyhome.idea.vim.api.VimStringParser
import com.maddyhome.idea.vim.api.VimTemplateManager
import com.maddyhome.idea.vim.api.VimVisualMotionGroup
import com.maddyhome.idea.vim.api.VimrcFileState
import com.maddyhome.idea.vim.api.VimscriptExecutor
import com.maddyhome.idea.vim.api.VimscriptFunctionService
import com.maddyhome.idea.vim.api.VimscriptParser
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar
import com.maddyhome.idea.vim.group.CommandGroup
import com.maddyhome.idea.vim.group.EditorGroup
import com.maddyhome.idea.vim.group.EffectiveIjOptions
import com.maddyhome.idea.vim.group.FileGroup
import com.maddyhome.idea.vim.group.GlobalIjOptions
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.group.IjVimOptionGroup
import com.maddyhome.idea.vim.group.IjVimPsiService
import com.maddyhome.idea.vim.group.MacroGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.TabService
import com.maddyhome.idea.vim.group.VimWindowGroup
import com.maddyhome.idea.vim.group.WindowGroup
import com.maddyhome.idea.vim.group.copy.PutGroup
import com.maddyhome.idea.vim.helper.IjActionExecutor
import com.maddyhome.idea.vim.helper.IjEditorHelper
import com.maddyhome.idea.vim.helper.IjVimStringParser
import com.maddyhome.idea.vim.helper.UndoRedoHelper
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.macro.VimMacro
import com.maddyhome.idea.vim.put.VimPut
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.ui.VimRcFileState
import com.maddyhome.idea.vim.undo.VimUndoRedo
import com.maddyhome.idea.vim.vimscript.Executor
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.yank.VimYankGroup
import com.maddyhome.idea.vim.yank.YankGroupBase
import javax.swing.JTextArea

internal class IjVimInjector : VimInjectorBase() {
  override fun <T : Any> getLogger(clazz: Class<T>): VimLogger = IjVimLogger(Logger.getInstance(clazz))

  override val fallbackWindow: VimEditor by lazy {
    TextComponentEditorImpl(null, JTextArea()).vim.also {
      optionGroup.initialiseLocalOptions(it, null, LocalOptionInitialisationScenario.DEFAULTS)
    }
  }

  override val actionExecutor: VimActionExecutor
    get() = service<IjActionExecutor>()
  override val exOutputPanel: VimExOutputPanelService
    get() = object : VimExOutputPanelService {
      override fun getPanel(editor: VimEditor): VimExOutputPanel {
        return ExOutputModel.getInstance(editor.ij)
      }
    }
  override val historyGroup: VimHistory
    get() = service<HistoryGroup>()
  override val extensionRegistrator: VimExtensionRegistrator
    get() = VimExtensionRegistrar
  override val tabService: TabService
    get() = service()
  override val regexpService: VimRegexpService
    get() = VimRegexServiceBase()
  override val clipboardManager: VimClipboardManager
    get() = service<IjClipboardManager>()
  override val searchHelper: VimSearchHelper
    get() = service<IjVimSearchHelper>()
  override val motion: VimMotionGroup
    get() = service<MotionGroup>()
  override val scroll: VimScrollGroup
    get() = service()
  override val lookupManager: VimLookupManager
    get() = service<IjVimLookupManager>()
  override val templateManager: VimTemplateManager
    get() = service<IjTemplateManager>()
  override val searchGroup: VimSearchGroup
    get() = service<IjVimSearchGroup>()
  override val put: VimPut
    get() = service<PutGroup>()
  override val window: VimWindowGroup
    get() = service<WindowGroup>()
  override val yank: VimYankGroup
    get() = service<YankGroupBase>()
  override val file: VimFile
    get() = service<FileGroup>()
  override val macro: VimMacro
    get() = service<MacroGroup>()
  override val undo: VimUndoRedo
    get() = service<UndoRedoHelper>()
  override val psiService: VimPsiService
    get() = service<IjVimPsiService>()
  override val nativeActionManager: NativeActionManager
    get() = service<IjNativeActionManager>()
  override val messages: VimMessages
    get() = service<IjVimMessages>()
  override val registerGroup: VimRegisterGroup
    get() = service()
  override val registerGroupIfCreated: VimRegisterGroup?
    get() = serviceIfCreated()
  override val changeGroup: VimChangeGroup
    get() = service()
  override val processGroup: VimProcessGroup
    get() = service()
  override val keyGroup: VimKeyGroup
    get() = service()

  override val markService: VimMarkService
    get() = service()
  override val jumpService: VimJumpService
    get() = service()
  override val application: VimApplication
    get() = service<IjVimApplication>()
  override val executionContextManager: ExecutionContextManager
    get() = service<IjExecutionContextManager>()
  override val enabler: VimEnabler
    get() = service<IjVimEnabler>()
  override val digraphGroup: VimDigraphGroup
    get() = service()
  override val visualMotionGroup: VimVisualMotionGroup
    get() = service()
  override val statisticsService: VimStatistics
    get() = service()
  override val commandGroup: VimCommandGroup
    get() = service<CommandGroup>()

  override val functionService: VimscriptFunctionService
    get() = service()
  override val variableService: VariableService
    get() = service()
  override val vimrcFileState: VimrcFileState
    get() = VimRcFileState
  override val vimscriptExecutor: VimscriptExecutor
    get() = service<Executor>()
  override val vimscriptParser: VimscriptParser
    get() = com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
  override val commandLine: VimCommandLineService
    get() = service()

  override val optionGroup: VimOptionGroup
    get() = service()
  override val parser: VimStringParser
    get() = service<IjVimStringParser>()

  override val systemInfoService: SystemInfoService
    get() = service()
  override val vimStorageService: VimStorageService
    get() = service()
  override val redrawService: VimRedrawService
    get() = service()

  @Deprecated("Please use VimInjector.vimState", replaceWith = ReplaceWith("vimState"))
  override fun commandStateFor(editor: VimEditor): VimStateMachine {
    return vimState
  }

  @Deprecated("Please use VimInjector.vimState", replaceWith = ReplaceWith("vimState"))
  override fun commandStateFor(editor: Any): VimStateMachine {
    return vimState
  }

  override val engineEditorHelper: EngineEditorHelper
    get() = service<IjEditorHelper>()
  override val editorGroup: VimEditorGroup
    get() = service<EditorGroup>()
}

/**
 * Convenience function to get the IntelliJ implementation specific global option accessor
 */
fun VimInjector.globalIjOptions(): GlobalIjOptions = (this.optionGroup as IjVimOptionGroup).getGlobalIjOptions()

/**
 * Convenience function to get the IntelliJ implementation specific option accessor for the given editor's scope
 */
fun VimInjector.ijOptions(editor: VimEditor): EffectiveIjOptions =
  (this.optionGroup as IjVimOptionGroup).getEffectiveIjOptions(editor)
