/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.editorMode
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import com.maddyhome.idea.vim.put.PutData
import org.jetbrains.annotations.NonNls

class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys(RWR_OPERATOR), owner, RwrMotion(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys(RWR_LINE), owner, RwrLine(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.X, injector.parser.parseKeys(RWR_VISUAL), owner, RwrVisual(), false)

    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("gr"), owner, injector.parser.parseKeys(RWR_OPERATOR), true)
    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("grr"), owner, injector.parser.parseKeys(RWR_LINE), true)
    putKeyMappingIfMissing(MappingMode.X, injector.parser.parseKeys("gr"), owner, injector.parser.parseKeys(RWR_VISUAL), true)
  }

  private class RwrVisual : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext) {
      val typeInEditor = SelectionType.fromSubMode(editor.subMode)
      editor.forEachCaret { caret ->
        val selectionStart = caret.selectionStart
        val selectionEnd = caret.selectionEnd

        val visualSelection = caret to VimSelection.create(selectionStart, selectionEnd - 1, typeInEditor, editor)
        doReplace(editor.ij, caret, PutData.VisualSelection(mapOf(visualSelection), typeInEditor))
      }
      editor.exitVisualModeNative()
    }
  }

  private class RwrMotion : ExtensionHandler {
    override val isRepeatable: Boolean = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      setOperatorFunction(Operator())
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class RwrLine : ExtensionHandler {
    override val isRepeatable: Boolean = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      val caretsAndSelections = mutableMapOf<VimCaret, VimSelection>()
      editor.forEachCaret { caret ->
        val logicalLine = caret.getLogicalPosition().line
        val lineStart = editor.getLineStartOffset(logicalLine)
        val lineEnd = editor.getLineEndOffset(logicalLine, true)

        val visualSelection = caret to VimSelection.create(lineStart, lineEnd, SelectionType.LINE_WISE, editor)
        caretsAndSelections += visualSelection

        doReplace(editor.ij, caret, PutData.VisualSelection(mapOf(visualSelection), SelectionType.LINE_WISE))
      }

      editor.forEachCaret { caret ->
        val vimStart = caretsAndSelections[caret]?.vimStart
        if (vimStart != null) {
          caret.moveToOffset(vimStart)
        }
      }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(vimEditor: VimEditor, context: ExecutionContext, selectionType: SelectionType): Boolean {
      val editor = (vimEditor as IjVimEditor).editor
      val range = getRange(editor) ?: return false
      val visualSelection = PutData.VisualSelection(
        mapOf(
          vimEditor.primaryCaret() to VimSelection.create(
            range.startOffset,
            range.endOffset - 1,
            selectionType,
            vimEditor
          )
        ),
        selectionType
      )
      // todo multicaret
      doReplace(editor, vimEditor.primaryCaret(), visualSelection)
      return true
    }

    private fun getRange(editor: Editor): TextRange? = when (editor.vim.mode) {
      VimStateMachine.Mode.COMMAND -> VimPlugin.getMark().getChangeMarks(editor.vim)
      VimStateMachine.Mode.VISUAL -> editor.caretModel.primaryCaret.run { TextRange(selectionStart, selectionEnd) }
      else -> null
    }
  }

  companion object {
    @NonNls
    private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"

    @NonNls
    private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"

    @NonNls
    private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"

    private fun doReplace(editor: Editor, caret: VimCaret, visualSelection: PutData.VisualSelection) {
      val lastRegisterChar = injector.registerGroup.lastRegisterChar
      val savedRegister = caret.registerStorage.getRegister(caret, lastRegisterChar) ?: return

      var usedType = savedRegister.type
      var usedText = savedRegister.text
      if (usedType.isLine && usedText?.endsWith('\n') == true) {
        // Code from original plugin implementation. Correct text for linewise selected text
        usedText = usedText.dropLast(1)
        usedType = SelectionType.CHARACTER_WISE
      }

      val textData = PutData.TextData(usedText, usedType, savedRegister.transferableData)

      val putData = PutData(
        textData,
        visualSelection,
        1,
        insertTextBeforeCaret = true,
        rawIndent = true,
        caretAfterInsertedText = false,
        putToLine = -1
      )
      ClipboardOptionHelper.IdeaputDisabler().use {
        VimPlugin.getPut().putText(
          IjVimEditor(editor),
          IjExecutionContext(EditorDataContext.init(editor)),
          putData,
          operatorArguments = OperatorArguments(
            editor.vimStateMachine?.isOperatorPending ?: false,
            0, editor.editorMode, editor.subMode
          )
        )
      }

      caret.registerStorage.saveRegister(caret, savedRegister.name, savedRegister)
      caret.registerStorage.saveRegister(caret, VimPlugin.getRegister().defaultRegister, savedRegister)
    }
  }
}
