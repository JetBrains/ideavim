/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils

import com.intellij.remoterobot.fixtures.Fixture
import com.intellij.remoterobot.utils.keyboard

@Suppress("unused")
fun Fixture.insertMode() {
  type("i")
}

fun Fixture.vimExit() {
  keyboard {
    escape()
    enterText("gg")
    enterText("0")
  }
}

fun Fixture.type(keys: String) {
  keyboard { enterText(keys) }
}
