/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.isLine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.copy.PutData
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.key.OperatorFunction
import org.jetbrains.annotations.NonNls


class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, parseKeys(RWR_OPERATOR), owner, RwrMotion(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.N, parseKeys(RWR_LINE), owner, RwrLine(), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.X, parseKeys(RWR_VISUAL), owner, RwrVisual(), false)

    putKeyMappingIfMissing(MappingMode.N, parseKeys("gr"), owner, parseKeys(RWR_OPERATOR), true)
    putKeyMappingIfMissing(MappingMode.N, parseKeys("grr"), owner, parseKeys(RWR_LINE), true)
    putKeyMappingIfMissing(MappingMode.X, parseKeys("gr"), owner, parseKeys(RWR_VISUAL), true)
  }

  private class RwrVisual : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val caretsAndSelections = mutableMapOf<Caret, VimSelection>()
      val typeInEditor = SelectionType.fromSubMode(editor.subMode)
      editor.vimForEachCaret { caret ->
        val selectionStart = caret.selectionStart
        val selectionEnd = caret.selectionEnd

        caretsAndSelections += caret to VimSelection.create(selectionStart, selectionEnd - 1, typeInEditor, editor)
      }
      doReplace(editor, PutData.VisualSelection(caretsAndSelections, typeInEditor))
      editor.exitVisualMode()
    }
  }

  private class RwrMotion : VimExtensionHandler {
    override fun isRepeatable(): Boolean = true

    override fun execute(editor: Editor, context: DataContext) {
      setOperatorFunction(Operator())
      executeNormalWithoutMapping(parseKeys("g@"), editor)
    }
  }

  private class RwrLine : VimExtensionHandler {
    override fun isRepeatable(): Boolean = true

    override fun execute(editor: Editor, context: DataContext) {
      val caretsAndSelections = mutableMapOf<Caret, VimSelection>()
      editor.vimForEachCaret { caret ->
        val logicalLine = caret.logicalPosition.line
        val lineStart = editor.document.getLineStartOffset(logicalLine)
        val lineEnd = editor.document.getLineEndOffset(logicalLine)

        caretsAndSelections += caret to VimSelection.create(lineStart, lineEnd, SelectionType.LINE_WISE, editor)
      }

      val visualSelection = PutData.VisualSelection(caretsAndSelections, SelectionType.LINE_WISE)
      doReplace(editor, visualSelection)

      editor.vimForEachCaret { caret ->
        val vimStart = caretsAndSelections[caret]?.vimStart
        if (vimStart != null) {
          MotionGroup.moveCaret(editor, caret, vimStart)
        }
      }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      val range = getRange(editor) ?: return false
      val visualSelection = PutData.VisualSelection(mapOf(editor.caretModel.primaryCaret to VimSelection.create(range.startOffset, range.endOffset - 1, selectionType, editor)), selectionType)
      doReplace(editor, visualSelection)
      return true
    }

    private fun getRange(editor: Editor): TextRange? = when (CommandState.getInstance(editor).mode) {
      CommandState.Mode.COMMAND -> VimPlugin.getMark().getChangeMarks(editor)
      CommandState.Mode.VISUAL -> editor.caretModel.primaryCaret.run { TextRange(selectionStart, selectionEnd) }
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

    private fun doReplace(editor: Editor, visualSelection: PutData.VisualSelection) {
      val savedRegister = VimPlugin.getRegister().lastRegister ?: return

      var usedType = savedRegister.type
      var usedText = savedRegister.text
      if (usedType.isLine && usedText?.endsWith('\n') == true) {
        // Code from original plugin implementation. Correct text for linewise selected text
        usedText = usedText.dropLast(1)
        usedType = SelectionType.CHARACTER_WISE
      }

      val textData = PutData.TextData(usedText, usedType, savedRegister.transferableData)

      val putData = PutData(textData, visualSelection, 1, insertTextBeforeCaret = true, rawIndent = true, caretAfterInsertedText = false, putToLine = -1)
      VimPlugin.getPut().putText(editor, EditorDataContext.init(editor), putData)

      VimPlugin.getRegister().saveRegister(savedRegister.name, savedRegister)
      VimPlugin.getRegister().saveRegister(VimPlugin.getRegister().defaultRegister, savedRegister)
    }
  }
}
