/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.lion

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.VisualOperation
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Port of vim-lion: operators that align text to a character.
 *
 * `[count]gl{motion}{char}` adds spaces before each `{char}` and `[count]gL{motion}{char}` adds spaces after it, so
 * that the characters (or the text after them) line up. `{char}` can be preceded by a number to skip occurrences, `/`
 * prompts for a pattern and a space aligns words.
 *
 * Supported variables: `g:lion_create_maps`, `g:lion_map_right`, `g:lion_map_left`, `g:lion_prompt` and
 * `b:lion_squeeze_spaces`/`g:lion_squeeze_spaces`. The mapping variables must be set before the extension is enabled.
 *
 * See https://github.com/tommcdo/vim-lion
 */
internal class VimLionExtension : VimExtension {

  override fun getName() = "lion"

  override fun init() {
    for (direction in LionDirection.entries) {
      putExtensionHandlerMapping(
        MappingMode.N,
        injector.parser.parseKeys(direction.plug),
        owner,
        LionOperatorHandler(direction),
        false,
      )
      putExtensionHandlerMapping(
        MappingMode.X,
        injector.parser.parseKeys(direction.visualPlug),
        owner,
        LionVisualHandler(direction),
        false,
      )
      VimExtensionFacade.exportOperatorFunction(direction.operatorFunction, LionOperatorFunction(direction))
    }

    if (globalVariable(CREATE_MAPS_VARIABLE)?.toVimNumber()?.booleanValue == false) return
    for (direction in LionDirection.entries) {
      val keys = globalVariable(direction.mapVariable)?.toVimString()?.value ?: direction.defaultKeys
      if (keys.isEmpty()) continue
      putKeyMappingIfMissing(
        MappingMode.N,
        injector.parser.parseKeys(keys),
        owner,
        injector.parser.parseKeys(direction.plug),
        true,
      )
      putKeyMappingIfMissing(
        MappingMode.X,
        injector.parser.parseKeys(keys),
        owner,
        injector.parser.parseKeys(direction.visualPlug),
        true,
      )
    }
  }

  /** Starts `g@`, which calls [LionOperatorFunction] with the range of the following motion */
  private class LionOperatorHandler(private val direction: LionDirection) : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      LionCount.update(editor, operatorArguments)
      injector.globalOptions().operatorfunc = direction.operatorFunction
      // The count belongs to the alignment, it must not reach the motion of g@
      KeyHandler.getInstance().keyHandlerState.commandBuilder.resetCount()
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class LionVisualHandler(private val direction: LionDirection) : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      LionCount.update(editor, operatorArguments)
      KeyHandler.getInstance().keyHandlerState.commandBuilder.resetCount()

      val caret = editor.primaryCaret()
      val inVisualMode = editor.inVisualMode
      if (inVisualMode) {
        // Lets a dot-repeat apply the alignment to a selection of the same size at the caret
        caret.vimLastVisualOperatorRange = VisualOperation.getRange(editor, caret, noneOfEnum())
      } else if (!editor.inRepeatMode) {
        return
      }

      val toLineEnd = if (inVisualMode) {
        caret.vimLastColumn >= VimMotionGroupBase.LAST_COLUMN
      } else {
        (caret.vimLastVisualOperatorRange?.columns ?: 0) >= VimMotionGroupBase.LAST_COLUMN
      }
      val selection = editor.collectSelections()?.get(caret) ?: return
      val range = selection.toLionRange(editor, toLineEnd)

      if (inVisualMode) {
        executeNormalWithoutMapping(injector.parser.parseKeys("<Esc>"), editor.ij)
      }
      align(editor, context, direction, range)
    }

    private fun VimSelection.toLionRange(editor: VimEditor, toLineEnd: Boolean): LionRange {
      val start = editor.offsetToBufferPosition(minOf(vimStart, vimEnd))
      val end = editor.offsetToBufferPosition(maxOf(vimStart, vimEnd))
      return when (type) {
        SelectionType.LINE_WISE -> LionRange.lineWise(start.line, end.line)
        SelectionType.CHARACTER_WISE -> LionRange.characterWise(start, end)
        SelectionType.BLOCK_WISE -> {
          val startColumn = minOf(start.column, end.column)
          val endColumn = if (toLineEnd) null else maxOf(start.column, end.column)
          LionRange.blockWise(start.line, end.line, startColumn, endColumn)
        }
      }
    }
  }

  private class LionOperatorFunction(private val direction: LionDirection) : OperatorFunction {
    override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean {
      val caret = editor.primaryCaret()
      val marks = injector.markService.getChangeMarks(caret) ?: return false
      val start = editor.offsetToBufferPosition(marks.startOffset)
      val end = editor.offsetToBufferPosition((marks.endOffset - 1).coerceAtLeast(marks.startOffset))
      val range = when (selectionType ?: SelectionType.CHARACTER_WISE) {
        SelectionType.LINE_WISE -> LionRange.lineWise(start.line, end.line)
        SelectionType.CHARACTER_WISE -> LionRange.characterWise(start, end)
        SelectionType.BLOCK_WISE -> LionRange.blockWise(
          start.line,
          end.line,
          minOf(start.column, end.column),
          maxOf(start.column, end.column),
        )
      }
      // An operator leaves the caret at the start of the text it worked on
      caret.moveToOffset(marks.startOffset)
      return align(editor, context, direction, range)
    }
  }
}

