/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActionIdCompletionTest {

  @Test
  fun `test action notation`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 16), ActionIdCompletion.findPrefix("nmap x <Action>(Ref"))
  }

  @Test
  fun `test action notation with empty prefix`() {
    assertEquals(ActionIdCompletion.Prefix("", 16), ActionIdCompletion.findPrefix("nmap x <Action>("))
  }

  @Test
  fun `test lowercase action notation`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 16), ActionIdCompletion.findPrefix("nmap x <action>(Ref"))
  }

  @Test
  fun `test closed action notation`() {
    assertNull(ActionIdCompletion.findPrefix("nmap x <Action>(ReformatCode)"))
  }

  @Test
  fun `test action notation followed by space`() {
    assertNull(ActionIdCompletion.findPrefix("nmap x <Action>(Ref oo"))
  }

  @Test
  fun `test action command`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 7), ActionIdCompletion.findPrefix("action Ref"))
  }

  @Test
  fun `test action command with colon and leading whitespace`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 10), ActionIdCompletion.findPrefix("  :action Ref"))
  }

  @Test
  fun `test action command with empty prefix`() {
    assertEquals(ActionIdCompletion.Prefix("", 7), ActionIdCompletion.findPrefix("action "))
  }

  @Test
  fun `test action command in mapping`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 15), ActionIdCompletion.findPrefix("nmap x :action Ref"))
  }

  @Test
  fun `test action command after bar`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 16), ActionIdCompletion.findPrefix("echo 1 | action Ref"))
  }

  @Test
  fun `test actionlist command and its abbreviations`() {
    assertEquals(ActionIdCompletion.Prefix("Ref", 11), ActionIdCompletion.findPrefix("actionlist Ref"))
    assertEquals(ActionIdCompletion.Prefix("Ref", 8), ActionIdCompletion.findPrefix("actionl Ref"))
  }

  @Test
  fun `test action command without separator`() {
    assertNull(ActionIdCompletion.findPrefix("action"))
  }

  @Test
  fun `test action command finished by key notation`() {
    assertNull(ActionIdCompletion.findPrefix("nmap x :action ReformatCode<CR>"))
  }

  @Test
  fun `test word containing action is not a command`() {
    assertNull(ActionIdCompletion.findPrefix("echo myaction Ref"))
    assertNull(ActionIdCompletion.findPrefix("actions Ref"))
  }

  @Test
  fun `test comment`() {
    assertNull(ActionIdCompletion.findPrefix("\" nmap x <Action>(Ref"))
    assertNull(ActionIdCompletion.findPrefix("  \" action Ref"))
  }

  @Test
  fun `test matches are case insensitive and sorted`() {
    val ids = listOf("ReformatCode", "Refactorings.QuickListPopupAction", "refresh", "EditorCopy")
    assertEquals(
      listOf("Refactorings.QuickListPopupAction", "ReformatCode", "refresh"),
      ActionIdCompletion.findMatches(ids, "ref"),
    )
  }

  @Test
  fun `test matches are distinct`() {
    assertEquals(listOf("ReformatCode"), ActionIdCompletion.findMatches(listOf("ReformatCode", "ReformatCode"), "Re"))
  }
}
