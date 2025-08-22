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
}
