/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

/**
 * Runs test only if the primary clipboard is not supported by the OS.
 * This is important for proper testing of both unnamed and unnamedplus registers.
 */
@Test
@DisabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*", disabledReason = "X11 DISPLAY variable is present")
annotation class TestWithoutPrimaryClipboard