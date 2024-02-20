/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiEditorUtil
import com.maddyhome.idea.vim.extension.ExtensionBeanClass
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.vimscript.model.commands.SetCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser

internal class UsePlugSyntaxInspection : LocalInspectionTool() {
  override fun getGroupDisplayName(): String {
    return "Vim"
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.name != ".ideavimrc" && file.name != "_ideavimrc") return PsiElementVisitor.EMPTY_VISITOR
    val plugins = buildPlugins()
    return object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (element !is LeafPsiElement) return
        val myScript = VimscriptParser.parse(element.text)
        myScript.units.forEach { unit ->
          if (unit is SetCommand) {
            val argument = unit.argument
            val alias = plugins[argument]
            if (alias != null) {
              holder.registerProblem(
                element,
                unit.rangeInScript.let { TextRange(it.startOffset, it.endOffset - 1) },
                """
                  Use `Plug` syntax for defining extensions
                """.trimIndent(),
                object : LocalQuickFix {
                  override fun getFamilyName(): String {
                    return "Use Plug syntax"
                  }

                  override fun applyFix(p0: Project, p1: ProblemDescriptor) {
                    val editor = PsiEditorUtil.findEditor(file)
                    editor?.document?.replaceString(
                      unit.rangeInScript.startOffset,
                      unit.rangeInScript.endOffset - 1,
                      "Plug '$alias'"
                    )
                  }
                }
              )
            }
          }
        }
      }
    }
  }

  private fun buildPlugins(): HashMap<String, String> {
    val res = HashMap<String, String>()
    VimExtension.EP_NAME.extensions.forEach { extension: ExtensionBeanClass ->
      val alias = extension.aliases?.first { it.name?.count { it == '/' } == 1 }?.name
        ?: extension.aliases?.firstOrNull()?.name
      val name = extension.name
      if (alias != null && name != null) {
        res[name] = alias
      }
    }
    return res
  }
}
