/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.miniai

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.group.findBlockRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.state.KeyHandlerState
import java.util.*

/**
 * Extend and create a/i textobjects
 *
 * <p>
 * mini ai provides the next motions:
 * <ul>
 *   <li>aq around any quotes.</li>
 *   <li>iq inside any quotes.</li>
 *   <li>ab around any parentheses, curly braces and square brackets.</li>
 *   <li>ib inside any parentheses, curly braces and square brackets.</li>
 * </ul>
 *
 * @author Osvaldo Cordova Aburto (@oca159)
 */
internal class MiniAI : VimExtension {

  companion object {
    // Constants for key mappings
    private const val PLUG_AQ = "<Plug>mini-ai-aq"
    private const val PLUG_IQ = "<Plug>mini-ai-iq"
    private const val PLUG_AB = "<Plug>mini-ai-ab"
    private const val PLUG_IB = "<Plug>mini-ai-ib"

    // Constants for key sequences
    private const val KEY_AQ = "aq"
    private const val KEY_IQ = "iq"
    private const val KEY_AB = "ab"
    private const val KEY_IB = "ib"
  }

  override fun getName() = "mini-ai"

  override fun init() {
    registerMappings()
  }

  private fun registerMappings() {
    val mappings = listOf(
      PLUG_AQ to AroundAnyQuotesHandler(),
      PLUG_IQ to InsideAnyQuotesHandler(),
      PLUG_AB to AroundAnyBracketsHandler(),
      PLUG_IB to InsideAnyBracketsHandler()
    )

    mappings.forEach { (key, handler) ->
      putExtensionHandlerMapping(MappingMode.XO, injector.parser.parseKeys(key), owner, handler, false)
    }

    val keyMappings = listOf(
      KEY_AQ to PLUG_AQ,
      KEY_IQ to PLUG_IQ,
      KEY_AB to PLUG_AB,
      KEY_IB to PLUG_IB
    )

    keyMappings.forEach { (key, plug) ->
      putKeyMapping(MappingMode.XO, injector.parser.parseKeys(key), owner, injector.parser.parseKeys(plug), true)
    }
  }


  private class InsideAnyQuotesHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      addAction(MotionInnerAnyQuoteProximityAction())
    }
  }

  private class AroundAnyQuotesHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      addAction(MotionOuterAnyQuoteProximityAction())
    }
  }

  private class InsideAnyBracketsHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      addAction(MotionInnerAnyBracketProximityAction())
    }
  }

  private class AroundAnyBracketsHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      addAction(MotionOuterAnyBracketProximityAction())
    }
  }

  class MotionInnerAnyQuoteProximityAction : TextObjectActionHandler() {

    override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

    override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

    override fun getRange(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
    ): TextRange? {
      return findClosestQuoteRange(editor, caret, false)
    }
  }

  class MotionOuterAnyQuoteProximityAction : TextObjectActionHandler() {

    override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

    override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

    override fun getRange(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
    ): TextRange? {
      return findClosestQuoteRange(editor, caret, true)
    }
  }

  class MotionInnerAnyBracketProximityAction : TextObjectActionHandler() {

    override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

    override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

    override fun getRange(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
    ): TextRange? {
      return findClosestBracketRange(editor, caret, count, false)
    }
  }
}

class MotionOuterAnyBracketProximityAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findClosestBracketRange(editor, caret, count, true)
  }
}

private fun addAction(action: TextObjectActionHandler) {
  val keyHandlerState: KeyHandlerState = KeyHandler.getInstance().keyHandlerState
  keyHandlerState.commandBuilder.addAction(action)
}

private fun findClosestBracketRange(
  editor: VimEditor,
  caret: ImmutableVimCaret,
  count: Int,
  isOuter: Boolean,
): TextRange? {
  return listOf('(', '[', '{')
    .mapNotNull { char ->
      findBlockRange(editor, caret, char, count, isOuter)?.let { range -> range to char }
    }
    .minByOrNull { it.first.distanceTo(caret.offset) }?.first
}

private fun findClosestQuoteRange(
  editor: VimEditor,
  caret: ImmutableVimCaret,
  isOuter: Boolean,
): TextRange? {
  return listOf('`', '"', '\'')
    .mapNotNull { char ->
      injector.searchHelper.findBlockQuoteInLineRange(editor, caret, char, isOuter)?.let { range -> range to char }
    }
    .minByOrNull { it.first.distanceTo(caret.offset) }?.first
}

private fun TextRange.distanceTo(caretOffset: Int): Int {
  return if (caretOffset < startOffset) {
    startOffset - caretOffset
  } else if (caretOffset > endOffset) {
    caretOffset - endOffset
  } else {
    0 // Caret is inside the range
  }
}