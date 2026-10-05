/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ActionIdCompletionTest : VimExTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    ACTION_IDS.forEach { ActionManager.getInstance().registerAction(it, NoopAction()) }
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    ACTION_IDS.forEach { ActionManager.getInstance().unregisterAction(it) }
    super.tearDown(testInfo)
  }

  @Test
  fun `test tab completes action id`() {
    typeText(":action ${PREFIX}A<Tab>")
    assertExText("action ${PREFIX}Alpha")
  }

  @Test
  fun `test tab cycles through action ids`() {
    typeText(":action ${PREFIX}B<Tab>")
    assertExText("action ${PREFIX}Beta")

    typeText("<Tab>")
    assertExText("action ${PREFIX}Bravo")

    typeText("<Tab>")
    assertExText("action ${PREFIX}Beta")
  }

  @Test
  fun `test shift tab cycles action ids backwards`() {
    typeText(":action ${PREFIX}B<S-Tab>")
    assertExText("action ${PREFIX}Bravo")
  }

  @Test
  fun `test action id completion is case insensitive`() {
    typeText(":action ${PREFIX.lowercase()}a<Tab>")
    assertExText("action ${PREFIX}Alpha")
  }

  @Test
  fun `test action id completion works with actionlist`() {
    typeText(":actionlist ${PREFIX}A<Tab>")
    assertExText("actionlist ${PREFIX}Alpha")
  }

  @Test
  fun `test action id with no matches does not change text`() {
    typeText(":action ${PREFIX}Zzz<Tab>")
    assertExText("action ${PREFIX}Zzz")
  }

  @Test
  fun `test tab completes action notation in mapping`() {
    typeText(":nmap x <lt>Action>(${PREFIX}A<Tab>")
    assertExText("nmap x <Action>(${PREFIX}Alpha")
  }

  @Test
  fun `test tab cycles action notation in mapping`() {
    typeText(":nnoremap x <lt>Action>(${PREFIX}B<Tab>")
    assertExText("nnoremap x <Action>(${PREFIX}Beta")

    typeText("<Tab>")
    assertExText("nnoremap x <Action>(${PREFIX}Bravo")
  }

  @Test
  fun `test action notation is case insensitive`() {
    typeText(":nmap x <lt>action>(${PREFIX.lowercase()}a<Tab>")
    assertExText("nmap x <action>(${PREFIX}Alpha")
  }

  @Test
  fun `test action notation after another action in mapping`() {
    typeText(":nmap x <lt>Action>(${PREFIX}Alpha)<lt>Action>(${PREFIX}Br<Tab>")
    assertExText("nmap x <Action>(${PREFIX}Alpha)<Action>(${PREFIX}Bravo")
  }

  @Test
  fun `test tab completes action command in mapping`() {
    typeText(":nmap x :action ${PREFIX}A<Tab>")
    assertExText("nmap x :action ${PREFIX}Alpha")
  }

  @Test
  fun `test closed action notation is not completed`() {
    typeText(":nmap x <lt>Action>(${PREFIX}A)<Tab>")
    assertExText("nmap x <Action>(${PREFIX}A)")
  }

  private class NoopAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {}
  }

  companion object {
    private const val PREFIX = "IdeaVimCompletionTest"
    private val ACTION_IDS = listOf("${PREFIX}Alpha", "${PREFIX}Beta", "${PREFIX}Bravo")
  }
}
