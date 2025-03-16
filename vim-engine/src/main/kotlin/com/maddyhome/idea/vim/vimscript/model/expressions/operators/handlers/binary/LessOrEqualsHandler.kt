/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

internal object LessOrEqualsHandler :
  BinaryOperatorWithIgnoreCaseOption(LessOrEqualsIgnoreCaseHandler, LessOrEqualsCaseSensitiveHandler)

internal object LessOrEqualsIgnoreCaseHandler : ComparisonOperatorHandler() {
  override fun compare(left: Double, right: Double) = left <= right
  override fun compare(left: Int, right: Int) = left <= right
  override fun compare(left: String, right: String) = left.compareTo(right, ignoreCase = true) <= 0
}

internal object LessOrEqualsCaseSensitiveHandler : ComparisonOperatorHandler() {
  override fun compare(left: Double, right: Double) = left <= right
  override fun compare(left: Int, right: Int) = left <= right
  override fun compare(left: String, right: String) = left <= right
}
