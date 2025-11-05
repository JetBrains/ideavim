/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.intellij.platform.testFramework.assertion.collectionAssertion.CollectionAssertions.assertEqualsOrdered
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class VimListTest : VimDataTypeTest() {
  @Test
  fun `test get value by index`() {
    val list = toVimList(1, 2, 3)
    assertEquals(VimInt(1), list[0])
  }

  @Test
  fun `test VimList does not have value semantics`() {
    val list1 = toVimList(1, 2, 3)
    val list2 = toVimList(1, 2, 3)
    assertNotEquals(list1, list2)
  }

  @Test
  fun `test throws when trying to convert to Float`() {
    val list = toVimList(1, 2, 3)
    val exception = assertThrows<ExException> {
      list.toVimFloat()
    }
    assertEquals("E893: Using a List as a Float", exception.message)
  }

  @Test
  fun `test throws when trying to convert to Number`() {
    val list = toVimList(1, 2, 3)
    val exception = assertThrows<ExException> {
      list.toVimNumber()
    }
    assertEquals("E745: Using a List as a Number", exception.message)
  }

  @Test
  fun `test throws when trying to convert to String`() {
    val list = toVimList(1, 2, 3)
    val exception = assertThrows<ExException> {
      list.toVimString()
    }
    assertEquals("E730: Using a List as a String", exception.message)
  }

  @Test
  fun `test output string for empty List`() {
    val list = VimList(mutableListOf())
    assertEquals("[]", list.toOutputString())
  }

  @Test
  fun `test output string for simple List`() {
    val list = toVimList(1, 2, 3)
    assertEquals("[1, 2, 3]", list.toOutputString())
  }

  @Test
  fun `test output string for nested List`() {
    val innerList = toVimList(9, 8, 7)
    val list = toVimList(1, innerList, 3)
    assertEquals("[1, [9, 8, 7], 3]", list.toOutputString())
  }

  @Test
  fun `test output string for List with mixed types`() {
    val dictionary = toVimDictionary("foo" to "bar", "baz" to 123)
    val list = toVimList(1, "two", 3.14, dictionary)
    assertEquals("[1, 'two', 3.14, {'foo': 'bar', 'baz': 123}]", list.toOutputString())
  }

  @Test
  fun `test output string for recursive List`() {
    val list = toVimList(1, 2, 3)
    list.values[1] = list
    assertEquals("[1, [...], 3]", list.toOutputString())
  }

  @Test
  fun `test output string for repeated references to current List`() {
    val list = toVimList(1, 2, 3, 4, 5, 6, 7)
    list.values[1] = list
    list.values[3] = list
    list.values[5] = list
    assertEquals("[1, [...], 3, [...], 5, [...], 7]", list.toOutputString())
  }

  @Test
  fun `test output string for indirectly recursive List`() {
    val list = toVimList(1, toVimList(4, toVimList(7, 8, 9), 6), 3)
    ((list.values[1] as VimList).values[1] as VimList).values[1] = list
    assertEquals("[1, [4, [7, [...], 9], 6], 3]", list.toOutputString())
  }

  @Test
  fun `test output string when List used inside Dictionary`() {
    val dictionary = toVimDictionary("foo" to 42)
    val list = toVimList(1, dictionary, 3)
    (list.values[1] as VimDictionary).dictionary[VimString("foo")] = list
    assertEquals("[1, {'foo': [...]}, 3]", list.toOutputString())
  }

  @Test
  fun `test output string for recursive use of inner sublist`() {
    val innerList = toVimList(9, 8, 7)
    innerList.values[1] = innerList
    val list = toVimList(1, innerList, 3)
    assertEquals("[1, [9, [...], 7], 3]", list.toOutputString())
  }

  @Test
  fun `test insertable string prints each item as a new line`() {
    val list = toVimList(1, 2, 3)
    assertEquals("1\n2\n3\n", list.toInsertableString())
  }

  @Test
  fun `test insertable string does not expand subitems`() {
    val list = toVimList(1, toVimList(2, 3), 4)
    assertEquals("1\n[2, 3]\n4\n", list.toInsertableString())
  }

  @Test
  fun `test insertable string expands recursive subitem only for 100 levels`() {
    val list = toVimList(1, 2, 3)
    list.values[1] = list
    val expected = """
      1
      [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [1, [{E724}]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
      3
      
    """.trimIndent()
    assertEquals(expected, list.toInsertableString())
  }

  @Test
  fun `test insertable string expands indirectly recursive subitem only for 100 levels`() {
    val list = toVimList(1, 2, 3)
    val list2 = toVimList(9, 8, 7)
    list.values[1] = list2
    list2.values[1] = list
    val expected = """
      1
      [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [1, [9, [{E724}]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
      3
      
    """.trimIndent()
    assertEquals(expected, list.toInsertableString())
  }

  @Test
  fun `test copy returns new instance with same value`() {
    val item = VimInt(42)
    val value = VimList(mutableListOf(item))
    val copy = value.copy()
    assertNotSame(value, copy)
    assertNotSame(value.values, copy.values)
    assertSame(value.values[0], copy.values[0])
  }

  @Test
  fun `test deepCopy returns new instance with new instance of value items`() {
    val value = toVimList(42) // let value=[42]
    val copy = value.deepCopy(useReferences = true)
    assertNotSame(value, copy)
    assertNotSame(value.values, copy.values)
    assertNotSame(value.values[0], copy.values[0])
  }

  @Test
  fun `test deepCopy returns new instance with new instance of reference items`() {
    val item = toVimList(42)
    val value = toVimList(item) // let value=[[42]]
    val copy = value.deepCopy(useReferences = true)
    assertNotSame(value, copy)
    assertNotSame(value.values, copy.values)
    assertNotSame(value.values[0], copy.values[0])
    (value.values[0] as VimList).values[0] = VimInt(99)
    assertEquals(42, ((copy.values[0] as VimList).values[0] as VimInt).value)
  }

  @Test
  fun `test deepCopy replaces same instances with copied value`() {
    val item = toVimList(1, 2, 3)
    val value = toVimList(item, item, item, toVimList(1, 2, 3)) // let value=[item, item, item, [1, 2, 3]]
    val copy = value.deepCopy(useReferences = true)

    assertNotSame(value.values[0], copy.values[0])
    assertEqualsOrdered(toVimList(1, 2, 3).values, (copy.values[0] as VimList).values)
    assertNotSame(value.values[1], copy.values[1])
    assertEqualsOrdered(toVimList(1, 2, 3).values, (copy.values[1] as VimList).values)
    assertNotSame(value.values[2], copy.values[3])
    assertEqualsOrdered(toVimList(1, 2, 3).values, (copy.values[2] as VimList).values)
    assertNotSame(value.values[3], copy.values[3])
    assertEqualsOrdered(toVimList(1, 2, 3).values, (copy.values[3] as VimList).values)

    // `copy[0] is copy[1] is copy[2] is not copy[3]`
    assertSame(copy.values[0], copy.values[1])
    assertSame(copy.values[1], copy.values[2])
    assertNotSame(copy.values[2], copy.values[3])
  }

  @Test
  fun `test deepCopy copies recursive List safely when sharing references`() {
    val value = VimList(mutableListOf())
    value.values.add(value)
    val copy = value.deepCopy(useReferences = true)
    assertNotSame(value, copy)
    assertSame(copy, copy.values[0])
    assertSame(copy, (copy.values[0] as VimList).values[0])
    assertSame(copy, ((copy.values[0] as VimList).values[0] as VimList).values[0])
    // Etc...
  }

  @Test
  fun `test deepCopy reports error with recursive List when not sharing references`() {
    val value = VimList(mutableListOf())
    value.values.add(value)
    val exception = assertThrows<ExException> {
      value.deepCopy(useReferences = false)
    }
    assertEquals("E698: Variable nested too deep for making a copy", exception.message)
  }
}
