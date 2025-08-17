/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.intellij.codeInsight.template.impl.editorActions.ExpandLiveTemplateByTabAction
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.TabAction
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.editor

/**
 * Provides ordering and prioritisation for actions mapped to Vim shortcuts
 *
 * We register [VimShortcutKeyAction] for any modifier-based shortcuts used in Vim mappings (e.g. `<C-V>` but not `V`).
 * If the IDE has multiple actions registered for the same shortcut, we typically want to promote [VimShortcutKeyAction]
 * so that it takes precedence over IDE actions. The action can also decide to disable itself, for example if the user
 * has chosen to use the IDE action instead of Vim, and then the next action in the now ordered list will be invoked.
 * This promoter is registered as "last", so we have a good chance or ensuring that our action is head of the list.
 *
 * This usually works fine (see VIM-2218) but things are a little more complex with Enter, Escape and Tab, which often
 * have multiple contextual actions, such as Escape removing Find Usage highlights as well as changing Vim mode.
 * Fortunately, actions for Escape and Enter are mostly implemented as [EditorActionHandler] instances, and IdeaVim can
 * control the order of IdeaVim's implementation in the `plugin.xml` registration.
 *
 * Tab doesn't seem to use the [EditorActionHandler] route, for some reason, and instead has multiple actions registered
 * to the `Tab` shortcut, such as expanding Live Templates (and Emmet patterns, see VIM-674). If we put
 * [VimShortcutKeyAction] before these actions, then we don't get these contextual actions. So we make sure to order
 * the list of actions such that [VimShortcutKeyAction] is after everything apart from [TabAction], which is responsible
 * for inserting the tab character.
 *
 * Most of the contextual actions are safe to call in all modes, apart from expanding Live Templates. This should only
 * be called in Insert/Select mode. We don't want to be modifying the document in Normal mode, unless the action is in
 * response to a visible prompt.
 *
 * We also need to make sure that we don't call [VimShortcutKeyAction] if the editor is in Insert mode, as it would
 * interfere with the normal behavior of the Tab key. The exception is if the user has remapped `Tab`.
 */
internal class VimActionsPromoter : ActionPromoter {
  override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction>? {
    val vimIndex = actions.indexOfFirst { it is AnActionWrapper && it.delegate is VimShortcutKeyAction }
    if (vimIndex == -1) return null

    val tabIndex = actions.indexOfFirst { it is TabAction }
    if (tabIndex != -1) {
      val editor = context.editor ?: return null
      if (editor.isIdeaVimDisabledHere) return null
      val mode = editor.vim.mode
      val vimAction = actions[vimIndex]

      val ordered = mutableListOf<AnAction>()
      actions.forEach {
        // Don't add VimShortcutKeyAction, yet
        if (it == vimAction) {
          return@forEach
        }

        // Add VimShortcutKeyAction just before TabAction.
        // TODO: We shouldn't add VimShortcutKeyAction for Tab in Insert mode
        // The action has a check for this and disables itself. This means we can't remap i_Tab
        if (it is TabAction) {
          ordered.add(vimAction)
        }

        // Unless we're in some kind of editable mode, don't allow ExpandLiveTemplateByTabAction. We don't want to
        // invoke an action that will edit text while we're in e.g., Normal. This isn't a hard and fast rule - we don't
        // remove InsertInlineCompletionAction or Next Edit Suggestions. These are ok because there are visible prompts,
        // so modifying text in Normal is expected.
        if (it is ExpandLiveTemplateByTabAction && mode != Mode.INSERT && mode != Mode.REPLACE && mode !is Mode.SELECT) {
          return@forEach
        }
        ordered.add(it)
      }

      // Returns all actions, in the order we've added them
      return ordered
    }
    else {
      // Return the action we want to promote
      return listOf(actions[vimIndex])
    }
  }
}
