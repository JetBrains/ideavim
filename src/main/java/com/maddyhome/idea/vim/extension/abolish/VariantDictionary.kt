/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

/**
 * The lookup table that powers `:Subvert`: every `(lhs, rhs)` pair contributes
 * three case-variants — lowercase, capitalised-first and UPPERCASE — and brace
 * alternatives multiply the pairs.
 */
internal fun buildVariantDictionary(lhsPattern: String, rhsPattern: String): Map<String, String> {
  val dictionary = mutableMapOf<String, String>()
  expandAllBraces(lhsPattern, rhsPattern).forEach { (lhsWord, rhsWord) ->
    addCaseVariants(dictionary, lhsWord, rhsWord)
  }
  return dictionary
}

/**
 * One brace per pass, then recurse — same trick as tpope's `s:expand_braces`.
 * Each call strictly reduces the brace count, so the recursion terminates.
 */
private fun expandAllBraces(lhs: String, rhs: String): List<Pair<String, String>> {
  val lhsBrace = parseBraces(lhs)
  if (!lhsBrace.hasSlot) return listOf(lhs to rhs)

  val rhsBrace = parseBraces(rhs)
  return pairAlternatives(lhsBrace, rhsBrace).flatMap { (l, r) -> expandAllBraces(l, r) }
}

private fun pairAlternatives(lhs: BracePattern, rhs: BracePattern): List<Pair<String, String>> {
  val lhsVariants = lhs.materialise()
  val rhsVariants = generateRhsVariants(rhs, lhs, lhsVariants)
  return lhsVariants.zip(rhsVariants)
}

private fun generateRhsVariants(
  rhs: BracePattern,
  lhs: BracePattern,
  lhsVariants: List<String>,
): List<String> = if (rhs.borrowsAlternatives()) {
  rhs.materialiseWith(lhs.alternatives)
} else {
  rhs.materialiseCycling(lhsVariants.size)
}

private fun BracePattern.borrowsAlternatives(): Boolean = !hasSlot || alternatives == listOf("")

private fun addCaseVariants(dictionary: MutableMap<String, String>, lhs: String, rhs: String) {
  dictionary[lhs.lowercase()] = rhs.lowercase()
  // PascalCase variant — tpope's `mixedcase`. Snake-cased input also matches camel/Pascal occurrences.
  dictionary[CaseStyle.PASCAL.recase(lhs)] = CaseStyle.PASCAL.recase(rhs)
  dictionary[lhs.uppercase()] = rhs.uppercase()
}
