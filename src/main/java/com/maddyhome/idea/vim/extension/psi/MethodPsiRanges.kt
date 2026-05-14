/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.psi

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.maddyhome.idea.vim.helper.PsiHelper

/**
 * Boundaries of a function/method definition for use by text objects.
 *
 * @property fullStart  offset including JavaDoc and preceding annotations (for `aM`).
 * @property definitionStart offset where the signature begins ignoring JavaDoc + annotations (for `am`).
 * @property end exclusive end offset of the entire method.
 * @property body offsets of the body's content (between the body braces, exclusive of the braces themselves),
 *                or null when no body can be located.
 */
internal data class MethodRanges(
  val fullStart: Int,
  val definitionStart: Int,
  val end: Int,
  val body: Pair<Int, Int>?,
)

/**
 * Locates the function/method definition that surrounds a caret offset by walking the PSI tree.
 *
 * Method detection uses a per-language dispatch table of PSI implementation class simple-names;
 * languages not in the table fall back to a name heuristic (`*Method*` / `*Function*`). String
 * matching is used so the main module doesn't need compile-time deps on each language plugin.
 */
internal object MethodPsiRanges {

  private val METHOD_CLASSES_BY_LANGUAGE = mapOf(
    "JAVA" to setOf("PsiMethodImpl"),
    "kotlin" to setOf("KtNamedFunction", "KtFunctionLiteral", "KtPropertyAccessor"),
    "Python" to setOf("PyFunctionImpl"),
    "JavaScript" to setOf("JSFunctionImpl", "JSFunctionExpressionImpl"),
    "TypeScript" to setOf("TypeScriptFunctionImpl", "TypeScriptFunctionExpressionImpl"),
    "go" to setOf("GoMethodDeclarationImpl", "GoFunctionDeclarationImpl"),
    "Dart" to setOf("DartMethodDeclarationImpl", "DartFunctionDeclarationImpl"),
    "PHP" to setOf("MethodImpl", "FunctionImpl"),
    "ruby" to setOf("RMethodImpl"),
    "Rust" to setOf("RsFunctionImpl"),
  )

  fun find(editor: Editor, offset: Int): MethodRanges? {
    val file = PsiHelper.getFile(editor) ?: return null
    val safeOffset = offset.coerceAtMost(file.textLength - 1).coerceAtLeast(0)
    val element = file.findElementAt(safeOffset) ?: return null
    val method = findEnclosingMethod(element) ?: return null
    return buildRanges(method)
  }

  private fun findEnclosingMethod(start: PsiElement): PsiElement? {
    val knownClasses = METHOD_CLASSES_BY_LANGUAGE[start.language.id].orEmpty()
    var current: PsiElement? = start
    while (current != null) {
      val name = current.javaClass.simpleName
      if (name in knownClasses || matchesMethodHeuristic(name)) return current
      current = current.parent
    }
    return null
  }

  private fun matchesMethodHeuristic(name: String): Boolean {
    if (name.contains("CodeBlock") || name.contains("Block") || name.contains("PlainText")) return false
    return name.contains("Method") || name.contains("Function")
  }

  private fun buildRanges(method: PsiElement): MethodRanges {
    val fullRange = method.textRange
    return MethodRanges(
      fullStart = fullRange.startOffset,
      definitionStart = computeDefinitionStart(method),
      end = fullRange.endOffset,
      body = computeBodyRange(method),
    )
  }

  /**
   * Returns the offset of the signature itself, skipping leading JavaDoc, whitespace, and
   * annotations. Some languages wrap annotations and modifiers in a single "modifier list"
   * element — in that case we walk inside it so that modifiers like `public` are preserved.
   */
  private fun computeDefinitionStart(method: PsiElement): Int {
    val firstSignificant = firstNonTrivialChild(method) ?: return method.textRange.startOffset
    if (looksLikeModifierList(firstSignificant)) {
      return startOfFirstNonAnnotation(firstSignificant)
        ?: firstSignificant.nextSibling?.textRange?.startOffset
        ?: firstSignificant.textRange.endOffset
    }
    return skipAnnotationSiblings(firstSignificant)
  }

  private fun firstNonTrivialChild(parent: PsiElement): PsiElement? {
    var child: PsiElement? = parent.firstChild
    while (child != null && (child is PsiComment || child is PsiWhiteSpace)) {
      child = child.nextSibling
    }
    return child
  }

  private fun looksLikeModifierList(element: PsiElement): Boolean =
    element.javaClass.simpleName.contains("ModifierList")

  private fun startOfFirstNonAnnotation(modifierList: PsiElement): Int? {
    var child: PsiElement? = modifierList.firstChild
    while (child != null && (isAnnotationLike(child) || child is PsiComment || child is PsiWhiteSpace)) {
      child = child.nextSibling
    }
    return child?.textRange?.startOffset
  }

  private fun skipAnnotationSiblings(start: PsiElement): Int {
    var current: PsiElement? = start
    while (current != null && isAnnotationLike(current)) {
      var next = current.nextSibling
      while (next != null && (next is PsiComment || next is PsiWhiteSpace)) next = next.nextSibling
      current = next
    }
    return current?.textRange?.startOffset ?: start.textRange.startOffset
  }

  private fun isAnnotationLike(element: PsiElement): Boolean {
    val name = element.javaClass.simpleName
    return name.contains("Annotation") && !name.contains("AnnotationsList") || name == "KtAnnotationEntry"
  }

  /**
   * Body content range (strictly between the outermost `{` and `}` of the method body), or null
   * when there is no body element (abstract / interface methods).
   */
  private fun computeBodyRange(method: PsiElement): Pair<Int, Int>? {
    val body = findBodyElement(method) ?: return null
    val text = body.text
    val range = body.textRange
    return if (text.startsWith("{") && text.endsWith("}")) {
      range.startOffset + 1 to range.endOffset - 1
    } else {
      range.startOffset to range.endOffset
    }
  }

  private fun findBodyElement(method: PsiElement): PsiElement? {
    var child: PsiElement? = method.firstChild
    while (child != null) {
      val name = child.javaClass.simpleName
      if (name.contains("CodeBlock") || name.contains("BlockExpression") || name == "PsiMethodBody") {
        return child
      }
      child = child.nextSibling
    }
    return null
  }
}
