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
 * Runs the test as on a windowing system with a PRIMARY selection (X11, XWayland), whatever the machine has.
 * PRIMARY is kept in memory, see `VimTestCase.primarySelection`.
 *
 * A test without this annotation runs as on a system without PRIMARY, see [TestWithoutPrimaryClipboard].
 */
@Test
annotation class TestWithPrimaryClipboard
