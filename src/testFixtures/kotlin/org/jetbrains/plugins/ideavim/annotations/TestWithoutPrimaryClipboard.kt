/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.annotations

import org.junit.jupiter.api.Test

/**
 * Runs the test as on a windowing system without a PRIMARY selection (Windows, macOS), whatever the machine has.
 * The `*` register is backed by the clipboard then.
 *
 * This is how every test without [TestWithPrimaryClipboard] runs as well; the annotation says that the test is about it.
 */
@Test
annotation class TestWithoutPrimaryClipboard
