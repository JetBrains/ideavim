/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import kotlin.math.max

/**
 * A port of kana/vim-textobj-user: a framework that lets users declaratively define their own text objects via
 * `textobj#user#plugin({name}, {specs})`.
 *
 * See: https://github.com/kana/vim-textobj-user
 */
internal class VimTextObjUserExtension : VimExtension {

  override fun getName(): String = "textobj-user"

  override fun init(initApi: VimInitApi) {
    injector.functionService.registerFunctionHandler(
      FUNCTION_NAME,
      TextObjUserPluginFunctionHandler(FUNCTION_NAME, getOwner()),
    )
  }

  override fun dispose() {
    injector.functionService.unregisterFunctionHandler(FUNCTION_NAME)
  }

  companion object {
    private const val FUNCTION_NAME = "textobj#user#plugin"
  }
}

/**
 * Handles `textobj#user#plugin({name}, {specs})` by registering a text object mapping for every `select` key in the
 * given specs.
 */
private class TextObjUserPluginFunctionHandler(
  override val name: String,
  private val owner: MappingOwner,
) : FunctionHandler {
  override val scope: Scope? = null

  override fun executeFunction(
    arguments: List<Expression>,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val specs = arguments[1].evaluate(editor, context, vimContext) as VimDictionary
    for ((_, spec) in specs.dictionary) {
      registerTextObject(spec as VimDictionary)
    }
    return VimString("")
  }

  private fun registerTextObject(spec: VimDictionary) {
    // "pattern" is either a single Vim regex, or a [header, footer] pair of Vim regexes.
    val patterns = spec["pattern"].toStringList()
    if (patterns.isEmpty()) return

    // A single "pattern" is wired to "select"; a [header, footer] pair is wired to "select-a" / "select-i".
    registerMappings(spec["select"], patterns, isInner = false)
    registerMappings(spec["select-a"], patterns, isInner = false)
    registerMappings(spec["select-i"], patterns, isInner = true)
  }

  private fun registerMappings(keys: VimDataType?, patterns: List<String>, isInner: Boolean) {
    for (key in keys.toStringList()) {
      putExtensionHandlerMapping(
        MappingMode.XO,
        injector.parser.parseKeys(key),
        owner,
        TextObjUserHandler(patterns, isInner),
        false,
      )
    }
  }
}

/**
 * Selects the text object defined by [patterns].
 *
 * Follows the operator-pending pattern used by the other text object extensions: when invoked as the motion of an
 * operator (e.g. `d`), it defers to a [TextObjectActionHandler]; otherwise it sets the selection directly.
 */
private class TextObjUserHandler(
  private val patterns: List<String>,
  private val isInner: Boolean,
) : ExtensionHandler {
  override val isRepeatable: Boolean get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val action = TextObjUserActionHandler(patterns, isInner)
    if (editor.mode is Mode.OP_PENDING) {
      KeyHandler.getInstance().keyHandlerState.commandBuilder.addAction(action)
      return
    }
    val count = max(1, operatorArguments.count0)
    editor.nativeCarets().forEach { caret ->
      val range = action.getRange(editor, caret, context, count, operatorArguments.count0) ?: return@forEach
      applyRange(editor, caret, range)
    }
  }

  private fun applyRange(editor: VimEditor, caret: VimCaret, range: TextRange) {
    SelectionVimListenerSuppressor.lock().use {
      if (editor.mode is Mode.VISUAL) {
        caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
      } else {
        (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
      }
    }
  }
}

private class TextObjUserActionHandler(
  private val patterns: List<String>,
  private val isInner: Boolean,
) : TextObjectActionHandler() {
  override val visualType: TextObjectVisualType get() = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val caretOffset = caret.offset
    return when (patterns.size) {
      1 -> singlePatternRange(editor, caretOffset)
      2 -> pairPatternRange(editor, caretOffset)
      else -> null
    }
  }

  /**
   * Selects the whole match of a single pattern: the one under the cursor, or else the next one ahead of it.
   */
  private fun singlePatternRange(editor: VimEditor, caretOffset: Int): TextRange? {
    val matches = VimRegex(patterns[0]).findAll(editor)
    val match = matches.firstOrNull { caretOffset in it.range.startOffset until it.range.endOffset }
      ?: matches.firstOrNull { it.range.startOffset >= caretOffset }
      ?: return null
    return TextRange(match.range.startOffset, match.range.endOffset)
  }

  /**
   * Selects text delimited by a [header, footer] pair of patterns. "select-a" ([isInner] `false`) spans the delimiters,
   * from the header start to the footer end; "select-i" spans only the text between them, from the header end to the
   * footer start.
   */
  private fun pairPatternRange(editor: VimEditor, caretOffset: Int): TextRange? {
    // The header at or before the cursor (so the cursor may sit on the header, the enclosed text, or the footer),
    // falling back to the next header ahead of the cursor.
    val headers = VimRegex(patterns[0]).findAll(editor)
    val header = headers.lastOrNull { it.range.startOffset <= caretOffset }
      ?: headers.firstOrNull { it.range.startOffset >= caretOffset }
      ?: return null
    val footer = VimRegex(patterns[1]).findAll(editor)
      .firstOrNull { it.range.startOffset >= header.range.endOffset }
      ?: return null

    return if (isInner) {
      TextRange(header.range.endOffset, footer.range.startOffset)
    } else {
      TextRange(header.range.startOffset, footer.range.endOffset)
    }
  }
}

/**
 * Flattens a vim-textobj-user field that accepts either a single string or a list of strings (`pattern`, `select`, ...)
 * into a list. Returns an empty list when the field is absent.
 */
private fun VimDataType?.toStringList(): List<String> = when (this) {
  is VimString -> listOf(value)
  is VimList -> values.map { (it as VimString).value }
  else -> emptyList()
}
