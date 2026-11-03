/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.format

import com.intellij.application.options.CodeStyle
import com.intellij.lang.Language
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.Strings
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.ExternalFormatProcessor
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.util.PsiUtilCore
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [FormatRemoteApi].
 *
 * Runs on the backend, so [CommandProcessor] groups the document modifications into a single undo
 * step. Caret positioning is left to the caller (vim-engine layer), which knows which caret the
 * range belongs to - important for per-caret handlers like `==`.
 */
internal class FormatRemoteApiImpl : FormatRemoteApi {
  override suspend fun format(
    editorId: EditorId,
    startOffset: Int,
    endOffset: Int,
  ) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val project = editor.project ?: return@onEdt
    val lines = expandToWholeLines(editor.document, startOffset, endOffset)

    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
    val externalFormatter = psiFile?.let { ExternalFormatProcessor.activeExternalFormatProcessor(it) }
    if (psiFile != null && externalFormatter != null) {
      formatExternally(externalFormatter, project, psiFile, lines)
    } else {
      autoIndentLines(editor, lines)
    }
  }

  /**
   * Works like [CodeStyle.getLineIndent], as the Enter handler does: a [LineIndentProvider] of the language answers
   * straight from the text, and the formatter, which needs the PSI to be up to date, is only asked if none does.
   *
   * The document is committed up front because the fallback to the formatter would otherwise commit it from inside the
   * read action.
   */
  override suspend fun lineIndent(editorId: EditorId, offset: Int): String? = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt null
    val project = editor.project ?: return@onEdt null
    val documentManager = PsiDocumentManager.getInstance(project)
    ApplicationManager.getApplication().runWriteAction { documentManager.commitDocument(editor.document) }

    ApplicationManager.getApplication().runReadAction<String?> {
      val psiFile = documentManager.getPsiFile(editor.document) ?: return@runReadAction null
      val language = PsiUtilCore.getLanguageAtOffset(psiFile, offset)
      val answers = askLineIndentProviders(project, editor, language, offset)
      val indent = answers.firstOrNull { !it.isDoNotAdjust() }
      val providersSayDoNotAdjust = answers.isNotEmpty() && answers.all { it.isDoNotAdjust() }
      when {
        indent != null -> indent
        providersSayDoNotAdjust -> null
        hasFormatter(psiFile) -> askFormatter(editor, language, offset)
        else -> null
      }
    }
  }

  /**
   * The answers of all the [LineIndentProvider]s of [language], not only of the first one like
   * [CodeStyle.getLineIndent]: in Rider, a dummy provider of C# answers [LineIndentProvider.DO_NOT_ADJUST] because the
   * Enter handler runs on the ReSharper backend, while another one works the indent out on the frontend. Rider has no
   * formatter for C# on the frontend, so without that provider there would be no indent at all.
   *
   * Providers that don't know the indent (null) are left out.
   */
  private fun askLineIndentProviders(project: Project, editor: Editor, language: Language, offset: Int): List<String> =
    LINE_INDENT_PROVIDER_EP.extensionList
      .filter { it.isSuitableFor(language) }
      .mapNotNull { it.getLineIndent(project, editor, language, offset) }

  /** [LineIndentProvider.DO_NOT_ADJUST] is told apart from an indent by its identity, not by its content */
  private fun String.isDoNotAdjust(): Boolean = Strings.areSameInstance(this, LineIndentProvider.DO_NOT_ADJUST)

  private fun hasFormatter(psiFile: PsiFile): Boolean = LanguageFormatting.INSTANCE.forContext(psiFile) != null

  private fun askFormatter(editor: Editor, language: Language, offset: Int): String? =
    CodeStyle.getLineIndent(editor, language, offset, true)

  /**
   * A file owned by an [ExternalFormatProcessor] (C/C++ in CLion Nova, where formatting lives in the
   * Radler process) cannot be handled by the auto-indent action: that action is delegated further,
   * to the external process, which reads its range from the editor selection - and a selection set
   * in the same event-loop turn as the dispatch does not get there in time, so the action falls back
   * to its no-selection behaviour and reformats the whole file.
   *
   * The processor is called directly rather than through `CodeStyleManager.reformatText`, which
   * re-derives the range from PSI smart pointers (`CoreCodeStyleUtil.RangeFormatInfo`). The C++ PSI
   * in Nova is a dummy tree of coarse nodes, so the range that comes out of it is not the one that
   * went in.
   *
   * Note that this reformats [lines] instead of only re-indenting them; Nova exposes no indent-only
   * entry point.
   */
  private fun formatExternally(
    formatter: ExternalFormatProcessor,
    project: Project,
    psiFile: PsiFile,
    lines: TextRange,
  ) {
    CommandProcessor.getInstance().executeCommand(project, {
      ApplicationManager.getApplication().runWriteAction {
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.getDocument(psiFile)?.let { documentManager.commitDocument(it) }
        formatter.format(psiFile, lines, false, false, true, -1)
      }
    }, COMMAND_NAME, null)
  }

  /** The auto-indent action takes its range from the editor selection, hence the selection dance. */
  private fun autoIndentLines(editor: Editor, lines: TextRange) {
    CommandProcessor.getInstance().executeCommand(editor.project, {
      val action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES)
      editor.caretModel.currentCaret.setSelection(lines.startOffset, lines.endOffset, false)
      try {
        ActionManager.getInstance().tryToExecute(action, null, editor.contentComponent, "IdeaVim", true).waitFor(5_000)
      } finally {
        editor.selectionModel.removeSelection()
      }
    }, COMMAND_NAME, null)
  }

  private fun expandToWholeLines(document: Document, startOffset: Int, endOffset: Int): TextRange {
    val startLine = document.getLineNumber(startOffset)
    var endLine = document.getLineNumber(endOffset)
    // A linewise range ends at the start of the line *after* the last one - don't pull that line in
    if (endLine > startLine && document.getLineStartOffset(endLine) == endOffset) {
      endLine--
    }
    return TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine))
  }

  private companion object {
    private const val COMMAND_NAME = "AutoIndent"

    /** [com.intellij.psi.codeStyle.lineIndent.LineIndentProviderEP] only gives the first provider of a language */
    private val LINE_INDENT_PROVIDER_EP = ExtensionPointName<LineIndentProvider>("com.intellij.lineIndentProvider")
  }
}
