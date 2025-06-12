/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.VimScope
import com.intellij.vim.api.scopes.getVariable
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorGroup
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VimScopeGetVariableTest {
  private lateinit var vimEditor: VimEditor
  private lateinit var context: ExecutionContext
  private lateinit var variableService: VariableService
  private lateinit var editorGroup: VimEditorGroup
  private lateinit var contextManager: ExecutionContextManager
  private lateinit var vimScope: VimScope

  @BeforeEach
  fun setUp() {
    vimEditor = mock(VimEditor::class.java)
    context = mock(ExecutionContext::class.java)
    variableService = mock(VariableService::class.java)
    editorGroup = mock(VimEditorGroup::class.java)
    contextManager = mock(ExecutionContextManager::class.java)

    val mockInjector = mock(VimInjector::class.java)

    `when`(mockInjector.variableService).thenReturn(variableService)
    `when`(mockInjector.editorGroup).thenReturn(editorGroup)
    `when`(mockInjector.executionContextManager).thenReturn(contextManager)

    `when`(mockInjector.executionContextManager.getEditorExecutionContext(vimEditor)).thenReturn(context)
    `when`(mockInjector.editorGroup.getFocusedEditor()).thenReturn(vimEditor)

    injector = mockInjector

    val listenerOwner = ListenerOwner.IdeaVim.System
    val mappingOwner = MappingOwner.IdeaVim.System

    vimScope = VimScopeImpl(listenerOwner, mappingOwner)
  }

  @AfterEach
  fun tearDown() {
    injector = mock(VimInjector::class.java)
  }

  @Test
  fun `test getVariable with Int`() {
    val variableName = "g:intVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "intVar")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val result: Int? = vimScope.getVariable<Int>(variableName)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with String`() {
    val variableName = "g:stringVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "stringVar")
    val vimString = VimString("hello")

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimString)

    val result: String? = vimScope.getVariable<String>(variableName)

    assertEquals("hello", result)
  }

  @Test
  fun `test getVariable with Double`() {
    val variableName = "g:doubleVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "doubleVar")
    val vimFloat = VimFloat(3.14)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimFloat)

    val result: Double? = vimScope.getVariable<Double>(variableName)

    assertEquals(3.14, result)
  }

  @Test
  fun `test getVariable with List of Int`() {
    val variableName = "g:listVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "listVar")
    val vimList = VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3)))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimList)

    val result: List<Int>? = vimScope.getVariable<List<Int>>(variableName)

    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test getVariable with Map of String to Int`() {
    val variableName = "g:mapVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "mapVar")
    val map = linkedMapOf<VimString, VimDataType>(
      VimString("one") to VimInt(1),
      VimString("two") to VimInt(2)
    )
    val vimDictionary = VimDictionary(map)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val result: Map<String, Int>? = vimScope.getVariable<Map<String, Int>>(variableName)

    assertEquals(mapOf("one" to 1, "two" to 2), result)
  }

  // Nested data type tests

  @Test
  fun `test getVariable with List of List of Int`() {
    val variableName = "g:nestedListVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "nestedListVar")
    val innerList1 = VimList(mutableListOf(VimInt(1), VimInt(2)))
    val innerList2 = VimList(mutableListOf(VimInt(3), VimInt(4)))
    val vimList = VimList(mutableListOf(innerList1, innerList2))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimList)

    val result: List<List<Int>>? = vimScope.getVariable<List<List<Int>>>(variableName)

    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
  }

  @Test
  fun `test getVariable with Map of String to List of Int`() {
    val variableName = "g:mapListVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "mapListVar")
    val map = linkedMapOf<VimString, VimDataType>(
      VimString("list1") to VimList(mutableListOf(VimInt(1), VimInt(2))),
      VimString("list2") to VimList(mutableListOf(VimInt(3), VimInt(4)))
    )
    val vimDictionary = VimDictionary(map)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val result: Map<String, List<Int>>? = vimScope.getVariable<Map<String, List<Int>>>(variableName)

    assertEquals(
      mapOf(
        "list1" to listOf(1, 2),
        "list2" to listOf(3, 4)
      ), result
    )
  }

  @Test
  fun `test getVariable with Map of String to Map of String to Int`() {
    val variableName = "g:nestedMapVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "nestedMapVar")
    val innerMap1 = VimDictionary(
      linkedMapOf(
        VimString("a") to VimInt(1),
        VimString("b") to VimInt(2)
      )
    )
    val innerMap2 = VimDictionary(
      linkedMapOf(
        VimString("c") to VimInt(3),
        VimString("d") to VimInt(4)
      )
    )
    val map = linkedMapOf<VimString, VimDataType>(
      VimString("map1") to innerMap1,
      VimString("map2") to innerMap2
    )
    val vimDictionary = VimDictionary(map)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val result: Map<String, Map<String, Int>>? = vimScope.getVariable<Map<String, Map<String, Int>>>(variableName)

    assertEquals(
      mapOf(
        "map1" to mapOf("a" to 1, "b" to 2),
        "map2" to mapOf("c" to 3, "d" to 4)
      ), result
    )
  }

  @Test
  fun `test getVariable with Map of String to List of List of Int`() {
    val variableName = "g:complexMapVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "complexMapVar")
    val innerList1 = VimList(mutableListOf(VimInt(1), VimInt(2)))
    val innerList2 = VimList(mutableListOf(VimInt(3), VimInt(4)))
    val nestedList1 = VimList(mutableListOf(innerList1, innerList2))

    val innerList3 = VimList(mutableListOf(VimInt(5), VimInt(6)))
    val innerList4 = VimList(mutableListOf(VimInt(7), VimInt(8)))
    val nestedList2 = VimList(mutableListOf(innerList3, innerList4))

    val map = linkedMapOf<VimString, VimDataType>(
      VimString("nested1") to nestedList1,
      VimString("nested2") to nestedList2
    )
    val vimDictionary = VimDictionary(map)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val result: Map<String, List<List<Int>>>? = vimScope.getVariable<Map<String, List<List<Int>>>>(variableName)

    assertEquals(
      mapOf(
        "nested1" to listOf(listOf(1, 2), listOf(3, 4)),
        "nested2" to listOf(listOf(5, 6), listOf(7, 8))
      ), result
    )
  }

  @Test
  fun `test getVariable with List of Map of String to Int`() {
    val variableName = "g:listMapVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "listMapVar")
    val map1 = VimDictionary(
      linkedMapOf(
        VimString("a") to VimInt(1),
        VimString("b") to VimInt(2)
      )
    )
    val map2 = VimDictionary(
      linkedMapOf(
        VimString("c") to VimInt(3),
        VimString("d") to VimInt(4)
      )
    )
    val vimList = VimList(mutableListOf(map1, map2))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimList)

    val result: List<Map<String, Int>>? = vimScope.getVariable<List<Map<String, Int>>>(variableName)

    assertEquals(
      listOf(
        mapOf("a" to 1, "b" to 2),
        mapOf("c" to 3, "d" to 4)
      ), result
    )
  }

  @Test
  fun `test getVariable throws exception when variable doesn't exist`() {
    val variableName = "g:nonExistentVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "nonExistentVar")

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(null)

    assertNull(vimScope.getVariable<String>(variableName))
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Int but got String`() {
    val variableName = "g:wrongTypeVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "wrongTypeVar")
    val vimString = VimString("not an int")

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimString)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<Int>(variableName)
    }

    assertEquals("Expected Int, but got VimString", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected String but got Int`() {
    val variableName = "g:wrongTypeVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "wrongTypeVar")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<String>(variableName)
    }

    assertEquals("Expected String, but got VimInt", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Double but got Int`() {
    val variableName = "g:wrongTypeVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "wrongTypeVar")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<Double>(variableName)
    }

    assertEquals("Expected Double, but got VimInt", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected List but got Map`() {
    val variableName = "g:wrongTypeVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "wrongTypeVar")
    val vimDictionary = VimDictionary(linkedMapOf(VimString("key") to VimInt(1)))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<List<Int>>(variableName)
    }

    assertEquals("Expected List, but got VimDictionary", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Map but got List`() {
    val variableName = "g:wrongTypeVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "wrongTypeVar")
    val vimList = VimList(mutableListOf(VimInt(1), VimInt(2)))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimList)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<Map<String, Int>>(variableName)
    }

    assertEquals("Expected Map, but got VimList", exception.message)
  }

  @Test
  fun `test getVariable with unsupported type throws exception`() {
    val variableName = "g:validVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "validVar")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<Boolean>(variableName)
    }

    assertEquals("Unsupported type: Boolean", exception.message)
  }

  @Test
  fun `test getVariable with empty List`() {
    val variableName = "g:emptyListVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "emptyListVar")
    val vimList = VimList(mutableListOf())

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimList)

    val result: List<Int>? = vimScope.getVariable<List<Int>>(variableName)

    assertEquals(emptyList(), result)
  }

  @Test
  fun `test getVariable with empty Map`() {
    val variableName = "g:emptyMapVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "emptyMapVar")
    val vimDictionary = VimDictionary(linkedMapOf())

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val result: Map<String, Int>? = vimScope.getVariable<Map<String, Int>>(variableName)

    assertEquals(emptyMap(), result)
  }

  @Test
  fun `test getVariable with invalid variable name format`() {
    val variableName = "invalidName"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "invalidName")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val result: Int? = vimScope.getVariable<Int>(variableName)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with different scope`() {
    val variableName = "b:bufferVar"
    val variable = Variable(Scope.BUFFER_VARIABLE, "bufferVar")
    val vimInt = VimInt(42)

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimInt)

    val result: Int? = vimScope.getVariable<Int>(variableName)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable throws exception when map key is not string but int`() {
    val variableName = "g:mapWithIntKey"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "mapWithIntKey")
    val vimDictionary = VimDictionary(linkedMapOf(VimString("key") to VimInt(1)))

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(vimDictionary)

    val exception = assertFailsWith<IllegalArgumentException> {
      vimScope.getVariable<Map<Int, Int>>(variableName)
    }

    assertEquals("Expected Map with String as key, but got VimDictionary", exception.message)
  }

  @Test
  fun `test getVariable with 4 level nested types`() {
    val variableName = "g:fourLevelNestedVar"
    val variable = Variable(Scope.GLOBAL_VARIABLE, "fourLevelNestedVar")

    // Level 4 (innermost): Map<String, Int>
    val level4Map1 = VimDictionary(
      linkedMapOf(
        VimString("x") to VimInt(1),
        VimString("y") to VimInt(2)
      )
    )
    val level4Map2 = VimDictionary(
      linkedMapOf(
        VimString("z") to VimInt(3),
        VimString("w") to VimInt(4)
      )
    )

    // Level 3: List<Map<String, Int>>
    val level3List1 = VimList(mutableListOf(level4Map1, level4Map2))
    val level3List2 = VimList(
      mutableListOf(
        VimDictionary(
          linkedMapOf(
            VimString("p") to VimInt(5),
            VimString("q") to VimInt(6)
          )
        )
      )
    )

    // Level 2: Map<String, List<Map<String, Int>>>
    val level2Map = VimDictionary(
      linkedMapOf(
        VimString("inner1") to level3List1,
        VimString("inner2") to level3List2
      )
    )

    // Level 1 (outermost): Map<String, Map<String, List<Map<String, Int>>>>
    val level1Map = VimDictionary(
      linkedMapOf(
        VimString("outer") to level2Map
      )
    )

    `when`(variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext))
      .thenReturn(level1Map)

    val result: Map<String, Map<String, List<Map<String, Int>>>>? =
      vimScope.getVariable<Map<String, Map<String, List<Map<String, Int>>>>>(variableName)

    assertEquals(
      mapOf(
        "outer" to mapOf(
          "inner1" to listOf(
            mapOf("x" to 1, "y" to 2),
            mapOf("z" to 3, "w" to 4)
          ),
          "inner2" to listOf(
            mapOf("p" to 5, "q" to 6)
          )
        )
      ), result
    )
  }
}
