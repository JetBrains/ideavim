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

package org.jetbrains.plugins.ideavim.extension.matchit

import org.jetbrains.plugins.ideavim.VimTestCase

class MatchitCMakeTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("matchit")
  }

  fun `test jump from if to else`() {
    doTest(
      "%",
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from else to endif`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        ${c}endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endif to if`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        ${c}endif()
      """.trimIndent(),
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from if to elseif in if-else structure`() {
    doTest(
      "%",
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from elseif to elseif`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from elseif to else`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        ${c}else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from else to endif in if-else structure`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        ${c}else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        ${c}endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endif to if in if-else structure`() {
    doTest(
      "%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        ${c}endif()
      """.trimIndent(),
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from foreach to endforeach`() {
    doTest(
      "%",
      """
        ${c}foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endforeach to foreach`() {
    doTest(
      "%",
      """
        foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      """
        ${c}foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from foreach to break`() {
    doTest(
      "%",
      """
        ${c}foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            ${c}break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from break to endforeach`() {
    doTest(
      "%",
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            ${c}break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from while to endwhile`() {
    doTest(
      "%",
      """
        ${c}while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        endwhile()
      """.trimIndent(),
      """
        while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        ${c}endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endwhile to while`() {
    doTest(
      "%",
      """
        while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        ${c}endwhile()
      """.trimIndent(),
      """
        ${c}while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from while to break`() {
    doTest(
      "%",
      """
        ${c}while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        endwhile()
      """.trimIndent(),
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            ${c}break
          endif()
        endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from break to endwhile`() {
    doTest(
      "%",
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            ${c}break
          endif()
        endwhile()
      """.trimIndent(),
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        ${c}endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from function to endfunction`() {
    doTest(
      "%",
      """
        ${c}function(foo)
          bar(x y z)
        endfunction()
      """.trimIndent(),
      """
        function(foo)
          bar(x y z)
        ${c}endfunction()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endfunction to function`() {
    doTest(
      "%",
      """
        function(foo)
          bar(x y z)
        ${c}endfunction()
      """.trimIndent(),
      """
        ${c}function(foo)
          bar(x y z)
        endfunction()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from macro to endmacro`() {
    doTest(
      "%",
      """
        ${c}macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        endmacro()
      """.trimIndent(),
      """
        macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        ${c}endmacro()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test jump from endmacro to macro`() {
    doTest(
      "%",
      """
        macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        ${c}endmacro()
      """.trimIndent(),
      """
        ${c}macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        endmacro()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  // Tests for reverse motion

  fun `test reverse jump from if to endif`() {
    doTest(
      "g%",
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        ${c}endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from else to if`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endif to else`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        else()
          message("Non-linux system")
        ${c}endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}else()
          message("Non-linux system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from if to endif in if-else block`() {
    doTest(
      "g%",
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        ${c}endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from elseif to if`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        ${c}if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from elseif in else block to elseif`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from else to elseif`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        ${c}else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        ${c}elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endif to else in if-else block`() {
    doTest(
      "g%",
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        else()
          message("Unknown system")
        ${c}endif()
      """.trimIndent(),
      """
        if (CMAKE_SYSTEM_NAME MATCHES "Linux")
          message("Linux")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
          message("MacOS")
        elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
          message("Windows")
        ${c}else()
          message("Unknown system")
        endif()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from foreach to endforeach`() {
    doTest(
      "g%",
      """
        ${c}foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endforeach to foreach`() {
    doTest(
      "g%",
      """
        foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      """
        ${c}foreach(X IN LISTS A B C)
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from foreach to endforeach over a break`() {
    doTest(
      "g%",
      """
        ${c}foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endforeach to break`() {
    doTest(
      "g%",
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        ${c}endforeach()
      """.trimIndent(),
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            ${c}break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from break to foreach`() {
    doTest(
      "g%",
      """
        foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            ${c}break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      """
        ${c}foreach(X IN LISTS A B C)
          if (X MATCHES "FOO")
            break
          endif()
          message(STATUS "X=${"\${X}"}")
        endforeach()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from while to endwhile`() {
    doTest(
      "g%",
      """
        ${c}while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        endwhile()
      """.trimIndent(),
      """
        while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        ${c}endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endwhile to while`() {
    doTest(
      "g%",
      """
        while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        ${c}endwhile()
      """.trimIndent(),
      """
        ${c}while(${"\${index}"} LESS 10)
          MATH(EXPR VAR "${"\${index}"}+1")
        endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from while to endwhile over a break`() {
    doTest(
      "g%",
      """
        ${c}while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        endwhile()
      """.trimIndent(),
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        ${c}endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endwhile to break`() {
    doTest(
      "g%",
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        ${c}endwhile()
      """.trimIndent(),
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            ${c}break
          endif()
        endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from break to while`() {
    doTest(
      "g%",
      """
        while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            ${c}break
          endif()
        endwhile()
      """.trimIndent(),
      """
        ${c}while (TRUE)
          MATH(EXPR VAR "${"\${index}+1\""}")
          if (${"\${index}"} EQUAL 5)
            break
          endif()
        endwhile()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from function to endfunction`() {
    doTest(
      "g%",
      """
        ${c}function(foo)
          bar(x y z)
        endfunction()
      """.trimIndent(),
      """
        function(foo)
          bar(x y z)
        ${c}endfunction()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endfunction to function`() {
    doTest(
      "g%",
      """
        function(foo)
          bar(x y z)
        ${c}endfunction()
      """.trimIndent(),
      """
        ${c}function(foo)
          bar(x y z)
        endfunction()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from macro to endmacro`() {
    doTest(
      "g%",
      """
        ${c}macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        endmacro()
      """.trimIndent(),
      """
        macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        ${c}endmacro()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }

  fun `test reverse jump from endmacro to macro`() {
    doTest(
      "g%",
      """
        macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        ${c}endmacro()
      """.trimIndent(),
      """
        ${c}macro(Foo arg)
          message("arg = ${"\${arg}\""}")
        endmacro()
      """.trimIndent(),
      fileName = "CMakeLists.txt"
    )
  }
}
