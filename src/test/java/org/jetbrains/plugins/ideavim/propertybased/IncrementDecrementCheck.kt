/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.NeovimTesting
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import kotlin.math.absoluteValue
import kotlin.math.sign

class IncrementDecrementTest : VimPropertyTestBase() {
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
    VimPlugin.getOptionService().appendValue(OptionScope.GLOBAL, OptionConstants.nrformats, "octal", OptionConstants.nrformats)
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val number = env.generateValue(testNumberGenerator, "Generate %s number")
        val editor = configureByText(number)
        try {
          moveCaretToRandomPlace(env, editor)

          NeovimTesting.setupEditor(editor, this)
          NeovimTesting.typeCommand(":set nrformats+=octal<CR>", this, editor)

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
    val action = injector.parser.parseKeys(key).single()
    env.logMessage("Use command: ${injector.parser.toKeyNotation(action)}.")
    VimTestCase.typeText(listOf(action), editor, editor.project)
    NeovimTesting.typeCommand(key, test, editor)

    IdeEventQueue.getInstance().flushQueue()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}

val differentFormNumberGenerator = Generator.from { env ->
  val form = env.generate(Generator.sampledFrom(/*2,*/ 8, 10, 16))
  env.generate(
    Generator.integers().suchThat { it != Int.MIN_VALUE }.map {
      val sign = it.sign
      val stringNumber = it.absoluteValue.toString(form)
      if (sign < 0) "-$stringNumber" else stringNumber
    }
  )
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
  env.generate(
    Generator.frequency(
      10, differentFormNumberGenerator,
      1, brokenNumberGenerator
    )
  )
}
