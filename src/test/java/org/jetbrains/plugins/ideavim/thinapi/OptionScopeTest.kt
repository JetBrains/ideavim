/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OptionScopeTest : VimTestCase() {
  private lateinit var vimScope: VimScope

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  @Test
  fun `test get option with integer value`() {
    vimScope.option {
      set<Int>("history", 40)
      val result = get<Int>("history")
      assertEquals(40, result)
    }
  }

  @Test
  fun `test get option with string value`() {
    vimScope.option {
      set<String>("selection", "inclusive")
      val result = get<String>("selection")
      assertEquals("inclusive", result)
    }
  }

  @Test
  fun `test get option with toggle value`() {
    vimScope.option {
      set<Boolean>("ignorecase", true)
      val result = get<Boolean>("ignorecase")
      assertTrue(result == true)
    }
  }

  @Test
  fun `test get non-existent option`() {
    vimScope.option {
      val result = get<Int>("nonexistentoption")
      assertNull(result)
    }
  }

  @Test
  fun `test set global option`() {
    vimScope.option {
      val result = setGlobal<Int>("history", 80)
      assertTrue(result)

      val value = get<Int>("history")
      assertEquals(80, value)
    }
  }

  @Test
  fun `test set local option`() {
    vimScope.option {
      val result = setLocal<Int>("history", 20)
      assertTrue(result)

      val value = get<Int>("history")
      assertEquals(20, value)
    }
  }

  @Test
  fun `test set effective option`() {
    vimScope.option {
      val result = set<Int>("history", 40)
      assertTrue(result)

      val value = get<Int>("history")
      assertEquals(40, value)
    }
  }

  @Test
  fun `test set non-existent option`() {
    vimScope.option {
      val result = set<Int>("nonexistentoption", 42)
      assertFalse(result)
    }
  }

  @Test
  fun `test reset option`() {
    vimScope.option {
      set<Int>("history", 100)
      assertEquals(100, get<Int>("history"))

      val result = reset("history")
      assertTrue(result)

      val value = get<Int>("history")
      assertEquals(50, value)
    }
  }

  @Test
  fun `test reset non-existent option`() {
    vimScope.option {
      val result = reset("nonexistentoption")
      assertFalse(result)
    }
  }

  @Test
  fun `test set option with wrong type`() {
    vimScope.option {
      assertThrows<ExException> {
        set<Int>("selection", 42)
      }
    }
  }

  @Test
  fun `test set toggle option with true value`() {
    vimScope.option {
      val result = set<Boolean>("ignorecase", true)
      assertTrue(result)

      val value = get<Boolean>("ignorecase")
      assertTrue(value == true)
    }
  }

  @Test
  fun `test set toggle option with false value`() {
    vimScope.option {
      val result = set<Boolean>("ignorecase", false)
      assertTrue(result)

      val value = get<Boolean>("ignorecase")
      assertTrue(value == false)
    }
  }

  @Test
  fun `test set and get multiple options`() {
    vimScope.option {
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
    vimScope.option {
      setGlobal<Int>("history", 100)

      setLocal<Int>("history", 20)

      assertEquals(20, get<Int>("history"))
    }
  }

  @Test
  fun `test get integer option as string returns null`() {
    vimScope.option {
      set<Int>("history", 42)

      assertThrows<IllegalArgumentException> {
        get<String>("history")
      }
    }
  }

  @Test
  fun `test get string option as integer returns null`() {
    vimScope.option {
      set<String>("selection", "inclusive")

      assertThrows<IllegalArgumentException> {
        get<Int>("selection")
      }
    }
  }

  @Test
  fun `test set integer option with string value throws exception`() {
    vimScope.option {
      assertThrows<ExException> {
        set<String>("history", "not-a-number")
      }
    }
  }

  @Test
  fun `test set boolean option with string value throws exception`() {
    vimScope.option {
      assertThrows<ExException> {
        set<String>("ignorecase", "not-a-boolean")
      }
    }
  }
}
