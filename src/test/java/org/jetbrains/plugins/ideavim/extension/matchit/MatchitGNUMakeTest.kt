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

class MatchitGNUMakeTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  fun `test jump from define to endef`() {
    doTest(
      "%",
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endef to define`() {
    doTest(
      "%",
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifdef to endif`() {
    doTest(
      "%",
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifdef`() {
    doTest(
      "%",
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifndef to endif`() {
    doTest(
      "%",
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifndef`() {
    doTest(
      "%",
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifeq to endif`() {
    doTest(
      "%",
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifeq`() {
    doTest(
      "%",
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifneq to endif`() {
    doTest(
      "%",
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifneq`() {
    doTest(
      "%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifneq to else`() {
    doTest(
      "%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifeq to else in ifeq-else block`() {
    doTest(
      "%",
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from else ifeq to else`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifeq in else block to else`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ${c}ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from else to endif in ifeq-else block`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifeq in ifeq-else block`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifneq to else in ifneq-else block`() {
    doTest(
      "%",
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from else ifneq to else`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from ifneq in else block to else`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ${c}ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from else to endif in ifneq-else block`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test jump from endif to ifneq in ifneq-else block`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  // Reverse tests

  @Test
  fun `test reverse jump from define to endef`() {
    doTest(
      "g%",
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endef to define`() {
    doTest(
      "g%",
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifdef to endif`() {
    doTest(
      "g%",
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to ifdef`() {
    doTest(
      "g%",
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifndef to endif`() {
    doTest(
      "g%",
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to ifndef`() {
    doTest(
      "g%",
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifeq to endif`() {
    doTest(
      "g%",
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to ifeq`() {
    doTest(
      "g%",
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifneq to endif`() {
    doTest(
      "g%",
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to ifneq`() {
    doTest(
      "g%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifneq to else`() {
    doTest(
      "g%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}else
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        else
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifeq to endif in ifeq-else block`() {
    doTest(
      "g%",
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from else ifeq to ifeq`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifeq in else block to ifeq`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ${c}ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from else to else in ifeq-else block`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to else in ifeq-else block`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifneq to endif in ifneq-else block`() {
    doTest(
      "g%",
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from else ifneq to ifneq`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from ifneq in else block to ifneq`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ${c}ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from else to else in ifneq-else block`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }

  @Test
  fun `test reverse jump from endif to else in ifneq-else block`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      fileName = "Makefile",
    )
  }
}
