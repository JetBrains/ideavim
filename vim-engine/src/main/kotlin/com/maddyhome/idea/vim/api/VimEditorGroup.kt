/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimEditorGroup {
  fun notifyIdeaJoin(editor: VimEditor)

  /** Called when a new editor is created and needs to be initialised by IdeaVim */
  fun editorCreated(editor: VimEditor) {}

  /** Called when an editor is being deinitialised by IdeaVim */
  fun editorDeinit(editor: VimEditor) {}

  /** Returns the current key repeat state, or null if not set (Mac-specific) */
  fun isKeyRepeat(): Boolean? = null

  /** Sets the key repeat state (Mac-specific) */
  fun setKeyRepeat(value: Boolean?) {}

  /** Closes any active editor search session for the given editor */
  fun closeEditorSearchSession(editor: VimEditor) {}

  /** Called when the 'number' or 'relativenumber' options change for the given editor */
  fun onNumberOptionChanged(editor: VimEditor) {}

  /** Loads editor state from a legacy state element */
  fun loadEditorStateData(element: Any) {}

  /**
   * Get a collection of all editors, including those that have not yet been initialised.
   *
   * You probably don't want to use this - use [getEditors]!
   *
   * This function is only useful during plugin initialisation. It will return all currently open editors, including
   * those that have not yet been initialised, so the plugin can correctly initialise them. When the plugin starts, the
   * IDE might already have editors open, either because the plugin is initialised after the IDE starts, or because the
   * IDE has reopened editors at startup.
   */
  fun getEditorsRaw(): Collection<VimEditor>

  /**
   * Get a collection of all initialised editors
   *
   * This will return all editors, including both main file editors and other editors in the UI such as the commit
   * message editor. Editors will have been initialised by IdeaVim - any listeners installed, all options set, etc. This
   * is important to prevent using editors in listener callbacks (such as option change notifications) before they've
   * been initialised.
   *
   * Implementors should take care to only return "local" editors. I.e., for IntelliJ, this function will not include
   * hidden editors that are used to handle requests from Code With Me guests.
   *
   * Note that this function returns editors for all projects, which is necessary because IdeaVim does not maintain
   * state per-project. This can have surprising consequences, such as search highlights updating across all open
   * top-level project frames, which is at odds with the behaviour of separate top-level windows in GVim/MacVim.
   *
   * Also note that it is possible for multiple editors in different projects to open the same file (document/buffer).
   */
  fun getEditors(): Collection<VimEditor>

  /**
   * Get a collection of all initialised editors for the given buffer
   *
   * This will return all editors that are currently displaying the given buffer. These are most likely to be main file
   * editors, but given the right buffer could return editors from the UI. Note that a document's file might be open in
   * multiple projects at the same time, and this function will return all such editors across all projects. Editors
   * will have been initialised by IdeaVim - any listeners installed, all options set, etc. This is important to prevent
   * using editors in listener callbacks (such as option change notifications) before they've been initialised.
   *
   * Implementors should take care to only return "local" editors. I.e. for IntelliJ, this function will not include
   * hidden editors that are used to handle requests from Code With Me guests.
   */
  fun getEditors(buffer: VimDocument): Collection<VimEditor>

  // TODO find a better place for methods below. Maybe make CaretVisualAttributesHelper abstract?
  fun updateCaretsVisualAttributes(editor: VimEditor)
  fun updateCaretsVisualPosition(editor: VimEditor)

  fun getFocusedEditor(): VimEditor?

  /**
   * Get the currently selected editor from the internal model.
   *
   * This uses FileEditorManager's selected editor, which updates when window-switching
   * commands like :wincmd execute, even before focus changes.
   *
   * @param projectId The project identifier to get the editor from.
   * @return The selected editor, or null if no editor is selected. Null can also be returned during the
   *   project initialization. If the null is returned, fallback to `injector.fallbackWindow`
   */
  fun getSelectedEditor(projectId: String): VimEditor?
}
