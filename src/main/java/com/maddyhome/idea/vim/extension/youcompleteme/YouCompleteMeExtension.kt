/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.youcompleteme

import com.intellij.openapi.actionSystem.IdeActions
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * Makes `<Tab>` cycle through the IntelliJ code completion popup, SuperTab / YouCompleteMe style.
 *
 * While the completion popup (lookup) is open, `<Tab>` selects the next item and `<S-Tab>` the previous one.
 * When no popup is open, `<Tab>` falls back to its normal insert-mode behaviour (indentation), so ordinary
 * tabbing is not broken.
 *
 * By default `<Tab>` is part of the `lookupkeys` option, which means that while a lookup is open the IDE -
 * not IdeaVim - receives the key and accepts the current item. To make Tab cycle instead, this extension
 * removes `<Tab>` and `<S-Tab>` from `lookupkeys` so that IdeaVim handles them, and maps them to the same
 * logic as the native `<C-N>`/`<C-P>` insert-mode completion keys.
 */
internal class YouCompleteMeExtension : VimExtension {
  override fun getName(): String = "youcompleteme"

  override fun init() {
    // Let IdeaVim - rather than the IDE's lookup - receive Tab/Shift-Tab while the popup is open.
    removeFromLookupKeys("<Tab>", "<S-Tab>")

    putExtensionHandlerMapping(
      MappingMode.I,
      injector.parser.parseKeys("<Tab>"),
      owner,
      CycleHandler(forward = true),
      false
    )
    putExtensionHandlerMapping(
      MappingMode.I,
      injector.parser.parseKeys("<S-Tab>"),
      owner,
      CycleHandler(forward = false),
      false
    )
  }

  private fun removeFromLookupKeys(vararg keys: String) {
    val option = IjOptions.lookupkeys
    val scope = OptionAccessScope.GLOBAL(null)
    var value = injector.optionGroup.getOptionValue(option, scope)
    for (key in keys) {
      value = option.removeValue(value, VimString(key)) as VimString
    }
    injector.optionGroup.setOptionValue(option, scope, value)
  }

  /**
   * Cycles the active completion lookup in the given direction, or falls back to the default Tab behaviour
   * (indentation) when there is no active lookup.
   */
  private class CycleHandler(private val forward: Boolean) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val lookup = injector.lookupManager.getActiveLookup(editor)
      if (lookup != null) {
        val caret = editor.primaryCaret()
        if (forward) lookup.down(caret, context) else lookup.up(caret, context)
      } else if (forward) {
        injector.actionExecutor.executeAction(editor, IdeActions.ACTION_EDITOR_TAB, context)
      }
    }
  }
}
