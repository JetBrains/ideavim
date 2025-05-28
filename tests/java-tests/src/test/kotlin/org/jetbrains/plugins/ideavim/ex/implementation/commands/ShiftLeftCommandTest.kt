/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class ShiftLeftCommandTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replce term codes")
  @Test
  fun `test simple left shift`() {
    val before = """        Lorem ipsum dolor sit amet,
                      |        ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<"))

    val after = """        Lorem ipsum dolor sit amet,
                      |    ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replce term codes")
  @Test
  fun `test double left shift`() {
    val before = """        Lorem ipsum dolor sit amet,
                      |        ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<<"))

    val after = """        Lorem ipsum dolor sit amet,
                      |${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replce term codes")
  @Test
  fun `test four times left shift`() {
    val before = """        Lorem ipsum dolor sit amet,
                      |                    ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<<<<"))

    val after = """        Lorem ipsum dolor sit amet,
                      |    ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replce term codes")
  @Test
  fun `test left shift no space`() {
    val before = """Lorem ipsum dolor sit amet,
                      |${c}consectetur adipiscing elit
                      |Sed in orci mauris.
                      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<"))

    val after = """Lorem ipsum dolor sit amet,
                      |${c}consectetur adipiscing elit
                      |Sed in orci mauris.
                      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "bad replce term codes")
  @Test
  fun `test range left shift`() {
    val before = """        Lorem ipsum dolor sit amet,
                      |        ${c}consectetur adipiscing elit
                      |        Sed in orci mauris.
                      |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("3,4<"))

    val after = """        Lorem ipsum dolor sit amet,
                      |        consectetur adipiscing elit
                      |    Sed in orci mauris.
                      |    ${c}Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test left shift with range and count`() {
    doTest(
      exCommand("3,4< 3"),
      """
        |        Lorem ipsum dolor sit amet,
        |        ${c}consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
        |        Lorem ipsum dolor sit amet,
        |        consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |        Lorem ipsum dolor sit amet,
        |        consectetur adipiscing elit
        |        Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
        |    Lorem ipsum dolor sit amet,
        |    ${c}consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test left shift with count`() {
    doTest(
      exCommand("< 3"),
      """
        |        Lorem ipsum dolor sit amet,
        |        ${c}consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |        Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    Sed in orci mauris.
        |    ${c}Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test left shift with invalid count`() {
    doTest(
      exCommand("< 3,4"),
      """
        |        Lorem ipsum dolor sit amet,
        |        ${c}consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |        Lorem ipsum dolor sit amet,
        |        ${c}consectetur adipiscing elit
        |        Sed in orci mauris.
        |        Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,4")
  }

  @Test
  fun `test multiple carets`() {
    val before = """    I found it in a legendary land
                      |${c}all rocks and lavender and tufted grass,
                      |    ${c}where it was settled on some sodden sand
                      |    hard by the$c torrent of a mountain pass.
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<"))

    val after = """    I found it in a legendary land
                      |${c}all rocks and lavender and tufted grass,
                      |${c}where it was settled on some sodden sand
                      |${c}hard by the torrent of a mountain pass.
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test shift left and undo`() {
    configureByJavaText(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        System.out.println("Hello");
      |        ${c}System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )

    enterCommand("<")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        System.out.println("Hello");
      |    ${c}System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        System.out.println("Hello");
      |        ${c}System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )
  }

  @Test
  fun `test shift left with range and undo`() {
    configureByJavaText(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        ${c}System.out.println("Hello");
      |        System.out.println("World");
      |        System.out.println("!");
      |    }
      |}
      """.trimMargin()
    )

    enterCommand("3,5<")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |    System.out.println("Hello");
      |    System.out.println("World");
      |    ${c}System.out.println("!");
      |    }
      |}
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        ${c}System.out.println("Hello");
      |        System.out.println("World");
      |        System.out.println("!");
      |    }
      |}
      """.trimMargin()
    )
  }

  @Test
  fun `test shift left with count and undo`() {
    configureByJavaText(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        ${c}System.out.println("Hello");
      |        System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )

    enterCommand("< 2")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |    System.out.println("Hello");
      |    ${c}System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |public class Example {
      |    public static void main(String[] args) {
      |        ${c}System.out.println("Hello");
      |        System.out.println("World");
      |    }
      |}
      """.trimMargin()
    )
  }
}
