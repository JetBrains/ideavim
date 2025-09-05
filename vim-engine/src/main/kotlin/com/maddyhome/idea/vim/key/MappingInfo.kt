/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.action.change.Extension.clean
import com.maddyhome.idea.vim.action.change.Extension.lastExtensionHandler
import com.maddyhome.idea.vim.action.change.VimRepeater.repeatHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.VimSelection.Companion.create
import com.maddyhome.idea.vim.handler.ExternalActionHandler
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.math.min

/**
 * @author vlan
 */
sealed class MappingInfo(
  val fromKeys: List<KeyStroke>,
  val isRecursive: Boolean,
  val owner: MappingOwner,
  val originalModes: Set<MappingMode>,
) : Comparable<MappingInfo>, MappingInfoLayer {

  @VimNlsSafe
  abstract override fun getPresentableString(): String

  abstract override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState)

  override fun compareTo(other: MappingInfo): Int {
    val size = fromKeys.size
    val otherSize = other.fromKeys.size
    val n = min(size, otherSize)
    for (i in 0 until n) {
      val diff = compareKeys(fromKeys[i], other.fromKeys[i])
      if (diff != 0) return diff
    }
    return size - otherSize
  }

  private fun compareKeys(key1: KeyStroke, key2: KeyStroke): Int {
    val c1 = key1.keyChar
    val c2 = key2.keyChar
    return when {
      c1 == KeyEvent.CHAR_UNDEFINED && c2 == KeyEvent.CHAR_UNDEFINED -> {
        val keyCodeDiff = key1.keyCode - key2.keyCode
        if (keyCodeDiff != 0) keyCodeDiff else key1.modifiers - key2.modifiers
      }

      c1 == KeyEvent.CHAR_UNDEFINED -> -1
      c2 == KeyEvent.CHAR_UNDEFINED -> 1
      else -> c1 - c2
    }
  }
}

class ToKeysMappingInfo(
  val toKeys: List<KeyStroke>,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
  originalModes: Set<MappingMode>,
) : MappingInfo(fromKeys, isRecursive, owner, originalModes) {
  override fun getPresentableString(): String = injector.parser.toKeyNotation(toKeys)

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToKeys' mapping info...")

    // From the Vim docs: If the {rhs} starts with the {lhs}, the first character is not mapped again.
    // E.g. `:map ab abcd`. When typing `ab`, Vim will process `abcd`, executing `a` and inserting `bcd`.
    // See `:help recursive_mapping`
    val lhsIsPrefixOfRhs = KeyHandler.isPrefix(fromKeys, toKeys)

    val keyHandler = KeyHandler.getInstance()
    LOG.trace { "Adding new keys to keyStack as toKeys of mapping. State before adding keys: ${keyHandler.keyStack.dump()}" }
    keyHandler.keyStack.addKeys(toKeys)
    try {
      var first = true
      while (keyHandler.keyStack.hasStroke()) {
        val keyStroke = keyHandler.keyStack.feedStroke()
        val allowKeyMappings = isRecursive && !(first && lhsIsPrefixOfRhs)
        keyHandler.handleKey(editor, keyStroke, context, allowKeyMappings, keyState)
        first = false
      }
    } finally {
      keyHandler.keyStack.removeFirst()
    }
  }

  override fun toString(): String {
    return "Mapping[$fromKeys -> $toKeys]"
  }

  companion object {
    private val LOG = vimLogger<ToKeysMappingInfo>()
  }
}

class ToExpressionMappingInfo(
  private val toExpression: Expression,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
  originalModes: Set<MappingMode>,
  private val originalString: String,
) : MappingInfo(fromKeys, isRecursive, owner, originalModes) {
  override fun getPresentableString(): String = originalString

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToExpression' mapping info...")

    val toKeys = injector.parser.parseKeys(toExpression.evaluate(editor, context, CommandLineVimLContext).toOutputString())

    // TODO: Merge similar code from ToKeysMappingInfo
    // From the Vim docs: If the {rhs} starts with the {lhs}, the first character is not mapped again.
    // E.g. `:map ab abcd`. When typing `ab`, Vim will process `abcd`, executing `a` and inserting `bcd`.
    // See `:help recursive_mapping`
    val lhsIsPrefixOfRhs = KeyHandler.isPrefix(fromKeys, toKeys)
    var first = true
    for (keyStroke in toKeys) {
      val allowKeyMappings = isRecursive && !(first && lhsIsPrefixOfRhs)
      val keyHandler = KeyHandler.getInstance()
      keyHandler.handleKey(editor, keyStroke, context, allowKeyMappings, keyState)
      first = false
    }
  }

  companion object {
    private val LOG = vimLogger<ToExpressionMappingInfo>()
  }
}

