/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchit

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchitCTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  fun `test jump from #if to #endif`() {
    doTest(
      "%",
      """
        $c#if !defined (VAL_1)
        #endif
      """.trimIndent(),
      """
        #if !defined (VAL_1)
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from whitespace before #if to #endif`() {
    doTest(
      "%",
      """
        $c   #if !defined (VAL_1)
        #endif
      """.trimIndent(),
      """
           #if !defined (VAL_1)
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #if to #elif`() {
    doTest(
      "%",
      """
        $c#if !defined (VAL_1)
          #define VAL_1 1
        #elif !defined (VAL_2)
          #define VAL_2 2
      """.trimIndent(),
      """
        #if !defined (VAL_1)
          #define VAL_1 1
        $c#elif !defined (VAL_2)
          #define VAL_2 2
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #if to #else`() {
    doTest(
      "%",
      """
        $c#if !defined (VAL_1)
          #define VAL_1 1
        #else
          #define VAL_2 2
      """.trimIndent(),
      """
        #if !defined (VAL_1)
          #define VAL_1 1
        $c#else
          #define VAL_2 2
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #elif to #else`() {
    doTest(
      "%",
      """
        $c#elif !defined (VAL_2)
        #else
      """.trimIndent(),
      """
        #elif !defined (VAL_2)
        $c#else
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from whitespace before #elif to #else`() {
    doTest(
      "%",
      """
        $c   #elif !defined (VAL_2)
        #else
      """.trimIndent(),
      """
           #elif !defined (VAL_2)
        $c#else
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #else to #endif`() {
    doTest(
      "%",
      """
        $c#else
        #endif
      """.trimIndent(),
      """
        #else
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from whitespace before #else to #endif`() {
    doTest(
      "%",
      """
        $c   #else !defined (VAL_2)
        #endif
      """.trimIndent(),
      """
           #else !defined (VAL_2)
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #endif to #if`() {
    doTest(
      "%",
      """
        #if !defined (VAL_1)
          #define VAL_1 1
        #elif !defined (VAL_2)
          #define VAL_2 2
        #else
          #define VAL_3 3
        $c#endif
      """.trimIndent(),
      """
        $c#if !defined (VAL_1)
          #define VAL_1 1
        #elif !defined (VAL_2)
          #define VAL_2 2
        #else
          #define VAL_3 3
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifdef to #endif`() {
    doTest(
      "%",
      """
        $c#ifdef DEBUG
        #endif
      """.trimIndent(),
      """
        #ifdef DEBUG
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifdef to #elif`() {
    doTest(
      "%",
      """
        $c#ifdef DEBUG
        #elif PROD
      """.trimIndent(),
      """
        #ifdef DEBUG
        $c#elif PROD
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifdef to #else`() {
    doTest(
      "%",
      """
        $c#ifdef DEBUG
        #else
      """.trimIndent(),
      """
        #ifdef DEBUG
        $c#else
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #endif to #ifdef`() {
    doTest(
      "%",
      """
        #ifdef DEBUG
        $c#endif
      """.trimIndent(),
      """
        $c#ifdef DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifndef to #endif`() {
    doTest(
      "%",
      """
        $c#ifndef DEBUG
        #endif
      """.trimIndent(),
      """
        #ifndef DEBUG
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifndef to #elif`() {
    doTest(
      "%",
      """
        $c#ifndef DEBUG
        #elif PROD
      """.trimIndent(),
      """
        #ifndef DEBUG
        $c#elif PROD
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #ifndef to #else`() {
    doTest(
      "%",
      """
        $c#ifndef DEBUG
        #else
      """.trimIndent(),
      """
        #ifndef DEBUG
        $c#else
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #endif to #ifndef`() {
    doTest(
      "%",
      """
        #ifndef DEBUG
        $c#endif
      """.trimIndent(),
      """
        $c#ifndef DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test don't jump from malformed #if`() {
    doTest(
      "%",
      """
        $c#ifff DEBUG
        #endif
      """.trimIndent(),
      """
        $c#ifff DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from #if with whitespace to #endif`() {
    doTest(
      "%",
      """
        # $c if DEBUG
        #endif
      """.trimIndent(),
      """
        #  if DEBUG
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test jump from nested #if to #endif`() {
    doTest(
      "%",
      """
        #ifdef CONDITION1
        #  ${c}ifdef CONDITION2
        #  endif
        #endif
      """.trimIndent(),
      """
        #ifdef CONDITION1
        #  ifdef CONDITION2
        $c#  endif
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  /*
   * Tests for reverse g% motion
   */

  @Test
  fun `test reverse jump from #if to #endif`() {
    doTest(
      "g%",
      """
        $c#if !defined (VAL_1)
        #endif
      """.trimIndent(),
      """
        #if !defined (VAL_1)
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from whitespace before #if to #endif`() {
    doTest(
      "g%",
      """
        $c   #if !defined (VAL_1)
        #endif
      """.trimIndent(),
      """
           #if !defined (VAL_1)
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #endif to #if with whitespace`() {
    doTest(
      "g%",
      """
        #  if DEBUG
        $c#endif
      """.trimIndent(),
      """
        $c#  if DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #endif to #else`() {
    doTest(
      "g%",
      """
        #else
          #define VAL_3 3
        $c#endif
      """.trimIndent(),
      """
        $c#else
          #define VAL_3 3
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #else to #elif`() {
    doTest(
      "g%",
      """
        #elif !defined (VAL_2)
          #define VAL_2 2
        $c#else
          #define VAL_3 3
        #endif
      """.trimIndent(),
      """
        $c#elif !defined (VAL_2)
          #define VAL_2 2
        #else
          #define VAL_3 3
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from whitespace before #else to #elif`() {
    doTest(
      "g%",
      """
        #elif !defined (VAL_2)
            #define VAL_2 2
        $c  #else
            #define VAL_3 3
        #endif
      """.trimIndent(),
      """
        $c#elif !defined (VAL_2)
            #define VAL_2 2
          #else
            #define VAL_3 3
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #elif to #if`() {
    doTest(
      "g%",
      """
        #if !defined (VAL_1)
          #define VAL_1 1
        $c#elif !defined (VAL_2)
          #define VAL_2 2
      """.trimIndent(),
      """
        $c#if !defined (VAL_1)
          #define VAL_1 1
        #elif !defined (VAL_2)
          #define VAL_2 2
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #ifdef to #endif`() {
    doTest(
      "g%",
      """
        $c#ifdef DEBUG
        #endif
      """.trimIndent(),
      """
        #ifdef DEBUG
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #endif to #ifdef`() {
    doTest(
      "g%",
      """
        #ifdef DEBUG
        $c#endif
      """.trimIndent(),
      """
        $c#ifdef DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #else to #ifdef`() {
    doTest(
      "g%",
      """
        #ifdef DEBUG
        $c#else
      """.trimIndent(),
      """
        $c#ifdef DEBUG
        #else
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #elif to #ifdef`() {
    doTest(
      "g%",
      """
        #ifdef DEBUG
        $c#elif PROD
      """.trimIndent(),
      """
        $c#ifdef DEBUG
        #elif PROD
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #ifndef to #endif`() {
    doTest(
      "g%",
      """
        $c#ifndef DEBUG
        #endif
      """.trimIndent(),
      """
        #ifndef DEBUG
        $c#endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #endif to #ifndef`() {
    doTest(
      "g%",
      """
        #ifndef DEBUG
        $c#endif
      """.trimIndent(),
      """
        $c#ifndef DEBUG
        #endif
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #elif to #ifndef`() {
    doTest(
      "g%",
      """
        #ifndef DEBUG
        $c#elif PROD
      """.trimIndent(),
      """
        $c#ifndef DEBUG
        #elif PROD
      """.trimIndent(),
      fileName = "main.c",
    )
  }

  @Test
  fun `test reverse jump from #else to #ifndef`() {
    doTest(
      "g%",
      """
        #ifndef DEBUG
        $c#else
      """.trimIndent(),
      """
        $c#ifndef DEBUG
        #else
      """.trimIndent(),
      fileName = "main.c",
    )
  }
}
