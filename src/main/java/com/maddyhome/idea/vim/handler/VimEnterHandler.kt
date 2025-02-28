/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.codeInsight.editorActions.AutoHardWrapHandler
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.formatting.LineWrappingUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.SplitLineAction
import com.intellij.openapi.editor.impl.CaretModelImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.removeUserData
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isPrimaryEditor
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.ide.isClionNova
import com.maddyhome.idea.vim.ide.isRider
import com.maddyhome.idea.vim.newapi.actionStartedFromVim
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

internal val commandContinuation = Key.create<EditorActionHandler>("commandContinuation")

/**
 * Handler that corrects the shape of the caret in python notebooks.
 *
 * By default, py notebooks show a thin caret after entering the cell.
 *   However, we're in normal mode, so this handler fixes it.
 */
internal class CaretShapeEnterEditorHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (VimPlugin.isEnabled() && enableOctopus) {
      invokeLater {
        editor.updateCaretsVisualAttributes()
      }
    }
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }
}

/**
 * This handler doesn't work in tests for ex commands
 *
 * About this handler: VIM-2974
 */
internal abstract class OctopusHandler(private val nextHandler: EditorActionHandler?) : EditorActionHandler() {

  abstract fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?)
  open fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    return true
  }

  final override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (isThisHandlerEnabled(editor, caret, dataContext)) {
      val executeInInvokeLater = executeInInvokeLater(editor)
      val executionHandler = {
        try {
          (dataContext as? UserDataHolder)?.putUserData(commandContinuation, nextHandler)
          executeHandler(editor, caret, dataContext)
        } finally {
          (dataContext as? UserDataHolder)?.removeUserData(commandContinuation)
        }
      }

      if (executeInInvokeLater) {
        // This `invokeLater` is used to escape the potential `runForEachCaret` function.
        //
        // The `runForEachCaret` function is disallowed to be called recursively. However, with this new handler, we lose
        //   control if we execute the code inside this function or not. See IDEA-300030 for details.
        // This means the code in IdeaVim MUST NOT call `runForEachCaret` function. While this is possible for most cases,
        //   the user may make a mapping to some intellij action where the `runForEachCaret` is called. This breaks
        //   the condition (see VIM-3103 for example).
        // Since we can't make sure we don't execute `runForEachCaret`, we have to "escape" out of this function. This is
        //   done by scheduling the execution of our code later via the invokeLater function.
        //
        // We run this job only once for a primary caret. In the handler itself, we'll multiply the execution by the
        //   number of carets. If we run this job for each caret, we may end up in the issue like VIM-3186.
        //   However, I think that we may do some refactoring to run this job for each caret (if needed).
        //
        // For the moment, the known case when the caret is null - work in injected editor - VIM-3195
        if (caret == null || caret == editor.caretModel.primaryCaret) {
          ApplicationManager.getApplication().invokeLater(executionHandler)
        }
      } else {
        executionHandler()
      }
    } else {
      nextHandler?.execute(editor, caret, dataContext)
    }
  }

  private fun executeInInvokeLater(editor: Editor): Boolean {
    // Currently we have a workaround for the PY console VIM-3157
    val fileName = FileDocumentManager.getInstance().getFile(editor.document)?.name
    if (
      fileName == "Python Console.py" || // This is the name in 232+
      fileName == "Python Console" // This is the name in 231
    ) return false
    return (editor.caretModel as? CaretModelImpl)?.isIteratingOverCarets ?: true
  }

  private fun isThisHandlerEnabled(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean {
    if (VimPlugin.isNotEnabled()) return false
    if (!isHandlerEnabled(editor, dataContext)) return false
    if (isNotActualKeyPress(dataContext)) return false
    if (!enableOctopus) return false
    return true
  }

  /**
   * In some cases IJ runs handlers to imitate "enter" or other key. In such cases we should not process it on the
   *   IdeaVim side because the user may have mappings on enter the we'll get an unexpected behaviour.
   * This method should return true if we detect that this handler is called in such case and this is not an
   *   actual keypress from the user.
   */
  private fun isNotActualKeyPress(dataContext: DataContext?): Boolean {
    if (dataContext != null) {
      // This flag is set when the enter handlers are executed as a part of moving the comment on the new line
      val dataManager = DataManager.getInstance()
      if (dataManager.loadFromDataContext(dataContext, AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY) == true) {
        return true
      }

      // From VIM-3177
      val wrapLongLineDuringFormattingInProgress = dataManager
        .loadFromDataContext(dataContext, LineWrappingUtil.WRAP_LONG_LINE_DURING_FORMATTING_IN_PROGRESS_KEY)
      if (wrapLongLineDuringFormattingInProgress == true) {
        return true
      }

      // From VIM-3203
      val splitLineInProgress = dataManager.loadFromDataContext(dataContext, SplitLineAction.SPLIT_LINE_KEY)
      if (splitLineInProgress == true) {
        return true
      }

      if (dataManager.loadFromDataContext(dataContext, StartNewLineDetectorBase.Util.key) == true) {
        return true
      }
    }

    if (dataContext?.actionStartedFromVim == true) return true

    return false
  }

  final override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return isThisHandlerEnabled(editor, caret, dataContext)
      || nextHandler?.isEnabled(editor, caret, dataContext) == true
  }
}

