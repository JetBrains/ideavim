/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.NeovimTesting
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import kotlin.math.absoluteValue
import kotlin.math.sign

class IncrementDecrementTest : VimPropertyTest() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testPlayingWithNumbers() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(numbers)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(IncrementDecrementActions(editor, this)))
        } finally {
          reset(editor)
        }
      }
    }
  }

  fun testPlayingWithNumbersGenerateNumber() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf(':')
    }
    OptionsManager.nrformats.append("octal")
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val number = env.generateValue(testNumberGenerator, "Generate %s number")
        val editor = configureByText(number)
        try {
          moveCaretToRandomPlace(env, editor)

          NeovimTesting.setupEditor(editor, this)
          NeovimTesting.typeCommand(":set nrformats+=octal<CR>", this)

          env.executeCommands(Generator.sampledFrom(IncrementDecrementActions(editor, this)))

          NeovimTesting.assertState(editor, this)
        } finally {
          reset(editor)
        }
      }
    }
  }
}

private class IncrementDecrementActions(private val editor: Editor, val test: VimTestCase) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val generator = Generator.sampledFrom("<C-A>", "<C-X>")
    val key = env.generateValue(generator, null)
    val action = parseKeys(key).single()
    env.logMessage("Use command: ${StringHelper.toKeyNotation(action)}.")
    VimTestCase.typeText(listOf(action), editor, editor.project)
    NeovimTesting.typeCommand(key, test)

    IdeEventQueue.getInstance().flushQueue()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}

val differentFormNumberGenerator = Generator.from { env ->
  val form = env.generate(Generator.sampledFrom(/*2,*/ 8, 10, 16))
  env.generate(Generator.integers().suchThat { it != Int.MIN_VALUE }.map {
    val sign = it.sign
    val stringNumber = it.absoluteValue.toString(form)
    if (sign < 0) "-$stringNumber" else stringNumber
  })
}

val brokenNumberGenerator = Generator.from { env ->
  val bigChar = env.generate(Generator.anyOf(Generator.charsInRange('8', '9'), Generator.charsInRange('G', 'Z')))
  val number = env.generate(differentFormNumberGenerator)
  if (number.length > 4) {
    val insertAt = env.generate(Generator.integers(4, number.length - 1))
    number.take(insertAt) + bigChar + number.substring(insertAt)
  } else "$number$bigChar"
}

val testNumberGenerator = Generator.from { env ->
  env.generate(Generator.frequency(
    10, differentFormNumberGenerator,
    1, brokenNumberGenerator
  ))
}

