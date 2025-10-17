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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VimDictionaryTest : VimDataTypeTest() {
  @Test
  fun `test VimDictionary does not have value semantics`() {
    val dictionary1 = toVimDictionary("key1" to 1, "key2" to 2)
    val dictionary2 = toVimDictionary("key1" to 1, "key2" to 2)
    assertNotEquals(dictionary1, dictionary2)
  }

  @Test
  fun `test throws when trying to convert to Float`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2)
    val exception = assertThrows<ExException> {
      dictionary.toVimFloat()
    }
    assertEquals("E894: Using a Dictionary as a Float", exception.message)
  }

  @Test
  fun `test throws when trying to convert to Number`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2)
    val exception = assertThrows<ExException> {
      dictionary.toVimNumber()
    }
    assertEquals("E728: Using a Dictionary as a Number", exception.message)
  }

  @Test
  fun `test throws when trying to convert to String`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2)
    val exception = assertThrows<ExException> {
      dictionary.toVimString()
    }
    assertEquals("E731: Using a Dictionary as a String", exception.message)
  }

  @Test
  fun `test output string for empty Dictionary`() {
    val dictionary = VimDictionary(linkedMapOf())
    assertEquals("{}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for Dictionary with single entry`() {
    val dictionary = toVimDictionary("key1" to 1)
    assertEquals("{'key1': 1}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for simple Dictionary`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to "value", "key3" to 3.14, "key4" to listOf(1, 2, 3))
    assertEquals("{'key1': 1, 'key2': 'value', 'key3': 3.14, 'key4': [1, 2, 3]}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for nested Dictionary`() {
    val innerDictionary = toVimDictionary("key1" to 1, "key2" to "value")
    val dictionary = toVimDictionary("key1" to 1, "key2" to innerDictionary)
    assertEquals("{'key1': 1, 'key2': {'key1': 1, 'key2': 'value'}}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for recursive Dictionary`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2, "key3" to 3)
    dictionary.dictionary[VimString("key2")] = dictionary
    assertEquals("{'key1': 1, 'key2': {...}, 'key3': 3}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for repeated recursive Dictionary`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2, "key3" to 3)
    dictionary.dictionary[VimString("key1")] = dictionary
    dictionary.dictionary[VimString("key3")] = dictionary
    assertEquals("{'key1': {...}, 'key2': 2, 'key3': {...}}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for indirectly recursive Dictionary`() {
    val innerDictionary = toVimDictionary("foo" to 1, "d" to 3, "bar" to 2)
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2, "key3" to 3)
    dictionary.dictionary[VimString("key2")] = innerDictionary
    innerDictionary.dictionary[VimString("d")] = innerDictionary
    assertEquals("{'key1': 1, 'key2': {'foo': 1, 'd': {...}, 'bar': 2}, 'key3': 3}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for repeated List value in Dictionary`() {
    val dictionary = toVimDictionary("key1" to toVimList(1, 2, 3), "key2" to 2, "key3" to toVimList(1, 2, 3))
    assertEquals("{'key1': [1, 2, 3], 'key2': 2, 'key3': [1, 2, 3]}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for Dictionary with indirectly nested lists`() {
    val innerList = toVimList(9, 8, 7)
    val dictionary = toVimDictionary(
      "key1" to toVimList(1, innerList, 3),
      "key2" to 2,
      "key3" to toVimList(1, innerList, 3)
    )
    assertEquals("{'key1': [1, [9, 8, 7], 3], 'key2': 2, 'key3': [1, [...], 3]}", dictionary.toOutputString())
  }

  @Test
  fun `test insertable string for simple Dictionary`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to "value", "key3" to 3.14, "key4" to listOf(1, 2, 3))
    assertEquals("{'key1': 1, 'key2': 'value', 'key3': 3.14, 'key4': [1, 2, 3]}", dictionary.toOutputString())
  }

  @Test
  fun `test insertable string for recursive Dictionary throws exception`() {
    val dictionary = toVimDictionary("key1" to 1, "key2" to 2, "key3" to 3)
    dictionary.dictionary[VimString("key2")] = dictionary
    val exception = assertThrows<ExException> {
      dictionary.toInsertableString()
    }
    assertEquals("E724: Variable nested too deep for displaying", exception.message)
  }

  // TODO: DeepCopy tests, when we implement Vim's deepcopy()
}
