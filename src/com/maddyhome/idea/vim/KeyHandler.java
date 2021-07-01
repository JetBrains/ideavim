/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.action.change.VimRepeater;
import com.maddyhome.idea.vim.action.change.insert.InsertCompletedDigraphAction;
import com.maddyhome.idea.vim.action.change.insert.InsertCompletedLiteralAction;
import com.maddyhome.idea.vim.action.macro.ToggleRecordingAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
import com.maddyhome.idea.vim.handler.ActionBeanClass;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.ShowCmd;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.actionSystem.CommonDataKeys.*;
import static com.intellij.openapi.actionSystem.PlatformDataKeys.PROJECT_FILE_DIRECTORY;

/**
 * This handles every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
public class KeyHandler {
  /**
   * Returns a reference to the singleton instance of this class
   *
   * @return A reference to the singleton
   */
  public static @NotNull KeyHandler getInstance() {
    if (instance == null) {
      instance = new KeyHandler();
    }
    return instance;
  }

  /**
   * Creates an instance
   */
  private KeyHandler() {
  }

  /**
   * Sets the original key handler
   *
   * @param origHandler The original key handler
   */
  public void setOriginalHandler(TypedActionHandler origHandler) {
    this.origHandler = origHandler;
  }

  /**
   * Gets the original key handler
   *
   * @return The original key handler
   */
  public TypedActionHandler getOriginalHandler() {
    return origHandler;
  }

  public static void executeVimAction(@NotNull Editor editor,
                                      @NotNull EditorActionHandlerBase cmd,
                                      DataContext context) {
    CommandProcessor.getInstance()
      .executeCommand(editor.getProject(), () -> cmd.execute(editor, getProjectAwareDataContext(editor, context)),
                      cmd.getId(), DocCommandGroupId.noneGroupId(editor.getDocument()), UndoConfirmationPolicy.DEFAULT,
                      editor.getDocument());
  }

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  @SuppressWarnings("deprecation")
  public static boolean executeAction(@NotNull AnAction action, @NotNull DataContext context) {
    final AnActionEvent event =
      new AnActionEvent(null, context, ActionPlaces.KEYBOARD_SHORTCUT, action.getTemplatePresentation(),
                        ActionManager.getInstance(), 0);

    if (action instanceof ActionGroup && !((ActionGroup)action).canBePerformed(context)) {
      // Some ActionGroups should not be performed, but shown as a popup
      ListPopup popup = JBPopupFactory.getInstance()
        .createActionGroupPopup(event.getPresentation().getText(), (ActionGroup)action, context, false, null, -1);

      Component component = context.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      if (component != null) {
        Window window = SwingUtilities.getWindowAncestor(component);
        if (window != null) {
          popup.showInCenterOf(window);
        }
        return true;
      }
      popup.showInFocusCenter();
      return true;
    }
    else {
      // beforeActionPerformedUpdate should be called to update the action. It fixes some rider-specific problems.
      //   because rider use async update method. See VIM-1819.
      action.beforeActionPerformedUpdate(event);
      if (event.getPresentation().isEnabled()) {
        // Executing listeners for action. I can't be sure that this code is absolutely correct,
        //   action execution process in IJ seems to be more complicated.
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        // [VERSION UPDATE] 212+
        actionManager.fireBeforeActionPerformed(action, event.getDataContext(), event);

        action.actionPerformed(event);

        // [VERSION UPDATE] 212+
        actionManager.fireAfterActionPerformed(action, event.getDataContext(), event);
        return true;
      }
    }
    return false;
  }

  /**
   * This is the main key handler for the Vim plugin. Every keystroke not handled directly by Idea is sent here for
   * processing.
   *
   * @param editor  The editor the key was typed into
   * @param key     The keystroke typed by the user
   * @param context The data context
   */
  public void handleKey(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull DataContext context) {
    handleKey(editor, key, context, true, false);
  }

  /**
   * Invoked before acquiring a write lock and actually handling the keystroke.
   * <p>
   * Drafts an optional {@link ActionPlan} that will be used as a base for zero-latency rendering in editor.
   *
   * @param editor  The editor the key was typed into
   * @param key     The keystroke typed by the user
   * @param context The data context
   * @param plan    The current action plan
   */
  public void beforeHandleKey(@NotNull Editor editor,
                              @NotNull KeyStroke key,
                              @NotNull DataContext context,
                              @NotNull ActionPlan plan) {

    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();

    if (mode == CommandState.Mode.INSERT || mode == CommandState.Mode.REPLACE) {
      VimPlugin.getChange().beforeProcessKey(editor, context, key, plan);
    }
  }

  /**
   * Handling input keys with additional parameters
   *
   * @param allowKeyMappings - If we allow key mappings or not
   * @param mappingCompleted - if true, we don't check if the mapping is incomplete
   *
   * TODO mappingCompleted and recursionCounter - we should find a more beautiful way to use them
   */
  public void handleKey(@NotNull Editor editor,
                        @NotNull KeyStroke key,
                        @NotNull DataContext context,
                        boolean allowKeyMappings,
                        boolean mappingCompleted) {
    if (handleKeyRecursionCount >= OptionsManager.INSTANCE.getMaxmapdepth().value()) {
      VimPlugin.showMessage(MessageHelper.message("E223"));
      VimPlugin.indicateError();
      return;
    }

    VimPlugin.clearError();
    // All the editor actions should be performed with top level editor!!!
    // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
    editor = HelperKt.getTopLevelEditor(editor);

    final CommandState editorState = CommandState.getInstance(editor);
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();

    // If this is a "regular" character keystroke, get the character
    char chKey = key.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? 0 : key.getKeyChar();

    // We only record unmapped keystrokes. If we've recursed to handle mapping, don't record anything.
    boolean shouldRecord = handleKeyRecursionCount == 0 && editorState.isRecording();
    handleKeyRecursionCount++;

    try {
      if (!allowKeyMappings || !handleKeyMapping(editor, key, context, mappingCompleted)) {
        if (isCommandCountKey(chKey, editorState)) {
          commandBuilder.addCountCharacter(key);
        } else if (isDeleteCommandCountKey(key, editorState)) {
          commandBuilder.deleteCountCharacter();
        } else if (isEditorReset(key, editorState)) {
          handleEditorReset(editor, key, context, editorState);
        }
        // If we got this far the user is entering a command or supplying an argument to an entered command.
        // First let's check to see if we are at the point of expecting a single character argument to a command.
        else if (isExpectingCharArgument(commandBuilder)) {
          handleCharArgument(key, chKey, editorState);
        }
        else if (editorState.getSubMode() == CommandState.SubMode.REGISTER_PENDING) {
          commandBuilder.addKey(key);
          handleSelectRegister(editorState, chKey);
        }
        // If we are this far, then the user must be entering a command or a non-single-character argument
        // to an entered command. Let's figure out which it is.
        else if (!handleDigraph(editor, key, context, editorState)) {
          // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
          // return the node matching this keystroke
          final Node<ActionBeanClass> node = mapOpCommand(key, commandBuilder.getChildNode(key), editorState);

          if (node instanceof CommandNode) {
            handleCommandNode(editor, context, key, (CommandNode<ActionBeanClass>) node, editorState);
            commandBuilder.addKey(key);
          } else if (node instanceof CommandPartNode) {
            commandBuilder.setCurrentCommandPartNode((CommandPartNode<ActionBeanClass>) node);
            commandBuilder.addKey(key);
          } else if (isSelectRegister(key, editorState)) {
            editorState.pushModes(CommandState.Mode.COMMAND, CommandState.SubMode.REGISTER_PENDING);
            commandBuilder.addKey(key);
          }
          else { // node == null

            // If we are in insert/replace mode send this key in for processing
            if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
              shouldRecord &= VimPlugin.getChange().processKey(editor, context, key);
            } else if (editorState.getMode() == CommandState.Mode.SELECT) {
              shouldRecord &= VimPlugin.getChange().processKeyInSelectMode(editor, context, key);
            } else if (editorState.getMappingState().getMappingMode() == MappingMode.CMD_LINE) {
              shouldRecord &= VimPlugin.getProcess().processExKey(editor, key);
            }
            // If we get here then the user has entered an unrecognized series of keystrokes
            else {
              commandBuilder.setCommandState(CurrentCommandState.BAD_COMMAND);
            }

            partialReset(editor);
          }
        }
      }
    }
    finally {
      handleKeyRecursionCount--;
    }

    // Do we have a fully entered command at this point? If so, let's execute it.
    if (commandBuilder.isReady()) {
      executeCommand(editor, context, editorState);
    }
    else if (commandBuilder.isBad()) {
      editorState.resetOpPending();
      editorState.resetRegisterPending();
      VimPlugin.indicateError();
      reset(editor);
    }

    // Don't record the keystroke that stops the recording (unmapped this is `q`)
    if (shouldRecord && editorState.isRecording()) {
      VimPlugin.getRegister().recordKeyStroke(key);
    }

    // This will update immediately, if we're on the EDT (which we are)
    ShowCmd.INSTANCE.update();
  }

  /**
   * See the description for {@link com.maddyhome.idea.vim.action.DuplicableOperatorAction}
   */
  private Node<ActionBeanClass> mapOpCommand(KeyStroke key, Node<ActionBeanClass> node, @NotNull CommandState editorState) {
    if (editorState.isDuplicateOperatorKeyStroke(key)) {
      return editorState.getCommandBuilder().getChildNode(KeyStroke.getKeyStroke('_'));
    }
    return node;
  }

  public static <T> boolean isPrefix(@NotNull List<T> list1, @NotNull List<T> list2) {
    if (list1.size() > list2.size()) {
      return false;
    }
    for (int i = 0; i < list1.size(); i++) {
      if (!list1.get(i).equals(list2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private void handleEditorReset(@NotNull Editor editor, @NotNull KeyStroke key, final @NotNull DataContext context, @NotNull CommandState editorState) {
    if (editorState.getCommandBuilder().isAtDefaultState()) {
      RegisterGroup register = VimPlugin.getRegister();
      if (register.getCurrentRegister() == register.getDefaultRegister()) {
        boolean indicateError = true;

        if (key.getKeyCode() == KeyEvent.VK_ESCAPE) {
          Ref<Boolean> executed = Ref.create();
          CommandProcessor.getInstance()
            .executeCommand(editor.getProject(),
                            () -> executed.set(KeyHandler.executeAction(IdeActions.ACTION_EDITOR_ESCAPE, context)),
                            "", null);
          indicateError = !executed.get();
        }

        if (indicateError) {
          VimPlugin.indicateError();
        }
      }
    }
    reset(editor);
    ChangeGroup.resetCaret(editor, false);
  }

  private boolean handleKeyMapping(final @NotNull Editor editor,
                                   final @NotNull KeyStroke key,
                                   final @NotNull DataContext context,
                                   boolean mappingCompleted) {

    final CommandState commandState = CommandState.getInstance(editor);
    final MappingState mappingState = commandState.getMappingState();
    final CommandBuilder commandBuilder = commandState.getCommandBuilder();

    if (commandBuilder.isAwaitingCharOrDigraphArgument()
      || commandBuilder.isBuildingMultiKeyCommand()
      || isMappingDisabledForKey(key, commandState)
      || commandState.getSubMode() == CommandState.SubMode.REGISTER_PENDING) {
      return false;
    }

    mappingState.stopMappingTimer();

    // Save the unhandled key strokes until we either complete or abandon the sequence.
    mappingState.addKey(key);

    final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mappingState.getMappingMode());

    // Returns true if any of these methods handle the key. False means that the key is unrelated to mapping and should
    // be processed as normal.
    return (handleUnfinishedMappingSequence(editor, mappingState, mapping, mappingCompleted))
      || handleCompleteMappingSequence(editor, context, mappingState, mapping, key)
      || handleAbandonedMappingSequence(editor, mappingState, context);
  }

  private boolean isMappingDisabledForKey(@NotNull KeyStroke key, @NotNull CommandState commandState) {
    // "0" can be mapped, but the mapping isn't applied when entering a count. Other digits are always mapped, even when
    // entering a count.
    // See `:help :map-modes`
    return key.getKeyChar() == '0' && commandState.getCommandBuilder().getCount() > 0;
  }

  private boolean handleUnfinishedMappingSequence(@NotNull Editor editor,
                                                  @NotNull MappingState mappingState,
                                                  @NotNull KeyMapping mapping,
                                                  boolean mappingCompleted) {
    if (mappingCompleted) return false;

    // Is there at least one mapping that starts with the current sequence? This does not include complete matches,
    // unless a sequence is also a prefix for another mapping. We eagerly evaluate the shortest mapping, so even if a
    // mapping is a prefix, it will get evaluated when the next character is entered.
    // Note that currentlyUnhandledKeySequence is the same as the state after commandState.getMappingKeys().add(key). It
    // would be nice to tidy ths up
    if (!mapping.isPrefix(mappingState.getKeys())) {
      return false;
    }

    // If the timeout option is set, set a timer that will abandon the sequence and replay the unhandled keys unmapped.
    // Every time a key is pressed and handled, the timer is stopped. E.g. if there is a mapping for "dweri", and the
    // user has typed "dw" wait for the timeout, and then replay "d" and "w" without any mapping (which will of course
    // delete a word)
    final Application application = ApplicationManager.getApplication();
    if (OptionsManager.INSTANCE.getTimeout().isSet()) {
      mappingState.startMappingTimer(actionEvent -> application.invokeLater(() -> {

        final List<KeyStroke> unhandledKeys = mappingState.detachKeys();

        if (editor.isDisposed() || isPluginMapping(unhandledKeys)) {
          return;
        }

        for (KeyStroke keyStroke : unhandledKeys) {
          handleKey(editor, keyStroke, EditorDataContext.init(editor, null), true, true);
        }
      }, ModalityState.stateForComponent(editor.getComponent())));
    }

    return true;
  }

  private boolean handleCompleteMappingSequence(@NotNull Editor editor,
                                                @NotNull DataContext context,
                                                @NotNull MappingState mappingState,
                                                @NotNull KeyMapping mapping,
                                                KeyStroke key) {

    // The current sequence isn't a prefix, check to see if it's a completed sequence.
    final MappingInfo currentMappingInfo = mapping.get(mappingState.getKeys());
    MappingInfo mappingInfo = currentMappingInfo;
    if (mappingInfo == null) {
      // It's an abandoned sequence, check to see if the previous sequence was a complete sequence.
      // TODO: This is incorrect behaviour
      // What about sequences that were completed N keys ago?
      // This should really be handled as part of an abandoned key sequence. We should also consolidate the replay
      // of cached keys - this happens in timeout, here and also in abandoned sequences.
      // Extract most of this method into handleMappingInfo. If we have a complete sequence, call it and we're done.
      // If it's not a complete sequence, handleAbandonedMappingSequence should do something like call
      // mappingState.detachKeys and look for the longest complete sequence in the returned list, evaluate it, and then
      // replay any keys not yet handled. NB: The actual implementation should be compared to Vim behaviour to see what
      // should actually happen.
      final ArrayList<KeyStroke> previouslyUnhandledKeySequence = new ArrayList<>();
      mappingState.getKeys().forEach(previouslyUnhandledKeySequence::add);
      if (previouslyUnhandledKeySequence.size() > 1) {
        previouslyUnhandledKeySequence.remove(previouslyUnhandledKeySequence.size() - 1);
        mappingInfo = mapping.get(previouslyUnhandledKeySequence);
      }
    }

    if (mappingInfo == null) {
      return false;
    }

    mappingState.resetMappingSequence();

    final EditorDataContext currentContext = EditorDataContext.init(editor, context);

    mappingInfo.execute(editor, context);

    // If we've just evaluated the previous key sequence, make sure to also handle the current key
    if (mappingInfo != currentMappingInfo) {
      handleKey(editor, key, currentContext, true, false);
    }

    return true;
  }

  private boolean handleAbandonedMappingSequence(@NotNull Editor editor,
                                                 @NotNull MappingState mappingState,
                                                 DataContext context) {

    // The user has terminated a mapping sequence with an unexpected key
    // E.g. if there is a mapping for "hello" and user enters command "help" the processing of "h", "e" and "l" will be
    //   prevented by this handler. Make sure the currently unhandled keys are processed as normal.

    final List<KeyStroke> unhandledKeyStrokes = mappingState.detachKeys();

    // If there is only the current key to handle, do nothing
    if (unhandledKeyStrokes.size() == 1) {
      return false;
    }

    // Okay, look at the code below. Why is the first key handled separately?
    // Let's assume the next mappings:
    //   - map ds j
    //   - map I 2l
    // If user enters `dI`, the first `d` will be caught be this handler because it's a prefix for `ds` command.
    //  After the user enters `I`, the caught `d` should be processed without mapping, and the rest of keys
    //  should be processed with mappings (to make I work)

    if (isPluginMapping(unhandledKeyStrokes)) {
      handleKey(editor, unhandledKeyStrokes.get(unhandledKeyStrokes.size() - 1), context, true, false);
    } else {
      handleKey(editor, unhandledKeyStrokes.get(0), context, false, false);

      for (KeyStroke keyStroke : unhandledKeyStrokes.subList(1, unhandledKeyStrokes.size())) {
        handleKey(editor, keyStroke, context, true, false);
      }
    }

    return true;
  }

  // The <Plug>mappings are not executed if they fail to map to something.
  //   E.g.
  //   - map <Plug>iA someAction
  //   - map I <Plug>i
  //   For `IA` someAction should be executed.
  //   But if the user types `Ib`, `<Plug>i` won't be executed again. Only `b` will be passed to keyHandler.
  private boolean isPluginMapping(List<KeyStroke> unhandledKeyStrokes) {
    return unhandledKeyStrokes.get(0).equals(StringHelper.PlugKeyStroke);
  }

  @SuppressWarnings("RedundantIfStatement")
  private boolean isCommandCountKey(char chKey, @NotNull CommandState editorState) {
    // Make sure to avoid handling '0' as the start of a count.
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();
    boolean notRegisterPendingCommand = editorState.getMode() == CommandState.Mode.COMMAND &&
                                        editorState.getSubMode() != CommandState.SubMode.REGISTER_PENDING;
    boolean visualMode = editorState.getMode() == CommandState.Mode.VISUAL;
    boolean opPendingMode = editorState.getMode() == CommandState.Mode.OP_PENDING;
    if (notRegisterPendingCommand || visualMode || opPendingMode) {
      if (commandBuilder.isExpectingCount() &&
          Character.isDigit(chKey) &&
          (commandBuilder.getCount() > 0 || chKey != '0')) {
        return true;
      }
    }
    return false;
  }

  private boolean isDeleteCommandCountKey(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    // See `:help N<Del>`
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();
    return (editorState.getMode() == CommandState.Mode.COMMAND ||
            editorState.getMode() == CommandState.Mode.VISUAL ||
            editorState.getMode() == CommandState.Mode.OP_PENDING) &&
           commandBuilder.isExpectingCount() &&
           commandBuilder.getCount() > 0 &&
           key.getKeyCode() == KeyEvent.VK_DELETE;
  }

  private boolean isEditorReset(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    return editorState.getMode() == CommandState.Mode.COMMAND && StringHelper.isCloseKeyStroke(key);
  }

  private boolean isSelectRegister(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    if (editorState.getMode() != CommandState.Mode.COMMAND && editorState.getMode() != CommandState.Mode.VISUAL) {
      return false;
    }

    if (editorState.getSubMode() == CommandState.SubMode.REGISTER_PENDING) {
      return true;
    }

    return key.getKeyChar() == '"' && !editorState.isOperatorPending() && editorState.getCommandBuilder().getExpectedArgumentType() == null;
  }

  private void handleSelectRegister(@NotNull CommandState commandState, char chKey) {
    commandState.resetRegisterPending();
    if (VimPlugin.getRegister().isValid(chKey)) {
      commandState.getCommandBuilder().pushCommandPart(chKey);
    }
    else {
      commandState.getCommandBuilder().setCommandState(CurrentCommandState.BAD_COMMAND);
    }
  }

  private boolean isExpectingCharArgument(@NotNull CommandBuilder commandBuilder) {
    return commandBuilder.getExpectedArgumentType() == Argument.Type.CHARACTER;
  }

  private void handleCharArgument(@NotNull KeyStroke key, char chKey, @NotNull CommandState commandState) {
    // We are expecting a character argument - is this a regular character the user typed?
    // Some special keys can be handled as character arguments - let's check for them here.
    if (chKey == 0) {
      switch (key.getKeyCode()) {
        case KeyEvent.VK_TAB:
          chKey = '\t';
          break;
        case KeyEvent.VK_ENTER:
          chKey = '\n';
          break;
      }
    }

    final CommandBuilder commandBuilder = commandState.getCommandBuilder();
    if (chKey != 0) {
      // Create the character argument, add it to the current command, and signal we are ready to process the command
      commandBuilder.completeCommandPart(new Argument(chKey));
    }
    else {
      // Oops - this isn't a valid character argument
      commandBuilder.setCommandState(CurrentCommandState.BAD_COMMAND);
    }
  }

  private boolean handleDigraph(@NotNull Editor editor,
                                @NotNull KeyStroke key,
                                @NotNull DataContext context,
                                @NotNull CommandState editorState) {

    // Support starting a digraph/literal sequence if the operator accepts one as an argument, e.g. 'r' or 'f'.
    // Normally, we start the sequence (in Insert or CmdLine mode) through a VimAction that can be mapped. Our
    // VimActions don't work as arguments for operators, so we have to special case here. Helpfully, Vim appears to
    // hardcode the shortcuts, and doesn't support mapping, so everything works nicely.
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();
    if (commandBuilder.getExpectedArgumentType() == Argument.Type.DIGRAPH) {
      if (DigraphSequence.isDigraphStart(key)) {
        editorState.startDigraphSequence();
        editorState.getCommandBuilder().addKey(key);
        return true;
      }
      if (DigraphSequence.isLiteralStart(key)) {
        editorState.startLiteralSequence();
        editorState.getCommandBuilder().addKey(key);
        return true;
      }
    }

    DigraphResult res = editorState.processDigraphKey(key, editor);
    if (ExEntryPanel.getInstance().isActive()) {
      switch (res.getResult()) {
        case DigraphResult.RES_HANDLED:
          setPromptCharacterEx(commandBuilder.isPuttingLiteral() ? '^' : key.getKeyChar());
          break;
        case DigraphResult.RES_DONE:
        case DigraphResult.RES_BAD:
          if (key.getKeyCode() == KeyEvent.VK_C && (key.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) {
            return false;
          } else {
            ExEntryPanel.getInstance().getEntry().clearCurrentAction();
          }
          break;
      }
    }

    switch (res.getResult()) {
      case DigraphResult.RES_HANDLED:
        editorState.getCommandBuilder().addKey(key);
        return true;

      case DigraphResult.RES_DONE:
        if (commandBuilder.getExpectedArgumentType() == Argument.Type.DIGRAPH) {
          commandBuilder.fallbackToCharacterArgument();
        }
        final KeyStroke stroke = res.getStroke();
        if (stroke == null) {
          return false;
        }
        editorState.getCommandBuilder().addKey(key);
        handleKey(editor, stroke, context);
        return true;

      case DigraphResult.RES_BAD:
        // BAD is an error. We were expecting a valid character, and we didn't get it.
        if (commandBuilder.getExpectedArgumentType() != null) {
          commandBuilder.setCommandState(CurrentCommandState.BAD_COMMAND);
        }
        return true;

      case DigraphResult.RES_UNHANDLED:
        // UNHANDLED means the key stroke made no sense in the context of a digraph, but isn't an error in the current
        // state. E.g. waiting for {char} <BS> {char}. Let the key handler have a go at it.
        if (commandBuilder.getExpectedArgumentType() == Argument.Type.DIGRAPH) {
          commandBuilder.fallbackToCharacterArgument();
          handleKey(editor, key, context);
          return true;
        }
        return false;
    }

    return false;
  }

  private void executeCommand(@NotNull Editor editor,
                              @NotNull DataContext context,
                              @NotNull CommandState editorState) {
    final Command command = editorState.getCommandBuilder().buildCommand();

    // If we were in "operator pending" mode, reset back to normal mode.
    editorState.resetOpPending();

    // Save off the command we are about to execute
    editorState.setExecutingCommand(command);

    Project project = editor.getProject();
    final Command.Type type = command.getType();
    if (type.isWrite()) {
      boolean modificationAllowed = EditorModificationUtil.checkModificationAllowed(editor);
      boolean writeRequested = EditorModificationUtil.requestWriting(editor);
      if (!modificationAllowed || !writeRequested) {
        VimPlugin.indicateError();
        reset(editor);
        return;
      }
    }

    if (!command.getFlags().contains(CommandFlags.FLAG_TYPEAHEAD_SELF_MANAGE)) {
      IdeEventQueue.getInstance().flushDelayedKeyEvents();
    }

    if (ApplicationManager.getApplication().isDispatchThread()) {
      Runnable action = new ActionRunner(editor, context, command);
      EditorActionHandlerBase cmdAction = command.getAction();
      String name = cmdAction.getId();

      if (type.isWrite()) {
        RunnableHelper.runWriteCommand(project, action, name, action);
      }
      else if (type.isRead()) {
        RunnableHelper.runReadCommand(project, action, name, action);
      }
      else {
        CommandProcessor.getInstance().executeCommand(project, action, name, action);
      }
    }
  }

  private void handleCommandNode(Editor editor,
                                 DataContext context,
                                 KeyStroke key,
                                 @NotNull CommandNode<ActionBeanClass> node,
                                 CommandState editorState) {
    // The user entered a valid command. Create the command and add it to the stack.
    final EditorActionHandlerBase action = node.getActionHolder().getInstance();
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();
    final Argument.Type expectedArgumentType = commandBuilder.getExpectedArgumentType();

    commandBuilder.pushCommandPart(action);

    if (!checkArgumentCompatibility(expectedArgumentType, action)) {
      commandBuilder.setCommandState(CurrentCommandState.BAD_COMMAND);
      return;
    }

    if (action.getArgumentType() == null || stopMacroRecord(node, editorState)) {
      commandBuilder.setCommandState(CurrentCommandState.READY);
    }
    else {
      final Argument.Type argumentType = action.getArgumentType();
      startWaitingForArgument(editor, context, key.getKeyChar(), action, argumentType, editorState);
      partialReset(editor);
    }

    // TODO In the name of God, get rid of EX_STRING, FLAG_COMPLETE_EX and all the related staff
    if (expectedArgumentType == Argument.Type.EX_STRING && action.getFlags().contains(CommandFlags.FLAG_COMPLETE_EX)) {
      /* The only action that implements FLAG_COMPLETE_EX is ProcessExEntryAction.
         * When pressing ':', ExEntryAction is chosen as the command. Since it expects no arguments, it is invoked and
           calls ProcessGroup#startExCommand, pushes CMD_LINE mode, and the action is popped. The ex handler will push
           the final <CR> through handleKey, which chooses ProcessExEntryAction. Because we're not expecting EX_STRING,
           this branch does NOT fire, and ProcessExEntryAction handles the ex cmd line entry.
         * When pressing '/' or '?', SearchEntry(Fwd|Rev)Action is chosen as the command. This expects an argument of
           EX_STRING, so startWaitingForArgument calls ProcessGroup#startSearchCommand. The ex handler pushes the final
           <CR> through handleKey, which chooses ProcessExEntryAction, and we hit this branch. We don't invoke
           ProcessExEntryAction, but pop it, set the search text as an argument on SearchEntry(Fwd|Rev)Action and invoke
           that instead.
         * When using '/' or '?' as part of a motion (e.g. "d/foo"), the above happens again, and all is good. Because
           the text has been applied as an argument on the last command, '.' will correctly repeat it.

         It's hard to see how to improve this. Removing EX_STRING means starting ex input has to happen in ExEntryAction
         and SearchEntry(Fwd|Rev)Action, and the ex command invoked in ProcessExEntryAction, but that breaks any initial
         operator, which would be invoked first (e.g. 'd' in "d/foo").
      */
      String text = VimPlugin.getProcess().endSearchCommand(editor);
      commandBuilder.popCommandPart();  // Pop ProcessExEntryAction
      commandBuilder.completeCommandPart(new Argument(text)); // Set search text on SearchEntry(Fwd|Rev)Action
      editorState.popModes(); // Pop CMD_LINE
    }
  }

  private boolean stopMacroRecord(CommandNode<ActionBeanClass> node, @NotNull CommandState editorState) {
    return editorState.isRecording() && node.getActionHolder().getInstance() instanceof ToggleRecordingAction;
  }

  private void startWaitingForArgument(Editor editor,
                                       DataContext context,
                                       char key,
                                       @NotNull EditorActionHandlerBase action,
                                       @NotNull Argument.Type argument,
                                       CommandState editorState) {
    final CommandBuilder commandBuilder = editorState.getCommandBuilder();
    switch (argument) {
      case MOTION:
        if (editorState.isDotRepeatInProgress() && VimRepeater.Extension.INSTANCE.getArgumentCaptured() != null) {
          commandBuilder.completeCommandPart(VimRepeater.Extension.INSTANCE.getArgumentCaptured());
        }
        editorState.pushModes(CommandState.Mode.OP_PENDING, CommandState.SubMode.NONE);
        break;
      case DIGRAPH:
        // Command actions represent the completion of a command. Showcmd relies on this - if the action represents a
        // part of a command, the showcmd output is reset part way through. This means we need to special case entering
        // digraph/literal input mode. We have an action that takes a digraph as an argument, and pushes it back through
        // the key handler when it's complete.
        if (action instanceof InsertCompletedDigraphAction) {
          editorState.startDigraphSequence();
          setPromptCharacterEx('?');
        } else if (action instanceof InsertCompletedLiteralAction) {
          editorState.startLiteralSequence();
          setPromptCharacterEx('^');
        }
        break;
      case EX_STRING:
        // The current Command expects an EX_STRING argument. E.g. SearchEntry(Fwd|Rev)Action. This won't execute until
        // state hits READY. Start the ex input field, push CMD_LINE mode and wait for the argument.
        VimPlugin.getProcess().startSearchCommand(editor, context, commandBuilder.getCount(), key);
        commandBuilder.setCommandState(CurrentCommandState.NEW_COMMAND);
        editorState.pushModes(CommandState.Mode.CMD_LINE, CommandState.SubMode.NONE);
        break;
    }
  }

  private boolean checkArgumentCompatibility(@Nullable Argument.Type expectedArgumentType, @NotNull EditorActionHandlerBase action) {
    return !(expectedArgumentType == Argument.Type.MOTION && action.getType() != Command.Type.MOTION);
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public static boolean executeAction(@NotNull @NonNls String name, @NotNull DataContext context) {
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(name);
    return action != null && executeAction(action, context);
  }

  /**
   * Partially resets the state of this handler. Resets the command count, clears the key list, resets the key tree
   * node to the root for the current mode we are in.
   *
   * @param editor The editor to reset.
   */
  public void partialReset(@NotNull Editor editor) {
    CommandState editorState = CommandState.getInstance(editor);
    editorState.getMappingState().resetMappingSequence();
    editorState.getCommandBuilder().resetInProgressCommandPart(getKeyRoot(editorState.getMappingState().getMappingMode()));
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument.
   *
   * @param editor The editor to reset.
   */
  public void reset(@NotNull Editor editor) {
    partialReset(editor);
    CommandState editorState = CommandState.getInstance(editor);
    editorState.getCommandBuilder().resetAll(getKeyRoot(editorState.getMappingState().getMappingMode()));
  }

  private @NotNull CommandPartNode<ActionBeanClass> getKeyRoot(MappingMode mappingMode) {
    return VimPlugin.getKey().getKeyRoot(mappingMode);
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  public void fullReset(@NotNull Editor editor) {
    VimPlugin.clearError();
    CommandState.getInstance(editor).reset();
    reset(editor);
    RegisterGroup registerGroup = VimPlugin.getRegisterIfCreated();
    if (registerGroup != null) {
      registerGroup.resetRegister();
    }
    VisualGroupKt.updateCaretState(editor);
    editor.getSelectionModel().removeSelection();
  }

  // This method is copied from com.intellij.openapi.editor.actionSystem.EditorAction.getProjectAwareDataContext
  private static @NotNull DataContext getProjectAwareDataContext(final @NotNull Editor editor,
                                                                 final @NotNull DataContext original) {
    if (PROJECT.getData(original) == editor.getProject()) {
      return new DialogAwareDataContext(original);
    }

    return dataId -> {
      if (PROJECT.is(dataId)) {
        final Project project = editor.getProject();
        if (project != null) {
          return project;
        }
      }
      return original.getData(dataId);
    };

  }

  private void setPromptCharacterEx(final char promptCharacter) {
    final ExEntryPanel exEntryPanel = ExEntryPanel.getInstance();
    if (exEntryPanel.isActive()) {
        exEntryPanel.getEntry().setCurrentActionPromptCharacter(promptCharacter);
    }
  }

  // This class is copied from com.intellij.openapi.editor.actionSystem.DialogAwareDataContext.DialogAwareDataContext
  private static final class DialogAwareDataContext implements DataContext {
    @SuppressWarnings("rawtypes")
    private static final DataKey[] keys = {PROJECT, PROJECT_FILE_DIRECTORY, EDITOR, VIRTUAL_FILE, PSI_FILE};
    private final Map<String, Object> values = new HashMap<>();

    DialogAwareDataContext(DataContext context) {
      for (DataKey<?> key : keys) {
        values.put(key.getName(), key.getData(context));
      }
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
      if (values.containsKey(dataId)) {
        return values.get(dataId);
      }
      final Editor editor = (Editor)values.get(EDITOR.getName());
      if (editor != null) {
        return DataManager.getInstance().getDataContext(editor.getContentComponent()).getData(dataId);
      }
      return null;
    }
  }

  /**
   * This was used as an experiment to execute actions as a runnable.
   */
  static class ActionRunner implements Runnable {
    @Contract(pure = true)
    ActionRunner(Editor editor, DataContext context, Command cmd) {
      this.editor = editor;
      this.context = context;
      this.cmd = cmd;
    }

    @Override
    public void run() {
      CommandState editorState = CommandState.getInstance(editor);

      editorState.getCommandBuilder().setCommandState(CurrentCommandState.NEW_COMMAND);

      final Character register = cmd.getRegister();
      if (register != null) {
        VimPlugin.getRegister().selectRegister(register);
      }

      executeVimAction(editor, cmd.getAction(), context);
      if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
        VimPlugin.getChange().processCommand(editor, cmd);
      }

      // Now the command has been executed let's clean up a few things.

      // By default, the "empty" register is used by all commands, so we want to reset whatever the last register
      // selected by the user was to the empty register
      VimPlugin.getRegister().resetRegister();

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.getSubMode() == CommandState.SubMode.SINGLE_COMMAND &&
          (!cmd.getFlags().contains(CommandFlags.FLAG_EXPECT_MORE))) {
        editorState.popModes();
        VisualGroupKt.resetShape(CommandStateHelper.getMode(editor), editor);
      }

      if (editorState.getCommandBuilder().isDone()) {
        KeyHandler.getInstance().reset(editor);
      }
    }

    private final Editor editor;
    private final DataContext context;
    private final Command cmd;
  }

  private TypedActionHandler origHandler;
  private int handleKeyRecursionCount = 0;

  private static KeyHandler instance;
}
