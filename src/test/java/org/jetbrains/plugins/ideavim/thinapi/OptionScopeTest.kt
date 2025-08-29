/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptionScopeTest : VimTestCase() {
  private lateinit var myVimApi: VimApi

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    myVimApi = VimApiImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  @Test
  fun `test get option with integer value`() {
    myVimApi.option {
      set<Int>("history", 40)
      val result = get<Int>("history")
      assertEquals(40, result)
    }
  }

  @Test
  fun `test get option with string value`() {
    myVimApi.option {
      set<String>("selection", "inclusive")
      val result = get<String>("selection")
      assertEquals("inclusive", result)
    }
  }

  @Test
  fun `test get option with toggle value`() {
    myVimApi.option {
      set<Boolean>("ignorecase", true)
      val result = get<Boolean>("ignorecase")
      assertTrue(result == true)
    }
  }

  @Test
  fun `test get non-existent option`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> { get<Int>("nonexistentoption") }
      assertTrue(exception.message!!.contains("not found"))
    }
  }

  @Test
  fun `test set global option`() {
    myVimApi.option {
      assertDoesNotThrow { setGlobal<Int>("history", 80) }
      val value = get<Int>("history")
      assertEquals(80, value)
    }
  }

  @Test
  fun `test set local option`() {
    myVimApi.option {
      assertDoesNotThrow { setLocal<Int>("history", 20) }

      val value = get<Int>("history")
      assertEquals(20, value)
    }
  }

  @Test
  fun `test set effective option`() {
    myVimApi.option {
      assertDoesNotThrow { set<Int>("history", 40) }

      val value = get<Int>("history")
      assertEquals(40, value)
    }
  }

  @Test
  fun `test set non-existent option`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> { set<Int>("nonexistentoption", 42) }
    }
  }

  @Test
  fun `test reset option`() {
    myVimApi.option {
      set<Int>("history", 100)
      assertEquals(100, get<Int>("history"))

      assertDoesNotThrow { reset("history") }

      val value = get<Int>("history")
      assertEquals(50, value)
    }
  }

  @Test
  fun `test reset non-existent option`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> { reset("nonexistentoption") }
    }
  }

  @Test
  fun `test set option with wrong type`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<Int>("selection", 42)
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test set toggle option with true value`() {
    myVimApi.option {
      assertDoesNotThrow { set<Boolean>("ignorecase", true) }

      val value = get<Boolean>("ignorecase")
      assertTrue(value == true)
    }
  }

  @Test
  fun `test set toggle option with false value`() {
    myVimApi.option {
      assertDoesNotThrow { set<Boolean>("ignorecase", false) }

      val value = get<Boolean>("ignorecase")
      assertTrue(value == false)
    }
  }

  @Test
  fun `test set and get multiple options`() {
    myVimApi.option {
      set<Int>("history", 40)
      set<Int>("ignorecase", 1)
      set<String>("selection", "inclusive")

      assertEquals(40, get<Int>("history"))
      assertEquals(1, get<Int>("ignorecase"))
      assertEquals("inclusive", get<String>("selection"))
    }
  }

  @Test
  fun `test global vs local option settings`() {
    myVimApi.option {
      setGlobal<Int>("history", 100)

      setLocal<Int>("history", 20)

      assertEquals(20, get<Int>("history"))
    }
  }

  @Test
  fun `test get integer option as string returns null`() {
    myVimApi.option {
      set<Int>("history", 42)

      assertThrows<IllegalArgumentException> {
        get<String>("history")
      }
    }
  }

  @Test
  fun `test get string option as integer returns null`() {
    myVimApi.option {
      set<String>("selection", "inclusive")

      assertThrows<IllegalArgumentException> {
        get<Int>("selection")
      }
    }
  }

  @Test
  fun `test set integer option with string value throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("history", "not-a-number")
      }
      assertTrue(exception.message!!.contains("E521"), exception.message)
    }
  }

  @Test
  fun `test set boolean option with string value throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("ignorecase", "not-a-boolean")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test get option with null type throws exception`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> {
        get<Nothing?>("history")
      }
    }
  }

  @Test
  fun `test set string list option with values`() {
    myVimApi.option {
      set<String>("virtualedit", "all,block")
      val result = get<String>("virtualedit")
      assertEquals("all,block", result)
    }
  }

  @Test
  fun `test set string list option with empty value`() {
    myVimApi.option {
      set<String>("virtualedit", "")
      val result = get<String>("virtualedit")
      assertEquals("", result)
    }
  }

  @Test
  fun `test set string list option with single value`() {
    myVimApi.option {
      set<String>("virtualedit", "all")
      val result = get<String>("virtualedit")
      assertEquals("all", result)
    }
  }

  @Test
  fun `test set string list option with invalid value throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("virtualedit", "invalidvalue")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test set string list option with duplicate values`() {
    myVimApi.option {
      set<String>("virtualedit", "all,all,block,block")
      val result = get<String>("virtualedit")
      assertEquals("all,all,block,block", result)
    }
  }

  @Test
  fun `test set string list option with spaces between values`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("virtualedit", "all, block")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test reset string list option to default`() {
    myVimApi.option {
      set<String>("virtualedit", "all,block")
      assertEquals("all,block", get<String>("virtualedit"))
      
      reset("virtualedit")
      assertEquals("", get<String>("virtualedit"))
    }
  }

  @Test
  fun `test set option with empty name throws exception`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> {
        set<Int>("", 42)
      }
    }
  }

  @Test
  fun `test get option with empty name throws exception`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> {
        get<Int>("")
      }
    }
  }

  @Test
  fun `test reset option with empty name throws exception`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> {
        reset("")
      }
    }
  }

  @Test
  fun `test set integer option with negative value`() {
    myVimApi.option {
      set<Int>("scroll", -10)
      val result = get<Int>("scroll")
      assertEquals(-10, result)
    }
  }

  @Test
  fun `test set integer option with zero value`() {
    myVimApi.option {
      set<Int>("history", 0)
      val result = get<Int>("history")
      assertEquals(0, result)
    }
  }

  @Test
  fun `test set integer option with maximum value`() {
    myVimApi.option {
      set<Int>("history", Int.MAX_VALUE)
      val result = get<Int>("history")
      assertEquals(Int.MAX_VALUE, result)
    }
  }

  @Test
  fun `test set integer option out of bounds throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<Int>("history", -1)
      }
      assertTrue(exception.message!!.contains("E487"))
    }
  }

  @Test
  fun `test set scroll jump with invalid negative value throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<Int>("scrolljump", -101)
      }
      assertTrue(exception.message!!.contains("E49"))
    }
  }

  @Test
  fun `test get boolean option as integer returns 0 or 1`() {
    myVimApi.option {
      set<Boolean>("ignorecase", false)
      assertEquals(0, get<Int>("ignorecase"))
      
      set<Boolean>("ignorecase", true)
      assertEquals(1, get<Int>("ignorecase"))
    }
  }

  @Test
  fun `test set integer option with boolean type for toggle option`() {
    myVimApi.option {
      set<Int>("ignorecase", 0)
      assertEquals(false, get<Boolean>("ignorecase"))
      
      set<Int>("ignorecase", 1)
      assertEquals(true, get<Boolean>("ignorecase"))
    }
  }

  @Test
  fun `test set string option with empty value`() {
    myVimApi.option {
      set<String>("selection", "")
      val result = get<String>("selection")
      assertEquals("", result)
    }
  }

  @Test
  fun `test global option persists across editors`() {
    myVimApi.option {
      setGlobal<Int>("history", 123)
      assertEquals(123, get<Int>("history"))
    }
  }

  @Test
  fun `test local option overrides global option`() {
    myVimApi.option {
      setGlobal<Int>("scroll", 10)
      setLocal<Int>("scroll", 20)
      
      assertEquals(20, get<Int>("scroll"))
    }
  }

  @Test
  fun `test reset local option falls back to global`() {
    myVimApi.option {
      setGlobal<Int>("scroll", 10)
      setLocal<Int>("scroll", 20)
      assertEquals(20, get<Int>("scroll"))
      
      reset("scroll")
      assertEquals(0, get<Int>("scroll"))
    }
  }

  @Test
  fun `test set multiple string list values`() {
    myVimApi.option {
      set<String>("whichwrap", "b,s,h,l")
      val result = get<String>("whichwrap")
      assertEquals("b,s,h,l", result)
    }
  }

  @Test
  fun `test set string list with invalid item throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("whichwrap", "b,invalid,s")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test matchpairs option with valid pairs`() {
    myVimApi.option {
      set<String>("matchpairs", "(:),{:},[:]")
      val result = get<String>("matchpairs")
      assertEquals("(:),{:},[:]", result)
    }
  }

  @Test
  fun `test matchpairs option with invalid format throws exception`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("matchpairs", "(:),abc,[:]")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test set option with very long string value throws exception`() {
    myVimApi.option {
      val longValue = "a".repeat(1000)
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("selection", longValue)
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test concurrent option modifications`() {
    myVimApi.option {
      set<Int>("history", 10)
      setGlobal<Int>("history", 20)
      setLocal<Int>("history", 30)
      
      assertEquals(30, get<Int>("history"))
    }
  }

  @Test
  fun `test option name case sensitivity`() {
    myVimApi.option {
      assertThrows<IllegalArgumentException> {
        get<Int>("HISTORY")
      }
    }
  }

  @Test
  fun `test set boolean option with integer value other than 0 or 1`() {
    myVimApi.option {
      set<Int>("ignorecase", 2)
      assertEquals(false, get<Boolean>("ignorecase"))
      
      set<Int>("ignorecase", -1)
      assertEquals(false, get<Boolean>("ignorecase"))
      
      set<Int>("ignorecase", 100)
      assertEquals(false, get<Boolean>("ignorecase"))
      
      set<Int>("ignorecase", 1)
      assertEquals(true, get<Boolean>("ignorecase"))
      
      set<Int>("ignorecase", 0)
      assertEquals(false, get<Boolean>("ignorecase"))
    }
  }

  @Test
  fun `test set list option with trailing comma`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("virtualedit", "all,block,")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test set list option with leading comma`() {
    myVimApi.option {
      val exception = assertThrows<IllegalArgumentException> {
        set<String>("virtualedit", ",all,block")
      }
      assertTrue(exception.message!!.contains("E474"))
    }
  }

  @Test
  fun `test get option using abbreviation`() {
    myVimApi.option {
      set<Int>("hi", 75)
      assertEquals(75, get<Int>("history"))
    }
  }

  @Test
  fun `test set option using abbreviation`() {
    myVimApi.option {
      set<Boolean>("ic", true)
      assertEquals(true, get<Boolean>("ignorecase"))
    }
  }

  @Test
  fun `test reset option using abbreviation`() {
    myVimApi.option {
      set<Int>("hi", 100)
      reset("hi")
      assertEquals(50, get<Int>("history"))
    }
  }

  @Test
  fun `test split with multiple values`() {
    myVimApi.option {
      set<String>("virtualedit", "block,all,insert")
      val values = get<String>("virtualedit")?.split()
      assertEquals(listOf("block", "all", "insert"), values)
    }
  }

  @Test
  fun `test split with single value`() {
    myVimApi.option {
      set<String>("virtualedit", "all")
      val values = get<String>("virtualedit")?.split()
      assertEquals(listOf("all"), values)
    }
  }

  @Test
  fun `test split with empty string`() {
    myVimApi.option {
      set<String>("virtualedit", "")
      val values = get<String>("virtualedit")?.split()
      assertEquals(listOf(""), values)  // Note: empty string splits to [""], not []
    }
  }

  @Test
  fun `test split with whichwrap option`() {
    myVimApi.option {
      set<String>("whichwrap", "b,s,h,l,<,>,[,]")
      val values = get<String>("whichwrap")?.split()
      assertEquals(listOf("b", "s", "h", "l", "<", ">", "[", "]"), values)
    }
  }

  @Test
  fun `test split integration with option manipulation`() {
    myVimApi.option {
      // Start with some values
      set<String>("virtualedit", "block,all")
      
      // Get current values as list
      val currentValues = get<String>("virtualedit")?.split()?.toMutableList() ?: mutableListOf()
      
      // Add a new value if not present
      if (!currentValues.contains("insert")) {
        currentValues.add("insert")
        set<String>("virtualedit", currentValues.joinToString(","))
      }
      
      assertEquals("block,all,insert", get<String>("virtualedit"))
      
      // Remove a value
      currentValues.remove("all")
      set<String>("virtualedit", currentValues.joinToString(","))
      
      assertEquals("block,insert", get<String>("virtualedit"))
    }
  }
}
