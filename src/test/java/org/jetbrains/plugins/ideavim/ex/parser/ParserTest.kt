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

package org.jetbrains.plugins.ideavim.ex.parser

import kotlin.math.pow

abstract class ParserTest {

  protected val ZERO_OR_MORE_SPACES = "@#\\\$#@\\\$!j zero or more spaces @#\\\$#@\\\$!j"

  protected fun getTextWithAllSpacesCombinations(text: String): List<String> {
    val combinations = mutableListOf<String>()

    val zeroOrMoreIndexes = text.allIndexesOf(ZERO_OR_MORE_SPACES)
    return if (zeroOrMoreIndexes.isNotEmpty()) {
      val allZeroOrMoreSpaceCombinations = getAllSpacesCombinations(2, zeroOrMoreIndexes.size)
      for (spacesCombination in allZeroOrMoreSpaceCombinations) {
        var newString = text
        for (space in spacesCombination) {
          newString = newString.replaceFirst(ZERO_OR_MORE_SPACES, space)
        }
        combinations.add(newString)
      }
      combinations
    } else {
      mutableListOf(text)
    }
  }

  private fun getAllSpacesCombinations(base: Int, exponent: Int): List<List<String>> {
    val spacesCombinations = mutableListOf<List<String>>()
    var counter = 0
    while (counter < base.pow(exponent)) {
      val combination = mutableListOf<String>()
      var binaryString = Integer.toBinaryString(counter)
      val leadingZeroesCount = exponent - binaryString.length
      binaryString = "0".repeat(leadingZeroesCount) + binaryString
      for (byte in binaryString) {
        combination.add(" ".repeat(Integer.parseInt(byte.toString())))
      }
      spacesCombinations.add(combination)
      counter += 1
    }
    return spacesCombinations
  }

  private fun String.allIndexesOf(seq: String): List<Int> {
    val indexes = mutableListOf<Int>()
    var index: Int = this.indexOf(seq)
    while (index >= 0) {
      indexes.add(index)
      index = this.indexOf(seq, index + 1)
    }
    return indexes
  }
}

private fun Int.pow(p: Int): Int {
  return this.toDouble().pow(p).toInt()
}
