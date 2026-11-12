/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class SystemRegisterPolicyTest {

  enum class Platform(val isPrimarySupported: Boolean) { X11(true), OTHER(false) }

  /** How the register of a write was chosen. */
  enum class Write(val isExplicit: Boolean, val isDelete: Boolean) {
    YANK(false, false),
    DELETE(false, true),
    NAMED_YANK(true, false),
    NAMED_DELETE(true, true),
  }

  @ParameterizedTest
  @CsvSource(
    delimiter = '|',
    useHeadersInDisplayName = true,
    textBlock = """clipboard              | platform | default register
                   ''                     | X11      | "
                   ''                     | OTHER    | "
                   ideaput,autoselect     | X11      | "
                   unnamed                | X11      | *
                   unnamed                | OTHER    | *
                   unnamedplus            | X11      | +
                   unnamedplus            | OTHER    | *
                   unnamed,unnamedplus    | X11      | +
                   unnamedplus,unnamed    | X11      | +
                   unnamed,unnamedplus    | OTHER    | *""",
  )
  fun `default register`(option: String, platform: Platform, expected: Char) {
    assertEquals(expected, SystemRegisterPolicy.defaultRegister(option.toList(), platform.isPrimarySupported))
  }

  // On X11, unnamedplus copies a yank to PRIMARY, and unnamed copies it to CLIPBOARD (Vim writes PRIMARY only).
  // With both, unnamedplus picks the register. A delete or a named register is never copied.
  @ParameterizedTest
  @CsvSource(
    delimiter = '|',
    useHeadersInDisplayName = true,
    textBlock = """clipboard              | platform | register | write        | selections
                   ''                     | X11      | "        | YANK         | ''
                   ''                     | X11      | a        | NAMED_YANK   | ''
                   unnamedplus            | X11      | _        | NAMED_DELETE | ''

                   ''                     | X11      | +        | NAMED_YANK   | CLIPBOARD
                   ''                     | X11      | *        | NAMED_YANK   | PRIMARY
                   ''                     | OTHER    | +        | NAMED_YANK   | CLIPBOARD
                   ''                     | OTHER    | *        | NAMED_YANK   | CLIPBOARD

                   unnamedplus            | X11      | +        | YANK         | CLIPBOARD,PRIMARY
                   unnamedplus            | X11      | +        | DELETE       | CLIPBOARD
                   unnamedplus            | X11      | +        | NAMED_YANK   | CLIPBOARD
                   unnamedplus            | X11      | *        | NAMED_YANK   | PRIMARY
                   unnamedplus            | OTHER    | *        | YANK         | CLIPBOARD
                   unnamedplus            | OTHER    | *        | DELETE       | CLIPBOARD

                   unnamed                | X11      | *        | YANK         | PRIMARY,CLIPBOARD
                   unnamed                | X11      | *        | DELETE       | PRIMARY
                   unnamed                | X11      | *        | NAMED_YANK   | PRIMARY
                   unnamed                | X11      | +        | NAMED_YANK   | CLIPBOARD
                   unnamed                | OTHER    | *        | YANK         | CLIPBOARD
                   unnamed                | OTHER    | *        | DELETE       | CLIPBOARD

                   unnamed,unnamedplus    | X11      | +        | YANK         | CLIPBOARD,PRIMARY
                   unnamed,unnamedplus    | X11      | +        | DELETE       | CLIPBOARD""",
  )
  fun `selections written`(option: String, platform: Platform, register: Char, write: Write, expected: String) {
    assertEquals(
      expected.toList().map(SystemSelection::valueOf),
      SystemRegisterPolicy.selectionsToWrite(
        register,
        write.isExplicit,
        write.isDelete,
        option.toList(),
        platform.isPrimarySupported,
      ),
    )
  }

  private fun String.toList(): List<String> = split(',').filter { it.isNotEmpty() }
}
