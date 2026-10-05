/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.maddyhome.idea.vim.api.ActionIdCompletion
import com.maddyhome.idea.vim.group.action.ActionIdInfo
import com.maddyhome.idea.vim.group.action.FrontendActionIds
import com.maddyhome.idea.vim.vimscript.services.VimRcService

/**
 * Completes IDE action IDs in the ideavimrc file, in `<Action>(...)` and in the `:action` argument.
 *
 * IdeaVim doesn't register a file type for the ideavimrc: the platform opens it with the bundled TextMate VimL
 * grammar, or the user's own file type association. So the contributor is registered for any language and recognises
 * the file by its name. It is registered in the common module because in split mode the completion runs on the
 * backend.
 */
internal class IdeaVimRcCompletionContributor : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    if (!isIdeaVimRc(parameters.originalFile)) return

    val text = parameters.originalFile.viewProvider.contents
    val prefix = ActionIdCompletion.findPrefix(lineBeforeOffset(text, parameters.offset))?.prefix ?: return

    // PlainPrefixMatcher is one of the matchers that can be sent to the frontend lookup in split mode
    val actionIdResult = result.withPrefixMatcher(PlainPrefixMatcher(prefix, true))
    val actions = FrontendActionIds.getInstance()
    ActionIdCompletion.findMatches(actions.allActionIds(), prefix).forEach {
      actionIdResult.addElement(createLookupElement(it, actions.findInfo(it)))
    }
    // Don't mix action IDs with the words from the file offered by the TextMate completion
    result.stopHere()
  }
}

/** `ReformatCode  Reformat Code` with the description, if any, aligned to the right */
private fun createLookupElement(actionId: String, info: ActionIdInfo?): LookupElement {
  return LookupElementBuilder.create(actionId)
    .withTailText(info?.text?.let { " $it" }, true)
    .withTypeText(info?.description)
}

/**
 * Opens the completion lookup when an action ID is about to be typed: after `<Action>(` or `:action `.
 */
internal class IdeaVimRcTypedHandler : TypedHandlerDelegate() {
  override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (charTyped != '(' && charTyped != ' ') return Result.CONTINUE
    if (!isIdeaVimRc(file)) return Result.CONTINUE

    // Called before the typed character is inserted
    val line = lineBeforeOffset(editor.document.charsSequence, editor.caretModel.offset) + charTyped
    if (ActionIdCompletion.findPrefix(line)?.prefix?.isEmpty() != true) return Result.CONTINUE

    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
    return Result.STOP
  }
}

private fun isIdeaVimRc(file: PsiFile): Boolean = file.name.endsWith(VimRcService.VIMRC_FILE_NAME)

private fun lineBeforeOffset(text: CharSequence, offset: Int): String {
  val lineStart = text.lastIndexOf('\n', offset - 1) + 1
  return text.substring(lineStart, offset)
}
