/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.intellij.idea.TestFor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * An AIR prompt ignores the platform's Enter action and breaks a line only with its own `Air.Chat.Prompt.NewLine`.
 */
@TestFor(issues = ["VIM-3786"])
class AirPromptNewLineTest : VimTestCase() {
  private lateinit var originalEnterHandler: EditorActionHandler

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    val editorActionManager = EditorActionManager.getInstance()
    originalEnterHandler = editorActionManager.getActionHandler(IdeActions.ACTION_EDITOR_ENTER)
    editorActionManager.setActionHandler(IdeActions.ACTION_EDITOR_ENTER, PromptLineBreakHandler(originalEnterHandler))
    ActionManager.getInstance().registerAction(AIR_NEW_LINE_ACTION, FakeAirNewLineAction())
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    EditorActionManager.getInstance().setActionHandler(IdeActions.ACTION_EDITOR_ENTER, originalEnterHandler)
    ActionManager.getInstance().unregisterAction(AIR_NEW_LINE_ACTION)
    super.tearDown(testInfo)
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test o opens a line below in AIR prompt`() {
    configurePrompt(
      """
        |first ${c}line
        |second line
      """.trimMargin()
    )
    typeText("o")
    assertState(
      """
        |first line
        |$c
        |second line
      """.trimMargin()
    )
    assertMode(Mode.INSERT)
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test O opens a line above in AIR prompt`() {
    configurePrompt(
      """
        |first line
        |second ${c}line
      """.trimMargin()
    )
    typeText("O")
    assertState(
      """
        |first line
        |$c
        |second line
      """.trimMargin()
    )
    assertMode(Mode.INSERT)
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test O opens a line above the first line in AIR prompt`() {
    configurePrompt("first ${c}line")
    typeText("O")
    assertState(
      """
        |$c
        |first line
      """.trimMargin()
    )
    assertMode(Mode.INSERT)
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test o with text is repeated in AIR prompt`() {
    configurePrompt("first ${c}line")
    typeText("oabc<Esc>.")
    assertState(
      """
        |first line
        |abc
        |ab${c}c
      """.trimMargin()
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  fun `test o in other editor still uses Enter action`() {
    configureByText("first ${c}line")
    typeText("o")
    assertState(
      """
        |first line
        |$c
      """.trimMargin()
    )
  }

  private fun configurePrompt(text: String) {
    configureByText(text)
    fixture.editor.document.putUserData(AIR_PROMPT_DOCUMENT, true)
  }

  /** Like AIR's `AirPromptLineBreakHandler` */
  private class PromptLineBreakHandler(private val original: EditorActionHandler) : EditorActionHandler() {
    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean =
      isPrompt(editor) || original.isEnabled(editor, caret, dataContext)

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
      if (isPrompt(editor)) return
      original.execute(editor, caret, dataContext)
    }

    private fun isPrompt(editor: Editor) = editor.document.getUserData(AIR_PROMPT_DOCUMENT) == true
  }

  /** Like AIR's `AirPromptNewLineAction` */
  private class FakeAirNewLineAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      WriteCommandAction.runWriteCommandAction(e.project) {
        EditorModificationUtilEx.insertStringAtCaret(editor, "\n")
      }
    }
  }

  private companion object {
    const val AIR_NEW_LINE_ACTION = "Air.Chat.Prompt.NewLine"
    val AIR_PROMPT_DOCUMENT: Key<Boolean> = Key.create("AIR_PROMPT_INPUT_DOCUMENT")
  }
}
