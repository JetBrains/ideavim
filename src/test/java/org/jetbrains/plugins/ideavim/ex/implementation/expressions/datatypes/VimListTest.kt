/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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

  @Disabled("Not yet implemented")
  @Test
  fun `test output string for recursive List`() {
    val list = toVimList(1, 2, 3)
    list.values[1] = list
    assertEquals("[1, [...], 3]", list.toOutputString())
  }

  @Disabled("Not yet implemented")
  @Test
  fun `test output string for repeated references to current List`() {
    val list = toVimList(1, 2, 3, 4, 5, 6, 7)
    list.values[1] = list
    list.values[3] = list
    list.values[5] = list
    assertEquals("[1, [...], 3, [...], 5, [...], 7]", list.toOutputString())
  }

  @Disabled("Not yet implemented")
  @Test
  fun `test output string for indirectly recursive List`() {
    val list = toVimList(1, toVimList(4, toVimList(7, 8, 9), 6), 3)
    ((list.values[1] as VimList).values[1] as VimList).values[1] = list
    assertEquals("[1, [4, [7, [...], 9], 6], 3]", list.toOutputString())
  }

  @Disabled("Not yet implemented")
  @Test
  fun `test output string when List used inside Dictionary`() {
    val dictionary = toVimDictionary("foo" to 42)
    val list = toVimList(1, dictionary, 3)
    (list.values[1] as VimDictionary).dictionary[VimString("foo")] = list
    assertEquals("[1, {'foo': [...]}, 3]", list.toOutputString())
  }

  @Disabled("Not yet implemented")
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

  @Disabled("Not yet implemented")
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

  @Disabled("Not yet implemented")
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

  // TODO: DeepCopy tests, when we implement Vim's deepcopy()
}
