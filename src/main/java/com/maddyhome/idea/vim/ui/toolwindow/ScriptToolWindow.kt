/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.toolwindow

import com.intellij.codeInsight.AutoPopupController
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.maddyhome.idea.vim.api.injector
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import kotlin.script.experimental.api.ScriptDiagnostic


class ScriptToolWindow(private val project: Project) : Disposable {
  private val panel = SimpleToolWindowPanel(true, true)
  private var editor: EditorEx? = null
  private var outputConsole: ConsoleViewImpl? = null

  init {
    setupUI()
  }

  private fun setupUI() {
    // Create the main panel with BorderLayout
    val contentPanel = JPanel(BorderLayout())
    contentPanel.border = JBUI.Borders.empty(10)

    // Create editor for Kotlin script
    editor = createEditor(project) ?: return

    val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))

    ActionButton(name = "Execute Script", icon = AllIcons.Actions.Execute) {
      val scriptCode = editor!!.document.text
      injector.kotlinScriptService.executeScript(
        sourceCode = scriptCode,
        onCompilationError = { diagnostics ->
          outputConsole?.clear()
          diagnostics.filter { it.severity == ScriptDiagnostic.Severity.ERROR }.forEach {
            outputConsole?.printScriptDiagnostic(it)
          }
        },
        onExecutionError = { diagnostics, output ->
          outputConsole?.clear()
          diagnostics.filter { it.severity == ScriptDiagnostic.Severity.ERROR }.forEach {
            outputConsole?.printScriptDiagnostic(it)
          }
        },
        onFinished = { output ->
          outputConsole?.clear()
          outputConsole?.setText(output, ConsoleViewContentType.NORMAL_OUTPUT)
        }
      )
    }.also { toolbar.add(it) }

    ActionButton(name = "Unload Changes", icon = AllIcons.Actions.Cancel) {
      unloadChanges()
      injector.application.invokeLater {
        editor?.document?.setText(DEFAULT_SCRIPT_TEMPLATE)
      }
      outputConsole?.setText("Script unloaded successfully.\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }.also { toolbar.add(it) }

    contentPanel.add(toolbar, BorderLayout.NORTH)

    // create console
    outputConsole = ConsoleViewImpl(project, true)
    contentPanel.add(outputConsole!!.component, BorderLayout.SOUTH)

    val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    splitPane.topComponent = JBScrollPane(editor!!.component)
    splitPane.bottomComponent = outputConsole!!.component
    splitPane.dividerLocation = splitPane.height - 200
    contentPanel.add(splitPane, BorderLayout.CENTER)

    panel.setContent(contentPanel)
  }

  private fun configureEditorSettings(settings: EditorSettings) {
    settings.isLineNumbersShown = true
    settings.isLineMarkerAreaShown = true
    settings.isFoldingOutlineShown = true
    settings.additionalLinesCount = 3
    settings.additionalColumnsCount = 3
    settings.isRightMarginShown = true
    settings.isAutoCodeFoldingEnabled = true
  }

  private fun createEditor(project: Project): EditorEx? {
    val editorFactory = EditorFactory.getInstance()
    val document = editorFactory.createDocument(DEFAULT_SCRIPT_TEMPLATE)
    val kotlinLanguage = Language.findLanguageByID("kotlin")?.associatedFileType as? FileType ?: return null

    injector.application.invokeLater {
      PsiDocumentManager.getInstance(project).commitDocument(document)
    }

    val editor = editorFactory.createEditor(document, project, kotlinLanguage, false) as EditorEx

    // Configure editor settings
    configureEditorSettings(editor.settings)

    val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(kotlinLanguage, project, editor.virtualFile)
    val highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, EditorColorsSchemeImpl(null))
    editor.highlighter = highlighter

    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)

    return editor
  }

  private fun unloadChanges() {
    injector.kotlinScriptService.unloadChanges()
  }

  fun getContent(): JComponent = panel

  override fun dispose() {
    unloadChanges()
    editor?.let {
      EditorFactory.getInstance().releaseEditor(it)
    }
    editor = null
  }

  companion object {
    private val DEFAULT_SCRIPT_TEMPLATE = """
            // IdeaVim Kotlin Script
            
            import com.intellij.vim.api.scopes.*
            import com.intellij.vim.api.*

            mappings {
                nmap("ll", "$")
            }
            
            println("Mapping added successfully.")
            
        """.trimIndent()
  }
}
