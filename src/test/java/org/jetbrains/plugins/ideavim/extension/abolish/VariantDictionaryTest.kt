/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.extension.abolish.buildVariantDictionary
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VariantDictionaryTest {

  @Test
  fun `a single pair produces three case variants`() {
    val dict = buildVariantDictionary("foo", "bar")
    assertEquals(
      mapOf(
        "foo" to "bar",
        "Foo" to "Bar",
        "FOO" to "BAR",
      ),
      dict,
    )
  }

  @Test
  fun `brace alternatives become separate dictionary entries`() {
    val dict = buildVariantDictionary("box{,es}", "bag{,s}")
    assertEquals(
      mapOf(
        "box" to "bag", "Box" to "Bag", "BOX" to "BAG",
        "boxes" to "bags", "Boxes" to "Bags", "BOXES" to "BAGS",
      ),
      dict,
    )
  }

  @Test
  fun `empty braces on the rhs reuse the lhs alternatives`() {
    val dict = buildVariantDictionary("anomol{y,ies}", "anomal{}")
    assertEquals(
      mapOf(
        "anomoly" to "anomaly", "Anomoly" to "Anomaly", "ANOMOLY" to "ANOMALY",
        "anomolies" to "anomalies", "Anomolies" to "Anomalies", "ANOMOLIES" to "ANOMALIES",
      ),
      dict,
    )
  }

  @Test
  fun `mixedcase already-capitalised lhs preserves its capitalisation`() {
    val dict = buildVariantDictionary("FooBar", "BazQux")
    assertEquals(
      mapOf(
        "foobar" to "bazqux",
        "FooBar" to "BazQux",
        "FOOBAR" to "BAZQUX",
      ),
      dict,
    )
  }

  @Test
  fun `two brace groups pair lockstep at each position and multiply across positions`() {
    val dict = buildVariantDictionary("{a,b}_{x,y}", "{p,q}_{r,s}")
    // PascalCase of single-letter atoms collapses to the uppercase form without the separator.
    assertEquals(
      mapOf(
        "a_x" to "p_r", "AX" to "PR", "A_X" to "P_R",
        "a_y" to "p_s", "AY" to "PS", "A_Y" to "P_S",
        "b_x" to "q_r", "BX" to "QR", "B_X" to "Q_R",
        "b_y" to "q_s", "BY" to "QS", "B_Y" to "Q_S",
      ),
      dict,
    )
  }

  @Test
  fun `snake_case lhs also matches PascalCase variant`() {
    val dict = buildVariantDictionary("foo_bar", "baz_qux")
    assertEquals(
      mapOf(
        "foo_bar" to "baz_qux",
        "FooBar" to "BazQux",
        "FOO_BAR" to "BAZ_QUX",
      ),
      dict,
    )
  }

  @Test
  fun `rhs alternatives cycle when the lhs has more of them`() {
    val dict = buildVariantDictionary("{red,green,blue,yellow}", "{warm,cool}")
    assertEquals(
      mapOf(
        "red" to "warm", "Red" to "Warm", "RED" to "WARM",
        "green" to "cool", "Green" to "Cool", "GREEN" to "COOL",
        "blue" to "warm", "Blue" to "Warm", "BLUE" to "WARM",
        "yellow" to "cool", "Yellow" to "Cool", "YELLOW" to "COOL",
      ),
      dict,
    )
  }
}
