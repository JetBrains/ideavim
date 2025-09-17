/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

/**
 * Tests the operators available for the `let` command.
 *
 * The `:let` command has a painful combinatorial matrix of lvalue kind (variable, indexed expression, sublist, etc.),
 * initial value, operator, rvalue. This tests the operators with simple variables, and ensures that the operators
 * calculate the right values and do the right data conversions.
 *
 * Note that we're testing with a variable lvalue here. The operator might change the type of the lvalue expression.
 * E.g. `let s='10.5' | let s+='20.5'` will convert the current lvalue and rvalue to Number, perform the operation, and
 * assign a Number (not a Float!) to the variable. This is not the case with all lvalue expressions! A register must be
 * a String. An option must be a String or Number. The operator cannot assign a Number to a String; it does not convert.
 */
class LetCommandOperatorsTest : VimTestCase("\n") {
  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("additionOperator")
  fun `test addition compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  @Test
  fun `test addition compound assignment operator on List modifies in-place`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let t=s")
    enterCommand("let s += [4, 5, 6]")
    assertCommandOutput("echo s", "[1, 2, 3, 4, 5, 6]")
    assertCommandOutput("echo t", "[1, 2, 3, 4, 5, 6]")
  }


  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("subtractionOperator")
  fun `test subtraction compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("multiplicationOperator")
  fun `test multiplication compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("divisionOperator")
  fun `test division compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("moduloOperator")
  fun `test modulo compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  @ParameterizedTest(name = "{0} | {1} → {2}")
  @MethodSource("concatenationOperator", "concatenation2Operator")
  fun `test concatenation compound assignment operator`(init: String, action: String, result: String) {
    assertLet(init, action, result)
  }

  companion object {
    @JvmStatic
    fun additionOperator() = buildList {
      withInitialValue("let s=10") {
        case("let s+=10", "20")
        case("let s+=20.5", "30.5")  // LValue Number is converted to Float
        case("let s+='20'", "30")    // RValue String is converted to Number
        case("let s+='foo'", "10")   // RValue String is converted to Number (0)
        case("let s+=[1,2,3]", "E734: Wrong variable type for +=")
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }

      withInitialValue("let s=10.5") {
        case("let s+=10", "20.5")      // RValue Number is converted to Float
        case("let s+=20.5", "31.0")
        case("let s+='10.5'", "20.5")  // String converts to Number, not Float, but result is Float
        case("let s+='foo'", "10.5")   // RValue String is converted to Number (0)
        case("let s+=[1,2,3]", "E734: Wrong variable type for +=")
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }

      withInitialValue("let s='10.5'") {
        case("let s+=10", "20")      // LValue String is converted to Number
        case("let s+=20.5", "30.5")  // String is converted to Number, operation converted to Float
        case("let s+='20.5'", "30")  // Strings are converted to Number
        case("let s+='0'", "10")     // Strings are converted to Number
        case("let s+=[1,2,3]", "E734: Wrong variable type for +=")
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }

      withInitialValue("let s='foo'") {
        case("let s+=10", "10")      // LValue String is converted to Number
        case("let s+=20.5", "20.5")  // String is converted to Number, operation converted to Float
        case("let s+='20.5'", "20")  // Strings are converted to Number
        case("let s+='0'", "0")      // Strings are converted to Number
        case("let s+=[1,2,3]", "E734: Wrong variable type for +=")
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s+=10", "E734: Wrong variable type for +=")
        case("let s+=10.5", "E734: Wrong variable type for +=")
        case("let s+='10.5'", "E734: Wrong variable type for +=")
        case("let s+='foo'", "E734: Wrong variable type for +=")
        case("let s+=[1,2,3]", "[1, 2, 3, 1, 2, 3]") // Needs further test to ensure modifies in-place
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s+=10", "E734: Wrong variable type for +=")
        case("let s+=10.5", "E734: Wrong variable type for +=")
        case("let s+='10.5'", "E734: Wrong variable type for +=")
        case("let s+='foo'", "E734: Wrong variable type for +=")
        case("let s+=[1,2,3]", "E734: Wrong variable type for +=")
        case("let s+={'key1': 1, 'key2': 2}", "E734: Wrong variable type for +=")
      }
    }

    @JvmStatic
    fun subtractionOperator() = buildList {
      withInitialValue("let s=30") {
        case("let s-=10", "20")
        case("let s-=20.5", "9.5")  // LValue Number is converted to Float
        case("let s-='20'", "10")   // RValue String is converted to Number
        case("let s-='foo'", "30")  // RValue String is converted to Number (0)
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }

      withInitialValue("let s=30.5") {
        case("let s-=10", "20.5")      // RValue Number is converted to Float
        case("let s-=20.5", "10.0")
        case("let s-='10.5'", "20.5")  // String converts to Number, not Float, but result is Float
        case("let s-='foo'", "30.5")   // RValue String is converted to Number (0)
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }

      withInitialValue("let s='30.5'") {
        case("let s-=10", "20")     // LValue String is converted to Number
        case("let s-=20.5", "9.5")  // String is converted to Number, operation converted to Float
        case("let s-='20.5'", "10") // Strings are converted to Number
        case("let s-='0'", "30")    // Strings are converted to Number
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }

      withInitialValue("let s='foo'") {
        case("let s-=10", "-10")      // LValue String is converted to Number
        case("let s-=20.5", "-20.5")  // String is converted to Number, operation converted to Float
        case("let s-='20.5'", "-20")  // Strings are converted to Number
        case("let s-='0'", "0")       // Strings are converted to Number
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s-=10", "E734: Wrong variable type for -=")
        case("let s-=10.5", "E734: Wrong variable type for -=")
        case("let s-='10.5'", "E734: Wrong variable type for -=")
        case("let s-='foo'", "E734: Wrong variable type for -=")
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s-=10", "E734: Wrong variable type for -=")
        case("let s-=10.5", "E734: Wrong variable type for -=")
        case("let s-='10.5'", "E734: Wrong variable type for -=")
        case("let s-='foo'", "E734: Wrong variable type for -=")
        case("let s-=[1,2,3]", "E734: Wrong variable type for -=")
        case("let s-={'key1': 1, 'key2': 2}", "E734: Wrong variable type for -=")
      }
    }

    @JvmStatic
    fun multiplicationOperator() = buildList {
      withInitialValue("let s=10") {
        case("let s*=10", "100")
        case("let s*=20.55", "205.5") // LValue Number is converted to Float
        case("let s*='20'", "200")    // RValue String is converted to Number
        case("let s*='foo'", "0")     // RValue String is converted to Number (0)
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }

      withInitialValue("let s=10.55") {
        case("let s*=10", "105.5")      // RValue Number is converted to Float
        case("let s*=20.5", "216.275")
        case("let s*='10.5'", "105.5")  // String converts to Number, not Float, but result is Float
        case("let s*='foo'", "0.0")     // RValue String is converted to Number (0)
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }

      withInitialValue("let s='30.5'") {
        case("let s*=10", "300")      // LValue String is converted to Number
        case("let s*=20.5", "615.0")  // String is converted to Number, operation converted to Float
        case("let s*='20.5'", "600")  // Strings are converted to Number
        case("let s*='0'", "0")       // Strings are converted to Number
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }

      withInitialValue("let s='foo'") {
        case("let s*=10", "0")      // LValue String is converted to Number
        case("let s*=20.5", "0.0")  // String is converted to Number, operation converted to Float
        case("let s*='20.5'", "0")  // Strings are converted to Number
        case("let s*='0'", "0")     // Strings are converted to Number
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s*=10", "E734: Wrong variable type for *=")
        case("let s*=10.5", "E734: Wrong variable type for *=")
        case("let s*='10.5'", "E734: Wrong variable type for *=")
        case("let s*='foo'", "E734: Wrong variable type for *=")
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s*=10", "E734: Wrong variable type for *=")
        case("let s*=10.5", "E734: Wrong variable type for *=")
        case("let s*='10.5'", "E734: Wrong variable type for *=")
        case("let s*='foo'", "E734: Wrong variable type for *=")
        case("let s*=[1,2,3]", "E734: Wrong variable type for *=")
        case("let s*={'key1': 1, 'key2': 2}", "E734: Wrong variable type for *=")
      }
    }

    @JvmStatic
    fun divisionOperator() = buildList {
      withInitialValue("let s=100") {
        case("let s/=10", "10")
        case("let s/=20.5", "4.878049") // LValue Number is converted to Float
        case("let s/='20'", "5")        // RValue String is converted to Number
        case("let s/='foo'", "${Int.MAX_VALUE}")  // Divide by 0! Avoided by converting to Float, resulting in Int.MaxValue
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }

      withInitialValue("let s=105.5") {
        case("let s/=10", "10.55")      // RValue Number is converted to Float
        case("let s/=20.5", "5.146341")
        case("let s/='10.5'", "10.55")  // String converts to Number, not Float, but result is Float
        case("let s/='foo'", "inf")     // RValue String is converted to Number (divide by 0!)
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }

      withInitialValue("let s='30.5'") {
        case("let s/=10", "3")          // LValue String is converted to Number
        case("let s/=20.5", "1.463415") // String is converted to Number, operation converted to Float
        case("let s/='20.5'", "1")      // Strings are converted to Number, result is Number
        case("let s/='0'", "${Int.MAX_VALUE}")  // Divide by 0! Avoided by converting to Float, resulting in Int.MaxValue
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }

      withInitialValue("let s='foo'") {
        case("let s/=10", "0")      // LValue String is converted to Number
        case("let s/=20.5", "0.0")  // String is converted to Number, operation converted to Float
        case("let s/='20.5'", "0")  // Strings are converted to Number
        case("let s/='0'", "0")     // Strings are converted to Number
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s/=10", "E734: Wrong variable type for /=")
        case("let s/=10.5", "E734: Wrong variable type for /=")
        case("let s/='10.5'", "E734: Wrong variable type for /=")
        case("let s/='foo'", "E734: Wrong variable type for /=")
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s/=10", "E734: Wrong variable type for /=")
        case("let s/=10.5", "E734: Wrong variable type for /=")
        case("let s/='10.5'", "E734: Wrong variable type for /=")
        case("let s/='foo'", "E734: Wrong variable type for /=")
        case("let s/=[1,2,3]", "E734: Wrong variable type for /=")
        case("let s/={'key1': 1, 'key2': 2}", "E734: Wrong variable type for /=")
      }
    }

    @JvmStatic
    fun moduloOperator() = buildList {
      withInitialValue("let s=15") {
        case("let s%=12", "3")
        case("let s%=20.5", "E734: Wrong variable type for %=")
        case("let s%='13'", "2")    // RValue String is converted to Number
        case("let s%='foo'", "0")
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }

      withInitialValue("let s=105.5") {
        case("let s%=10", "E734: Wrong variable type for %=")
        case("let s%=20.5", "E734: Wrong variable type for %=")
        case("let s%='10.5'", "E734: Wrong variable type for %=")
        case("let s%='foo'", "E734: Wrong variable type for %=")
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }

      withInitialValue("let s='31.5'") {
        case("let s%=13", "5")          // LValue String is converted to Number
        case("let s%=20.5", "E734: Wrong variable type for %=")
        case("let s%='13.5'", "5")      // Strings are converted to Number, result is Number
        case("let s%='0'", "0")
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }

      withInitialValue("let s='foo'") {
        case("let s%=10", "0")      // LValue String is converted to Number
        case("let s%=20.5", "E734: Wrong variable type for %=")
        case("let s%='20.5'", "0")  // Strings are converted to Number
        case("let s%='0'", "0")     // Strings are converted to Number
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s%=10", "E734: Wrong variable type for %=")
        case("let s%=10.5", "E734: Wrong variable type for %=")
        case("let s%='10.5'", "E734: Wrong variable type for %=")
        case("let s%='foo'", "E734: Wrong variable type for %=")
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s%=10", "E734: Wrong variable type for %=")
        case("let s%=10.5", "E734: Wrong variable type for %=")
        case("let s%='10.5'", "E734: Wrong variable type for %=")
        case("let s%='foo'", "E734: Wrong variable type for %=")
        case("let s%=[1,2,3]", "E734: Wrong variable type for %=")
        case("let s%={'key1': 1, 'key2': 2}", "E734: Wrong variable type for %=")
      }
    }

    @JvmStatic
    fun concatenationOperator() = getConcatenationOperatorCases(".=")
    @JvmStatic
    fun concatenation2Operator() = getConcatenationOperatorCases("..=")

    private fun getConcatenationOperatorCases(operator: String) = buildList {
      withInitialValue("let s=100") {
        case("let s $operator 10", "'10010'")      // Operator converts to String
        case("let s $operator 20.5", "E734: Wrong variable type for $operator")
        case("let s $operator '20'", "'10020'")    // LValue Number is converted to String
        case("let s $operator 'foo'", "'100foo'")  // LValue Number is converted to String
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }

      withInitialValue("let s=105.5") {
        case("let s $operator 10", "E734: Wrong variable type for $operator")
        case("let s $operator 20.5", "E734: Wrong variable type for $operator")
        case("let s $operator '10.5'", "E734: Wrong variable type for $operator")
        case("let s $operator 'foo'", "E734: Wrong variable type for $operator")
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }

      withInitialValue("let s='30.5'") {
        case("let s $operator 10", "'30.510'")   // RValue Number is converted to String
        case("let s $operator 20.5", "E734: Wrong variable type for $operator")
        case("let s $operator '20.5'", "'30.520.5'")
        case("let s $operator '0'", "'30.50'")
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }

      withInitialValue("let s='foo'") {
        case("let s $operator 10", "'foo10'")  // LValue String is converted to Number
        case("let s $operator 20.5", "E734: Wrong variable type for $operator")
        case("let s $operator '20.5'", "'foo20.5'")
        case("let s $operator '0'", "'foo0'")
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }

      withInitialValue("let s=[1,2,3]") {
        case("let s $operator 10", "E734: Wrong variable type for $operator")
        case("let s $operator 10.5", "E734: Wrong variable type for $operator")
        case("let s $operator '10.5'", "E734: Wrong variable type for $operator")
        case("let s $operator 'foo'", "E734: Wrong variable type for $operator")
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }

      withInitialValue("let s={'key1': 1, 'key2': 2}") {
        case("let s $operator 10", "E734: Wrong variable type for $operator")
        case("let s $operator 10.5", "E734: Wrong variable type for $operator")
        case("let s $operator '10.5'", "E734: Wrong variable type for $operator")
        case("let s $operator 'foo'", "E734: Wrong variable type for $operator")
        case("let s $operator [1,2,3]", "E734: Wrong variable type for $operator")
        case("let s $operator {'key1': 1, 'key2': 2}", "E734: Wrong variable type for $operator")
      }
    }

    private fun MutableList<Arguments>.withInitialValue(init: String, block: CaseBuilder.() -> Unit) {
      val builder = CaseBuilder(init)
      block(builder)
      this.addAll(builder.cases)
    }

    private class CaseBuilder(private val init: String) {
      val cases = mutableListOf<Arguments>()

      fun case(operation: String, result: String) {
        cases.add(Arguments.of(init, operation, result))
      }
    }
  }

  private fun assertLet(init: String, action: String, result: String) {
    val letCommand = injector.vimscriptParser.parseLetCommand(init) as? LetCommand
    val lvalue = letCommand?.lvalue?.originalString
    enterCommand(init)
    enterCommand(action)
    if (result.startsWith("E")) {
      if (!injector.messages.isError()) {
        enterCommand("echo string($lvalue)")
        val actual = injector.outputPanel.getCurrentOutputPanel()?.text
        assertEquals(
          true,
          injector.messages.isError(),
          injector.messages.getStatusBarMessage()
            ?: "Expected error \"$result\". No error found. Actual result: $actual"
        )
      }
      else {
        assertPluginError(true)
      }
      assertPluginErrorMessage(result)
    } else {
      assertPluginError(false)
      assertCommandOutput("echo string($lvalue)", result)
    }
  }
}
