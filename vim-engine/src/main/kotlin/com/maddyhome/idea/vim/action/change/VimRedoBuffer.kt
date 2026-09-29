/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

/**
 * The keys that made up the last change, which `.` feeds back through the key handler.
 *
 * This is Vim's redo buffer (`redobuff` in getchar.c). A change is remembered as the keys typed for it and nothing
 * else - no record of what it did to the document - which is what lets `.` work when the IDE contributes edits of its
 * own while we are recording: an auto-closed bracket, a reformat, or the closing tag that `XmlTagNameSynchronizer`
 * renames for us (VIM-1153). Replaying the keys asks the IDE to do its part again where the repeat is happening.
 *
 * Only unmapped keys are recorded, the same keys a macro records, so a change made through a mapping is repeated by
 * running the mapping again.
 */
object VimRedoBuffer {
  private var lastChange: RecordedChange? = null
  private var recording = Recording()

  /** True while `.` is replaying, when nothing is recorded so that a second `.` repeats the same change. */
  private var isReplaying: Boolean = false

  val isEmpty: Boolean
    get() = lastChange == null

  fun recordKey(key: KeyStroke) {
    if (!isReplaying) recording.add(key)
  }

  /**
   * Records keys that stand in for text the IDE inserted, which `.` has no keys of its own to replay.
   *
   * Accepting a completion item is the case this exists for - see the caller in `IdeaSpecifics`.
   */
  fun recordSynthesizedKeys(keys: List<KeyStroke>) {
    if (isReplaying || !recording.isCollectingInsert) return
    keys.forEach { recording.add(it) }
  }

  /** Marks [key] as part of the command's `["x][count]` prefix, which [RecordedChange] renders instead of replaying. */
  fun markCountKey(key: KeyStroke) {
    if (!isReplaying) recording.markCount(key)
  }

  fun markRegisterKey(key: KeyStroke) {
    if (!isReplaying) recording.markRegister(key)
  }

  /** Drops [key] again, for a count character deleted with `N<Del>`. */
  fun dropKey(key: KeyStroke) {
    if (!isReplaying) recording.drop(key)
  }

  /** Called from [VimRepeater.saveLastChange], where IdeaVim already decides what the last change was. */
  fun onChange(command: Command) {
    if (!isReplaying) recording.changeMade(command)
  }

  fun commandFinished(editor: VimEditor) {
    if (isReplaying) return
    val mode = editor.mode
    when {
      recording.isChangeFinishedIn(mode) -> commit()
      recording.isStillBeingEnteredIn(mode) -> recording.startNextCommand(mode)
      else -> reset()
    }
  }

  /**
   * Called when a key means nothing in the current state.
   *
   * Only a command being entered in Normal mode is given up on. A key the IDE ends up handling itself does not
   * abandon an insert session or a Visual selection, and Vim does not clear its redo buffer for one either.
   */
  fun commandAborted(editor: VimEditor) {
    if (isReplaying) return
    if (!recording.isCollectingInsert && editor.mode is Mode.NORMAL) reset()
  }

  /** [overrideCount] is used in place of the recorded count if one was typed before `.`. */
  fun keysForReplay(overrideCount: Int): List<KeyStroke> {
    val change = lastChange?.withRedoRegister() ?: return emptyList()
    lastChange = change
    return change.toKeyStrokes(overrideCount)
  }

  /** Runs [block] with recording frozen, so that replaying a change does not record it again. */
  fun <T> replaying(block: () -> T): T {
    val wasReplaying = isReplaying
    isReplaying = true
    try {
      return block()
    } finally {
      isReplaying = wasReplaying
    }
  }

  /** Forgets the change being recorded, keeping the last committed one. */
  private fun reset() {
    recording = Recording()
  }

  fun resetAll() {
    lastChange = null
    reset()
  }

  private fun commit() {
    lastChange = recording.toChange()
    logger.trace { "Recorded change: $lastChange" }
    reset()
  }

  private val logger = vimLogger<VimRedoBuffer>()
}

/**
 * A change `.` can repeat: the keys to feed back, plus the `["x][count]` prefix to render in front of them.
 *
 * The prefix is rendered rather than replayed (Vim's `prep_redo()`) so that a count typed before `.` *replaces* the
 * original rather than multiplying it, and so that `2d3w` repeats as `6dw` - see `:help .`.
 */
private data class RecordedChange(
  val keys: List<KeyStroke>,
  val register: Char?,
  val count: Int,
  val isVisual: Boolean,
) {
  fun toKeyStrokes(overrideCount: Int): List<KeyStroke> = buildList {
    register?.let { addAll(keysOf("\"$it")) }
    if (isVisual) {
      addAll(keysOf("1v"))
    } else {
      val effectiveCount = if (overrideCount > 0) overrideCount else count
      if (effectiveCount > 0) addAll(keysOf(effectiveCount.toString()))
    }
    addAll(keys)
  }

  /** `:help redo-register`: repeating a change that named a numbered register uses the next one, so `"1p....` pastes
   * the whole delete history rather than the same line five times. */
  fun withRedoRegister(): RecordedChange =
    if (register in '1'..'8') copy(register = register!!.inc()) else this

  override fun toString() = "register=$register, count=$count, visual=$isVisual, keys=${keys.toNotation()}"
}

