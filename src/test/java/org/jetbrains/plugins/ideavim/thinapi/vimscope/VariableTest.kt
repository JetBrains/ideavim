/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.setVariable
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
    myVimApi = VimApiImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  @Test
  fun `test getVariable with integer value`() {
    myVimApi.setVariable("g:testInt", 42)
    val result = myVimApi.getVariable<Int>("g:testInt")
    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with string value`() {
    myVimApi.setVariable("g:testString", "hello")
    val result = myVimApi.getVariable<String>("g:testString")
    assertEquals("hello", result)
  }

  @Test
  fun `test getVariable with float value`() {
    myVimApi.setVariable("g:testFloat", 1.5f)
    val result = myVimApi.getVariable<Float>("g:testFloat")
    assertEquals(1.5f, result)
  }

  @Test
  fun `test getVariable with double value`() {
    myVimApi.setVariable("g:testDouble", 3.14)
    val result = myVimApi.getVariable<Double>("g:testDouble")
    assertEquals(3.14, result)
  }

  @Test
  fun `test getVariable with list value`() {
    myVimApi.setVariable("g:testList", listOf(1, 2, 3))
    val result = myVimApi.getVariable<List<Int>>("g:testList")
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test getVariable with map value`() {
    myVimApi.setVariable("g:testMap", mapOf("a" to 1, "b" to 2))
    val result = myVimApi.getVariable<Map<String, Int>>("g:testMap")
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test getVariable with non-existent variable`() {
    val result = myVimApi.getVariable<Int>("g:nonExistentVar")
    assertNull(result)
  }

  @Test
  fun `test getVariable with global scope`() {
    myVimApi.setVariable("g:globalVar", "global")
    val result = myVimApi.getVariable<String>("g:globalVar")
    assertEquals("global", result)
  }

  @Test
  fun `test getVariable with buffer scope`() {
    myVimApi.setVariable("b:bufferVar", "buffer")
    val result = myVimApi.getVariable<String>("b:bufferVar")
    assertEquals("buffer", result)
  }

  @Test
  fun `test getVariable with window scope`() {
    myVimApi.setVariable("w:windowVar", "window")
    val result = myVimApi.getVariable<String>("w:windowVar")
    assertEquals("window", result)
  }

  @Test
  fun `test setVariable with integer value`() {
    myVimApi.setVariable("g:testSetInt", 42)
    val result = myVimApi.getVariable<Int>("g:testSetInt")
    assertEquals(42, result)
  }

  @Test
  fun `test setVariable with string value`() {
    myVimApi.setVariable("g:testSetString", "hello")
    val result = myVimApi.getVariable<String>("g:testSetString")
    assertEquals("hello", result)
  }

  @Test
  fun `test setVariable with float value`() {
    myVimApi.setVariable("g:testSetFloat", 2.5f)
    val result = myVimApi.getVariable<Float>("g:testSetFloat")
    assertEquals(2.5f, result)
  }

  @Test
  fun `test setVariable with double value`() {
    myVimApi.setVariable("g:testSetDouble", 3.14)
    val result = myVimApi.getVariable<Double>("g:testSetDouble")
    assertEquals(3.14, result)
  }

  @Test
  fun `test setVariable with list value`() {
    myVimApi.setVariable("g:testSetList", listOf(1, 2, 3))
    val result = myVimApi.getVariable<List<Int>>("g:testSetList")
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test setVariable with map value`() {
    myVimApi.setVariable("g:testSetMap", mapOf("a" to 1, "b" to 2))
    val result = myVimApi.getVariable<Map<String, Int>>("g:testSetMap")
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test setVariable with global scope`() {
    myVimApi.setVariable("g:globalSetVar", "global")
    val result = myVimApi.getVariable<String>("g:globalSetVar")
    assertEquals("global", result)
  }

  @Test
  fun `test setVariable with buffer scope`() {
    myVimApi.setVariable("b:bufferSetVar", "buffer")
    val result = myVimApi.getVariable<String>("b:bufferSetVar")
    assertEquals("buffer", result)
  }

  @Test
  fun `test setVariable with window scope`() {
    myVimApi.setVariable("w:windowSetVar", "window")
    val result = myVimApi.getVariable<String>("w:windowSetVar")
    assertEquals("window", result)
  }

  @Test
  fun `test variable with default global scope`() {
    myVimApi.setVariable("testDefaultScope", 100)
    val result = myVimApi.getVariable<Int>("g:testDefaultScope")
    assertEquals(100, result)
  }

  @Test
  fun `test overwriting variable value`() {
    myVimApi.setVariable("g:overwriteTest", "original")
    assertEquals("original", myVimApi.getVariable<String>("g:overwriteTest"))
    myVimApi.setVariable("g:overwriteTest", "updated")
    assertEquals("updated", myVimApi.getVariable<String>("g:overwriteTest"))
  }

  @Test
  fun `test variable name with special characters`() {
    myVimApi.setVariable("g:var_with_underscore", "special")
    val result = myVimApi.getVariable<String>("g:var_with_underscore")
    assertEquals("special", result)
  }

  @Test
  fun `test empty string variable`() {
    myVimApi.setVariable("g:emptyString", "")
    val result = myVimApi.getVariable<String>("g:emptyString")
    assertEquals("", result)
  }

  @Test
  fun `test nested list variable`() {
    myVimApi.setVariable("g:nestedList", listOf(listOf(1, 2), listOf(3, 4)))
    val result = myVimApi.getVariable<List<List<Int>>>("g:nestedList")
    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
  }

  @Test
  fun `test vim scope variable`() {
    val count1 = myVimApi.getVariable<Int>("v:count1")
    assertEquals(1, count1)
  }
}
