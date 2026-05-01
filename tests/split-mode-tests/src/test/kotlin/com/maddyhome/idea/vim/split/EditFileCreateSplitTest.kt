/*
 * Copyright 2003-2026 The IdeaVim authors
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

class EditFileCreateSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `test edit non-existent file creates it and opens it in the editor`() {
    openFile("src/placeholder.txt")
    pause(2_000)

    exCommand("e src/brand_new_file.txt")
    pause(3_000)

    typeVimAndEscape("ihello from IdeaVim")
    pause(1_000)

    assertEditorContains("hello from IdeaVim")

    val file = projectDir.resolve("src/brand_new_file.txt")
    assert(file.toFile().exists()) {
      "File should exist at: ${file.toAbsolutePath()}. projectDir=$projectDir"
    }
  }
}