class ToHandlerMappingInfo(
  private val extensionHandler: ExtensionHandler,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
  originalModes: Set<MappingMode>,
) : MappingInfo(fromKeys, isRecursive, owner, originalModes) {
  override fun getPresentableString(): String = "call ${extensionHandler.javaClass.canonicalName}"

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToHandler' mapping info...")

    // Cache isOperatorPending in case the extension changes the mode while moving the caret
    // See CommonExtensionTest
    val shouldCalculateOffsets: Boolean = editor.mode is Mode.OP_PENDING

    val startOffsets: Map<ImmutableVimCaret, Int> = editor.carets().associateWith { it.offset }

    if (extensionHandler.isRepeatable) {
      clean()
    }

    val handler = extensionHandler
    if (handler is ExtensionHandler.WithCallback) {
      handler._backingFunction = Runnable {
        myFun(shouldCalculateOffsets, editor, startOffsets, keyState)

        if (shouldCalculateOffsets) {
          injector.application.invokeLater {
            val keyHandler = KeyHandler.getInstance()
            keyHandler.finishedCommandPreparation(
              editor,
              context,
              null,
              false,
              keyState,
            )
          }
        }
      }
    }

    val operatorArguments = OperatorArguments(keyState.commandBuilder.calculateCount0Snapshot(), editor.mode)
    val register = keyState.commandBuilder.registerSnapshot
    if (register != null) {
      injector.registerGroup.selectRegister(register)
    }
    injector.actionExecutor.executeCommand(
      editor,
      { extensionHandler.execute(editor, context, operatorArguments) },
      "Vim " + extensionHandler.javaClass.simpleName,
      null,
    )

    if (extensionHandler.isRepeatable) {
      lastExtensionHandler = extensionHandler
      argumentCaptured = null
      repeatHandler = true
    }

    if (handler !is ExtensionHandler.WithCallback) {
      myFun(shouldCalculateOffsets, editor, startOffsets, keyState)
    }
  }

  companion object {
    private val LOG = vimLogger<ToHandlerMappingInfo>()

    private fun myFun(
      shouldCalculateOffsets: Boolean,
      editor: VimEditor,
      startOffsets: Map<ImmutableVimCaret, Int>,
      keyState: KeyHandlerState,
    ) {
      if (shouldCalculateOffsets && !keyState.commandBuilder.hasCurrentCommandPartArgument()) {
        val offsets: MutableMap<ImmutableVimCaret, VimSelection> = HashMap()
        for (caret in editor.carets()) {
          var startOffset = startOffsets[caret]
          if (caret.hasSelection()) {
            val vimSelection =
              create(caret.vimSelectionStart, caret.offset, editor.mode.selectionType ?: CHARACTER_WISE, editor)
            offsets[caret] = vimSelection
            editor.mode = Mode.NORMAL()
          } else if (startOffset != null && startOffset != caret.offset) {
            // Command line motions are always characterwise exclusive
            var endOffset = caret.offset
            if (startOffset < endOffset) {
              endOffset = (endOffset - 1)
            } else {
              startOffset = (startOffset - 1)
            }
            val vimSelection = create(startOffset, endOffset, CHARACTER_WISE, editor)
            offsets[caret] = vimSelection
            // FIXME: what is the comment below about?...
            // Move caret to the initial offset for better undo action
            //  This is not a necessary thing, but without it undo action look less convenient
            editor.currentCaret().moveToOffset(startOffset)
          }
        }
        if (offsets.isNotEmpty()) {
          keyState.commandBuilder.addAction(ExternalActionHandler(offsets))
        }
      }
    }
  }
}

class ToActionMappingInfo(
  val action: String,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
  originalModes: Set<MappingMode>,
) : MappingInfo(fromKeys, isRecursive, owner, originalModes) {
  override fun getPresentableString(): String = "action $action"

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToAction' mapping...")
    injector.actionExecutor.executeAction(editor, name = action, context = context)
  }

  companion object {
    private val LOG = vimLogger<ToActionMappingInfo>()
  }
}
