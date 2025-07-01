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
import kotlin.test.assertTrue

class ConvertToVimDataTypeTest {

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
  fun `test convertToVimDataType with Int`() {
    val value = 42
    val type = typeOf<Int>()

    val result = variableService.convertToVimDataType(value, type)

    assertTrue(result is VimInt)
    assertEquals(42, result.value)
  }

  @Test
  fun `test convertToVimDataType with String`() {
    val value = "hello"
    val type = typeOf<String>()

    val result = variableService.convertToVimDataType(value, type)

    assertTrue(result is VimString)
    assertEquals("hello", result.value)
  }

  @Test
  fun `test convertToVimDataType with Boolean true`() {
    val value = true
    val type = typeOf<Boolean>()

    val result = variableService.convertToVimDataType(value, type)

    assertTrue(result is VimInt)
    assertEquals(1, result.value)
  }

  @Test
  fun `test convertToVimDataType with Boolean false`() {
    val value = false
    val type = typeOf<Boolean>()

    val result = variableService.convertToVimDataType(value, type)

    assertTrue(result is VimInt)
    assertEquals(0, result.value)
  }

  @Test
  fun `test convertToVimDataType with Double`() {
    val value = 3.14
    val type = typeOf<Double>()

    val result = variableService.convertToVimDataType(value, type)

    assertTrue(result is VimFloat)
    assertEquals(3.14, result.value)
  }

