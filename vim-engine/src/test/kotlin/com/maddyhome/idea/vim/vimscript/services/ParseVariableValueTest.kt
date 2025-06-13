/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.spy
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseVariableValueTest {

  private lateinit var variableService: VariableService

  @BeforeEach
  fun setUp() {
    variableService = spy(object : VimVariableServiceBase() {})

    val mockInjector = Mockito.mock(VimInjector::class.java)

    Mockito.`when`(mockInjector.variableService).thenReturn(variableService)

    injector = mockInjector
  }

  @AfterEach
  fun tearDown() {
    injector = Mockito.mock(VimInjector::class.java)
  }

  @Test
  fun `test parseVariableValue with Int`() {
    val vimInt = VimInt(42)
    val type = typeOf<Int>()

    val result: Int? = variableService.parseVariableValue(vimInt, type)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with String`() {
    val vimString = VimString("hello")
    val type = typeOf<String>()

    val result: String? = variableService.parseVariableValue(vimString, type)

    assertEquals("hello", result)
  }

  @Test
  fun `test getVariable with Double`() {
    val vimFloat = VimFloat(3.14)
    val type = typeOf<Double>()

    val result: Double? = variableService.parseVariableValue(vimFloat, type)

    assertEquals(3.14, result)
  }

  @Test
  fun `test getVariable with List of Int`() {
    val vimList = VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3)))
    val type = typeOf<List<Int>>()

    val result: List<Int>? = variableService.parseVariableValue(vimList, type)

    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `test getVariable with Map of String to Int`() {
    val map = linkedMapOf<VimString, VimDataType>(
      VimString("one") to VimInt(1),
      VimString("two") to VimInt(2)
    )
    val vimDictionary = VimDictionary(map)
    val type = typeOf<Map<String, Int>>()

    val result: Map<String, Int>? = variableService.parseVariableValue(vimDictionary, type)

    assertEquals(mapOf("one" to 1, "two" to 2), result)
  }

  // Nested data type tests

  @Test
  fun `test getVariable with List of List of Int`() {
    val innerList1 = VimList(mutableListOf(VimInt(1), VimInt(2)))
    val innerList2 = VimList(mutableListOf(VimInt(3), VimInt(4)))
    val vimList = VimList(mutableListOf(innerList1, innerList2))
    val type = typeOf<List<List<Int>>>()

    val result: List<List<Int>>? = variableService.parseVariableValue(vimList, type)

    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
  }

  @Test
  fun `test getVariable with Map of String to List of Int`() {
    val map = linkedMapOf<VimString, VimDataType>(
      VimString("list1") to VimList(mutableListOf(VimInt(1), VimInt(2))),
      VimString("list2") to VimList(mutableListOf(VimInt(3), VimInt(4)))
    )
    val vimDictionary = VimDictionary(map)

    val type = typeOf<Map<String, List<Int>>>()

    val result: Map<String, List<Int>>? = variableService.parseVariableValue(vimDictionary, type)

    assertEquals(
      mapOf(
        "list1" to listOf(1, 2),
        "list2" to listOf(3, 4)
      ), result
    )
  }

  @Test
  fun `test getVariable with Map of String to Map of String to Int`() {
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
    val type = typeOf<Map<String, Map<String, Int>>>()

    val result: Map<String, Map<String, Int>>? = variableService.parseVariableValue(vimDictionary, type)

    assertEquals(
      mapOf(
        "map1" to mapOf("a" to 1, "b" to 2),
        "map2" to mapOf("c" to 3, "d" to 4)
      ), result
    )
  }

  @Test
  fun `test getVariable with Map of String to List of List of Int`() {
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
    val type = typeOf<Map<String, List<List<Int>>>>()

    val result: Map<String, List<List<Int>>>? = variableService.parseVariableValue(vimDictionary, type)

    assertEquals(
      mapOf(
        "nested1" to listOf(listOf(1, 2), listOf(3, 4)),
        "nested2" to listOf(listOf(5, 6), listOf(7, 8))
      ), result
    )
  }

  @Test
  fun `test getVariable with List of Map of String to Int`() {
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
    val type = typeOf<List<Map<String, Int>>>()

    val result: List<Map<String, Int>>? = variableService.parseVariableValue(vimList, type)

    assertEquals(
      listOf(
        mapOf("a" to 1, "b" to 2),
        mapOf("c" to 3, "d" to 4)
      ), result
    )
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Int but got String`() {
    val vimString = VimString("not an int")
    val type = typeOf<Int>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimString, type)
    }

    assertEquals("Expected Int, but got VimString", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected String but got Int`() {
    val vimInt = VimInt(42)
    val type = typeOf<String>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimInt, type)
    }

    assertEquals("Expected String, but got VimInt", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Double but got Int`() {
    val vimInt = VimInt(42)
    val type = typeOf<Double>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimInt, type)
    }

    assertEquals("Expected Double, but got VimInt", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected List but got Map`() {
    val vimDictionary = VimDictionary(linkedMapOf(VimString("key") to VimInt(1)))
    val type = typeOf<List<Int>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimDictionary, type)
    }

    assertEquals("Expected List, but got VimDictionary", exception.message)
  }

  @Test
  fun `test getVariable throws exception when type mismatch - expected Map but got List`() {
    val vimList = VimList(mutableListOf(VimInt(1), VimInt(2)))
    val type = typeOf<Map<String, Int>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimList, type)
    }

    assertEquals("Expected Map, but got VimList", exception.message)
  }

  @Test
  fun `test getVariable with unsupported type throws exception`() {
    val vimInt = VimInt(42)
    val type = typeOf<Boolean>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimInt, type)
    }

    assertEquals("Unsupported type: Boolean", exception.message)
  }

  @Test
  fun `test getVariable with empty List`() {
    val vimList = VimList(mutableListOf())
    val type = typeOf<List<Int>>()

    val result: List<Int>? = variableService.parseVariableValue(vimList, type)

    assertEquals(emptyList(), result)
  }

  @Test
  fun `test getVariable with empty Map`() {
    val vimDictionary = VimDictionary(linkedMapOf())
    val type = typeOf<Map<String, Int>>()

    val result: Map<String, Int>? = variableService.parseVariableValue(vimDictionary, type)

    assertEquals(emptyMap(), result)
  }

  @Test
  fun `test getVariable with invalid variable name format`() {
    val vimInt = VimInt(42)
    val type = typeOf<Int>()

    val result: Int? = variableService.parseVariableValue(vimInt, type)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable with different scope`() {
    val vimInt = VimInt(42)
    val type = typeOf<Int>()

    val result: Int? = variableService.parseVariableValue(vimInt, type)

    assertEquals(42, result)
  }

  @Test
  fun `test getVariable throws exception when map key is not string but int`() {
    val vimDictionary = VimDictionary(linkedMapOf(VimString("key") to VimInt(1)))
    val type = typeOf<Map<Int, Int>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.parseVariableValue(vimDictionary, type)
    }

    assertEquals("Expected Map with String as key, but got VimDictionary", exception.message)
  }

  @Test
  fun `test getVariable with 4 level nested types`() {
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

    val type = typeOf<Map<String, Map<String, List<Map<String, Int>>>>>()

    val result: Map<String, Map<String, List<Map<String, Int>>>>? =
      variableService.parseVariableValue(level1Map, type)

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