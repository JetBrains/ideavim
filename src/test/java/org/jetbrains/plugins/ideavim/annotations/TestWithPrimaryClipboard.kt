/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Runs test only if the primary clipboard is supported by the OS.
 * This is important for proper testing of both unnamed and unnamedplus registers.
 */
@Test
@EnabledOnOs(OS.LINUX)
@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*", disabledReason = "X11 DISPLAY variable is not present")
annotation class TestWithPrimaryClipboard
