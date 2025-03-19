/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.AdditionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.BinaryOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ConcatenationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DivisionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DoesNotMatchHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.EqualToHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterThanHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterThanOrEqualToHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsNotHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessThanHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessThanOrEqualToHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalAndHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalOrHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MatchesHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ModulusHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MultiplicationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.SubtractionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.NotEqualToHandler

enum class BinaryOperator(val value: String, internal val handler: BinaryOperatorHandler) {
  MULTIPLICATION("*", MultiplicationHandler),
  DIVISION("/", DivisionHandler),
  ADDITION("+", AdditionHandler),
  SUBTRACTION("-", SubtractionHandler),
  CONCATENATION(".", ConcatenationHandler),
  CONCATENATION2("..", ConcatenationHandler),
  LESS_THAN("<", LessThanHandler()),
  LESS_THAN_IGNORE_CASE("<?", LessThanHandler(ignoreCase = true)),
  LESS_THAN_CASE_SENSITIVE("<#", LessThanHandler(ignoreCase = false)),
  GREATER_THAN(">", GreaterThanHandler()),
  GREATER_THAN_IGNORE_CASE(">?", GreaterThanHandler(ignoreCase = true)),
  GREATER_THAN_CASE_SENSITIVE(">#", GreaterThanHandler(ignoreCase = false)),
  EQUAL_TO("==", EqualToHandler()),
  EQUAL_TO_IGNORE_CASE("==?", EqualToHandler(ignoreCase = true)),
  EQUAL_TO_CASE_SENSITIVE("==#", EqualToHandler(ignoreCase = false)),
  NOT_EQUAL_TO("!=", NotEqualToHandler()),
  NOT_EQUAL_TO_IGNORE_CASE("!=?", NotEqualToHandler(ignoreCase = true)),
  NOT_EQUAL_CASE_SENSITIVE("!=#", NotEqualToHandler(ignoreCase = false)),
  GREATER_THAN_OR_EQUAL_TO(">=", GreaterThanOrEqualToHandler()),
  GREATER_THAN_OR_EQUAL_TO_IGNORE_CASE(">=?", GreaterThanOrEqualToHandler(ignoreCase = true)),
  GREATER_THAN_OR_EQUAL_TO_CASE_SENSITIVE(">=#", GreaterThanOrEqualToHandler(ignoreCase = false)),
  LESS_THAN_OR_EQUAL_TO("<=", LessThanOrEqualToHandler()),
  LESS_THAN_OR_EQUAL_TO_IGNORE_CASE("<=?", LessThanOrEqualToHandler(ignoreCase = true)),
  LESS_THAN_OR_EQUAL_TO_CASE_SENSITIVE("<=#", LessThanOrEqualToHandler(ignoreCase = false)),
  MODULUS("%", ModulusHandler),
  LOGICAL_AND("&&", LogicalAndHandler),
  LOGICAL_OR("||", LogicalOrHandler),
  IS("is", IsHandler()),
  IS_IGNORE_CASE("is?", IsHandler(ignoreCase = true)),
  IS_CASE_SENSITIVE("is#", IsHandler(ignoreCase = false)),
  IS_NOT("isnot", IsNotHandler()),
  IS_NOT_IGNORE_CASE("isnot?", IsNotHandler(ignoreCase = true)),
  IS_NOT_CASE_SENSITIVE("isnot#", IsNotHandler(ignoreCase = false)),
  MATCHES("=~", MatchesHandler()),
  MATCHES_IGNORE_CASE("=~?", MatchesHandler(ignoreCase = true)),
  MATCHES_CASE_SENSITIVE("=~#", MatchesHandler(ignoreCase = false)),
  DOES_NOT_MATCH("!~", DoesNotMatchHandler()),
  DOES_NOT_MATCH_IGNORE_CASE("!~?", DoesNotMatchHandler(ignoreCase = true)),
  DOES_NOT_MATCH_CASE_SENSITIVE("!~#", DoesNotMatchHandler(ignoreCase = false)),
  ;

  companion object {
    fun getByValue(value: String): BinaryOperator? {
      return entries.firstOrNull { it.value == value }
    }
  }
}
