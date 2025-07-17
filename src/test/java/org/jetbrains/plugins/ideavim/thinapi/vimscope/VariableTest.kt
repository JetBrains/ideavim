/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
  fun `test lockvar with global variable`() {
    vimScope.setVariable("g:lockVarTest", "locked")
    vimScope.lockvar("g:lockVarTest")
    assertTrue(vimScope.islocked("g:lockVarTest"))
  }

  @Test
  fun `test lockvar with buffer variable`() {
    vimScope.setVariable("b:lockVarTest", "locked")
    vimScope.lockvar("b:lockVarTest")
    assertTrue(vimScope.islocked("b:lockVarTest"))
  }

  @Test
  fun `test lockvar with window variable`() {
    vimScope.setVariable("w:lockVarTest", "locked")
    vimScope.lockvar("w:lockVarTest")
    assertTrue(vimScope.islocked("w:lockVarTest"))
  }

  @Test
  fun `test lockvar with depth parameter`() {
    vimScope.setVariable("g:lockVarDepthTest", listOf(1, 2, 3))
    vimScope.lockvar("g:lockVarDepthTest", 2)
    assertTrue(vimScope.islocked("g:lockVarDepthTest"))
  }

  @Test
  fun `test unlockvar with global variable`() {
    vimScope.setVariable("g:unlockVarTest", "unlocked")
    vimScope.lockvar("g:unlockVarTest")
    assertTrue(vimScope.islocked("g:unlockVarTest"))
    vimScope.unlockvar("g:unlockVarTest")
    assertFalse(vimScope.islocked("g:unlockVarTest"))
  }

  @Test
  fun `test unlockvar with buffer variable`() {
    vimScope.setVariable("b:unlockVarTest", "unlocked")
    vimScope.lockvar("b:unlockVarTest")
    assertTrue(vimScope.islocked("b:unlockVarTest"))
    vimScope.unlockvar("b:unlockVarTest")
    assertFalse(vimScope.islocked("b:unlockVarTest"))
  }

  @Test
  fun `test unlockvar with window variable`() {
    vimScope.setVariable("w:unlockVarTest", "unlocked")
    vimScope.lockvar("w:unlockVarTest")
    assertTrue(vimScope.islocked("w:unlockVarTest"))
    vimScope.unlockvar("w:unlockVarTest")
    assertFalse(vimScope.islocked("w:unlockVarTest"))
  }

  @Test
  fun `test unlockvar with depth parameter`() {
    vimScope.setVariable("g:unlockVarDepthTest", listOf(1, 2, 3))
    vimScope.lockvar("g:unlockVarDepthTest", 2)
    assertTrue(vimScope.islocked("g:unlockVarDepthTest"))
    vimScope.unlockvar("g:unlockVarDepthTest", 2)
    assertFalse(vimScope.islocked("g:unlockVarDepthTest"))
  }

  @Test
  fun `test islocked with unlocked variable`() {
    vimScope.setVariable("g:isLockedTest", "test")
    assertFalse(vimScope.islocked("g:isLockedTest"))
  }

  @Test
  fun `test islocked with locked variable`() {
    vimScope.setVariable("g:isLockedTest2", "test")
    vimScope.lockvar("g:isLockedTest2")
    assertTrue(vimScope.islocked("g:isLockedTest2"))
  }

  @Test
  fun `test islocked with non-existent variable`() {
    assertFalse(vimScope.islocked("g:nonExistentVar"))
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
  fun `test locking and attempting to modify variable`() {
    vimScope.setVariable("g:lockModifyTest", "original")
    vimScope.lockvar("g:lockModifyTest")
    assertTrue(vimScope.islocked("g:lockModifyTest"))

    val exception = assertFailsWith<ExException> {
      vimScope.setVariable("g:lockModifyTest", "updated")
    }
    assertEquals("E741: Value is locked: g:lockModifyTest", exception.message)
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

  @Test
  fun `test locking and attempting to modify buffer variable`() {
    vimScope.setVariable("b:lockModifyTest", "original")
    vimScope.lockvar("b:lockModifyTest")
    assertTrue(vimScope.islocked("b:lockModifyTest"))

    val exception = assertFailsWith<ExException> {
      vimScope.setVariable("b:lockModifyTest", "updated")
    }
    assertEquals("E741: Value is locked: b:lockModifyTest", exception.message)
  }

  @Test
  fun `test locking and attempting to modify window variable`() {
    vimScope.setVariable("w:lockModifyTest", "original")
    vimScope.lockvar("w:lockModifyTest")
    assertTrue(vimScope.islocked("w:lockModifyTest"))

    val exception = assertFailsWith<ExException> {
      vimScope.setVariable("w:lockModifyTest", "updated")
    }
    assertEquals("E741: Value is locked: w:lockModifyTest", exception.message)
  }

  @Test
  fun `test locking with depth and attempting to modify variable`() {
    vimScope.setVariable("g:lockDepthTest", listOf(1, 2, 3))
    vimScope.lockvar("g:lockDepthTest", 2)
    assertTrue(vimScope.islocked("g:lockDepthTest"))

    val exception = assertFailsWith<ExException> {
      vimScope.setVariable("g:lockDepthTest", listOf(4, 5, 6))
    }
    assertEquals("E741: Value is locked: g:lockDepthTest", exception.message)
  }

  @Test
  fun `test multiple locking with depth requires multiple unlocking`() {
    // Create a nested list to test locking with depth
    vimScope.setVariable("g:multiLockDepthTest", listOf(listOf(1, 2), listOf(3, 4)))

    // Lock the variable with depth 1 (locks only the outer list)
    vimScope.lockvar("g:multiLockDepthTest", 1)
    assertTrue(vimScope.islocked("g:multiLockDepthTest"))

    // Verify that attempting to modify the outer list throws an exception
    val exception1 = assertFailsWith<ExException> {
      vimScope.setVariable("g:multiLockDepthTest", listOf(listOf(5, 6), listOf(7, 8)))
    }
    assertEquals("E741: Value is locked: g:multiLockDepthTest", exception1.message)

    // Unlock the variable
    vimScope.unlockvar("g:multiLockDepthTest", 1)
    assertFalse(vimScope.islocked("g:multiLockDepthTest"))

    // Now we should be able to modify the variable
    vimScope.setVariable("g:multiLockDepthTest", listOf(listOf(5, 6), listOf(7, 8)))
    val result1 = vimScope.getVariable<List<List<Int>>>("g:multiLockDepthTest")
    assertEquals(listOf(listOf(5, 6), listOf(7, 8)), result1)

    // Lock the variable with depth 2 (locks both the outer list and inner lists)
    vimScope.lockvar("g:multiLockDepthTest", 2)
    assertTrue(vimScope.islocked("g:multiLockDepthTest"))

    // Verify that attempting to modify the outer list throws an exception
    val exception2 = assertFailsWith<ExException> {
      vimScope.setVariable("g:multiLockDepthTest", listOf(listOf(9, 10), listOf(11, 12)))
    }
    assertEquals("E741: Value is locked: g:multiLockDepthTest", exception2.message)

    // Unlock the variable with depth 2 (unlocks both the outer list and inner lists)
    vimScope.unlockvar("g:multiLockDepthTest", 2)
    assertFalse(vimScope.islocked("g:multiLockDepthTest"))

    // Now we should be able to modify the variable
    vimScope.setVariable("g:multiLockDepthTest", listOf(listOf(9, 10), listOf(11, 12)))
    val result2 = vimScope.getVariable<List<List<Int>>>("g:multiLockDepthTest")
    assertEquals(listOf(listOf(9, 10), listOf(11, 12)), result2)
  }
}
