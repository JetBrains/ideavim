/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingProcessor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.key.KeyStack
import com.maddyhome.idea.vim.key.consumers.CharArgumentConsumer
import com.maddyhome.idea.vim.key.consumers.CommandCountConsumer
import com.maddyhome.idea.vim.key.consumers.CommandKeyConsumer
import com.maddyhome.idea.vim.key.consumers.DeleteCommandCountConsumer
import com.maddyhome.idea.vim.key.consumers.DigraphConsumer
import com.maddyhome.idea.vim.key.consumers.EditorResetConsumer
import com.maddyhome.idea.vim.key.consumers.ForcedMotionConsumer
import com.maddyhome.idea.vim.key.consumers.LangMapConsumer
import com.maddyhome.idea.vim.key.consumers.ModalInputConsumer
import com.maddyhome.idea.vim.key.consumers.ModeInputConsumer
import com.maddyhome.idea.vim.key.consumers.SelectRegisterConsumer
import com.maddyhome.idea.vim.key.consumers.StartSelectRegisterConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.ConcurrentLinkedDeque
import javax.swing.KeyStroke

/**
 * This handles every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
// TODO for future refactorings (PRs are welcome)
// 1. avoid using handleKeyRecursionCount & shouldRecord
// 2. maybe we can live without allowKeyMappings: Boolean
class KeyHandler {
  private val keyConsumers: List<KeyConsumer> = listOf(
    ModalInputConsumer(), // Must be first
    LangMapConsumer(),    // Must be before MappingProcessor
    MappingProcessor,     // Must be as early in pipeline as possible
    CommandCountConsumer(),
    DeleteCommandCountConsumer(),
    EditorResetConsumer(),
    StartSelectRegisterConsumer(),  // Must be before command key consumer, so " isn't treated as a command char
    SelectRegisterConsumer(),
    ForcedMotionConsumer(),
    DigraphConsumer(),    // Must be before command key consumer, to process {char}<BS>{char}
    // Must be before char argument consumer, to convert and repost digraph/literal key sequences
    // and non-digraph/literal sequences fall through as char arguments
    CommandKeyConsumer(), // Must be before argument consumers, because c_CTRL-R is both a command prefix and a command
    // expecting a character-based argument
    CharArgumentConsumer(),
    ModeInputConsumer()   // Must be last to accept the keystroke as typed input
  )
  private var handleKeyRecursionCount = 0
  internal var maxMapDepthReached = false

  private var commandListener: ConcurrentLinkedDeque<() -> Unit> = ConcurrentLinkedDeque()

  /**
   * This is an internal API of IdeaVim. External plugins must not use it.
   */
  @ApiStatus.Internal
  fun addCommandListener(listener: () -> Unit) {
    commandListener.add(listener)
  }

  /**
   * This is an internal API of IdeaVim. External plugins must not use it.
   */
  @ApiStatus.Internal
  fun removeAllCommandListeners() {
    commandListener.clear()
  }

  // KeyHandlerState requires injector.keyGroup to be initialized and that's why we don't create it immediately and have this here
  // TODO figure out a better solution
  private val defaultKeyHandlerState by lazy { KeyHandlerState() }
  private var mutableKeyHandlerState: KeyHandlerState? = null
  var keyHandlerState: KeyHandlerState
    get() = mutableKeyHandlerState ?: defaultKeyHandlerState
    private set(value) {
      mutableKeyHandlerState = value
    }

  val keyStack: KeyStack = KeyStack()
  val modalEntryKeys: MutableList<KeyStroke> = ArrayList()

  var lastUsedEditorInfo: LastUsedEditorInfo = LastUsedEditorInfo(-1, false)

  /**
   * Main entry point to process every keystroke for Vim functionality
   *
   * The key handler will process every keystroke according to current mode and state of e.g. command builder, mapping
   * and digraphs. The keystroke will potentially be mapped and used as part of a Vim motion or operator, or inserted
   * directly into the editor or current command line/search entry. The source of the keystroke is important to decide
   * if the key should be mapped, or if `'langmap'` should be applied.
   *
   * @param editor    The editor the key was typed into
   * @param key       The keystroke typed by the user
   * @param keySource Where did the keystroke come from? E.g. typed, result of a map or non-recursive map, etc.
   * @param context   The data context
   * @param keyState  Various state for managing the keystroke, such as command builder, mapping and digraph state
   */
  fun handleKey(
    editor: VimEditor,
    key: KeyStroke,
    keySource: KeySource,
    context: ExecutionContext,
    keyState: KeyHandlerState,
  ) {
    if (keySource == KeySource.TYPED) {
      commandListener.forEach { it() }
    }
    val result = processKey(key, editor, keySource, KeyProcessResult.SynchronousKeyProcessBuilder(keyState))
    if (result is KeyProcessResult.Executable) {
      result.execute(editor, context)
    }
  }

  // Deprecated. Has external usages
  @Deprecated(
    "Use handleKey(editor, key, keySource, context, keyState)",
    replaceWith = ReplaceWith("handleKey(editor, key, KeySource.TYPED, context, keyState)")
  )
  @ApiStatus.ScheduledForRemoval
  fun handleKey(editor: VimEditor, key: KeyStroke, context: ExecutionContext, keyState: KeyHandlerState) {
    handleKey(editor, key, KeySource.TYPED, context, keyState)
  }

  // Deprecated. Has external usages
  @Suppress("unused")
  @Deprecated(
    "Use `handleKey(editor, key, context, allowKeyMappings, keyState)` instead.",
    replaceWith = ReplaceWith("handleKey(editor, key, context, allowKeyMappings, keyState)")
  )
  @ApiStatus.ScheduledForRemoval
  fun handleKey(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyState: KeyHandlerState,
  ) {
    val keySource = if (allowKeyMappings) KeySource.TYPED else KeySource.MAPPED_NON_RECURSIVE
    handleKey(editor, key, keySource, context, keyState)
  }

  /**
   * This method determines whether IdeaVim can handle the passed key or not.
   * For instance, if there is no mapping for <F5>, we should return 'KeyProcessResult.Unknown' to inform the IDE that
   * we did not process the keypress, and therefore need to propagate it further.
   * Alternatively, if we understand the key, we return a 'KeyProcessResult.Executable', which contains a runnable that
   * could execute the key if needed.
   */
  private fun processKey(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): KeyProcessResult {
    synchronized(lock) {
      logger.trace {
        """
        ------- Key Handler -------
        Start key processing. source: $keySource
        Key: $key
        Mode: ${editor.mode}
        State: ${keyProcessResultBuilder.state}
      """.trimIndent()
      }

      val maxMapDepth = injector.globalOptions().maxmapdepth
      if (handleKeyRecursionCount >= maxMapDepth && keySource != KeySource.LANG_MAP) {
        maxMapDepthReached = true
        keyProcessResultBuilder.addExecutionStep { _, lambdaEditor, _ ->
          logger.warn("Key handling, maximum recursion of the key received. maxdepth=$maxMapDepth")
          injector.messages.showErrorMessage(lambdaEditor, injector.messages.message("E223"))
        }
        return keyProcessResultBuilder.build()
      }

      injector.messages.clearError()
      if (handleKeyRecursionCount == 0) {
        maxMapDepthReached = false
      }
      // We only record unmapped keystrokes. If we've recursed to handle mapping, don't record anything.
      val shouldRecord = handleKeyRecursionCount == 0 && injector.registerGroup.isRecording

      // Langmap mapping does not affect recursion depth. However, we can still hit infinite recursion with 'langremap',
      // which applies 'langmap' to the output of a mapping, because the output of 'langmap' can be mapped again,
      // recursively.
      // E.g. `nmap b a`, `set langmap=ax`, `nmap x b`: 'b' -> 'a' (nmap) -> 'x' (langmap) -> 'b' (nmap) -> ...
      if (keySource != KeySource.LANG_MAP) {
        handleKeyRecursionCount++
      }

      try {
        val isProcessed = processConsumer(key, editor, keySource, keyProcessResultBuilder)
        if (isProcessed) {
          logger.trace { "Key was successfully caught by consumer" }
          keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
            finishedCommandPreparation(lambdaEditor, lambdaContext, key, shouldRecord, lambdaKeyState)
          }
        } else {
          // Key wasn't processed by any of the consumers, so we reset our key state
          onUnknownKey(editor, keyProcessResultBuilder.state)
          updateState(keyProcessResultBuilder.state)
          if (keySource != KeySource.LANG_MAP) {
            handleKeyRecursionCount-- // because onFinish will not be executed for unknown
          }
          return KeyProcessResult.Unknown
        }
      } finally {
        if (keySource != KeySource.LANG_MAP) {
          keyProcessResultBuilder.onFinish = { handleKeyRecursionCount-- }
        }
      }
      return keyProcessResultBuilder.build()
    }
  }

  private fun processConsumer(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    keyConsumers.forEach {
      if (!it.isApplicable(key, editor, keySource, keyProcessResultBuilder)) return@forEach
      // This line is separated so a breakpoint can be set on consumeKey :)
      val consumed = it.consumeKey(key, editor, keySource, keyProcessResultBuilder)
      if (consumed) return true
    }
    return false
  }

  /**
   * Use this to execute key handling without recording them in macro
   */
  fun withoutRecording(unit: () -> Unit) {
    handleKeyRecursionCount++
    try {
      unit()
    } finally {
      handleKeyRecursionCount--
    }
  }

  internal fun finishedCommandPreparation(
    editor: VimEditor,
    context: ExecutionContext,
    key: KeyStroke?,
    shouldRecord: Boolean,
    keyState: KeyHandlerState,
  ) {
    // Do we have a fully entered command at this point? If so, let's execute it.
    val commandBuilder = keyState.commandBuilder

    if (commandBuilder.isReady) {
      logger.trace("Ready command builder. Execute command.")
      executeCommand(editor, context, injector.vimState, keyState)
    }

    // Don't record the keystroke that stops the recording (unmapped this is `q`)
    if (shouldRecord && injector.registerGroup.isRecording && key != null) {
      injector.registerGroup.recordKeyStroke(key)
      modalEntryKeys.forEach { injector.registerGroup.recordKeyStroke(it) }
      modalEntryKeys.clear()
    }

    // This will update immediately, if we're on the EDT (which we are)
    injector.messages.updateStatusBar(editor)
    logger.trace("----------- Key Handler Finished -----------")
  }

  private fun onUnknownKey(editor: VimEditor, keyState: KeyHandlerState) {
    editor.resetOpPending()
    editor.isReplaceCharacter = false
    // Note that this will also reset the CommandBuilder to NEW_COMMAND
    reset(keyState, editor.mode)
  }

  fun setBadCommand(editor: VimEditor, keyState: KeyHandlerState) {
    onUnknownKey(editor, keyState)
    injector.messages.indicateError()
  }

  private fun executeCommand(
    editor: VimEditor,
    context: ExecutionContext,
    editorState: VimStateMachine,
    keyState: KeyHandlerState,
  ) {
    logger.trace("Command execution")
    val command = keyState.commandBuilder.buildCommand()
    val operatorArguments = OperatorArguments(command.rawCount, editorState.mode)

    //true if goes form insert to OpPending
    val isSingleCommandFromInsert = when (val m = editor.mode) {
      is Mode.NORMAL -> m.isInsertPending || m.isReplacePending
      is Mode.OP_PENDING -> m.returnTo is Mode.INSERT || m.returnTo is Mode.REPLACE
      else -> false
    }

    // If we were in "operator pending" mode, reset back to normal mode.
    // But opening command line should not reset operator pending mode (e.g. `d/foo`)
    if (!command.flags.contains(CommandFlags.FLAG_START_EX)) {
      editor.resetOpPending()
    }

    // Save off the command we are about to execute
    editorState.executingCommand = command
    val type = command.type
    if (type.isWrite) {
      if (!editor.isWritable()) {
        injector.messages.indicateError()
        reset(keyState, editorState.mode)
        logger.warn("File is not writable")
        return
      }
    }

    val action: Runnable =
      ActionRunner(editor, context, command, keyState, operatorArguments, isSingleCommandFromInsert)
    val cmdAction = command.action
    val name = cmdAction.id
    if (cmdAction.executesNestedCommands) {
      // Macro-like actions must not hold an open command while replaying keystrokes, otherwise a replayed `u` can't
      // undo an earlier change made by the same macro. See [EditorActionHandlerBase.executesNestedCommands].
      action.run()
    } else {
      injector.actionExecutor.executeCommand(editor, action, name, action)
    }
  }

  /**
   * Partially resets the state of this handler. Resets the command count, clears the key list, resets the key tree
   * node to the root for the current mode we are in.
   *
   * @param editor The editor to reset.
   */
  fun partialReset(editor: VimEditor) {
    logger.trace { "Partial reset is executed" }
    keyHandlerState.partialReset(editor.mode)
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument.
   *
   * @param editor The editor to reset.
   */
  fun reset(editor: VimEditor) {
    logger.trace { "Reset is executed" }
    editor.isReplaceCharacter = false
    editor.resetOpPending()
    keyHandlerState.partialReset(editor.mode)
    keyHandlerState.commandBuilder.resetAll(injector.keyGroup.getBuiltinCommandsTrie(editor.mode.toMappingMode()))
  }

  // TODO we should have a single reset method
  fun reset(keyState: KeyHandlerState, mode: Mode) {
    logger.trace { "Reset is executed" }
    injector.commandLine.getActiveCommandLine()?.clearCurrentAction()
    keyHandlerState.partialReset(mode)
    keyState.commandBuilder.resetAll(injector.keyGroup.getBuiltinCommandsTrie(mode.toMappingMode()))
  }

  fun updateState(keyState: KeyHandlerState) {
    logger.trace { "State updated" }
    logger.trace { keyState.toString() }
    this.keyHandlerState = keyState
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  fun fullReset(editor: VimEditor) {
    logger.trace { "Full reset" }
    injector.messages.clearError()

    editor.mode = Mode.NORMAL()
    injector.vimState.executingCommand = null
    keyHandlerState.digraphSequence.reset()

    reset(keyHandlerState, editor.mode)
    injector.registerGroupIfCreated?.resetRegister()
    editor.removeSelection()
  }

  fun setPromptCharacterEx(promptCharacter: Char) {
    val commandLine = injector.commandLine.getActiveCommandLine() ?: return
    commandLine.setPromptCharacter(promptCharacter)
  }

  /**
   * This was used as an experiment to execute actions as a runnable.
   */
  internal class ActionRunner(
    val editor: VimEditor,
    val context: ExecutionContext,
    val cmd: Command,
    val keyState: KeyHandlerState,
    val operatorArguments: OperatorArguments,
    val isSingleCommandFromInsert: Boolean = false,
  ) : Runnable {
    override fun run() {
      val editorState = injector.vimState

      val register = cmd.register
      if (register != null) {
        injector.registerGroup.selectRegister(register)
      }
      injector.actionExecutor.executeVimAction(editor, cmd.action, context, operatorArguments)

      if (isSingleCommandFromInsert && editorState.mode is Mode.INSERT) {
        restoreCursorAfterInsertNormal(editor, editorState)
      }

      if (editorState.mode is Mode.INSERT || editorState.mode is Mode.REPLACE) {
        injector.changeGroup.processCommand(editor, cmd)
      }

      // Now the command has been executed let's clean up a few things.

      // By default, the "empty" register is used by all commands, so we want to reset whatever the last register
      // selected by the user was to the empty register
      injector.registerGroup.resetRegister()

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.mode is Mode.NORMAL && !cmd.flags.contains(CommandFlags.FLAG_EXPECT_MORE)) {
        editor.mode = editorState.mode.returnTo
        if (isSingleCommandFromInsert) {
          restoreCursorAfterInsertNormal(editor, editorState)
        }
      }

      instance.reset(keyState, editorState.mode)
    }

    private fun restoreCursorAfterInsertNormal(editor: VimEditor, editorState: VimStateMachine) {
      for (caret in editor.nativeCarets()) {
        if (editorState.wasCaretAtEndOfLineBeforeInsertNormal || editorState.deletedToEndOfLine) {
          val line = caret.getBufferPosition().line
          caret.moveToOffset(editor.getLineEndOffset(line, true))
        }
      }
      editorState.wasCaretAtEndOfLineBeforeInsertNormal = false
      editorState.deletedToEndOfLine = false
    }
  }

  companion object {
    val lock: Any = Object()
    private val logger: VimLogger = vimLogger<KeyHandler>()

    internal fun <T> isPrefix(list1: List<T>, list2: List<T>): Boolean {
      if (list1.size > list2.size) {
        return false
      }
      for (i in list1.indices) {
        if (list1[i] != list2[i]) {
          return false
        }
      }
      return true
    }

    private val instance = KeyHandler()

    @JvmStatic
    fun getInstance(): KeyHandler = instance
  }
}

