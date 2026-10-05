/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.jetbrains.rd.ui.actions

/**
 * Test stand-in for the interface of the same name from the remote development client (Rider, JetBrains Client),
 * which is not available on the test classpath. IdeaVim detects backend delegating actions by this interface name.
 */
interface PossiblyDelegatingToBackendAction {
  fun beforeDelegatingToBackend()
}
