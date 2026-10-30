/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.mark

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffTool
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.editor.DiffEditorViewerFileEditor
import com.intellij.diff.editor.DiffViewerVirtualFile
import com.intellij.diff.editor.SimpleDiffVirtualFile
import com.intellij.diff.impl.CacheDiffRequestChainProcessor
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.diff.impl.DiffSettingsHolder.DiffSettings
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffTool
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleDiffTool
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Side
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeViewDiffRequestProcessor
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.VimJumpServiceImpl
import com.maddyhome.idea.vim.mark.JumpSource
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

/**
 * VIM-4351: jumps recorded in a diff editor bring back the diff and the side they were recorded in
 *
 * Every test records a jump in a diff the way `gd` and Ctrl+Click do (`saveJumpLocation` before navigating), moves to
 * another file and comes back with `<C-O>`.
 */
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class DiffEditorJumpTest : VimSplitWindowTestCase() {
  // Lines of the left side: "first line", "third line", "fourth line"
  private val before = "first line\nthird line\nfourth line\n"

  // Lines of the right side: "first line", "second line", "third line", "fourth line"
  private val after = "first line\nsecond line\nthird line\nfourth line\n"

  private lateinit var definition: Editor
  private lateinit var changedFile: VirtualFile

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    ApplicationManager.getApplication().invokeAndWait {
      changedFile = fixture.createFile("Changed.txt", after)
    }
    definition = openNewBufferWindow("Definition.txt", "fun definition() {}\n")
    // IDE navigation is mirrored into the jump list asynchronously, which would add entries these tests never made
    enterCommand("set noideaunifyjumps")
    useDiffTool(SimpleDiffTool.INSTANCE)
  }

  @Test
  fun `test control-O returns to the right side of the diff`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.RIGHT), line = 2, column = 3)

    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test control-O returns to the left side of the diff`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 1, column = 2)

    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.LEFT, line = 1, column = 2)
  }

  @Test
  fun `test control-O returns to the right side of the diff after the IDE navigated away`() {
    enterCommand("set ideaunifyjumps")
    val diff = openDiff(before)

    navigateLikeTheIde(diff, Side.RIGHT, line = 2, column = 3, target = definition)
    waitForJumpRecordedInDiff()
    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test control-O returns to the left side of the diff after the IDE navigated away`() {
    enterCommand("set ideaunifyjumps")
    val diff = openDiff(before)

    navigateLikeTheIde(diff, Side.LEFT, line = 1, column = 2, target = definition)
    waitForJumpRecordedInDiff()
    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.LEFT, line = 1, column = 2)
  }

  @Test
  fun `test the IDE navigating away from unified view records the side of the line`() {
    enterCommand("set ideaunifyjumps")
    useDiffTool(UnifiedDiffTool.INSTANCE)
    val diff = openDiff(before)
    val editor = unifiedEditor(diff)

    navigateLikeTheIde(diff, editor, line = unifiedLine(diff, Side.RIGHT, 1), column = 4, target = definition)
    waitForJumpRecordedInDiff()
    jumpBackFrom(definition)

    assertCaretInUnifiedDiff(diff, Side.RIGHT, line = 1, column = 4)
  }

  @Test
  fun `test control-O from the right side of a closed diff opens the file`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.RIGHT), line = 2, column = 3)
    closeDiff(diff)

    jumpBackFrom(definition)

    assertCaretInFile(changedFile, line = 2, column = 3)
  }

  @Test
  fun `test control-O skips the left side of a closed diff`() {
    val older = openNewBufferWindow("Older.txt", "one\ntwo\nthree\n")
    recordJump(older, line = 1, column = 1)
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 1, column = 2)
    closeDiff(diff)

    jumpBackFrom(definition)

    assertCaretInFile(older.virtualFile!!, line = 1, column = 1)
  }

  @Test
  fun `test the left side of a closed diff is dropped from the jump list`() {
    val older = openNewBufferWindow("Older.txt", "one\ntwo\nthree\n")
    recordJump(older, line = 1, column = 1)
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 1, column = 2)
    closeDiff(diff)

    jumpBackFrom(definition)
    selectWindow(definition)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |>  0     2    1 ${older.virtualFile!!.path}
        |   1     1    0 fun definition() {}
      """.trimMargin(),
    )
  }

  @Test
  fun `test left side goes back to the diff it was recorded in, not to another diff of the same file`() {
    val original = openDiff(before)
    recordJump(sideEditor(original, Side.LEFT), line = 1, column = 2)
    val other = openDiff("first line\nfourth line\n")
    sideEditor(other, Side.LEFT)

    jumpBackFrom(definition)

    assertCaretInDiff(original, Side.LEFT, line = 1, column = 2)
  }

  @Test
  fun `test right side of a closed diff goes to another diff of the same file`() {
    val original = openDiff(before)
    recordJump(sideEditor(original, Side.RIGHT), line = 2, column = 3)
    val other = openDiff("first line\nfourth line\n")
    sideEditor(other, Side.RIGHT)
    closeDiff(original)

    jumpBackFrom(definition)

    assertCaretInDiff(other, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test left side of a closed diff is not restored in another diff of the same file`() {
    val older = openNewBufferWindow("Older.txt", "one\ntwo\nthree\n")
    recordJump(older, line = 1, column = 1)
    val original = openDiff(before)
    recordJump(sideEditor(original, Side.LEFT), line = 1, column = 2)
    val other = openDiff("first line\nfourth line\n")
    sideEditor(other, Side.LEFT)
    closeDiff(original)

    jumpBackFrom(definition)

    assertCaretInFile(older.virtualFile!!, line = 1, column = 1)
  }

  @Test
  fun `test control-O switches a diff chain back to the file of the jump`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val chain = openChainDiff(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(chain, Side.RIGHT), line = 2, column = 3)
    showChainEntry(chain, 1)

    jumpBackFrom(definition)

    assertCaretInDiff(chain, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test control-O switches a chain of version control changes back by the path of the change`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val chain = openChangesChain(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(chain, Side.RIGHT), line = 2, column = 3)
    showChainEntry(chain, 1)

    jumpBackFrom(definition)

    assertCaretInDiff(chain, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test control-O switches a diff chain back for the left side`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val chain = openChainDiff(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(chain, Side.LEFT), line = 1, column = 2)
    showChainEntry(chain, 1)

    jumpBackFrom(definition)

    assertCaretInDiff(chain, Side.LEFT, line = 1, column = 2)
  }

  @Test
  fun `test control-O switches the diff preview of the changes view back to the file of the jump`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val preview = openChangesPreview(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(preview, Side.RIGHT), line = 2, column = 3)
    selectInChangesView(preview, otherFile)

    jumpBackFrom(definition)

    assertCaretInDiff(preview, Side.RIGHT, line = 2, column = 3)
  }

  @Test
  fun `test control-O selects the file of the jump in the tree of changes when the platform can show a change`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val preview = openChangesPreview(changedFile to before, otherFile to "other\n", canShowChange = true)
    recordJump(sideEditor(preview, Side.RIGHT), line = 2, column = 3)
    selectInChangesView(preview, otherFile)

    jumpBackFrom(definition)

    assertCaretInDiff(preview, Side.RIGHT, line = 2, column = 3)
    assertEquals(changedFile.path, selectedInChangesView(preview))
  }

  @Test
  fun `test control-O leaves the selection in the tree of changes when the platform cannot show a change`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val preview = openChangesPreview(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(preview, Side.RIGHT), line = 2, column = 3)
    selectInChangesView(preview, otherFile)

    jumpBackFrom(definition)

    assertCaretInDiff(preview, Side.RIGHT, line = 2, column = 3)
    assertEquals(otherFile.path, selectedInChangesView(preview))
  }

  @Test
  fun `test control-O switches the diff preview of the changes view back for the left side`() {
    val otherFile = createFile("Other.txt", "other\nlines\n")
    val preview = openChangesPreview(changedFile to before, otherFile to "other\n")
    recordJump(sideEditor(preview, Side.LEFT), line = 1, column = 2)
    selectInChangesView(preview, otherFile)

    jumpBackFrom(definition)

    assertCaretInDiff(preview, Side.LEFT, line = 1, column = 2)
  }

  @Test
  fun `test right side recorded in side-by-side view comes back in unified view`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.RIGHT), line = 1, column = 4)
    switchDiffTool(diff, UnifiedDiffTool.INSTANCE)

    jumpBackFrom(definition)

    assertCaretInUnifiedDiff(diff, Side.RIGHT, line = 1, column = 4)
  }

  @Test
  fun `test left side recorded in side-by-side view comes back in unified view`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 2, column = 4)
    switchDiffTool(diff, UnifiedDiffTool.INSTANCE)

    jumpBackFrom(definition)

    assertCaretInUnifiedDiff(diff, Side.LEFT, line = 2, column = 4)
  }

  @Test
  fun `test right side recorded in unified view comes back in side-by-side view`() {
    useDiffTool(UnifiedDiffTool.INSTANCE)
    val diff = openDiff(before)
    recordJump(unifiedEditor(diff), line = unifiedLine(diff, Side.RIGHT, 1), column = 4)
    switchDiffTool(diff, SimpleDiffTool.INSTANCE)

    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.RIGHT, line = 1, column = 4)
  }

  @Test
  fun `test left side recorded in unified view comes back in side-by-side view`() {
    useDiffTool(UnifiedDiffTool.INSTANCE)
    // Unified view shows an unchanged line once, as a line of both sides, so the jump needs a line only the left has
    val diff = openDiff("first line\nremoved line\nthird line\nfourth line\n")
    recordJump(unifiedEditor(diff), line = unifiedLine(diff, Side.LEFT, 1), column = 4)
    switchDiffTool(diff, SimpleDiffTool.INSTANCE)

    jumpBackFrom(definition)

    assertCaretInDiff(diff, Side.LEFT, line = 1, column = 4)
  }

  @Test
  fun `test unchanged line recorded in unified view is a jump to the right side`() {
    useDiffTool(UnifiedDiffTool.INSTANCE)
    val diff = openDiff(before)
    recordJump(unifiedEditor(diff), line = unifiedLine(diff, Side.RIGHT, 2), column = 1)
    closeDiff(diff)

    jumpBackFrom(definition)

    assertCaretInFile(changedFile, line = 2, column = 1)
  }

  @Test
  fun `test control-O inside the same diff side moves the caret`() {
    val diff = openDiff(before)
    val right = sideEditor(diff, Side.RIGHT)
    recordJump(right, line = 0, column = 2)
    moveCaret(right, line = 3, column = 0)

    jumpBackFrom(right)

    assertCaretInDiff(diff, Side.RIGHT, line = 0, column = 2)
  }

  @Test
  fun `test same line in the diff and in a plain editor are two jumps`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.RIGHT), line = 2, column = 0)
    ApplicationManager.getApplication().invokeAndWait { fixture.openFileInEditor(changedFile) }
    recordJump(fixture.editor, line = 2, column = 0)

    selectWindow(definition)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     3    0 ${changedFile.path} (diff, right)
        |   1     3    0 ${changedFile.path}
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test jumps shows which side of a diff a jump is on`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 1, column = 2)
    recordJump(sideEditor(diff, Side.RIGHT), line = 2, column = 3)

    selectWindow(definition)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     2    2 ${changedFile.path} (diff, left)
        |   1     3    3 ${changedFile.path} (diff, right)
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test only the right side of a diff is saved, as a plain file jump`() {
    val diff = openDiff(before)
    recordJump(sideEditor(diff, Side.LEFT), line = 1, column = 2)
    recordJump(sideEditor(diff, Side.RIGHT), line = 2, column = 3)

    val jumps = (injector.jumpService as VimJumpServiceImpl).state
      .getChildren("project")
      .flatMap { it.getChildren("jump") }

    assertEquals(1, jumps.size, "The left side has no file to come back to after a restart")
    assertEquals("2", jumps.single().getAttributeValue("line"))
    assertEquals("3", jumps.single().getAttributeValue("column"))
    assertEquals(changedFile.path, jumps.single().getAttributeValue("filename"))
  }

  // ======================== Recording and jumping ========================

  /** Records a jump at the given position, the way `gd` does before navigating (IdeaSpecifics.saveJumpBeforeGoto) */
  private fun recordJump(editor: Editor, line: Int, column: Int) {
    moveCaret(editor, line, column)
    ApplicationManager.getApplication().invokeAndWait {
      injector.jumpService.saveJumpLocation(editor.vim)
    }
  }

  private fun moveCaret(editor: Editor, line: Int, column: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      editor.caretModel.moveToLogicalPosition(LogicalPosition(line, column))
    }
  }

  private fun jumpBackFrom(editor: Editor) {
    val keys = injector.parser.parseKeys("<C-O>")
    if (editor === definition) {
      selectWindow(editor)
      typeText(keys)
    } else {
      // The data context of an editor inside a diff has no editor in tests, so the IDE would not deliver the shortcut to
      // IdeaVim. Hand the keys to the key handler directly
      VimTestCase.typeText(keys, editor, fixture.project)
    }
  }

  /**
   * Navigates to [target] the way the IDE's Go to Declaration does (Cmd+B, Ctrl+Click): IdeaVim doesn't see it, the IDE
   * records it in its navigation history, and `ideaunifyjumps` brings it into the jump list
   */
  private fun navigateLikeTheIde(diffFile: VirtualFile, side: Side, line: Int, column: Int, target: Editor) {
    val editor = sideEditor(diffFile, side)
    ApplicationManager.getApplication().invokeAndWait {
      // The side the user works in, which the viewer tracks by the focus
      (processor(diffFile).activeViewer as TwosideTextDiffViewer).currentSide = side
    }
    navigateLikeTheIde(diffFile, editor, line, column, target)
  }

  private fun navigateLikeTheIde(diffFile: VirtualFile, editor: Editor, line: Int, column: Int, target: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.openFile(diffFile, true)
      editor.caretModel.moveToLogicalPosition(LogicalPosition(line, column))
    }
    waitForIdeNavigationToBeMirrored()
    ApplicationManager.getApplication().invokeAndWait {
      CommandProcessor.getInstance().executeCommand(fixture.project, {
        IdeDocumentHistory.getInstance(fixture.project).includeCurrentCommandAsNavigation()
        fileEditorManager.openFile(target.virtualFile!!, true)
      }, "Go to Declaration", null)
    }
  }

  /**
   * Waits until a jump just made no longer hides the IDE's navigation from the jump list
   *
   * Opening the diff is a navigation the IDE mirrors into the jump list too. For a short while after any jump, the jump
   * list ignores the IDE's navigation, which would otherwise mirror IdeaVim's own jumps a second time.
   */
  private fun waitForIdeNavigationToBeMirrored() {
    waitUntil { System.currentTimeMillis() > injector.jumpService.lastJumpTimeStamp }
  }

  private fun waitForJumpRecordedInDiff() {
    val recorded = waitUntil {
      var found = false
      ApplicationManager.getApplication().invokeAndWait {
        found = injector.jumpService.getJumps(definition.vim.jumpListId).any { it.source is JumpSource.Diff }
      }
      found
    }
    check(recorded) { "The IDE navigation was not recorded as a jump in the diff" }
  }

  // ======================== Opening diffs ========================

  private fun openDiff(leftText: String): VirtualFile {
    val request = SimpleDiffRequest(
      "Review",
      olderRevision(leftText, changedFile),
      DiffContentFactory.getInstance().create(fixture.project, changedFile),
      "Before",
      "After",
    )
    return open(SimpleDiffVirtualFile(request))
  }

  /**
   * The left side of a diff from version control: the text of an older revision. Its document has a file of its own,
   * which is not the file on disk, so the side highlights the code like the file does
   */
  private fun olderRevision(text: String, file: VirtualFile): DiffContent =
    DiffContentFactory.getInstance().createFromBytes(fixture.project, text.toByteArray(), file)

  /** A diff of several files in one tab, as the Commit tool window opens it */
  private fun openChainDiff(vararg changes: Pair<VirtualFile, String>): VirtualFile {
    val producers = changes.map { (file, leftText) -> ChangeProducer(file, leftText) }
    return open(ChainDiffVirtualFile(SimpleDiffRequestChain.fromProducers(producers), "Review"))
  }

  /**
   * A diff preview that follows the selection of a tree of changes, as the Commit tool window, VCS Log and Shelf show
   * one. Selecting another file in the tree shows it in the same tab
   */
  private fun openChangesPreview(
    vararg changes: Pair<VirtualFile, String>,
    canShowChange: Boolean = false,
  ): VirtualFile {
    val processorChanges = changes.map { (file, leftText) ->
      val path = VcsUtil.getFilePath(file)
      Change(SimpleContentRevision(leftText, path, "1"), CurrentContentRevision(path))
    }
    return open(object : DiffViewerVirtualFile("Preview") {
      override fun createViewer(project: Project) =
        if (canShowChange) ShowChangePreviewProcessor(processorChanges) else ChangesPreviewProcessor(processorChanges)
    })
  }

  private fun selectedInChangesView(preview: VirtualFile): String {
    var path = ""
    ApplicationManager.getApplication().invokeAndWait { path = (processor(preview) as ChangesPreviewProcessor).selectedPath }
    return path
  }

  /** Selects [file] in the tree of changes that [preview] follows */
  private fun selectInChangesView(preview: VirtualFile, file: VirtualFile) {
    ApplicationManager.getApplication().invokeAndWait {
      val processor = processor(preview) as ChangesPreviewProcessor
      processor.selectedPath = file.path
      processor.refresh(false)
    }
    waitForViewer(preview) { viewer ->
      viewer is TwosideTextDiffViewer && viewer.getEditor(Side.RIGHT).document.text != after
    }
  }

  /** Stands in for the tree of changes: one change is selected, and the preview shows it */
  private open inner class ChangesPreviewProcessor(private val changes: List<Change>) :
    ChangeViewDiffRequestProcessor(fixture.project, "IdeaVim") {
    var selectedPath: String = ChangesUtil.getFilePath(changes.first()).path

    init {
      refresh(false)
    }

    override fun iterateSelectedChanges(): Iterable<Wrapper> =
      changes.filter { ChangesUtil.getFilePath(it).path == selectedPath }.map { ChangeWrapper(it) }

    override fun iterateAllChanges(): Iterable<Wrapper> = changes.map { ChangeWrapper(it) }

    override fun selectChange(change: Wrapper) {
      selectedPath = change.filePath.path
    }
  }

  /**
   * A preview on a platform that can show a change from outside, 2026.3 and later (IJPL-248190). Like the platform, it
   * also selects the change in the tree
   *
   * Once the platform has the method, this becomes an `override` and the test no longer compiles without it.
   */
  private inner class ShowChangePreviewProcessor(changes: List<Change>) : ChangesPreviewProcessor(changes) {
    @Suppress("unused") // Called by reflection
    fun showChange(path: FilePath): Boolean {
      val change = iterateAllChanges().find { it.filePath == path } ?: return false
      setCurrentChange(change)
      selectChange(change)
      return true
    }
  }

  /**
   * A diff of several changes in version control in one tab. Their diffs are named after the file alone, so only the
   * changes know the path
   */
  private fun openChangesChain(vararg changes: Pair<VirtualFile, String>): VirtualFile {
    val producers = changes.map { (file, leftText) -> VersionControlChangeProducer(file, leftText) }
    return open(ChainDiffVirtualFile(ChangeDiffRequestChain(producers, 0), "Review"))
  }

  private inner class VersionControlChangeProducer(private val file: VirtualFile, private val leftText: String) :
    ChangeDiffRequestChain.Producer {
    override fun getName(): String = file.name

    override fun getFilePath(): FilePath = VcsUtil.getFilePath(file)

    override fun getFileStatus(): FileStatus = FileStatus.MODIFIED

    override fun process(context: UserDataHolder, indicator: ProgressIndicator): DiffRequest =
      ChangeProducer(file, leftText).process(context, indicator)
  }

  /**
   * Loads the diff of one file of a chain
   *
   * A diff that only its name ties to the file: the name is the path of the file, as `ChangeDiffRequestProducer` names
   * its diffs. Such a chain is matched by the names of its diffs.
   */
  private inner class ChangeProducer(private val file: VirtualFile, private val leftText: String) : DiffRequestProducer {
    override fun getName(): String = file.path

    override fun process(context: UserDataHolder, indicator: ProgressIndicator): DiffRequest =
      SimpleDiffRequest(
        file.name,
        olderRevision(leftText, file),
        DiffContentFactory.getInstance().create(fixture.project, file),
        "Before",
        "After",
      )
  }

  private fun open(diffFile: VirtualFile): VirtualFile {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.openFile(diffFile, true)
    }
    return diffFile
  }

  private fun closeDiff(diffFile: VirtualFile) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.closeFile(diffFile)
    }
  }

  private fun createFile(name: String, text: String): VirtualFile {
    var file: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait { file = fixture.createFile(name, text) }
    return file!!
  }

  /** Shows another file of the chain in the same tab, as the "next file" button does */
  private fun showChainEntry(diffFile: VirtualFile, index: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      (processor(diffFile) as CacheDiffRequestChainProcessor).setCurrentRequest(index)
    }
    waitForViewer(diffFile) { viewer ->
      viewer is TwosideTextDiffViewer && viewer.getEditor(Side.RIGHT).document.text != after
    }
  }

  /** Diff tool used for diffs opened from now on */
  private fun useDiffTool(tool: DiffTool) {
    ApplicationManager.getApplication().invokeAndWait {
      DiffSettings.getSettings().diffToolsOrder = listOf(tool.javaClass.canonicalName)
    }
  }

  /** Switches an open diff between side-by-side and unified view, as the toolbar combo box does */
  private fun switchDiffTool(diffFile: VirtualFile, tool: DiffTool) {
    ApplicationManager.getApplication().invokeAndWait {
      val processor = processor(diffFile)
      DiffRequestProcessor::class.java.getDeclaredMethod("switchToDiffTool", DiffTool::class.java)
        .apply { isAccessible = true }
        .invoke(processor, tool)
    }
    when (tool) {
      is UnifiedDiffTool -> unifiedViewer(diffFile)
      else -> waitForViewer(diffFile) { it is TwosideTextDiffViewer }
    }
  }

  // ======================== Diff viewers ========================

  private fun processors(diffFile: VirtualFile): List<DiffRequestProcessor> =
    fileEditorManager.getEditors(diffFile)
      .filterIsInstance<DiffEditorViewerFileEditor>()
      .map { it.editorViewer }
      .filterIsInstance<DiffRequestProcessor>()

  private fun processor(diffFile: VirtualFile): DiffRequestProcessor = processors(diffFile).single()

  private fun waitForViewer(diffFile: VirtualFile, ready: (FrameDiffTool.DiffViewer) -> Boolean): FrameDiffTool.DiffViewer {
    var viewer: FrameDiffTool.DiffViewer? = null
    val appeared = waitUntil {
      ApplicationManager.getApplication().invokeAndWait {
        viewer = processors(diffFile)
          .firstNotNullOfOrNull { it.activeViewer }
          ?.takeIf(ready)
      }
      viewer != null
    }
    check(appeared) { "The diff viewer for $diffFile is not ready" }
    return viewer!!
  }

  private fun sideEditor(diffFile: VirtualFile, side: Side): Editor =
    (waitForViewer(diffFile) { it is TwosideTextDiffViewer } as TwosideTextDiffViewer).getEditor(side)

  private fun unifiedViewer(diffFile: VirtualFile): UnifiedDiffViewer =
    waitForViewer(diffFile) {
      it is UnifiedDiffViewer && it.diffChanges != null && !it.hasPendingRediff()
    } as UnifiedDiffViewer

  private fun unifiedEditor(diffFile: VirtualFile): Editor = unifiedViewer(diffFile).editor

  private fun unifiedLine(diffFile: VirtualFile, side: Side, line: Int): Int {
    var result = -1
    ApplicationManager.getApplication().invokeAndWait {
      result = unifiedViewer(diffFile).transferLineToOnesideStrict(side, line)
    }
    check(result >= 0) { "Line $line of the $side side is not shown in the unified view" }
    return result
  }

  // ======================== Assertions ========================

  private fun assertCaretInDiff(diffFile: VirtualFile, side: Side, line: Int, column: Int) {
    val editor = sideEditor(diffFile, side)
    assertSelectedFile(diffFile)
    assertCaret(editor, line, column)
    ApplicationManager.getApplication().invokeAndWait {
      val viewer = processor(diffFile).activeViewer as TwosideTextDiffViewer
      assertEquals(side, viewer.currentSide, "The jump should make its side the current side of the diff")
    }
  }

  private fun assertCaretInUnifiedDiff(diffFile: VirtualFile, side: Side, line: Int, column: Int) {
    val unifiedLine = unifiedLine(diffFile, side, line)
    assertSelectedFile(diffFile)
    assertCaret(unifiedEditor(diffFile), unifiedLine, column)
  }

  private fun assertCaretInFile(file: VirtualFile, line: Int, column: Int) {
    assertSelectedFile(file)
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait { editor = fileEditorManager.selectedTextEditor }
    assertCaret(editor!!, line, column)
  }

  private fun assertSelectedFile(file: VirtualFile) {
    var selected: VirtualFile? = null
    waitUntil(timeout = 2_000) {
      ApplicationManager.getApplication().invokeAndWait { selected = fileEditorManager.selectedEditor?.file }
      selected == file
    }
    assertEquals(file, selected, "<C-O> should bring back $file")
  }

  private fun assertCaret(editor: Editor, line: Int, column: Int) {
    var position: LogicalPosition? = null
    waitUntil(timeout = 2_000) {
      ApplicationManager.getApplication().invokeAndWait { position = editor.caretModel.logicalPosition }
      position == LogicalPosition(line, column)
    }
    assertEquals(LogicalPosition(line, column), position)
  }
}
