/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimTagServiceBase

/**
 * Tag stack service, see "h tag-stack"
 *
 * Deliberately not a PersistentStateComponent: Vim does not save the tag stack in viminfo, so neither do we. All of the
 * behaviour lives in [VimTagServiceBase].
 */
internal class VimTagServiceImpl : VimTagServiceBase()