/**
 * Known conflicts & solutions:
 * - Smart step into - set handler after
 * - Python notebooks - set handler after
 * - Ace jump - set handler after
 * - Lookup - doesn't intersect with enter anymore
 * - App code - set handler after
 * - Template - doesn't intersect with enter anymore
 * - rd.client.editor.enter - set handler before. Otherwise, rider will add new line on enter even in normal mode
 * - inline.completion.enter - set handler before. Otherwise, AI completion is not invoked on enter.
 *
 * This rule is disabled due to VIM-3124
 * - before terminalEnter - not necessary, but terminalEnter causes "file is read-only" tooltip for readonly files VIM-3122
 * - `first` is set to satisfy sorting condition "before terminalEnter".
 *
 *
 * DO NOT add handlers that force to add "first" ordering. This doesn't work with jupyterCommandModeEnterKeyHandler (see VIM-3124)
 */
internal class VimEnterHandler(nextHandler: EditorActionHandler?) : VimKeyHandler(nextHandler) {
  override val key: String = "<CR>"

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    if (!super.isHandlerEnabled(editor, dataContext)) return false
    // This is important for one-line editors, to turn off enter.
    // Some one-line editors rely on the fact that there are no enter actions registered. For example, hash search in git
    // See VIM-2974 for example where it was broken
    return !editor.isOneLineMode
  }
}

/**
 * Known conflicts & solutions:
 *
 * - Smart step into - set handler after
 * - Python notebooks - set handler before - yes, we have `<CR>` as "after" and `<esc>` as before. I'm not completely sure
 *   why this combination is correct, but other versions don't work.
 * - Ace jump - set handler after
 * - Lookup - It disappears after putting our esc before templateEscape. But I'm not sure why it works like that
 * - App code - Need to review
 * - Template - Need to review
 * - before backend.escape - to handle our handlers before Rider processing. Also, without this rule, we get problems like VIM-3146
 */
internal class VimEscHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<Esc>"

  private val ideaVimSupportDialog
    get() = injector.globalIjOptions().ideavimsupport.contains(IjOptionConstants.ideavimsupport_dialog)

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    return editor.isPrimaryEditor() ||
      EditorHelper.isFileEditor(editor) && vimStateNeedsToHandleEscape(editor) ||
      ideaVimSupportDialog && vimStateNeedsToHandleEscape(editor)
  }

  private fun vimStateNeedsToHandleEscape(editor: Editor): Boolean {
    return !editor.vim.mode.inNormalMode || KeyHandler.getInstance().keyHandlerState.mappingState.hasKeys
  }
}

/**
 * Rider (and CLion Nova) uses a separate handler for esc to close the completion. IdeaOnlyEscapeHandlerAction is especially
 *   designer to get all the esc presses, and if there is a completion close it and do not pass the execution further.
 *   This doesn't work the same as in IJ.
 * In IdeaVim, we'd like to exit insert mode on closing completion. This is a requirement as the change of this
 *   behaviour causes a lot of complaining from users. Since the rider handler gets execution control, we don't
 *    receive an event and don't exit the insert mode.
 * To fix it, this special handler exists only for rider and stands before the rider's handler. We don't execute the
 *   handler from rider because the autocompletion is closed automatically anyway.
 */
