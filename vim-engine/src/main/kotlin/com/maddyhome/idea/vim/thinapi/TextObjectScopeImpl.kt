/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.TextObjectRange
import com.intellij.vim.api.scopes.TextObjectScope
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Implementation of [TextObjectScope] that registers text objects using IdeaVim's internal mechanisms.
 */
internal class TextObjectScopeImpl(
  private val pluginName: String,
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : TextObjectScope {

  override fun register(
    keys: String,
    registerDefaultMapping: Boolean,
    preserveSelectionAnchor: Boolean,
    rangeProvider: VimApi.(count: Int) -> TextObjectRange?,
  ) {
    val plugKeys = "<Plug>($pluginName-$keys)"

    // Create the extension handler that wraps the text object logic
    val extensionHandler = TextObjectExtensionHandler(listenerOwner, mappingOwner, preserveSelectionAnchor, rangeProvider)

    // Register the <Plug> mapping in visual and operator-pending modes
    injector.keyGroup.putKeyMapping(
      modes = MappingMode.XO,
      fromKeys = injector.parser.parseKeys(plugKeys),
      owner = mappingOwner,
      extensionHandler = extensionHandler,
      recursive = false,
    )

    // Optionally register the default key mapping
    if (registerDefaultMapping) {
      val fromKeys = injector.parser.parseKeys(keys)
      val toKeys = injector.parser.parseKeys(plugKeys)

      // Only add mapping if no mapping to this <Plug> already exists
      val filteredModes = MappingMode.XO.filterNotTo(HashSet()) {
        injector.keyGroup.hasmapto(it, toKeys)
      }

      if (filteredModes.isNotEmpty()) {
        injector.keyGroup.putKeyMapping(
          modes = filteredModes,
          fromKeys = fromKeys,
          owner = mappingOwner,
          toKeys = toKeys,
          recursive = true,
        )
      }
    }
  }
}

/**
 * Extension handler that executes a text object range provider and handles
 * the mode-specific selection logic.
 */
private class TextObjectExtensionHandler(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
  private val preserveSelectionAnchor: Boolean,
  private val rangeProvider: VimApi.(count: Int) -> TextObjectRange?,
) : ExtensionHandler {

  override val isRepeatable: Boolean = false

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ) {
    val keyHandlerState = KeyHandler.getInstance().keyHandlerState
    val count = operatorArguments.count1

    // Create a TextObjectActionHandler that computes the range
    val textObjectHandler = ApiTextObjectActionHandler(listenerOwner, mappingOwner, preserveSelectionAnchor, rangeProvider)

    if (editor.mode !is Mode.OP_PENDING) {
      // In visual or normal mode: directly execute the selection for each caret
      for (caret in editor.carets()) {
        val range = textObjectHandler.getRange(editor, caret, context, count, operatorArguments.count0)
        if (range != null) {
          if (editor.mode is Mode.VISUAL) {
            caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
          } else {
            caret.moveToOffset(range.startOffset)
          }
        }
      }
    } else {
      // In operator-pending mode: add the handler to the command builder
      keyHandlerState.commandBuilder.addAction(textObjectHandler)
    }
  }
}

/**
 * TextObjectActionHandler that delegates to the API's range provider.
 */
private class ApiTextObjectActionHandler(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
  override val preserveSelectionAnchor: Boolean,
  private val rangeProvider: VimApi.(count: Int) -> TextObjectRange?,
) : TextObjectActionHandler() {

  // Will be set based on the result of rangeProvider
  private var computedVisualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override val visualType: TextObjectVisualType
    get() = computedVisualType

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val vimApi = VimApiImpl(listenerOwner, mappingOwner)

    // Execute the range provider
    val apiRange = vimApi.rangeProvider(count) ?: return null

    // Convert API range to internal TextRange and set visual type
    return when (apiRange) {
      is TextObjectRange.CharacterWise -> {
        computedVisualType = TextObjectVisualType.CHARACTER_WISE
        TextRange(apiRange.start, apiRange.end)
      }
      is TextObjectRange.LineWise -> {
        computedVisualType = TextObjectVisualType.LINE_WISE
        val startOffset = editor.getLineStartOffset(apiRange.startLine)
        val endOffset = editor.getLineEndOffset(apiRange.endLine, true)
        TextRange(startOffset, endOffset)
      }
    }
  }
}
