/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.psi

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.maddyhome.idea.vim.helper.PsiHelper

/**
 * Locates the smallest class-like declaration that surrounds a caret offset by walking the PSI
 * tree.
 *
 * Class detection uses a per-language dispatch table of PSI implementation class simple-names;
 * languages not in the table fall back to a name heuristic. String matching is used so the main
 * module doesn't need compile-time deps on each language plugin.
 */
internal object ClassRangeFinder {

  private val CLASS_CLASSES_BY_LANGUAGE = mapOf(
    "JAVA" to setOf("PsiClassImpl", "PsiAnonymousClassImpl", "PsiEnumConstantInitializerImpl"),
    "kotlin" to setOf("KtClass", "KtObjectDeclaration", "KtEnumEntry"),
    "Python" to setOf("PyClassImpl"),
    "JavaScript" to setOf("ES6ClassImpl"),
    "TypeScript" to setOf("TypeScriptClassImpl"),
    "go" to setOf("GoTypeDeclarationImpl"),
    "Dart" to setOf("DartClassDefinitionImpl"),
    "PHP" to setOf("PhpClassImpl"),
    "ruby" to setOf("RClassImpl", "RModuleImpl"),
    "Rust" to setOf("RsStructItemImpl", "RsImplItemImpl", "RsTraitItemImpl", "RsEnumItemImpl"),
  )

  fun find(editor: Editor, offset: Int): Pair<Int, Int>? {
    val file = PsiHelper.getFile(editor) ?: return null
    val safeOffset = offset.coerceAtMost(file.textLength - 1).coerceAtLeast(0)
    val element = file.findElementAt(safeOffset) ?: return null
    val classElement = findEnclosingClass(element) ?: return null
    val range = classElement.textRange
    return range.startOffset to range.endOffset
  }

  private fun findEnclosingClass(start: PsiElement): PsiElement? {
    val knownClasses = CLASS_CLASSES_BY_LANGUAGE[start.language.id].orEmpty()
    var current: PsiElement? = start
    while (current != null) {
      val name = current.javaClass.simpleName
      if (name in knownClasses || matchesClassHeuristic(name)) return current
      current = current.parent
    }
    return null
  }

  private fun matchesClassHeuristic(name: String): Boolean {
    if (name.contains("PlainText") || name.contains("File")) return false
    return name.endsWith("ClassImpl") ||
      name.endsWith("InterfaceImpl") ||
      name.endsWith("EnumImpl") ||
      name.endsWith("TraitImpl") ||
      name.endsWith("ObjectDeclaration") ||
      name.endsWith("StructImpl") ||
      name.endsWith("ImplItemImpl")
  }
}
