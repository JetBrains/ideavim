/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
