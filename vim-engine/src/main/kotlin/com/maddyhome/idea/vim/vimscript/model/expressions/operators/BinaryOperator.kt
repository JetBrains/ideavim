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
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DoesntMatchHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.EqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterOrEqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsNotHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessOrEqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalAndHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalOrHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MatchesHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ModulusHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MultiplicationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.SubtractionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.UnequalsHandler

enum class BinaryOperator(val value: String, internal val handler: BinaryOperatorHandler) {
  MULTIPLICATION("*", MultiplicationHandler),
  DIVISION("/", DivisionHandler),
  ADDITION("+", AdditionHandler),
  SUBTRACTION("-", SubtractionHandler),
  CONCATENATION(".", ConcatenationHandler),
  CONCATENATION2("..", ConcatenationHandler),
  LESS("<", LessHandler()),
  LESS_IGNORE_CASE("<?", LessHandler(ignoreCase = true)),
  LESS_CASE_SENSITIVE("<#", LessHandler(ignoreCase = false)),
  GREATER(">", GreaterHandler()),
  GREATER_IGNORE_CASE(">?", GreaterHandler(ignoreCase = true)),
  GREATER_CASE_SENSITIVE(">#", GreaterHandler(ignoreCase = false)),
  EQUALS("==", EqualsHandler()),
  EQUALS_IGNORE_CASE("==?", EqualsHandler(ignoreCase = true)),
  EQUALS_CASE_SENSITIVE("==#", EqualsHandler(ignoreCase = false)),
  UNEQUALS("!=", UnequalsHandler()),
  UNEQUALS_IGNORE_CASE("!=?", UnequalsHandler(ignoreCase = true)),
  UNEQUALS_CASE_SENSITIVE("!=#", UnequalsHandler(ignoreCase = false)),
  GREATER_OR_EQUALS(">=", GreaterOrEqualsHandler()),
  GREATER_OR_EQUALS_IGNORE_CASE(">=?", GreaterOrEqualsHandler(ignoreCase = true)),
  GREATER_OR_EQUALS_CASE_SENSITIVE(">=#", GreaterOrEqualsHandler(ignoreCase = false)),
  LESS_OR_EQUALS("<=", LessOrEqualsHandler()),
  LESS_OR_EQUALS_IGNORE_CASE("<=?", LessOrEqualsHandler(ignoreCase = true)),
  LESS_OR_EQUALS_CASE_SENSITIVE("<=#", LessOrEqualsHandler(ignoreCase = false)),
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
  DOESNT_MATCH("!~", DoesntMatchHandler()),
  DOESNT_MATCH_IGNORE_CASE("!~?", DoesntMatchHandler(ignoreCase = true)),
  DOESNT_MATCH_CASE_SENSITIVE("!~#", DoesntMatchHandler(ignoreCase = false)),
  ;

  companion object {
    fun getByValue(value: String): BinaryOperator? {
      return entries.firstOrNull { it.value == value }
    }
  }
}
