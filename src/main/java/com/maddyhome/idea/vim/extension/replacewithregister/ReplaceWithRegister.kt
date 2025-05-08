/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimCopiedText
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.isLine
import com.maddyhome.idea.vim.state.mode.selectionType
import org.jetbrains.annotations.NonNls

internal class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys(RWR_OPERATOR), owner, RwrMotion(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys(RWR_LINE), owner, RwrLine(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.X, injector.parser.parseKeys(RWR_VISUAL), owner, RwrVisual(), false)

    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("gr"), owner, injector.parser.parseKeys(RWR_OPERATOR), true)
    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("grr"), owner, injector.parser.parseKeys(RWR_LINE), true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("gr"), owner, injector.parser.parseKeys(RWR_VISUAL), true)

    VimExtensionFacade.exportOperatorFunction(OPERATOR_FUNC, Operator())
  }

  private class RwrVisual : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val typeInEditor = editor.mode.selectionType ?: SelectionType.CHARACTER_WISE
      editor.sortedCarets().forEach { caret ->
        val selectionStart = caret.selectionStart
        val selectionEnd = caret.selectionEnd

        val visualSelection = caret to VimSelection.create(selectionStart, selectionEnd - 1, typeInEditor, editor)
        doReplace(editor.ij, context.ij, caret, PutData.VisualSelection(mapOf(visualSelection), typeInEditor))
      }
      editor.exitVisualMode()
    }
  }

  private class RwrMotion : ExtensionHandler {
    override val isRepeatable: Boolean = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      injector.globalOptions().operatorfunc = OPERATOR_FUNC
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class RwrLine : ExtensionHandler {
    override val isRepeatable: Boolean = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val caretsAndSelections = mutableMapOf<ImmutableVimCaret, VimSelection>()
      editor.carets().forEach { caret ->
        val logicalLine = caret.getBufferPosition().line
        val lineStart = editor.getLineStartOffset(logicalLine)
        val lineEnd = editor.getLineEndOffset(logicalLine + operatorArguments.count1 - 1, true)

        val visualSelection = caret to VimSelection.create(lineStart, lineEnd, SelectionType.LINE_WISE, editor)
        caretsAndSelections += visualSelection

        doReplace(editor.ij, context.ij, caret, PutData.VisualSelection(mapOf(visualSelection), SelectionType.LINE_WISE))
      }

      editor.sortedCarets().forEach { caret ->
        val vimStart = caretsAndSelections[caret]?.vimStart
        if (vimStart != null) {
          caret.moveToOffset(vimStart)
        }
      }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean {
      val ijEditor = (editor as IjVimEditor).editor
      val range = getRange(ijEditor) ?: return false
      val visualSelection = PutData.VisualSelection(
        mapOf(
          editor.primaryCaret() to VimSelection.create(
            range.startOffset,
            range.endOffset - 1,
            selectionType ?: SelectionType.CHARACTER_WISE,
            editor,
          ),
        ),
        selectionType ?: SelectionType.CHARACTER_WISE,
      )
      // todo multicaret
      doReplace(ijEditor, context.ij, editor.primaryCaret(), visualSelection)
      return true
    }

    // todo make it work with multiple carets
    private fun getRange(editor: Editor): TextRange? = when (editor.vim.mode) {
      is Mode.NORMAL -> injector.markService.getChangeMarks(editor.caretModel.primaryCaret.vim)
      is Mode.VISUAL -> editor.caretModel.primaryCaret.run { TextRange(selectionStart, selectionEnd) }
      else -> null
    }
  }

  companion object {
    @NonNls private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
    @NonNls private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
    @NonNls private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
    @NonNls private const val OPERATOR_FUNC = "ReplaceWithRegisterOperatorFunc"
  }
}

private fun doReplace(editor: Editor, context: DataContext, caret: ImmutableVimCaret, visualSelection: PutData.VisualSelection) {
  val registerGroup = injector.registerGroup
  val lastRegisterChar = if (editor.caretModel.caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
  val savedRegister = caret.registerStorage.getRegister(editor.vim, context.vim, lastRegisterChar) ?: return

  var usedType = savedRegister.type
  var usedText = savedRegister.text

  val selection = visualSelection.caretsAndSelections.values.first()
  val startOffset: Int = selection.vimStart
  val endOffset: Int = selection.vimEnd + 1

  val isEmptySelection: Boolean = startOffset == endOffset

  if (isEmptySelection) {
    val editor: VimEditor = editor.vim

    val beforeChar: String = editor.getText(startOffset - 1, startOffset)
    val afterChar: String = editor.getText(endOffset, endOffset + 1)

    usedText = beforeChar + usedText + afterChar
  }

  if (usedType.isLine && usedText?.endsWith('\n') == true) {
    // Code from original plugin implementation. Correct text for linewise selected text
    usedText = usedText.dropLast(1)
    usedType = SelectionType.CHARACTER_WISE
  }

  val copiedText = IjVimCopiedText(usedText, (savedRegister.copiedText as IjVimCopiedText).transferableData)
  val textData = PutData.TextData(savedRegister.name, copiedText, usedType)

  val putData = PutData(
    textData,
    visualSelection,
    1,
    insertTextBeforeCaret = true,
    rawIndent = true,
    caretAfterInsertedText = false,
    putToLine = -1,
  )
  val vimEditor = editor.vim
  ClipboardOptionHelper.IdeaputDisabler().use {
    VimPlugin.getPut().putText(
      vimEditor,
      context.vim,
      putData,
      saveToRegister = false
    )
  }
}