/**
 * This class was created to manage Fleet input processing.
 * Fleet needs to synchronously determine if the key will be handled by the plugin or should be passed elsewhere.
 * The key processing itself will be executed asynchronously at a later time.
 */
sealed interface KeyProcessResult {
  /**
   * Key input that is not recognized by IdeaVim and should be passed to IDE.
   */
  object Unknown : KeyProcessResult

  /**
   * Key input that is recognized by IdeaVim and can be executed.
   * Key handling is a two-step process:
   * 1. Determine if the key should be processed and how (is it a command, mapping, or something else).
   * 2. Execute the recognized command.
   * This class should be returned after the first step is complete.
   * It will continue the key handling and finish the process.
   */
  class Executable(
    private val originalState: KeyHandlerState,
    private val preProcessState: KeyHandlerState,
    private val processing: KeyProcessing,
  ) : KeyProcessResult {

    companion object {
      private val logger = vimLogger<KeyProcessResult>()
    }

    fun execute(editor: VimEditor, context: ExecutionContext) {
      synchronized(KeyHandler.lock) {
        val keyHandler = KeyHandler.getInstance()
        if (keyHandler.keyHandlerState != originalState) {
          logger.error("Unexpected editor state. Aborting command execution.")
        }
        keyHandler.updateState(preProcessState)
        processing(preProcessState, editor, context)
      }
    }
  }

