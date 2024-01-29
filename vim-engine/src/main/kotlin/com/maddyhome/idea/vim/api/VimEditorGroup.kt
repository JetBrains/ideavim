/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimEditorGroup {
  public fun notifyIdeaJoin(editor: VimEditor)

  /**
   * Get a collection of all editors
   *
   * This will return all editors, including both main file editors and other editors in the UI such as the commit
   * message editor. Implementors should take care to only return "local" editors. I.e. for IntelliJ, this function will
   * not include hidden editors that are used to handle requests from Code With Me guests.
   *
   * Note that this function returns editors for all projects, which is necessary because IdeaVim does not maintain
   * per-project state. This can have surprising consequences, such as search highlights updating across all open
   * top-level project frames, which is at odds with the behaviour of separate top-level windows in GVim/MacVim.
   *
   * Also note that it is possible for multiple editors in different projects to open the same file (document/buffer).
   */
  public fun localEditors(): Collection<VimEditor>

  /**
   * Get a collection of all editors for the given buffer
   * editor
   * This will return all editors that are currently displaying the given buffer. These are most likely to be main file
   * editors, but given the right buffer could return editors from the UI. Note that a document's file might be open in
   * multiple projects at the same time, and this function will return all such editors across all projects.
   *
   * Implementors should take care to only return "local" editors. I.e. for IntelliJ, this function will not include
   * hidden editors that are used to handle requests from Code With Me guests.
   */
  public fun localEditors(buffer: VimDocument): Collection<VimEditor>
}
