/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Mode
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.ResourceGuard
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.VimVariablesScope
import com.intellij.vim.api.caretId
import com.intellij.vim.api.caretInfo
import com.intellij.vim.api.scopes.VimInitPluginScope
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.Transaction
import com.intellij.vim.api.scopes.VimPluginScope
import com.intellij.vim.api.scopes.vimPluginScope
import com.intellij.vim.api.toMappingMode
import com.intellij.vim.api.toMode
import com.intellij.vim.api.toRegisterType
import com.intellij.vim.api.toTextSelectionType
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

class VimPluginApiImpl : VimPluginApi {

  private val resourceGuard: IntelliJResourceGuard = IntelliJResourceGuard()

  override fun getResourceGuard(): ResourceGuard {
    return resourceGuard
  }

  override fun getRegisterContent(
    read: Read,
    caretId: CaretId,
    register: Char,
  ): String? {
    val editor: VimEditor = read.editor
    val context: ExecutionContext = read.context

    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.registerStorage.getRegister(editor, context, register)?.text
  }

  override fun getCurrentRegisterName(
    read: Read,
    caretId: CaretId,
  ): Char {
    val editor: VimEditor = read.editor
    val caretCount: Int = editor.carets().size
    val registerGroup = injector.registerGroup

    val lastRegisterChar: Char =
      if (caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
    return lastRegisterChar
  }

  override fun getRegisterType(
    read: Read,
    caretId: CaretId,
    register: Char,
  ): RegisterType? {
    val editor: VimEditor = read.editor
    val context: ExecutionContext = read.context

    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.registerStorage.getRegister(editor, context, register)?.type?.toRegisterType()
  }

  override fun addMapping(
    scope: VimInitPluginScope,
    fromKeys: String,
    toKeys: String,
    isRecursive: Boolean,
    vararg mode: Mode,
  ) {
    val modes: Set<MappingMode> = mode.map { it.toMappingMode() }.toSet()
    injector.keyGroup.putKeyMapping(
      modes = modes,
      fromKeys = injector.parser.parseKeys(fromKeys),
      toKeys = injector.parser.parseKeys(toKeys),
      recursive = isRecursive,
      owner = MappingOwner.IdeaVim.System
    )
  }

  override fun addMapping(
    fromKeys: String,
    scope: VimInitPluginScope,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: VimPluginScope.() -> Unit,
    vararg mode: Mode,
  ) {
    val modes: Set<MappingMode> = mode.map { it.toMappingMode() }.toSet()
    val vimApi: VimPluginApi = scope.vimPluginApi
    val extensionHandler: ExtensionHandler = object : ExtensionHandler {
      override val isRepeatable: Boolean
        get() = isRepeatable

      override fun execute(
        editor: VimEditor,
        context: ExecutionContext,
        operatorArguments: OperatorArguments,
      ) {
        vimPluginScope(editor, context, vimApi) {
          action()
        }
      }
    }
    injector.keyGroup.putKeyMapping(
      modes = modes,
      fromKeys = injector.parser.parseKeys(fromKeys),
      owner = MappingOwner.IdeaVim.System,
      recursive = false,
      extensionHandler = extensionHandler
    )
  }

  override fun removeMapping(
    scope: VimInitPluginScope,
    fromKeys: String,
    vararg mode: Mode,
  ) {
    val modes: Set<MappingMode> = mode.map { it.toMappingMode() }.toSet()
    injector.keyGroup.removeKeyMapping(
      modes = modes,
      keys = injector.parser.parseKeys(fromKeys)
    )
  }

  override fun exportOperatorFunction(
    name: String,
    scope: VimInitPluginScope,
    function: VimPluginScope.() -> Boolean,
  ) {
    val vimApi: VimPluginApi = scope.vimPluginApi
    val operatorFunction: OperatorFunction = object : OperatorFunction {
      override fun apply(
        editor: VimEditor,
        context: ExecutionContext,
        selectionType: SelectionType?,
      ): Boolean {
        return vimPluginScope(editor, context, vimApi) {
          function()
        }
      }
    }
    VimExtensionFacade.exportOperatorFunction(name, operatorFunction)
  }

  override fun setOperatorFunction(scope: VimPluginScope, name: String) {
    injector.globalOptions().operatorfunc = name
  }

  override fun executeNormal(scope: VimPluginScope, command: String) {
    val editor: VimEditor = scope.editor
    executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
  }

  override fun getMode(scope: VimPluginScope): Mode {
    val editor: VimEditor = scope.editor
    return editor.mode.toMappingMode().toMode()
  }

  override fun getSelectionTypeForCurrentMode(scope: VimPluginScope): TextSelectionType? {
    val editor: VimEditor = scope.editor
    val typeInEditor = editor.mode.selectionType ?: return null
    return typeInEditor.toTextSelectionType()
  }

  override fun exitVisualMode(scope: VimPluginScope) {
    val editor: VimEditor = scope.editor
    editor.exitVisualMode()
  }

  override fun deleteText(
    transaction: Transaction,
    startOffset: Int,
    endOffset: Int,
  ) {
    val editor: VimEditor = transaction.editor
    editor.deleteString(TextRange(startOffset, endOffset))
  }

  override fun replaceText(
    transaction: Transaction,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  ) {
    val editor: VimEditor = transaction.editor
    val replaceSequenceSize = endOffset - startOffset - 1
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    if (replaceSequenceSize == 0) {
      (editor as MutableVimEditor).insertText(caret, startOffset, text)
    } else {
      val isLastCharInLine = endOffset == editor.getLineEndOffset(caret.getLine(), true) + 1
      val preparedText = if (isLastCharInLine) text.plus("\n") else text
      (editor as MutableVimEditor).replaceString(startOffset, endOffset, preparedText)
    }
  }

  override fun replaceTextBlockwise(
    transaction: Transaction,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>,
  ) {
    val editor: VimEditor = transaction.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    val firstLine = editor.offsetToBufferPosition(startOffset).line
    val lastLine = text.size + firstLine - 1

    val startDiff = startOffset - editor.getLineStartOffset(firstLine)
    val endDiff = editor.getLineEndOffset(firstLine, true) - endOffset

    text.zip(firstLine..lastLine).forEach { (lineText, line) ->
      val lineStartOffset = editor.getLineStartOffset(line)
      val lineEndOffset = editor.getLineEndOffset(line, true)

      if (line == firstLine) {
        (editor as MutableVimEditor).replaceString(lineStartOffset + startDiff, lineEndOffset - endDiff, lineText)
      } else {
        (editor as MutableVimEditor).insertText(caret, lineStartOffset + startDiff, lineText)
      }
    }
  }

  override fun getChangeMarks(
    read: Read,
    caretId: CaretId,
  ): Pair<Int, Int>? {
    val editor: VimEditor = read.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    val changeMarks: TextRange = injector.markService.getChangeMarks(caret) ?: return null
    return Pair(changeMarks.startOffset, changeMarks.endOffset)
  }

  override fun getVisualMarks(
    read: Read,
    caretId: CaretId,
  ): Pair<Int, Int>? {
    val editor: VimEditor = read.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return Pair(caret.selectionStart, caret.selectionEnd)
  }

  override fun getLineStartOffset(read: Read, line: Int): Int {
    val editor: VimEditor = read.editor
    return editor.getLineStartOffset(line)
  }

  override fun getLineEndOffset(read: Read, line: Int, allowEnd: Boolean): Int {
    val editor: VimEditor = read.editor
    return editor.getLineEndOffset(line, allowEnd)
  }

  override fun getAllCaretIds(read: Read): List<CaretId> {
    val editor: VimEditor = read.editor
    return editor.carets().map { caret -> caret.caretId }
  }

  override fun getALlCaretIdsSortedByOffset(read: Read): List<CaretId> {
    val editor: VimEditor = read.editor
    return editor.sortedCarets().map { caret -> caret.caretId }
  }

  override fun getCaretLine(read: Read, caretId: CaretId): Int? {
    val editor: VimEditor = read.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.getBufferPosition().line
  }

  override fun getAllCaretsData(read: Read): List<CaretData> {
    val editor: VimEditor = read.editor
    return editor.carets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretsDataSortedByOffset(read: Read): List<CaretData> {
    val editor: VimEditor = read.editor
    return editor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun updateCaret(
    transaction: Transaction,
    caretId: CaretId,
    caretInfo: CaretInfo,
  ) {
    val editor: VimEditor = transaction.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    caret.moveToOffset(caretInfo.offset)
    caretInfo.selection?.let { (start, end) ->
      caret.setSelection(start, end)
    } ?: caret.removeSelection()
  }

  override fun getCaretInfo(
    read: Read,
    caretId: CaretId,
  ): CaretInfo? {
    val editor: VimEditor = read.editor
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.caretInfo
  }

  override fun addCaret(
    transaction: Transaction,
    caretInfo: CaretInfo,
  ): CaretId {
    TODO("Not yet implemented")
  }

  override fun removeCaret(
    transaction: Transaction,
    caretId: CaretId,
  ) {
    TODO("Not yet implemented")
  }

  override fun getVimVariableInt(
    scope: VimPluginScope,
    vimVariablesScope: VimVariablesScope,
    name: String,
  ): Int? {
    /**
     * Variable value should probably be obtained like this:
     * val variable = injector.variableService.getNullableVariableValue(Variable(name, Scope.VIM_VARIABLE), editor, context, vimContext)
     *
     * However, I don't know how to obtain vimContext, so I implemented workaround since vimContext is not required to get value
     * of the count and count1 vim variables.
     */
    return if (vimVariablesScope == VimVariablesScope.VIM_SCOPE) {
      when (name) {
        "count" -> VimInt(KeyHandler.getInstance().keyHandlerState.commandBuilder.calculateCount0Snapshot()).toString()
          .toIntOrNull()

        "count1" -> VimInt(
          KeyHandler.getInstance().keyHandlerState.commandBuilder.calculateCount0Snapshot().coerceAtLeast(1)
        ).toString().toIntOrNull()

        else -> null
      }

    } else {
      null
    }
  }
}