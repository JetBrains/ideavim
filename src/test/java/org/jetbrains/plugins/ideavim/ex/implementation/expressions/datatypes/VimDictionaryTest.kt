/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.intellij.platform.testFramework.assertion.collectionAssertion.CollectionAssertions.assertEqualsOrdered
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref.Type
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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

  // Note that these do not test the output for dictionary functions! This only tests the output of a dictionary
  // containing a function reference and a partial function reference.
  @Test
  fun `test output string for Dictionary with function entry`() {
    val dictionary = toVimDictionary("k" to getVimFuncref(), "thing" to "bar")
    assertEquals("{'k': function('Fake'), 'thing': 'bar'}", dictionary.toOutputString())
  }

  @Test
  fun `test output string for Dictionary with partial function entry`() {
    val partialDictionary = toVimDictionary("thing" to "oink")
    val dictionary = toVimDictionary("k" to getVimFuncref(dictionary = partialDictionary), "thing" to "bar")
    assertEquals("{'k': function('Fake', {'thing': 'oink'}), 'thing': 'bar'}", dictionary.toOutputString())
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

  @Test
  fun `test copy returns new instance with same value`() {
    val dictionary = toVimDictionary("key" to 42)
    val copy = dictionary.copy()
    assertNotSame(dictionary, copy)
    assertNotSame(dictionary.dictionary, copy.dictionary)
    assertSame(dictionary.dictionary[VimString("key")], copy.dictionary[VimString("key")])
  }

  @Test
  fun `test deepCopy returns new instance with new instance of value items`() {
    val dictionary = toVimDictionary("key" to 42) // let dictionary={'key':42}
    val copy = dictionary.deepCopy(useReferences = true)
    assertNotSame(dictionary, copy)
    assertNotSame(dictionary.dictionary, copy.dictionary)
    assertNotSame(dictionary.dictionary[VimString("key")], copy.dictionary[VimString("key")])
    assertEquals(VimInt(42), copy.dictionary[VimString("key")])
  }

  @Test
  fun `test deepCopy returns new instance with new instance of reference item`() {
    val list = toVimList(1, 2, 3)
    val value = toVimDictionary("key" to list)  // let dictionary={'key':[1,2,3]}
    val copy = value.deepCopy(useReferences = true)
    assertNotSame(value, copy)
    assertNotSame(value.dictionary, copy.dictionary)
    assertNotSame(value.dictionary[VimString("key")], copy.dictionary[VimString("key")])
    val newList = assertInstanceOf<VimList>(copy.dictionary[VimString("key")])
    assertEqualsOrdered(toVimList(1, 2, 3).values, newList.values)
    (value.dictionary[VimString("key")] as VimList).values[0] = VimInt(10)
    assertEquals(VimInt(10), (value.dictionary[VimString("key")] as VimList).values[0])
    assertEquals(VimInt(1), (copy.dictionary[VimString("key")] as VimList).values[0])
  }

  @Test
  fun `test deepCopy replaces same instances with copied value`() {
    val item = toVimDictionary("a" to 1, "b" to 2, "c" to 3)
    val value = toVimDictionary("k1" to item, "k2" to item, "k3" to item, "k4" to toVimDictionary("a" to 1, "b" to 2, "c" to 3))
    val copy = value.deepCopy(useReferences = true)

    assertNotSame(item, copy.dictionary[VimString("k1")])
    assertEquals(VimInt(1), (copy.dictionary[VimString("k1")] as VimDictionary).dictionary[VimString("a")])
    assertNotSame(item, copy.dictionary[VimString("k2")])
    assertEquals(VimInt(1), (copy.dictionary[VimString("k2")] as VimDictionary).dictionary[VimString("a")])
    assertNotSame(item, copy.dictionary[VimString("k3")])
    assertEquals(VimInt(1), (copy.dictionary[VimString("k3")] as VimDictionary).dictionary[VimString("a")])
    assertNotSame(value.dictionary[VimString("k4")], copy.dictionary[VimString("k4")])
    assertEquals(VimInt(1), (copy.dictionary[VimString("k4")] as VimDictionary).dictionary[VimString("a")])

    // `copy['k1'] is copy['k2'] is copy['k3'] is not copy['k4']`
    assertSame(copy.dictionary[VimString("k1")], copy.dictionary[VimString("k2")])
    assertSame(copy.dictionary[VimString("k2")], copy.dictionary[VimString("k3")])
    assertNotSame(copy.dictionary[VimString("k3")], copy.dictionary[VimString("k4")])
  }

  @Test
  fun `test deepCopy copies recursive Dictionary safely when sharing references`() {
    val value = VimDictionary(linkedMapOf())
    value.dictionary[VimString("k1")] = value
    val copy = value.deepCopy(useReferences = true)
    assertNotSame(value, copy)
    assertSame(copy, copy.dictionary[VimString("k1")])
    assertSame(copy, (copy.dictionary[VimString("k1")] as VimDictionary).dictionary[VimString("k1")])
    // Etc...
  }

  @Test
  fun `test deepCopy reports error with recursive Dictionary when not sharing references`() {
    val value = VimDictionary(linkedMapOf())
    value.dictionary[VimString("k1")] = value
    val exception = assertThrows<ExException> {
      value.deepCopy(useReferences = false)
    }
    assertEquals("E698: Variable nested too deep for making a copy", exception.message)
  }

  private fun getVimFuncref(
    arguments: VimList? = null,
    dictionary: VimDictionary? = null,
    type: Type = Type.FUNCREF,
  ): VimFuncref {
    return VimFuncref(FakeHandler, arguments ?: VimList(mutableListOf()), dictionary, type, isImplicitPartial = false)
  }

  // We'll never call this
  object FakeHandler: UnaryFunctionHandler<VimDataType>() {
    init {
      name = "Fake"
    }

    override fun doFunction(
      arguments: Arguments,
      editor: VimEditor,
      context: ExecutionContext,
      vimContext: VimLContext,
    ): VimDataType {
      TODO("Not yet implemented")
    }
  }
}
