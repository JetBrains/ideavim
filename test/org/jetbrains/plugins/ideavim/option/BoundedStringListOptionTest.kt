/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.option.BoundedStringListOption
import junit.framework.TestCase

class BoundedStringListOptionTest: TestCase() {
  private val option =
    BoundedStringListOption(
      "myOpt", "myOpt", arrayOf("Monday", "Tuesday"),
      arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    )

  fun `test set valid list`() {
    option.set("Thursday,Friday")
    assertEquals("Thursday,Friday", option.value)
  }

  fun `test set list with invalid value`() {
    option.set("Blue")
    assertEquals("Monday,Tuesday", option.value)
  }

  fun `test append single item`() {
    option.append("Wednesday")
    assertEquals("Monday,Tuesday,Wednesday", option.value)
  }

  fun `test append invalid item`() {
    option.append("Blue")
    assertEquals("Monday,Tuesday", option.value)
  }

  fun `test append list`() {
    option.append("Wednesday,Thursday")
    assertEquals("Monday,Tuesday,Wednesday,Thursday", option.value)
  }

  fun `test append list with invalid item`() {
    option.append("Wednesday,Blue")
    assertEquals("Monday,Tuesday", option.value)
  }

  fun `test prepend item`() {
    option.prepend("Wednesday")
    assertEquals("Wednesday,Monday,Tuesday", option.value)
  }

  fun `test prepend invalid item`() {
    option.prepend("Blue")
    assertEquals("Monday,Tuesday", option.value)
  }

  fun `test prepend list`() {
    option.prepend("Wednesday,Thursday")
    assertEquals("Wednesday,Thursday,Monday,Tuesday", option.value)
  }

  fun `test prepend list with invalid item`() {
    option.prepend("Wednesday,Blue")
    assertEquals("Monday,Tuesday", option.value)
  }

  fun `test remove item`() {
    option.remove("Monday")
    assertEquals("Tuesday", option.value)
  }

  fun `test remove list`() {
    option.remove("Monday,Tuesday")
    assertEquals("", option.value)
  }

  fun `test remove list with invalid value`() {
    option.remove("Monday,Blue")
    assertEquals("Monday,Tuesday", option.value)
  }
}