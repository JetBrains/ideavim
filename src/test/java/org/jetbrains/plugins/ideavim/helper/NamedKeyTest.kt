/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.maddyhome.idea.vim.helper.NamedKey
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class NamedKeyTest {
  @Test
  fun `test finds a key created before the first lookup`() {
    val key = Key.create<Boolean>(uniqueName())
    val namedKey = NamedKey(key.toString())

    assertSame(key, namedKey.find())
  }

  @Test
  fun `test finds a key created after a failed lookup`() {
    val name = uniqueName()
    val namedKey = NamedKey(name)
    assertNull(namedKey.find())

    Key.create<Boolean>("unrelated key")
    val key = Key.create<Boolean>(name)
    Key.create<Boolean>("another unrelated key")

    assertSame(key, namedKey.find())
  }

  @Test
  fun `test does not find a key that still doesn't exist`() {
    val namedKey = NamedKey(uniqueName())

    assertNull(namedKey.find())
    Key.create<Boolean>("unrelated key")
    assertNull(namedKey.find())
  }

  @Test
  fun `test gets user data`() {
    val key = Key.create<Boolean>(uniqueName())
    val holder = UserDataHolderBase()
    val namedKey = NamedKey(key.toString())
    assertNull(namedKey.getUserData(holder))

    holder.putUserData(key, true)

    assertEquals(true, namedKey.getUserData(holder))
  }

  private fun uniqueName() = "NamedKeyTest.${System.nanoTime()}"
}
