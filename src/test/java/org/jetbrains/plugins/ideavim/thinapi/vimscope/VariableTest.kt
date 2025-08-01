/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.scopes.VimScope
import com.intellij.vim.api.scopes.getVariable
import com.intellij.vim.api.scopes.setVariable
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VariableTest : VimTestCase() {
  private lateinit var vimScope: VimScope

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  @Test
  fun `test getVariable with integer value`() {
    vimScope.setVariable("g:testInt", 42)
    val result = vimScope.getVariable<Int>("g:testInt")
    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with string value`() {
    vimScope.setVariable("g:testString", "hello")
    val result = vimScope.getVariable<String>("g:testString")
    assertEquals("hello", result)
  }

  @Test
  fun `test getVariable with float value`() {
    vimScope.setVariable("g:testFloat", 1.5f)
    val result = vimScope.getVariable<Float>("g:testFloat")
    assertEquals(1.5f, result)
  }

  @Test
  fun `test getVariable with double value`() {
    vimScope.setVariable("g:testDouble", 3.14)
    val result = vimScope.getVariable<Double>("g:testDouble")
    assertEquals(3.14, result)
  }

  @Test
  fun `test getVariable with list value`() {
    vimScope.setVariable("g:testList", listOf(1, 2, 3))
    val result = vimScope.getVariable<List<Int>>("g:testList")
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test getVariable with map value`() {
    vimScope.setVariable("g:testMap", mapOf("a" to 1, "b" to 2))
    val result = vimScope.getVariable<Map<String, Int>>("g:testMap")
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test getVariable with non-existent variable`() {
    val result = vimScope.getVariable<Int>("g:nonExistentVar")
    assertNull(result)
  }

  @Test
  fun `test getVariable with global scope`() {
    vimScope.setVariable("g:globalVar", "global")
    val result = vimScope.getVariable<String>("g:globalVar")
    assertEquals("global", result)
  }

  @Test
  fun `test getVariable with buffer scope`() {
    vimScope.setVariable("b:bufferVar", "buffer")
    val result = vimScope.getVariable<String>("b:bufferVar")
    assertEquals("buffer", result)
  }

  @Test
  fun `test getVariable with window scope`() {
    vimScope.setVariable("w:windowVar", "window")
    val result = vimScope.getVariable<String>("w:windowVar")
    assertEquals("window", result)
  }

  @Test
  fun `test setVariable with integer value`() {
    vimScope.setVariable("g:testSetInt", 42)
    val result = vimScope.getVariable<Int>("g:testSetInt")
    assertEquals(42, result)
  }

  @Test
  fun `test setVariable with string value`() {
    vimScope.setVariable("g:testSetString", "hello")
    val result = vimScope.getVariable<String>("g:testSetString")
    assertEquals("hello", result)
  }

  @Test
  fun `test setVariable with float value`() {
    vimScope.setVariable("g:testSetFloat", 2.5f)
    val result = vimScope.getVariable<Float>("g:testSetFloat")
    assertEquals(2.5f, result)
  }

  @Test
  fun `test setVariable with double value`() {
    vimScope.setVariable("g:testSetDouble", 3.14)
    val result = vimScope.getVariable<Double>("g:testSetDouble")
    assertEquals(3.14, result)
  }

  @Test
  fun `test setVariable with list value`() {
    vimScope.setVariable("g:testSetList", listOf(1, 2, 3))
    val result = vimScope.getVariable<List<Int>>("g:testSetList")
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test setVariable with map value`() {
    vimScope.setVariable("g:testSetMap", mapOf("a" to 1, "b" to 2))
    val result = vimScope.getVariable<Map<String, Int>>("g:testSetMap")
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `test setVariable with global scope`() {
    vimScope.setVariable("g:globalSetVar", "global")
    val result = vimScope.getVariable<String>("g:globalSetVar")
    assertEquals("global", result)
  }

  @Test
  fun `test setVariable with buffer scope`() {
    vimScope.setVariable("b:bufferSetVar", "buffer")
    val result = vimScope.getVariable<String>("b:bufferSetVar")
    assertEquals("buffer", result)
  }

  @Test
  fun `test setVariable with window scope`() {
    vimScope.setVariable("w:windowSetVar", "window")
    val result = vimScope.getVariable<String>("w:windowSetVar")
    assertEquals("window", result)
  }

  @Test
  fun `test variable with default global scope`() {
    vimScope.setVariable("testDefaultScope", 100)
    val result = vimScope.getVariable<Int>("g:testDefaultScope")
    assertEquals(100, result)
  }

  @Test
  fun `test overwriting variable value`() {
    vimScope.setVariable("g:overwriteTest", "original")
    assertEquals("original", vimScope.getVariable<String>("g:overwriteTest"))
    vimScope.setVariable("g:overwriteTest", "updated")
    assertEquals("updated", vimScope.getVariable<String>("g:overwriteTest"))
  }

  @Test
  fun `test variable name with special characters`() {
    vimScope.setVariable("g:var_with_underscore", "special")
    val result = vimScope.getVariable<String>("g:var_with_underscore")
    assertEquals("special", result)
  }

  @Test
  fun `test empty string variable`() {
    vimScope.setVariable("g:emptyString", "")
    val result = vimScope.getVariable<String>("g:emptyString")
    assertEquals("", result)
  }

  @Test
  fun `test nested list variable`() {
    vimScope.setVariable("g:nestedList", listOf(listOf(1, 2), listOf(3, 4)))
    val result = vimScope.getVariable<List<List<Int>>>("g:nestedList")
    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
  }

  @Test
  fun `test vim scope variable`() {
    val count1 = vimScope.getVariable<Int>("v:count1")
    assertEquals(1, count1)
  }
}
