/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.keyStroke
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.propertybased.samples.simpleText
import org.junit.jupiter.api.Test

// TODO: 25.01.2021 Add neovim test integration
class MapPropertyTest : VimPropertyTestBase() {
  // Disabled, it works too long
  @Suppress("TestFunctionName", "unused")
  fun /*test*/RandomMappings() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(simpleText)
        try {
          editor.caretModel.moveToOffset(58) // At the word "lavender"

          val mappingCommands = generateSequence {
            val mode = env.generateValue(Generator.sampledFrom(modesList), null)

            val fromKeys = generateSequence {
              env.generateValue(Generator.sampledFrom(keysPlaying), null)
            }.take(env.generateValue(Generator.integers(1, 10), null)).joinToString(separator = "")

            val toKeys = generateSequence {
              env.generateValue(Generator.sampledFrom(keysPlaying), null)
            }.take(env.generateValue(Generator.integers(1, 10), null)).joinToString(separator = "")

            "$mode $fromKeys $toKeys"
          }.take(env.generateValue(Generator.integers(1, 10), null)).toList()

          val enteringKeys = generateSequence {
            env.generateValue(Generator.sampledFrom(keysPlaying), null)
          }.take(env.generateValue(Generator.integers(1, 10), null)).joinToString(separator = "")

          env.logMessage("Commands:\n${mappingCommands.joinToString("\n")}")
          mappingCommands.forEach { typeText(injector.parser.parseKeys(exCommand(it))) }

          env.logMessage("Enter keys: $enteringKeys")
          typeText(injector.parser.parseKeys(enteringKeys))
        } finally {
          reset(editor)
        }
      }
    }
  }

  @Test
  fun testEmpty() {
    // Just an empty test
  }
}

// I think, it would be enough to test normal mode only. Not sure if it's true
private val modesList = arrayListOf(
  "map", /*"nmap", "vmap", "xmap", "omap", "imap", "vmap",*/
  "noremap", /*, "nnoremap", "vnoremap", "xnoremap", "onoremap", "inoremap", "vnoremap"*/
)

private val keysPlaying = arrayListOf("h", "j", "k", "l", "w", "b", "e")
