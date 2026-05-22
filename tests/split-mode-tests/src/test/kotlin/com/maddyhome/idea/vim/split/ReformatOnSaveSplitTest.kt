/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Regression coverage for VIM-XXXX: in split mode, `:w` saved the document directly via
 * `FileDocumentManager.saveDocument()` instead of dispatching the platform `SaveDocument`
 * action. As a result, "Settings > Tools > Actions on Save > Reformat code" never ran.
 *
 * The project here enables [Reformat on Save] via `.idea/workspace.xml`. A Java file with
 * deliberately broken whitespace is opened, a line is edited via Vim (so it counts as
 * changed for the "process changed text only" mode), and `:w` is issued. If `:w` invokes
 * the `SaveDocument` action, the reformat runs and the line gets corrected spaces.
 */
class ReformatOnSaveSplitTest : IdeaVimStarterTestBase() {

  override fun beforeContextCreated() {
    val ideaDir = projectDir.resolve(".idea")
    ideaDir.createDirectories()
    ideaDir.resolve("workspace.xml").writeText(
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <project version="4">
        <component name="FormatOnSaveOptions">
          <option name="myRunOnSave" value="true" />
          <option name="myAllFileTypesSelected" value="true" />
          <option name="myProcessChangedTextOnly" value="false" />
        </component>
      </project>
      """.trimIndent(),
    )
  }

  @Test
  fun `write command triggers reformat on save`() {
    openFile(
      createFile(
        "src/ReformatOnSave.java",
        """
        public class ReformatOnSave {
            int x=1;
        }
        """.trimIndent() + "\n",
      ),
    )

    // Edit line 2 so that even "process changed text only" reformat covers this line.
    goToLine(2)
    typeVimAndEscape("A // edit")
    pause()

    exCommand("w")
    pause(2000)

    assertEditorContains(
      "int x = 1;",
      "Reformat on Save should add spaces around '=' when :w invokes the SaveDocument action",
    )
  }
}
