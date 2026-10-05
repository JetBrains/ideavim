/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.maddyhome.idea.vim.group.action.ActionIdInfo
import com.maddyhome.idea.vim.group.action.FrontendActionIds
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IdeaVimRcCompletionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    val actionManager = ActionManager.getInstance()
    actionManager.registerAction("${PREFIX}Alpha", NoopAction("Alpha text", "Alpha description"))
    actionManager.registerAction("${PREFIX}Beta", NoopAction("Beta text", null))
    actionManager.registerAction("${PREFIX}Bravo", NoopAction(null, null))
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    ACTION_IDS.forEach { ActionManager.getInstance().unregisterAction(it) }
    FrontendActionIds.getInstance().actions = emptyMap()
    super.tearDown(testInfo)
  }

  @Test
  fun `test completes action notation`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}A<caret>")
    assertText("nmap x <Action>(${PREFIX}Alpha")
  }

  @Test
  fun `test offers only matching action ids`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}B<caret>")
    assertEquals(setOf("${PREFIX}Beta", "${PREFIX}Bravo"), fixture.lookupElementStrings?.toSet())
  }

  @Test
  fun `test action id completion is case insensitive`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX.lowercase()}a<caret>")
    assertText("nmap x <Action>(${PREFIX}Alpha")
  }

  @Test
  fun `test completes action command`() {
    complete(".ideavimrc", "action ${PREFIX}A<caret>")
    assertText("action ${PREFIX}Alpha")
  }

  @Test
  fun `test completes action command in mapping`() {
    complete(".ideavimrc", "nnoremap x :action ${PREFIX}A<caret><CR>")
    assertText("nnoremap x :action ${PREFIX}Alpha<CR>")
  }

  @Test
  fun `test completes on second line`() {
    complete(".ideavimrc", "set number\nnmap x <Action>(${PREFIX}A<caret>\nset hlsearch")
    assertText("set number\nnmap x <Action>(${PREFIX}Alpha\nset hlsearch")
  }

  @Test
  fun `test completes action ids received from frontend`() {
    receiveFromFrontend(ActionIdInfo("${PREFIX}FrontendOnly", null, null))
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}F<caret>")
    assertText("nmap x <Action>(${PREFIX}FrontendOnly")
  }

  @Test
  fun `test lookup shows action text and description`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}<caret>")
    assertPresentation("${PREFIX}Alpha", " Alpha text", "Alpha description")
  }

  @Test
  fun `test lookup shows action text without description`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}<caret>")
    assertPresentation("${PREFIX}Beta", " Beta text", null)
  }

  @Test
  fun `test lookup shows action without text`() {
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}<caret>")
    assertPresentation("${PREFIX}Bravo", null, null)
  }

  @Test
  fun `test lookup shows text and description received from frontend`() {
    receiveFromFrontend(
      ActionIdInfo("${PREFIX}FrontendOnly", "Frontend text", "Frontend description"),
      ActionIdInfo("${PREFIX}FrontendOther", null, null),
    )
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}F<caret>")
    assertPresentation("${PREFIX}FrontendOnly", " Frontend text", "Frontend description")
  }

  @Test
  fun `test lookup prefers local text over the one received from frontend`() {
    receiveFromFrontend(ActionIdInfo("${PREFIX}Alpha", "Frontend text", "Frontend description"))
    complete(".ideavimrc", "nmap x <Action>(${PREFIX}<caret>")
    assertPresentation("${PREFIX}Alpha", " Alpha text", "Alpha description")
  }

  @Test
  fun `test completes in xdg ideavimrc`() {
    complete("ideavimrc", "nmap x <Action>(${PREFIX}A<caret>")
    assertText("nmap x <Action>(${PREFIX}Alpha")
  }

  @Test
  fun `test completes in windows ideavimrc`() {
    complete("_ideavimrc", "nmap x <Action>(${PREFIX}A<caret>")
    assertText("nmap x <Action>(${PREFIX}Alpha")
  }

  @Test
  fun `test does not complete in comment`() {
    complete(".ideavimrc", "\" nmap x <Action>(${PREFIX}A<caret>")
    assertNoActionIds()
  }

  @Test
  fun `test does not complete outside of action id`() {
    complete(".ideavimrc", "nmap x ${PREFIX}A<caret>")
    assertNoActionIds()
  }

  @Test
  fun `test does not complete in other files`() {
    complete("notes.txt", "nmap x <Action>(${PREFIX}A<caret>")
    assertNoActionIds()
  }

  private fun complete(fileName: String, content: String) {
    configureByTextX(fileName, content)
    fixture.completeBasic()
  }

  private fun receiveFromFrontend(vararg actions: ActionIdInfo) {
    FrontendActionIds.getInstance().actions = actions.associateBy { it.id }
  }

  private fun assertPresentation(actionId: String, tailText: String?, typeText: String?) {
    val element = fixture.lookupElements.orEmpty().single { it.lookupString == actionId }
    val presentation = LookupElementPresentation.renderElement(element)
    assertEquals(tailText, presentation.tailText)
    assertEquals(typeText, presentation.typeText)
  }

  private fun assertText(expected: String) {
    assertEquals(expected, fixture.editor.document.text)
  }

  private fun assertNoActionIds() {
    assertFalse(fixture.editor.document.text.contains("${PREFIX}Alpha"))
    assertFalse(fixture.lookupElementStrings.orEmpty().any { it.startsWith(PREFIX) })
  }

  private class NoopAction(text: String?, description: String?) : AnAction(text, description, null) {
    override fun actionPerformed(e: AnActionEvent) {}
  }

  companion object {
    private const val PREFIX = "IdeaVimRcCompletionTest"
    private val ACTION_IDS = listOf("${PREFIX}Alpha", "${PREFIX}Beta", "${PREFIX}Bravo")
  }
}
