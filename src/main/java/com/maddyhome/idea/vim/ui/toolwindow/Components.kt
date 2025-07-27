/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.toolwindow

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Dimension
import javax.swing.Icon
import kotlin.script.experimental.api.ScriptDiagnostic


fun ActionButton(name: String, icon: Icon, onClick: () -> Unit): ActionButton {
  val dynamicAction = object : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      onClick()
    }
  }
  ActionManager.getInstance().registerAction("IdeaVim config: " + name, dynamicAction)
  dynamicAction.templatePresentation.icon = icon
  val presentation: Presentation? = dynamicAction.templatePresentation.clone()
  presentation?.text = name
  return ActionButton(dynamicAction, presentation, "DynamicRunAction", Dimension(22, 24))
}

fun ConsoleViewImpl.setText(text: String, contentType: ConsoleViewContentType) {
  clear()
  print(text, contentType)
  this.scrollTo(0)
}

fun ConsoleViewImpl.printScriptDiagnostic(diagnostic: ScriptDiagnostic) {
  val severityContentType = when (diagnostic.severity) {
    ScriptDiagnostic.Severity.ERROR -> createContentType("SEVERITY_ERROR", JBColor.RED)
    ScriptDiagnostic.Severity.WARNING -> createContentType("SEVERITY_WARNING", JBColor.ORANGE)
    ScriptDiagnostic.Severity.INFO -> createContentType("SEVERITY_INFO", JBColor.BLUE)
    ScriptDiagnostic.Severity.DEBUG -> createContentType("SEVERITY_DEBUG", JBColor.GRAY)
    ScriptDiagnostic.Severity.FATAL -> createContentType("SEVERITY_FATAL", JBColor.RED, bold = true)
  }
  val codeContentType = createContentType("DIAGNOSTIC_CODE", JBColor.GREEN)
  val exceptionContentType = createContentType("EXCEPTION", JBColor.RED)

  val locationText = diagnostic.location?.let { location ->
    "at line ${location.start.line} column ${location.start.col}"
  } ?: ""

  print("${diagnostic.severity}: ${diagnostic.message} $locationText\n", severityContentType)

  print("Exit code: ", ConsoleViewContentType.NORMAL_OUTPUT)
  print("${diagnostic.code}\n", codeContentType)

  diagnostic.exception?.let {
    print("Exception: ", ConsoleViewContentType.NORMAL_OUTPUT)
    print("${it.javaClass.name}: ${it.message}\n", exceptionContentType)
    it.stackTrace.forEach { stackTraceElement ->
      print("\tat $stackTraceElement\n", exceptionContentType)
    }
  }
  print("\n", ConsoleViewContentType.NORMAL_OUTPUT) // Add space between diagnostics
}

fun createContentType(name: String, color: Color, bold: Boolean = false): ConsoleViewContentType {
  val attributes = TextAttributes(color, null, null, null, if (bold) 1 else 0)
  return ConsoleViewContentType(name, attributes)
}