  /**
   * This class serves as a wrapper around the key handling algorithm and should be used with care:
   * We process keys in two steps:
   * 1. We first determine if IdeaVim can handle the key or not. At this stage, you should avoid modifying anything
   *    except state: KeyHandlerState. This is because it is not guaranteed that the key will be handled by IdeaVim at
   *    all, and we want to minimize possible side effects.
   * 2. If it's confirmed that the key will be handled, add all the key handling processes as execution steps,
   *    slated for later execution.
   *
   * Please note that execution steps could depend on KeyHandlerState, and because of that we cannot change the state
   * after adding an execution step. This is because an execution step does not anticipate changes to the state.
   * If there's need to alter the state following any of the execution steps, wrap the state modification as an
   * execution step. This will allow state modification to occur later rather than immediately.
   */
  abstract class KeyProcessResultBuilder {
    abstract val state: KeyHandlerState
    protected val processings: MutableList<KeyProcessing> = mutableListOf()
    var onFinish: (() -> Unit)? = null // FIXME I'm a dirty hack to support recursion counter

    fun addExecutionStep(keyProcessing: KeyProcessing) {
      processings.add(keyProcessing)
    }

    abstract fun build(): KeyProcessResult
  }

  // Works with existing state and modifies it during execution
  // It's the way IdeaVim worked for the long time and for this class we do not create
  // unnecessary objects and assume that the code will be executed immediately
  class SynchronousKeyProcessBuilder(override val state: KeyHandlerState) : KeyProcessResultBuilder() {
    override fun build(): KeyProcessResult {
      return Executable(state, state) { keyHandlerState, vimEditor, executionContext ->
        try {
          for (processing in processings) {
            processing(keyHandlerState, vimEditor, executionContext)
          }
        } finally {
          onFinish?.let { it() }
        }
      }
    }
  }

  // Works with a clone of current state, nothing is modified during the builder work (key processing)
  // The new state will be applied later, when we run Executable.execute() (it may not be run at all)
  class AsyncKeyProcessBuilder(originalState: KeyHandlerState) : KeyProcessResultBuilder() {
    private val originalState: KeyHandlerState = KeyHandler.getInstance().keyHandlerState
    override val state: KeyHandlerState = originalState.clone()

    override fun build(): KeyProcessResult {
      return Executable(originalState, state) { keyHandlerState, vimEditor, executionContext ->
        try {
          for (processing in processings) {
            processing(keyHandlerState, vimEditor, executionContext)
          }
        } finally {
          onFinish?.let { it() }
          KeyHandler.getInstance().updateState(state)
        }
      }
    }
  }
}

typealias KeyProcessing = (KeyHandlerState, VimEditor, ExecutionContext) -> Unit

data class LastUsedEditorInfo(
  val hash: Int,
  /**
   * If true, this editor was initialized in insert mode
   */
  val isInsertModeForced: Boolean,
)
