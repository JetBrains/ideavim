/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

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
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class VimFuncrefTest : VimDataTypeTest() {
  @Test
  fun `test does not have value semantics`() {
    val funcref1 = getVimFuncref()
    val funcref2 = getVimFuncref()
    assertNotEquals(funcref1, funcref2)
  }

  @Test
  fun `test throws when trying to convert to Float`() {
    val funcref = getVimFuncref()
    val exception = assertThrows<ExException> {
      funcref.toVimFloat()
    }
    assertEquals("E891: Using a Funcref as a Float", exception.message)
  }

  @Test
  fun `test throws when trying to convert to Number`() {
    val funcref = getVimFuncref()
    val exception = assertThrows<ExException> {
      funcref.toVimNumber()
    }
    assertEquals("E703: Using a Funcref as a Number", exception.message)
  }

  @Test
  fun `test throws when trying to convert to String`() {
    val funcref = getVimFuncref()
    val exception = assertThrows<ExException> {
      funcref.toVimString()
    }
    assertEquals("E729: Using a Funcref as a String", exception.message)
  }

  @Test
  fun `test output string for funcref`() {
    val funcref = getVimFuncref(type = Type.FUNCREF)
    assertEquals("function('Fake')", funcref.toOutputString())
  }

  @Test
  fun `test output string for function is simply function name`() {
    val funcref = getVimFuncref(type = Type.FUNCTION)
    assertEquals("Fake", funcref.toOutputString())
  }

  @Test
  fun `test output string for lambda`() {
    val funcref = getVimFuncref(type = Type.LAMBDA)
    assertEquals("function('Fake')", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial`() {
    val arguments = toVimList(1, 2, 3)
    val funcref = getVimFuncref(arguments)
    assertEquals("function('Fake', [1, 2, 3])", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial dictionary function`() {
    val dictionary = toVimDictionary("key1" to 12, "key2" to "something", "key3" to toVimList(1, 2, 3))
    val funcref = getVimFuncref(dictionary = dictionary)
    assertEquals("function('Fake', {'key1': 12, 'key2': 'something', 'key3': [1, 2, 3]})", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial dictionary function with arguments`() {
    val arguments = toVimList(1, 2, 3)
    val dictionary = toVimDictionary("key1" to 12, "key2" to "something", "key3" to toVimList(1, 2, 3))
    val funcref = getVimFuncref(arguments, dictionary)
    assertEquals("function('Fake', [1, 2, 3], {'key1': 12, 'key2': 'something', 'key3': [1, 2, 3]})", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial function with recursive arguments list`() {
    val arguments = toVimList(1, 2, 3)
    arguments.values[1] = arguments
    val funcref = getVimFuncref(arguments)
    assertEquals("function('Fake', [1, [...], 3])", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial function with recursive inner arguments list`() {
    val innerList = toVimList(1, 2, 3)
    innerList.values[1] = innerList
    val arguments = toVimList(9, innerList, 7)
    val funcref = getVimFuncref(arguments)
    assertEquals("function('Fake', [9, [1, [...], 3], 7])", funcref.toOutputString())
  }

  @Test
  fun `test output string for partial function with list used in arguments and dictionary`() {
    val list = toVimList(1, 2, 3)
    val dictionary = toVimDictionary("foo" to list)
    val funcref = getVimFuncref(arguments = list, dictionary = dictionary)
    assertEquals("function('Fake', [1, 2, 3], {'foo': [1, 2, 3]})", funcref.toOutputString())
  }

  @VimBehaviorDiffers(
    originalVimAfter = "function('Fake', [1, 2, 3], {'foo': [1, 2, 3], 'bar': [1, 2, 3]})",
    description = "The dictionary parameter displays differently to the same dictionary as a top level variable"
  )
  @Test
  fun `test output string for partial function with repeated list used in dictionary`() {
    val list = toVimList(1, 2, 3)
    val dictionary = toVimDictionary("foo" to list, "bar" to list)
    val funcref = getVimFuncref(arguments = list, dictionary = dictionary)
    assertEquals("function('Fake', [1, 2, 3], {'foo': [1, 2, 3], 'bar': [...]})", funcref.toOutputString())
  }

  @Test
  fun `test throws when trying to create insertable string`() {
    val funcref = getVimFuncref()
    val exception = assertThrows<ExException> {
      funcref.toInsertableString()
    }
    assertEquals("E729: Using a Funcref as a String", exception.message)
  }

  @Test
  fun `test copy returns new instance with same value`() {
    val item = VimInt(42)
    val key = VimString("key")
    val value = VimInt(42)
    val dictionary = VimDictionary(linkedMapOf(key to value))
    val funcref = VimFuncref(FakeHandler, VimList(mutableListOf(item)), dictionary, Type.FUNCTION, isImplicitPartial = false)
    val copy = funcref.copy()
    assertNotSame(funcref, copy)

    assertNotSame(funcref.arguments, copy.arguments)
    assertNotSame(funcref.arguments.values, copy.arguments.values)
    assertSame(funcref.arguments.values[0], copy.arguments.values[0])

    assertNotSame(funcref.dictionary, copy.dictionary)
    assertNotSame(funcref.dictionary?.dictionary, copy.dictionary?.dictionary)
    assertSame(funcref.dictionary?.dictionary[key], copy.dictionary?.dictionary[key])
  }

  @Test
  fun `test deepCopy returns new instance with same value`() {
    val item = VimInt(42)
    val key = VimString("key")
    val value = VimInt(42)
    val dictionary = VimDictionary(linkedMapOf(key to value))
    val funcref = VimFuncref(FakeHandler, VimList(mutableListOf(item)), dictionary, Type.FUNCTION, isImplicitPartial = false)
    val copy = funcref.deepCopy(useReferences = true) as VimFuncref
    assertNotSame(funcref, copy)

    assertNotSame(funcref.arguments, copy.arguments)
    assertNotSame(funcref.arguments.values, copy.arguments.values)
    assertSame(funcref.arguments.values[0], copy.arguments.values[0])

    assertNotSame(funcref.dictionary, copy.dictionary)
    assertNotSame(funcref.dictionary?.dictionary, copy.dictionary?.dictionary)
    assertSame(funcref.dictionary?.dictionary[key], copy.dictionary?.dictionary[key])
  }

  // Note that function execution is not tested here. It is better tested as part of function calls and dictionary
  // function calls, especially wrt partial and dictionary functions.
  // See FunctionCallTest and DictionaryFunctionCallTest

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