  @Test
  fun `test convertToVimDataType with List of Int`() {
    val value = listOf(1, 2, 3)
    val type = typeOf<List<Int>>()

    val vimList = variableService.convertToVimDataType(value, type)

    assertTrue(vimList is VimList)
    assertEquals(3, vimList.values.size)
    assertTrue(vimList.values[0] is VimInt)
    assertEquals(1, (vimList.values[0] as VimInt).value)
    assertEquals(2, (vimList.values[1] as VimInt).value)
    assertEquals(3, (vimList.values[2] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with Map of String to Int`() {
    val value = mapOf("one" to 1, "two" to 2)
    val type = typeOf<Map<String, Int>>()

    val vimDict = variableService.convertToVimDataType(value, type)

    assertTrue(vimDict is VimDictionary)
    assertEquals(2, vimDict.dictionary.size)
    assertTrue(vimDict.dictionary.containsKey(VimString("one")))
    assertTrue(vimDict.dictionary.containsKey(VimString("two")))
    assertEquals(1, (vimDict.dictionary[VimString("one")] as VimInt).value)
    assertEquals(2, (vimDict.dictionary[VimString("two")] as VimInt).value)
  }

  // Nested data type tests

  @Test
  fun `test convertToVimDataType with List of List of Int`() {
    val value = listOf(listOf(1, 2), listOf(3, 4))
    val type = typeOf<List<List<Int>>>()

    val outerList = variableService.convertToVimDataType(value, type)

    assertTrue(outerList is VimList)
    assertEquals(2, outerList.values.size)

    val innerList1 = outerList.values[0] as VimList
    assertEquals(2, innerList1.values.size)
    assertEquals(1, (innerList1.values[0] as VimInt).value)
    assertEquals(2, (innerList1.values[1] as VimInt).value)

    val innerList2 = outerList.values[1] as VimList
    assertEquals(2, innerList2.values.size)
    assertEquals(3, (innerList2.values[0] as VimInt).value)
    assertEquals(4, (innerList2.values[1] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with Map of String to List of Int`() {
    val value = mapOf(
      "list1" to listOf(1, 2),
      "list2" to listOf(3, 4)
    )
    val type = typeOf<Map<String, List<Int>>>()

    val vimDict = variableService.convertToVimDataType(value, type)

    assertTrue(vimDict is VimDictionary)
    assertEquals(2, vimDict.dictionary.size)

    val list1 = vimDict.dictionary[VimString("list1")] as VimList
    assertEquals(2, list1.values.size)
    assertEquals(1, (list1.values[0] as VimInt).value)
    assertEquals(2, (list1.values[1] as VimInt).value)

    val list2 = vimDict.dictionary[VimString("list2")] as VimList
    assertEquals(2, list2.values.size)
    assertEquals(3, (list2.values[0] as VimInt).value)
    assertEquals(4, (list2.values[1] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with Map of String to Map of String to Int`() {
    val value = mapOf(
      "map1" to mapOf("a" to 1, "b" to 2),
      "map2" to mapOf("c" to 3, "d" to 4)
    )
    val type = typeOf<Map<String, Map<String, Int>>>()

    val outerDict = variableService.convertToVimDataType(value, type)

    assertTrue(outerDict is VimDictionary)
    assertEquals(2, outerDict.dictionary.size)

    val innerDict1 = outerDict.dictionary[VimString("map1")] as VimDictionary
    assertEquals(2, innerDict1.dictionary.size)
    assertEquals(1, (innerDict1.dictionary[VimString("a")] as VimInt).value)
    assertEquals(2, (innerDict1.dictionary[VimString("b")] as VimInt).value)

    val innerDict2 = outerDict.dictionary[VimString("map2")] as VimDictionary
    assertEquals(2, innerDict2.dictionary.size)
    assertEquals(3, (innerDict2.dictionary[VimString("c")] as VimInt).value)
    assertEquals(4, (innerDict2.dictionary[VimString("d")] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with Map of String to List of List of Int`() {
    val value = mapOf(
      "nested1" to listOf(listOf(1, 2), listOf(3, 4)),
      "nested2" to listOf(listOf(5, 6), listOf(7, 8))
    )
    val type = typeOf<Map<String, List<List<Int>>>>()

    val outerDict = variableService.convertToVimDataType(value, type)

    assertTrue(outerDict is VimDictionary)
    assertEquals(2, outerDict.dictionary.size)

    val nestedList1 = outerDict.dictionary[VimString("nested1")] as VimList
    assertEquals(2, nestedList1.values.size)

    val innerList1 = nestedList1.values[0] as VimList
    assertEquals(2, innerList1.values.size)
    assertEquals(1, (innerList1.values[0] as VimInt).value)
    assertEquals(2, (innerList1.values[1] as VimInt).value)

    val innerList2 = nestedList1.values[1] as VimList
    assertEquals(2, innerList2.values.size)
    assertEquals(3, (innerList2.values[0] as VimInt).value)
    assertEquals(4, (innerList2.values[1] as VimInt).value)

    val nestedList2 = outerDict.dictionary[VimString("nested2")] as VimList
    assertEquals(2, nestedList2.values.size)

    val innerList3 = nestedList2.values[0] as VimList
    assertEquals(2, innerList3.values.size)
    assertEquals(5, (innerList3.values[0] as VimInt).value)
    assertEquals(6, (innerList3.values[1] as VimInt).value)

    val innerList4 = nestedList2.values[1] as VimList
    assertEquals(2, innerList4.values.size)
    assertEquals(7, (innerList4.values[0] as VimInt).value)
    assertEquals(8, (innerList4.values[1] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with List of Map of String to Int`() {
    val value = listOf(
      mapOf("a" to 1, "b" to 2),
      mapOf("c" to 3, "d" to 4)
    )
    val type = typeOf<List<Map<String, Int>>>()

    val outerList = variableService.convertToVimDataType(value, type)

    assertTrue(outerList is VimList)
    assertEquals(2, outerList.values.size)

    val map1 = outerList.values[0] as VimDictionary
    assertEquals(2, map1.dictionary.size)
    assertEquals(1, (map1.dictionary[VimString("a")] as VimInt).value)
    assertEquals(2, (map1.dictionary[VimString("b")] as VimInt).value)

    val map2 = outerList.values[1] as VimDictionary
    assertEquals(2, map2.dictionary.size)
    assertEquals(3, (map2.dictionary[VimString("c")] as VimInt).value)
    assertEquals(4, (map2.dictionary[VimString("d")] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType throws exception with Map with non-String keys`() {
    val value = mapOf(1 to "one", 2 to "two")
    val type = typeOf<Map<Int, String>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.convertToVimDataType(value, type)
    }

    assertEquals("Map must have String keys for conversion to VimDictionary", exception.message)
  }

  @Test
  fun `test convertToVimDataType with empty List`() {
    val value = emptyList<Int>()
    val type = typeOf<List<Int>>()

    val vimList = variableService.convertToVimDataType(value, type)

    assertTrue(vimList is VimList)
    assertEquals(0, vimList.values.size)
  }

  @Test
  fun `test convertToVimDataType with empty Map`() {
    val value = emptyMap<String, Int>()
    val type = typeOf<Map<String, Int>>()

    val vimDict = variableService.convertToVimDataType(value, type)

    assertTrue(vimDict is VimDictionary)
    assertEquals(0, vimDict.dictionary.size)
  }

  @Test
  fun `test convertToVimDataType with 4 level nested types`() {
    // Level 4 (innermost): Map<String, Int>
    val level4Map1 = mapOf("x" to 1, "y" to 2)
    val level4Map2 = mapOf("z" to 3, "w" to 4)

    // Level 3: List<Map<String, Int>>
    val level3List1 = listOf(level4Map1, level4Map2)
    val level3List2 = listOf(mapOf("p" to 5, "q" to 6))

    // Level 2: Map<String, List<Map<String, Int>>>
    val level2Map = mapOf(
      "inner1" to level3List1,
      "inner2" to level3List2
    )

    // Level 1 (outermost): Map<String, Map<String, List<Map<String, Int>>>>
    val level1Map = mapOf("outer" to level2Map)

    val type = typeOf<Map<String, Map<String, List<Map<String, Int>>>>>()

    val outerDict = variableService.convertToVimDataType(level1Map, type)

    assertTrue(outerDict is VimDictionary)
    assertEquals(1, outerDict.dictionary.size)

    val level2Dict = outerDict.dictionary[VimString("outer")] as VimDictionary
    assertEquals(2, level2Dict.dictionary.size)

    val level3List1Result = level2Dict.dictionary[VimString("inner1")] as VimList
    assertEquals(2, level3List1Result.values.size)

    val level4Map1Result = level3List1Result.values[0] as VimDictionary
    assertEquals(2, level4Map1Result.dictionary.size)
    assertEquals(1, (level4Map1Result.dictionary[VimString("x")] as VimInt).value)
    assertEquals(2, (level4Map1Result.dictionary[VimString("y")] as VimInt).value)

    val level4Map2Result = level3List1Result.values[1] as VimDictionary
    assertEquals(2, level4Map2Result.dictionary.size)
    assertEquals(3, (level4Map2Result.dictionary[VimString("z")] as VimInt).value)
    assertEquals(4, (level4Map2Result.dictionary[VimString("w")] as VimInt).value)

    val level3List2Result = level2Dict.dictionary[VimString("inner2")] as VimList
    assertEquals(1, level3List2Result.values.size)

    val level4Map3Result = level3List2Result.values[0] as VimDictionary
    assertEquals(2, level4Map3Result.dictionary.size)
    assertEquals(5, (level4Map3Result.dictionary[VimString("p")] as VimInt).value)
    assertEquals(6, (level4Map3Result.dictionary[VimString("q")] as VimInt).value)
  }

  @Test
  fun `test convertToVimDataType with List containing null throws exception`() {
    val value = listOf(1, null, 3)
    val type = typeOf<List<Int?>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.convertToVimDataType(value, type)
    }

    assertEquals("List cannot contain null values", exception.message)
  }

  @Test
  fun `test convertToVimDataType with Map containing null key throws exception`() {
    val value = mapOf("one" to 1, null to 2)
    val type = typeOf<Map<String?, Int>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.convertToVimDataType(value, type)
    }

    assertEquals("Map must have String keys for conversion to VimDictionary", exception.message)
  }

  @Test
  fun `test convertToVimDataType with Map containing null value throws exception`() {
    val value = mapOf("one" to 1, "two" to null)
    val type = typeOf<Map<String, Int?>>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.convertToVimDataType(value, type)
    }

    assertEquals("Map cannot contain null keys or values", exception.message)
  }

  @Test
  fun `test convertToVimDataType with unsupported type throws exception`() {
    class UnsupportedType
    val value = UnsupportedType()
    val type = typeOf<UnsupportedType>()

    val exception = assertFailsWith<IllegalArgumentException> {
      variableService.convertToVimDataType(value, type)
    }

    assertEquals("Unsupported type: UnsupportedType", exception.message)
  }
}
