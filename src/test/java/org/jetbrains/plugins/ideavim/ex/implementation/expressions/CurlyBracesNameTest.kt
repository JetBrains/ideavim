/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase

class CurlyBracesNameTest : VimTestCase() {

  fun `test name with expression`() {
    configureByText("\n")
    typeText(commandToKeys("let x1 = 10"))
    typeText(commandToKeys("echo x{0 + 1}"))
    assertExOutput("10\n")
  }

  fun `test name with inner template`() {
    configureByText("\n")
    typeText(commandToKeys("let eleven = 11"))
    typeText(commandToKeys("let z = 1"))
    typeText(commandToKeys("let y1 = 'leven'"))
    typeText(commandToKeys("echo e{y{z}}"))
    assertExOutput("11\n")
  }

  fun `test multiple templates inside a template`() {
    configureByText("\n")
    typeText(commandToKeys("let x1 = 'el'"))
    typeText(commandToKeys("let x2 = 'ev'"))
    typeText(commandToKeys("let x3 = 'en'"))
    typeText(commandToKeys("let eleven = 'twelve'"))
    typeText(commandToKeys("let twelve = 42"))

    typeText(commandToKeys("echo {x1}{x2}{x3}"))
    assertExOutput("twelve\n")

    typeText(commandToKeys("echo {{x1}{x2}{x3}}"))
    assertExOutput("42\n")
  }
}