/**
 * The keys recorded so far, and what is known about the change they may turn out to be.
 *
 * A change is finished as soon as its command has run, unless it left us in Insert mode, in which case the recording
 * stays open until Insert mode is left: `cw` is a change before the text it inserts has been typed, and the whole of
 * `cwfoo<Esc>` has to be replayed. Keys that turn out not to be a change are thrown away, but only once the command
 * they belong to has really finished - `v`, `d` and `/` all complete as commands part way through a change.
 */
private class Recording {
  private val keys = mutableListOf<KeyStroke>()
  private val countIndices = mutableSetOf<Int>()
  private val registerIndices = mutableSetOf<Int>()
  private var lastAddedKey: KeyStroke? = null

  /** Where the command being entered starts in [keys], and where the change does. */
  private var commandStart = 0
  private var changeStart = 0

  /**
   * What is known about the change, rather than the [Command] it came from.
   *
   * A `Command` can reach an editor - `d/foo<CR>` carries an [com.maddyhome.idea.vim.command.Argument.ExString] whose
   * `processing` lambda captures one - and this is held by a static singleton, which is exactly where
   * `LeakHunter.checkProjectLeak` looks for a retained project. Keeping the two values we need cannot leak.
   */
  private var hasChange = false
  private var changeRegister: Char? = null
  private var changeCount = 0

  private var isVisual = false

  var isCollectingInsert: Boolean = false
    private set

  fun add(key: KeyStroke) {
    if (keys.size >= MAX_KEYS) return
    keys.add(key)
    lastAddedKey = key
  }

  fun markCount(key: KeyStroke) {
    if (isLastAdded(key)) countIndices.add(keys.lastIndex)
  }

  fun markRegister(key: KeyStroke) {
    if (isLastAdded(key)) registerIndices.add(keys.lastIndex)
  }

  fun drop(key: KeyStroke) {
    if (!isLastAdded(key)) return
    countIndices.remove(keys.lastIndex)
    registerIndices.remove(keys.lastIndex)
    keys.removeAt(keys.lastIndex)
    lastAddedKey = null
  }

  /**
   * The count of a mapping's right-hand side never reaches [add], so a mark can only ever apply to the key [add] last
   * accepted - never to the key that triggered the mapping.
   */
  private fun isLastAdded(key: KeyStroke) = lastAddedKey == key && keys.lastOrNull() == key

  fun changeMade(command: Command) {
    if (isCollectingInsert) return // A change made from within the insert session, e.g. `i<C-O>x`, belongs to it
    hasChange = true
    changeRegister = command.register
    changeCount = command.rawCount
    changeStart = commandStart
  }

  fun isChangeFinishedIn(mode: Mode) = hasChange && !mode.isInsertSession()

  fun isStillBeingEnteredIn(mode: Mode) = hasChange || mode !is Mode.NORMAL

  fun startNextCommand(mode: Mode) {
    if (hasChange) {
      isCollectingInsert = true
    } else {
      if (mode is Mode.VISUAL || mode is Mode.SELECT) isVisual = true
      // The prefix of a command that is only part of the change - the `2` of `v2erX` - is replayed verbatim
      countIndices.clear()
      registerIndices.clear()
    }
    commandStart = keys.size
  }

  fun toChange(): RecordedChange {
    check(hasChange) { "No change to record" }
    if (isVisual) return visualChange()
    // Only a prefix that was really typed is rendered again. A change made through a mapping carries the mapping's
    // count on its command but not in the recorded keys, and rendering it would apply it twice
    val prefixIndices = countIndices + registerIndices
    return RecordedChange(
      keys = keys.filterIndexed { index, _ -> index !in prefixIndices },
      register = changeRegister.takeIf { registerIndices.isNotEmpty() },
      count = if (countIndices.isEmpty()) 0 else changeCount,
      isVisual = false,
    )
  }

  /**
   * The operator alone, replayed after `1v` to reselect an area of the same size (`:help v_1v`, Vim's
   * `redo_VIsual_*`).
   *
   * The selection is deliberately dropped: re-running the motion would measure the new text instead, and `v2e` covers
   * "I found it" on one line and "all rocks" on the next. Everything the operator was entered with is already in
   * these keys, so there is no prefix left to render.
   */
  private fun visualChange() =
    RecordedChange(keys.subList(changeStart, keys.size).toList(), register = null, count = 0, isVisual = true)

  /**
   * `i<C-O>` is Normal mode for a single command but still part of the insert session, and the Operator-pending and
   * Command-line modes it can lead to are too.
   */
  private fun Mode.isInsertSession(): Boolean = when (this) {
    is Mode.INSERT, is Mode.REPLACE -> true
    else -> returnTo.let { it !== this && it.isInsertSession() }
  }

  private companion object {
    private const val MAX_KEYS = 10000
  }
}

private fun keysOf(text: String) = injector.parser.stringToKeys(text)

private fun List<KeyStroke>.toNotation() = injector.parser.toKeyNotation(this)