/**
 * `gl` is [RIGHT]: spaces go before the alignment character. `gL` is [LEFT]: spaces go after it, before the text that
 * follows.
 */
private enum class LionDirection(
  val plug: String,
  val visualPlug: String,
  val operatorFunction: String,
  val mapVariable: String,
  val defaultKeys: String,
) {
  RIGHT("<Plug>LionRight", "<Plug>VLionRight", "LionAlignRight", "lion_map_right", "gl"),
  LEFT("<Plug>LionLeft", "<Plug>VLionLeft", "LionAlignLeft", "lion_map_left", "gL"),
}

/**
 * The count of the last alignment.
 *
 * A dot-repeat calls the handler again with the count given to `.`, so the original count is kept unless `.` has one.
 */
private object LionCount {
  var value = 0
    private set

  fun update(editor: VimEditor, operatorArguments: OperatorArguments) {
    if (!editor.inRepeatMode || operatorArguments.count0 > 0) {
      value = operatorArguments.count0
    }
  }
}

/**
 * What to align to, read from the user after the motion: `{char}`, optionally preceded by the number of the first
 * occurrence to align.
 */
private class LionInput(val pattern: String, val isRegex: Boolean, val skip: Int) {
  fun toPattern(direction: LionDirection): LionPattern {
    val matchEnd = direction == LionDirection.LEFT
    return if (isRegex) RegexLionPattern(pattern, matchEnd) else LiteralLionPattern(pattern, matchEnd)
  }
}

private fun align(editor: VimEditor, context: ExecutionContext, direction: LionDirection, range: LionRange): Boolean {
  val input = readInput(editor, context) ?: return false
  val pattern = try {
    input.toPattern(direction)
  } catch (_: VimRegexException) {
    injector.messages.indicateError()
    return false
  }

  // Changing a line keeps the caret at the same column, even if text is inserted before it
  val caret = editor.primaryCaret()
  val caretPosition = caret.getBufferPosition()

  if (isSqueezeSpacesEnabled(editor, context)) {
    for (line in range.lines) {
      val text = editor.getLineText(line)
      val squeezed = LionAligner.squeezeSpaces(text)
      if (squeezed != text) {
        val lineStart = editor.getLineStartOffset(line)
        injector.changeGroup.replaceText(editor, caret, lineStart, lineStart + text.length, squeezed)
      }
    }
  }

  val tabSize = editor.ij.settings.getTabSize(editor.ij.project).coerceAtLeast(1)
  LionAligner(editor, range, pattern, direction == LionDirection.LEFT, tabSize).align(LionCount.value, input.skip)

  val lineLength = editor.getLineText(caretPosition.line).length
  caret.moveToBufferPosition(
    BufferPosition(caretPosition.line, caretPosition.column.coerceAtMost((lineLength - 1).coerceAtLeast(0))),
  )
  return true
}

private fun readInput(editor: VimEditor, context: ExecutionContext): LionInput? {
  var key = VimExtensionFacade.inputKeyStroke(editor.ij)
  var skip = 0

  if (key.keyChar in '1'..'9') {
    val digits = StringBuilder().append(key.keyChar)
    while (true) {
      key = VimExtensionFacade.inputKeyStroke(editor.ij)
      if (key.keyChar !in '0'..'9') break
      digits.append(key.keyChar)
    }
    // A number on its own is the text to align to
    if (key.isEnter()) return LionInput(digits.toString(), isRegex = false, skip = 0)
    skip = (digits.toString().toIntOrNull() ?: return null) - 1
  }

  if (key.isCloseKeyStroke() || key.keyChar == KeyEvent.CHAR_UNDEFINED) return null
  return when (val char = key.keyChar) {
    '/' -> {
      val prompt = globalVariable(PROMPT_VARIABLE)?.toVimString()?.value ?: DEFAULT_PROMPT
      @Suppress("DEPRECATION")
      val text = injector.commandLine.inputString(editor, context, prompt, null) ?: return null
      if (text.isEmpty()) LionInput("/", isRegex = false, skip) else LionInput(text, isRegex = true, skip)
    }
    // Aligns the first space after each word
    ' ' -> LionInput("\\S\\zs\\s", isRegex = true, skip)
    else -> LionInput(char.toString(), isRegex = false, skip)
  }
}

private fun KeyStroke.isEnter(): Boolean = keyCode == KeyEvent.VK_ENTER || keyChar == '\n' || keyChar == '\r'

/** The buffer variable wins over the global one, even when it is off */
private fun isSqueezeSpacesEnabled(editor: VimEditor, context: ExecutionContext): Boolean {
  val value = injector.variableService.getNullableVariableValue(
    VariableExpression(Scope.BUFFER_VARIABLE, SQUEEZE_SPACES_VARIABLE),
    editor,
    context,
    VimPluginContext,
  ) ?: globalVariable(SQUEEZE_SPACES_VARIABLE)
  return value?.toVimNumber()?.booleanValue ?: false
}

private fun globalVariable(name: String): VimDataType? = injector.variableService.getGlobalVariableValue(name)

private const val CREATE_MAPS_VARIABLE = "lion_create_maps"
private const val SQUEEZE_SPACES_VARIABLE = "lion_squeeze_spaces"
private const val PROMPT_VARIABLE = "lion_prompt"
private const val DEFAULT_PROMPT = "Pattern [/]: "
