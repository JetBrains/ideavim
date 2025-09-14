/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class StringFunctionTest : VimTestCase("\n") {
  @Test
  fun `test string() outputs Number`() {
    enterCommand("let s=12")
    assertCommandOutput("echo string(s)", "12")
  }
  @Test
  fun `test string() outputs Float`() {
    enterCommand("let s=12.34")
    assertCommandOutput("echo string(s)", "12.34")
  }

  @Test
  fun `test string() outputs Float to 6 decimal places`() {
    enterCommand("let s=12.3456789")
    assertCommandOutput("echo string(s)", "12.345679")
  }

  @Test
  fun `test string() outputs Float with scientific notation`() {
    enterCommand("let s=3.2e-5")
    assertCommandOutput("echo string(s)", "3.2e-5")
  }

  @Test
  fun `test string() outputs Float Nan`() {
    enterCommand("let s=0.0 / 0.0")
    assertCommandOutput("echo string(s)", "nan")
  }

  @Test
  fun `test string() outputs Float inf`() {
    enterCommand("let s=1.0 / 0.0")
    assertCommandOutput("echo string(s)", "inf")
  }
  @Test
  fun `test string() outputs Float -inf`() {
    enterCommand("let s=-1.0 / 0.0")
    assertCommandOutput("echo string(s)", "-inf")
  }

  @Test
  fun `test string() outputs String wrapped in single quotes`() {
    enterCommand("let s='12'")
    assertCommandOutput("echo string(s)", "'12'")
  }

  @Test
  fun `test string() containing double quotes`() {
    enterCommand("let s='\"12\"'")
    assertCommandOutput("echo string(s)", "'\"12\"'")
  }

  @Test
  fun `test string() containing single quotes is escaped`() {
    enterCommand("let s=\"o'clock\"")
    assertCommandOutput("echo string(s)", "'o''clock'")
  }

  @Test
  fun `test string() containing multiple single quotes is escaped`() {
    enterCommand("let s=\"o'c'l'o'c'k\"")
    assertCommandOutput("echo string(s)", "'o''c''l''o''c''k'")
  }

  @Test
  fun `test string() containing multiple consecutive single quotes is escaped`() {
    enterCommand("let s=\"o''clock\"")
    assertCommandOutput("echo string(s)", "'o''''clock'")
  }

  @Test
  fun `test string() outputs Dictionary`() {
    enterCommand("let s={'a': 1, 'b': 2}")
    assertCommandOutput("echo string(s)", "{'a': 1, 'b': 2}")
  }

  @Test
  fun `test string() outputs List`() {
    enterCommand("let s=[1, 2.34, 'hello']")
    assertCommandOutput("echo string(s)", "[1, 2.34, 'hello']")
  }

  // TODO: Handle recursive entries in List and Dictionary
}