internal class VimEscForRiderHandler(nextHandler: EditorActionHandler) : VimKeyHandler(nextHandler) {
  override val key: String = "<Esc>"

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    if (!enableOctopus) return false
    return LookupManager.getActiveLookup(editor) != null
  }
}

/**
 * Empty logger for esc presses
 *
 * As we made a migration to the new way of handling esc keys (VIM-2974), we may face several issues around that
 * One of the possible issues is that some plugin may also register a shortcut for this key and do not pass
 * the control to the next handler. In this way, the esc won't work, but there will be no exceptions.
 * This handler, that should stand in front of handlers change, just logs the event of pressing the key
 * and passes the execution.
 */
internal class VimEscLoggerHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (enableOctopus) {
      LOG.info("Esc pressed")
    }
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }

  companion object {
    val LOG = logger<VimEscLoggerHandler>()
  }
}

/**
 * Workaround to support "Start New Line" action in normal mode.
 * IJ executes enter handler on "Start New Line". This causes an issue that IdeaVim thinks that this is just an enter key.
 * This thing should be refactored, but for now we'll use this workaround VIM-3159
 *
 * The Same thing happens with "Start New Line Before Current" action.
 */
internal class StartNewLineDetector(nextHandler: EditorActionHandler) : StartNewLineDetectorBase(nextHandler)
internal class StartNewLineBeforeCurrentDetector(nextHandler: EditorActionHandler) :
  StartNewLineDetectorBase(nextHandler)

internal open class StartNewLineDetectorBase(private val nextHandler: EditorActionHandler) : EditorActionHandler() {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (enableOctopus) {
      DataManager.getInstance().saveInDataContext(dataContext, Util.key, true)
    }
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }

  object Util {
    val key = Key.create<Boolean>("vim.is.start.new.line")
  }

  companion object {
    val LOG = logger<VimEscLoggerHandler>()
  }
}

/**
 * Empty logger for enter presses
 *
 * As we made a migration to the new way of handling enter keys (VIM-2974), we may face several issues around that
 * One of the possible issues is that some plugin may also register a shortcut for this key and do not pass
 * the control to the next handler. In this way, the esc won't work, but there will be no exceptions.
 * This handler, that should stand in front of handlers change, just logs the event of pressing the key
 * and passes the execution.
 */
internal class VimEnterLoggerHandler(private val nextHandler: EditorActionHandler) : EditorActionHandler() {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    if (enableOctopus) {
      LOG.info("Enter pressed")
    }
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }

  companion object {
    val LOG = logger<VimEnterLoggerHandler>()
  }
}

internal abstract class VimKeyHandler(nextHandler: EditorActionHandler?) : OctopusHandler(nextHandler) {

  abstract val key: String

  override fun executeHandler(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    val enterKey = key(key)
    val context = dataContext?.vim ?: injector.executionContextManager.getEditorExecutionContext(editor.vim)
    val keyHandler = KeyHandler.getInstance()
    keyHandler.handleKey(editor.vim, enterKey, context, keyHandler.keyHandlerState)
  }

  override fun isHandlerEnabled(editor: Editor, dataContext: DataContext?): Boolean {
    val enterKey = key(key)
    return isOctopusEnabled(enterKey, editor)
  }
}

internal fun isOctopusEnabled(s: KeyStroke, editor: Editor): Boolean {
  if (!enableOctopus) return false
  // CMD line has a different processing mechanizm: the processing actions are registered
  //   for the input field component. These keys are not dispatched via the octopus handler.
  if (editor.vim.mode is Mode.CMD_LINE) return false
  // Turn off octopus for some IDEs. They have issues with ENTER and ESC on the octopus like VIM-3815
  if (isRider() || PlatformUtils.isJetBrainsClient() || isClionNova()) return false
  when {
    s.keyCode == KeyEvent.VK_ENTER && s.modifiers == 0 -> return true
    s.keyCode == KeyEvent.VK_ESCAPE && s.modifiers == 0 -> return true
  }
  return false
}

internal val enableOctopus: Boolean
  get() = injector.application.isOctopusEnabled()
