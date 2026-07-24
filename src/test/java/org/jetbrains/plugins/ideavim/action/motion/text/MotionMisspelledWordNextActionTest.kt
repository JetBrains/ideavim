/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.spellchecker.SpellCheckerSeveritiesProvider
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the `]s` motion (jump to the next misspelled word).
 *
 * The motion finds the next word marked with a [SpellCheckerSeveritiesProvider.TYPO] highlight. In a running IDE
 * those highlights are produced by the spellchecker inspection, but that inspection's implementation lives in a
 * plugin that isn't on the test classpath. To keep the tests deterministic and independent of any dictionary, we
 * register a small annotator that marks a fixed set of "misspelled" words with TYPO severity, then force a
 * highlighting pass before running the motion. This exercises the exact same code path the real motion uses.
 */
@Suppress("SpellCheckingInspection", "RemoveCurlyBracesFromTemplate")
class MotionMisspelledWordNextActionTest : VimTestCase() {

  private class TypoAnnotator : Annotator {
    private val misspelledWords = setOf("recieve", "teh", "mispell", "typpo", "wolrd")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
      if (element !is PsiFile) return
      Regex("""\w+""").findAll(element.text).forEach { match ->
        if (match.value in misspelledWords) {
          holder.newSilentAnnotation(SpellCheckerSeveritiesProvider.TYPO)
            .range(TextRange(match.range.first, match.range.last + 1))
            .create()
        }
      }
    }
  }

  private fun markTypos(@Suppress("UNUSED_PARAMETER") editor: Editor) {
    LanguageAnnotators.INSTANCE.addExplicitExtension(
      PlainTextLanguage.INSTANCE,
      TypoAnnotator(),
      fixture.testRootDisposable,
    )
    fixture.doHighlighting()
  }

  @Test
  fun `test move to next misspelled word`() {
    doTest(
      "]s",
      "${c}I recieve teh mispell here",
      "I ${c}recieve teh mispell here",
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test move to next misspelled word from within a misspelled word`() {
    doTest(
      "]s",
      "I rec${c}ieve teh mispell here",
      "I recieve ${c}teh mispell here",
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test move to next misspelled word with count`() {
    doTest(
      "2]s",
      "${c}I recieve teh mispell here",
      "I recieve ${c}teh mispell here",
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test count larger than remaining misspelled words stops at last`() {
    doTest(
      "9]s",
      "${c}I recieve teh mispell here",
      "I recieve teh ${c}mispell here",
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test move to next misspelled word across lines`() {
    doTest(
      "]s",
      """
        |${c}This line is fine
        |But this one has a typpo
      """.trimMargin(),
      """
        |This line is fine
        |But this one has a ${c}typpo
      """.trimMargin(),
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test no motion when no misspelled words after caret`() {
    doTest(
      "]s",
      "I recieve teh mispell ${c}here",
      "I recieve teh mispell ${c}here",
      afterEditorInitialized = ::markTypos,
    )
  }

  @Test
  fun `test no motion when text has no misspelled words`() {
    doTest(
      "]s",
      "${c}the quick brown fox",
      "${c}the quick brown fox",
      afterEditorInitialized = ::markTypos,
    )
  }
}
