/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.get
import com.intellij.vim.api.scopes.set
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VariableTest : VimTestCase() {
  private lateinit var myVimApi: VimApi

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    myVimApi = VimApiImpl(listenerOwner, mappingOwner, null)

    configureByText("\n")
  }

  @Test
  fun `test getVariable with integer value`() {
    myVimApi.variables { set("g:testInt", 42) }
    val result = myVimApi.variables { get<Int>("g:testInt") }
    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with string value`() {
    myVimApi.variables { set("g:testString", "hello") }
    val result = myVimApi.variables { get<String>("g:testString") }
    assertEquals("hello", result)
  }

  @Test
  fun `test getVariable with float value`() {
    myVimApi.variables { set("g:testFloat", 1.5f) }
    val result = myVimApi.variables { get<Float>("g:testFloat") }
    assertEquals(1.5f, result)
  }

  @Test
  fun `test getVariable with double value`() {
    myVimApi.variables { set("g:testDouble", 3.14) }
    val result = myVimApi.variables { get<Double>("g:testDouble") }
    assertEquals(3.14, result)
  }

  @Test
  fun `test getVariable with list value`() {
    myVimApi.variables { set("g:testList", listOf(1, 2, 3)) }
    val result = myVimApi.variables { get<List<Int>>("g:testList") }
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test getVariable with map value`() {
    myVimApi.variables { set("g:testMap", mapOf("a" to 1, "b" to 2)) }
    val result = myVimApi.variables { get<Map<String, Int>>("g:testMap") }
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test getVariable with non-existent variable`() {
    val result = myVimApi.variables { get<Int>("g:nonExistentVar") }
    assertNull(result)
  }

  @Test
  fun `test getVariable with global scope`() {
    myVimApi.variables { set("g:globalVar", "global") }
    val result = myVimApi.variables { get<String>("g:globalVar") }
    assertEquals("global", result)
  }

  @Test
  fun `test getVariable with buffer scope`() {
    myVimApi.variables { set("b:bufferVar", "buffer") }
    val result = myVimApi.variables { get<String>("b:bufferVar") }
    assertEquals("buffer", result)
  }

  @Test
  fun `test getVariable with window scope`() {
    myVimApi.variables { set("w:windowVar", "window") }
    val result = myVimApi.variables { get<String>("w:windowVar") }
    assertEquals("window", result)
  }

  @Test
  fun `test setVariable with integer value`() {
    myVimApi.variables { set("g:testSetInt", 42) }
    val result = myVimApi.variables { get<Int>("g:testSetInt") }
    assertEquals(42, result)
  }

  @Test
  fun `test setVariable with string value`() {
    myVimApi.variables { set("g:testSetString", "hello") }
    val result = myVimApi.variables { get<String>("g:testSetString") }
    assertEquals("hello", result)
  }

  @Test
  fun `test setVariable with float value`() {
    myVimApi.variables { set("g:testSetFloat", 2.5f) }
    val result = myVimApi.variables { get<Float>("g:testSetFloat") }
    assertEquals(2.5f, result)
  }

  @Test
  fun `test setVariable with double value`() {
    myVimApi.variables { set("g:testSetDouble", 3.14) }
    val result = myVimApi.variables { get<Double>("g:testSetDouble") }
    assertEquals(3.14, result)
  }

  @Test
  fun `test setVariable with list value`() {
    myVimApi.variables { set("g:testSetList", listOf(1, 2, 3)) }
    val result = myVimApi.variables { get<List<Int>>("g:testSetList") }
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test setVariable with map value`() {
    myVimApi.variables { set("g:testSetMap", mapOf("a" to 1, "b" to 2)) }
    val result = myVimApi.variables { get<Map<String, Int>>("g:testSetMap") }
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test setVariable with global scope`() {
    myVimApi.variables { set("g:globalSetVar", "global") }
    val result = myVimApi.variables { get<String>("g:globalSetVar") }
    assertEquals("global", result)
  }

  @Test
  fun `test setVariable with buffer scope`() {
    myVimApi.variables { set("b:bufferSetVar", "buffer") }
    val result = myVimApi.variables { get<String>("b:bufferSetVar") }
    assertEquals("buffer", result)
  }

  @Test
  fun `test setVariable with window scope`() {
    myVimApi.variables { set("w:windowSetVar", "window") }
    val result = myVimApi.variables { get<String>("w:windowSetVar") }
    assertEquals("window", result)
  }

  @Test
  fun `test variable with default global scope`() {
    myVimApi.variables { set("testDefaultScope", 100) }
    val result = myVimApi.variables { get<Int>("g:testDefaultScope") }
    assertEquals(100, result)
  }

  @Test
  fun `test overwriting variable value`() {
    myVimApi.variables { set("g:overwriteTest", "original") }
    assertEquals("original", myVimApi.variables { get<String>("g:overwriteTest") })
    myVimApi.variables { set("g:overwriteTest", "updated") }
    assertEquals("updated", myVimApi.variables { get<String>("g:overwriteTest") })
  }

  @Test
  fun `test variable name with special characters`() {
    myVimApi.variables { set("g:var_with_underscore", "special") }
    val result = myVimApi.variables { get<String>("g:var_with_underscore") }
    assertEquals("special", result)
  }

  @Test
  fun `test empty string variable`() {
    myVimApi.variables { set("g:emptyString", "") }
    val result = myVimApi.variables { get<String>("g:emptyString") }
    assertEquals("", result)
  }

  @Test
  fun `test nested list variable`() {
    myVimApi.variables { set("g:nestedList", listOf(listOf(1, 2), listOf(3, 4))) }
    val result = myVimApi.variables { get<List<List<Int>>>("g:nestedList") }
    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
  }

  @Test
  fun `test vim scope variable`() {
    val count1 = myVimApi.variables { get<Int>("v:count1") }
    assertEquals(1, count1)
  }
}
