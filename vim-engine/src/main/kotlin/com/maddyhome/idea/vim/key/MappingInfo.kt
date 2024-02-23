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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.VimSelection.Companion.create
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
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
public sealed class MappingInfo(
  public val fromKeys: List<KeyStroke>,
  public val isRecursive: Boolean,
  public val owner: MappingOwner,
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

public class ToKeysMappingInfo(
  public val toKeys: List<KeyStroke>,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
) : MappingInfo(fromKeys, isRecursive, owner) {
  override fun getPresentableString(): String = injector.parser.toKeyNotation(toKeys)

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToKeys' mapping info...")
    val editorDataContext = injector.executionContextManager.onEditor(editor, context)
    val fromIsPrefix = KeyHandler.isPrefix(fromKeys, toKeys)
    val keyHandler = KeyHandler.getInstance()
    LOG.trace { "Adding new keys to keyStack as toKeys of mapping. State before adding keys: ${keyHandler.keyStack.dump()}" }
    keyHandler.keyStack.addKeys(toKeys)
    try {
      var first = true
      while (keyHandler.keyStack.hasStroke()) {
        val keyStroke = keyHandler.keyStack.feedStroke()
        val recursive = isRecursive && !(first && fromIsPrefix)
        keyHandler.handleKey(editor, keyStroke, editorDataContext, recursive, false, keyState)
        first = false
      }
    } finally {
      keyHandler.keyStack.removeFirst()
    }
  }

  override fun toString(): String {
    return "Mapping[$fromKeys -> $toKeys]"
  }

  public companion object {
    private val LOG = vimLogger<ToKeysMappingInfo>()
  }
}

public class ToExpressionMappingInfo(
  private val toExpression: Expression,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
  private val originalString: String,
) : MappingInfo(fromKeys, isRecursive, owner) {
  override fun getPresentableString(): String = originalString

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToExpression' mapping info...")
    val editorDataContext = injector.executionContextManager.onEditor(editor, context)
    val toKeys = injector.parser.parseKeys(toExpression.evaluate(editor, context, CommandLineVimLContext).toString())
    val fromIsPrefix = KeyHandler.isPrefix(fromKeys, toKeys)
    var first = true
    for (keyStroke in toKeys) {
      val recursive = isRecursive && !(first && fromIsPrefix)
      val keyHandler = KeyHandler.getInstance()
      keyHandler.handleKey(editor, keyStroke, editorDataContext, recursive, false, keyState)
      first = false
    }
  }

  public companion object {
    private val LOG = vimLogger<ToExpressionMappingInfo>()
  }
}

public class ToHandlerMappingInfo(
  private val extensionHandler: ExtensionHandler,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
) : MappingInfo(fromKeys, isRecursive, owner) {
  override fun getPresentableString(): String = "call ${extensionHandler.javaClass.canonicalName}"

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToHandler' mapping info...")
    val vimStateMachine = VimStateMachine.getInstance(editor)

    // Cache isOperatorPending in case the extension changes the mode while moving the caret
    // See CommonExtensionTest
    // TODO: Is this legal? Should we assert in this case?
    val shouldCalculateOffsets: Boolean = vimStateMachine.isOperatorPending(editor.mode)

    val startOffsets: Map<ImmutableVimCaret, Offset> = editor.carets().associateWith { it.offset }

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
              VimStateMachine.getInstance(editor),
              null,
              false,
              keyState
            )
          }
        }
      }
    }

    val operatorArguments = OperatorArguments(vimStateMachine.isOperatorPending(editor.mode), keyState.commandBuilder.count, vimStateMachine.mode)
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

  public companion object {
    private val LOG = vimLogger<ToHandlerMappingInfo>()

    private fun myFun(
      shouldCalculateOffsets: Boolean,
      editor: VimEditor,
      startOffsets: Map<ImmutableVimCaret, Offset>,
      keyState: KeyHandlerState,
    ) {
      if (shouldCalculateOffsets && !keyState.commandBuilder.hasCurrentCommandPartArgument()) {
        val offsets: MutableMap<ImmutableVimCaret, VimSelection> = HashMap()
        for (caret in editor.carets()) {
          var startOffset = startOffsets[caret]
          if (caret.hasSelection()) {
            val vimSelection =
              create(caret.vimSelectionStart, caret.offset.point, editor.mode.selectionType ?: CHARACTER_WISE, editor)
            offsets[caret] = vimSelection
            editor.mode = Mode.NORMAL()
          } else if (startOffset != null && startOffset.point != caret.offset.point) {
            // Command line motions are always characterwise exclusive
            var endOffset = caret.offset
            if (startOffset.point < endOffset.point) {
              endOffset = (endOffset.point - 1).offset
            } else {
              startOffset = (startOffset.point - 1).offset
            }
            val vimSelection = create(startOffset.point, endOffset.point, CHARACTER_WISE, editor)
            offsets[caret] = vimSelection
            // FIXME: what is the comment below about?...
            // Move caret to the initial offset for better undo action
            //  This is not a necessary thing, but without it undo action look less convenient
            editor.currentCaret().moveToOffset(startOffset.point)
          }
        }
        if (offsets.isNotEmpty()) {
          keyState.commandBuilder.completeCommandPart(Argument(offsets))
        }
      }
    }
  }
}

public class ToActionMappingInfo(
  public val action: String,
  fromKeys: List<KeyStroke>,
  isRecursive: Boolean,
  owner: MappingOwner,
) : MappingInfo(fromKeys, isRecursive, owner) {
  override fun getPresentableString(): String = "action $action"

  override fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    LOG.debug("Executing 'ToAction' mapping...")
    val editorDataContext = injector.executionContextManager.onEditor(editor, context)
    val dataContext = injector.executionContextManager.onCaret(editor.currentCaret(), editorDataContext)
    injector.actionExecutor.executeAction(action, dataContext)
  }

  public companion object {
    private val LOG = vimLogger<ToActionMappingInfo>()
  }
}
