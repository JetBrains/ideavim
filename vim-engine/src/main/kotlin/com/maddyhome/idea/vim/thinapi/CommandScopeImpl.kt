/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.CommandScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlinx.coroutines.runBlocking

class CommandScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : CommandScope {
  override fun register(
    command: String,
    block: suspend VimApi.(String, Int, Int) -> Unit,
  ) {
    val commandHandler = object : CommandAliasHandler {
      override fun execute(
        command: String,
        range: com.maddyhome.idea.vim.ex.ranges.Range,
        editor: VimEditor,
        context: ExecutionContext,
      ) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner, editor.projectId)
        val lineRange = range.getLineRange(editor, editor.primaryCaret())
        runBlocking { vimApi.block(command, lineRange.startLine, lineRange.endLine) }
      }
    }
    injector.pluginService.addCommand(command, commandHandler)
  }

  override fun exportOperatorFunction(name: String, function: suspend VimApi.() -> Boolean) {
    val operatorFunction: OperatorFunction = object : OperatorFunction {
      override fun apply(
        editor: VimEditor,
        context: ExecutionContext,
        selectionType: SelectionType?,
      ): Boolean {
        var returnValue = false
        injector.actionExecutor.executeCommand(editor, {
          runBlocking {
            returnValue = VimApiImpl(listenerOwner, mappingOwner, editor.projectId).function()
          }
        }, "Insert Text", null)
        return returnValue
      }
    }
    injector.pluginService.exportOperatorFunction(name, operatorFunction)
  }

  override suspend fun setOperatorFunction(name: String) {
    injector.globalOptions().operatorfunc = name
  }
}
