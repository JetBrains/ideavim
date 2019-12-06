/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.maddyhome.idea.vim.action.DuplicableOperatorAction;
import com.maddyhome.idea.vim.action.change.VimRepeater;
import com.maddyhome.idea.vim.action.macro.ToggleRecordingAction;
import com.maddyhome.idea.vim.action.motion.search.SearchEntryFwdAction;
import com.maddyhome.idea.vim.action.motion.search.SearchEntryRevAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intellij.openapi.actionSystem.CommonDataKeys.*;
import static com.intellij.openapi.actionSystem.PlatformDataKeys.PROJECT_FILE_DIRECTORY;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * This handlers every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
public class KeyHandler {
  /**
   * Returns a reference to the singleton instance of this class
   *
   * @return A reference to the singleton
   */
  @NotNull
  public static KeyHandler getInstance() {
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
  public static boolean executeAction(@NotNull AnAction action, @NotNull DataContext context) {
    final AnActionEvent event =
      new AnActionEvent(null, context, ActionPlaces.ACTION_SEARCH, action.getTemplatePresentation(),
                        ActionManager.getInstance(), 0);

    if (action instanceof ActionGroup && !((ActionGroup)action).canBePerformed(context)) {
      // Some of the AcitonGroups should not be performed, but shown as a popup
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
      // beforeActionPerformedUpdate should be called to update the action. It fixes some rider-specific problems
      //   because rider use async update method. See VIM-1819
      action.beforeActionPerformedUpdate(event);
      if (event.getPresentation().isEnabled()) {
        action.actionPerformed(event);
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
    handleKey(editor, key, context, true);
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

  public void handleKey(@NotNull Editor editor,
                        @NotNull KeyStroke key,
                        @NotNull DataContext context,
                        boolean allowKeyMappings) {
    VimPlugin.clearError();
    // All the editor actions should be performed with top level editor!!!
    // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
    editor = HelperKt.getTopLevelEditor(editor);
    final CommandState editorState = CommandState.getInstance(editor);

    // If this is a "regular" character keystroke, get the character
    char chKey = key.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? 0 : key.getKeyChar();

    final boolean isRecording = editorState.isRecording();
    boolean shouldRecord = true;

    // Check for command count before key mappings - otherwise e.g. ':map 0 ^' breaks command counts that contain a zero
    if (isCommandCount(editorState, chKey)) {
      // Update the count
      count = count * 10 + (chKey - '0');
    }
    else if (!waitCommandFinish(editor) && allowKeyMappings && handleKeyMapping(editor, key, context)) {
      if (editorState.getMappingMode() != MappingMode.OP_PENDING ||
          currentCmd.isEmpty() ||
          currentCmd.peek().getArgument() == null ||
          Objects.requireNonNull(currentCmd.peek().getArgument()).getType() != Argument.Type.OFFSETS) {
        return;
      }
    }
    // Pressing delete while entering a count "removes" the last digit entered
    // Unlike the digits, this must be checked *after* checking for key mappings
    else if (isDeleteCommandCount(key, editorState)) {
      // "Remove" the last digit sent to us
      count /= 10;
    }
    else if (isEditorReset(key, editorState)) {
      handleEditorReset(editor, key, context);
    }
    // If we got this far the user is entering a command or supplying an argument to an entered command.
    // First let's check to see if we are at the point of expecting a single character argument to a command.
    else if (currentArg == Argument.Type.CHARACTER) {
      handleCharArgument(key, chKey);
    }
    // If we are this far, then the user must be entering a command or a non-single-character argument
    // to an entered command. Let's figure out which it is
    else {
      // For debugging purposes we track the keys entered for this command
      keys.add(key);

      // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
      // return the node matching this keystroke
      Node node = editorState.getCurrentNode().get(key);

      if (handleDigraph(editor, key, context, node)) return;

      node = mapOpCommand(key, node, editorState);

      if (node instanceof CommandNode) {
        handleCommandNode(editor, context, key, (CommandNode)node, editorState);
      }
      else if (node instanceof CommandPartNode) {
        editorState.setCurrentNode((CommandPartNode)node);
      }
      else {
        if (lastWasBS && lastChar != 0 && OptionsManager.INSTANCE.getDigraph().isSet()) {
          char dig = VimPlugin.getDigraph().getDigraph(lastChar, key.getKeyChar());
          key = KeyStroke.getKeyStroke(dig);
        }

        // If we are in insert/replace mode send this key in for processing
        if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
          if (!VimPlugin.getChange().processKey(editor, context, key)) {
            shouldRecord = false;
          }
        }
        else if (editorState.getMode() == CommandState.Mode.SELECT) {
          if (!VimPlugin.getChange().processKeyInSelectMode(editor, context, key)) {
            shouldRecord = false;
          }
        }
        else if (editorState.getMappingMode() == MappingMode.CMD_LINE) {
          if (!VimPlugin.getProcess().processExKey(editor, key)) {
            shouldRecord = false;
          }
        }
        // If we get here then the user has entered an unrecognized series of keystrokes
        else {
          state = State.BAD_COMMAND;
        }

        lastChar = lastWasBS && lastChar != 0 ? 0 : key.getKeyChar();
        lastWasBS = false;
        partialReset(editor);
      }
    }

    // Do we have a fully entered command at this point? If so, lets execute it
    if (state == State.READY) {
      executeCommand(editor, key, context, editorState);
    }
    else if (state == State.BAD_COMMAND) {
      if (editorState.getMappingMode() == MappingMode.OP_PENDING) {
        editorState.popState();
      }
      VimPlugin.indicateError();
      reset(editor);
    }
    else if (isRecording && shouldRecord) {
      VimPlugin.getRegister().recordKeyStroke(key);
    }
  }

  private boolean waitCommandFinish(@NotNull Editor editor) {
    return !(CommandState.getInstance(editor).getCurrentNode() instanceof RootNode);
  }

  /**
   * See the description for {@link com.maddyhome.idea.vim.action.DuplicableOperatorAction}
   */
  private Node mapOpCommand(KeyStroke key, Node node, @NotNull CommandState editorState) {
    if (editorState.getMappingMode() == MappingMode.OP_PENDING && !currentCmd.empty()) {
      EditorActionHandlerBase action = currentCmd.peek().getAction();
      if (action instanceof DuplicableOperatorAction &&
          ((DuplicableOperatorAction)action).getDuplicateWith() == key.getKeyChar()) {
        return editorState.getCurrentNode().get(KeyStroke.getKeyStroke('_'));
      }
    }
    return node;
  }

  private static <T> boolean isPrefix(@NotNull List<T> list1, @NotNull List<T> list2) {
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

  private void handleEditorReset(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull final DataContext context) {
    if (count == 0 && currentArg == null && currentCmd.size() == 0) {
      RegisterGroup register = VimPlugin.getRegister();
      if (register.getCurrentRegister() == register.getDefaultRegister()) {
        if (key.getKeyCode() == KeyEvent.VK_ESCAPE) {
          CommandProcessor.getInstance()
            .executeCommand(editor.getProject(), () -> KeyHandler.executeAction("EditorEscape", context), "", null);
        }
        VimPlugin.indicateError();
      }
    }
    reset(editor);
    ChangeGroup.resetCaret(editor, false);
  }

  private boolean handleKeyMapping(@NotNull final Editor editor,
                                   @NotNull final KeyStroke key,
                                   @NotNull final DataContext context) {
    if (state == State.CHAR_OR_DIGRAPH) return false;

    final CommandState commandState = CommandState.getInstance(editor);
    commandState.stopMappingTimer();

    final MappingMode mappingMode = commandState.getMappingMode();

    final List<KeyStroke> mappingKeys = commandState.getMappingKeys();
    final List<KeyStroke> fromKeys = new ArrayList<>(mappingKeys);
    fromKeys.add(key);

    final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mappingMode);
    final MappingInfo currentMappingInfo = mapping.get(fromKeys);
    final MappingInfo prevMappingInfo = mapping.get(mappingKeys);
    final MappingInfo mappingInfo = currentMappingInfo != null ? currentMappingInfo : prevMappingInfo;

    final Application application = ApplicationManager.getApplication();

    if (mapping.isPrefix(fromKeys)) {
      // Okay, there is some mapping that starts with inserted key sequence. So,
      //   either the user will continue to enter the mapping, or (if timeout option is set)
      //   the entered command should be executed. Here we set up the times that will execute
      //   typed keys after some delay.
      // E.g. there is a map for "dweri". If the user types "d", "w" they mean either "dweri" or "dw" command.
      //   If the user will continue typing "e", "r" and "i", the timer will be cancelled. If the user will
      //   not type anything, the "dw" command will be executed.
      mappingKeys.add(key);
      if (!application.isUnitTestMode() && OptionsManager.INSTANCE.getTimeout().isSet()) {
        commandState.startMappingTimer(actionEvent -> application.invokeLater(() -> {
          final KeyStroke firstKey = mappingKeys.get(0);
          mappingKeys.clear();
          if (editor.isDisposed() || firstKey.equals(parseKeys("<Plug>").get(0))) {
            return;
          }
          for (KeyStroke keyStroke : fromKeys) {
            handleKey(editor, keyStroke, new EditorDataContext(editor), false);
          }
        }, ModalityState.stateForComponent(editor.getComponent())));
      }
      return true;
    }
    else if (mappingInfo != null) {
      // Okay, there is a mapping for the entered key sequence
      //   now the another key sequence should be executed, or the handler that attached to this command
      mappingKeys.clear();

      final List<KeyStroke> toKeys = mappingInfo.getToKeys();
      final VimExtensionHandler extensionHandler = mappingInfo.getExtensionHandler();
      final EditorDataContext currentContext = new EditorDataContext(editor);
      if (toKeys != null) {
        // Here is a mapping to another key sequence
        final boolean fromIsPrefix = isPrefix(mappingInfo.getFromKeys(), toKeys);
        boolean first = true;
        for (KeyStroke keyStroke : toKeys) {
          final boolean recursive = mappingInfo.isRecursive() && !(first && fromIsPrefix);
          handleKey(editor, keyStroke, currentContext, recursive);
          first = false;
        }
      }
      else if (extensionHandler != null) {
        // Here is a mapping to some vim handler
        final CommandProcessor processor = CommandProcessor.getInstance();
        final boolean isPendingMode = CommandState.getInstance(editor).getMappingMode() == MappingMode.OP_PENDING;
        Map<Caret, Integer> startOffsets =
          editor.getCaretModel().getAllCarets().stream().collect(Collectors.toMap(Function.identity(), Caret::getOffset));

        if (extensionHandler.isRepeatable()) {
          VimRepeater.Extension.INSTANCE.clean();
        }
        processor.executeCommand(editor.getProject(), () -> extensionHandler.execute(editor, context),
                                 "Vim " + extensionHandler.getClass().getSimpleName(), null);

        if (extensionHandler.isRepeatable()) {
          VimRepeater.Extension.INSTANCE.setLastExtensionHandler(extensionHandler);
          VimRepeater.Extension.INSTANCE.setArgumentCaptured(null);
          VimRepeater.INSTANCE.setRepeatHandler(true);
        }
        if (isPendingMode &&
            !currentCmd.isEmpty() &&
            currentCmd.peek().getArgument() == null) {
          Map<Caret, VimSelection> offsets = new HashMap<>();

          for (Caret caret : editor.getCaretModel().getAllCarets()) {
            @Nullable Integer startOffset = startOffsets.get(caret);
            if (caret.hasSelection()) {
              final VimSelection vimSelection = VimSelection.Companion
                .create(UserDataManager.getVimSelectionStart(caret), caret.getOffset(),
                        SelectionType.fromSubMode(CommandStateHelper.getSubMode(editor)), editor);
              offsets.put(caret, vimSelection);
              commandState.popState();
            }
            else if (startOffset != null && startOffset != caret.getOffset()) {
              // Command line motions are always characterwise exclusive
              int endOffset = caret.getOffset();
              if (startOffset < endOffset) {
                endOffset -= 1;
              } else {
                startOffset -= 1;
              }
              final VimSelection vimSelection = VimSelection.Companion
                .create(startOffset, endOffset, SelectionType.CHARACTER_WISE, editor);
              offsets.put(caret, vimSelection);

              try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
                // Move caret to the initial offset for better undo action
                //  This is not a necessary thing, but without it undo action look less convenient
                editor.getCaretModel().moveToOffset(startOffset);
              }
            }
          }

          if (!offsets.isEmpty()) {
            currentCmd.peek().setArgument(new Argument(offsets));
            state = State.READY;
          }
        }
      }

      // NB: mappingInfo MUST be non-null here, so if equal
      //  then prevMappingInfo is also non-null; this also
      //  means that the prev mapping was a prefix, but the
      //  next key typed (`key`) was not part of that
      if (prevMappingInfo == mappingInfo) {
        handleKey(editor, key, currentContext);
      }
      return true;
    }
    else {
      // If the user enters a command that starts with known mapping, but it is not exactly this mapping,
      //   mapping handler prevents further processing of there keys.
      // E.g. if there is a mapping for "hello" and user enters command "help"
      //   the processing of "h", "e" and "l" will be prevented by this handler.
      //   However, these keys should be processed as usual when user enters "p"
      //   and the following for loop does exactly that.
      //
      // Okay, look at the code below. Why is the first key handled separately?
      // Let's assume the next mappings:
      //   - map ds j
      //   - map I 2l
      // If user enters `dI`, the first `d` will be caught be this handler because it's a prefix for `ds` command.
      //  After the user enters `I`, the caught `d` should be processed without mapping and the rest of keys
      //  should be processed with mappings (to make I work)
      //
      // Additionally, the <Plug>mappings are not executed if the are failed to map to somethings.
      //   E.g.
      //   - map <Plug>iA  someAction
      //   - map I <Plug>i
      //   For `IA` someAction should be executed.
      //   But if the user types `Ib`, `<Plug>i` won't be executed again. Only `b` will be passed to keyHandler.
      if (mappingKeys.isEmpty()) return false;

      // Well, this will always be false, but just for protection
      if (fromKeys.isEmpty()) return false;
      final List<KeyStroke> unhandledKeys = new ArrayList<>(fromKeys);
      mappingKeys.clear();

      if (unhandledKeys.get(0).equals(parseKeys("<Plug>").get(0))) {
        handleKey(editor, unhandledKeys.get(unhandledKeys.size() - 1), context);
      } else {
        handleKey(editor, unhandledKeys.get(0), context, false);
        for (KeyStroke keyStroke : unhandledKeys.subList(1, unhandledKeys.size())) {
          handleKey(editor, keyStroke, context, true);
        }
      }
      return true;
    }
  }

  private boolean isDeleteCommandCount(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    return (editorState.getMode() == CommandState.Mode.COMMAND || editorState.getMode() == CommandState.Mode.VISUAL) &&
           state == State.NEW_COMMAND &&
           currentArg != Argument.Type.CHARACTER &&
           currentArg != Argument.Type.DIGRAPH &&
           key.getKeyCode() == KeyEvent.VK_DELETE &&
           count != 0;
  }

  private boolean isEditorReset(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    return (editorState.getMode() == CommandState.Mode.COMMAND) && StringHelper.isCloseKeyStroke(key);
  }

  private void handleCharArgument(@NotNull KeyStroke key, char chKey) {
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

    if (chKey != 0) {
      // Create the character argument, add it to the current command, and signal we are ready to process
      // the command
      Argument arg = new Argument(chKey);
      Command cmd = currentCmd.peek();
      cmd.setArgument(arg);
      state = State.READY;
    }
    else {
      // Oops - this isn't a valid character argument
      state = State.BAD_COMMAND;
    }
  }

  private boolean isCommandCount(@NotNull CommandState editorState, char chKey) {
    return (editorState.getMode() == CommandState.Mode.COMMAND || editorState.getMode() == CommandState.Mode.VISUAL) &&
           state == State.NEW_COMMAND &&
           currentArg != Argument.Type.CHARACTER &&
           currentArg != Argument.Type.DIGRAPH &&
           Character.isDigit(chKey) &&
           (count != 0 || chKey != '0');
  }

  private boolean handleDigraph(@NotNull Editor editor,
                                @NotNull KeyStroke key,
                                @NotNull DataContext context,
                                @Nullable Node node) {
    if (digraph == null && !(node instanceof CommandNode) && DigraphSequence.isDigraphStart(key)) {
      digraph = new DigraphSequence();
    }
    if (digraph != null) {
      DigraphSequence.DigraphResult res = digraph.processKey(key, editor);
      switch (res.getResult()) {
        case DigraphSequence.DigraphResult.RES_OK:
          return true;
        case DigraphSequence.DigraphResult.RES_BAD:
          digraph = null;
          return true;
        case DigraphSequence.DigraphResult.RES_DONE:
          if (currentArg == Argument.Type.DIGRAPH) {
            currentArg = Argument.Type.CHARACTER;
          }
          digraph = null;
          final KeyStroke stroke = res.getStroke();
          if (stroke == null) {
            return false;
          }
          handleKey(editor, stroke, context);
          return true;
      }
    }
    return false;
  }

  private void executeCommand(@NotNull Editor editor,
                              @NotNull KeyStroke key,
                              @NotNull DataContext context,
                              @NotNull CommandState editorState) {
    // Let's go through the command stack and merge it all into one command. At this time there should never
    // be more than two commands on the stack - one is the actual command and the other would be a motion
    // command argument needed by the first command
    Command cmd = currentCmd.pop();
    while (currentCmd.size() > 0) {
      Command top = currentCmd.pop();
      top.setArgument(new Argument(cmd));
      cmd = top;
    }

    // If we have a command and a motion command argument, both could possibly have their own counts. We
    // need to adjust the counts so the motion gets the product of both counts and the count associated with
    // the command gets reset. Example 3c2w (change 2 words, three times) becomes c6w (change 6 words)
    final Argument arg = cmd.getArgument();
    if (arg != null && arg.getType() == Argument.Type.MOTION) {
      final Command mot = arg.getMotion();
      // If no count was entered for either command then nothing changes. If either had a count then
      // the motion gets the product of both.
      int cnt = cmd.getRawCount() == 0 && mot.getRawCount() == 0 ? 0 : cmd.getCount() * mot.getCount();
      mot.setCount(cnt);
      cmd.setCount(0);
    }

    // If we were in "operator pending" mode, reset back to normal mode.
    if (editorState.getMappingMode() == MappingMode.OP_PENDING) {
      editorState.popState();
    }

    // Save off the command we are about to execute
    editorState.setCommand(cmd);

    if (lastChar != 0 && !lastWasBS) {
      lastWasBS = key.equals(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
    }
    else {
      lastChar = 0;
    }

    Project project = editor.getProject();
    final Command.Type type = cmd.getType();
    if (type.isWrite() && !editor.getDocument().isWritable()) {
      VimPlugin.indicateError();
      reset(editor);
    }

    if (!cmd.getFlags().contains(CommandFlags.FLAG_TYPEAHEAD_SELF_MANAGE)) {
      IdeEventQueue.getInstance().flushDelayedKeyEvents();
    }

    if (ApplicationManager.getApplication().isDispatchThread()) {
      Runnable action = new ActionRunner(editor, context, cmd, key);
      EditorActionHandlerBase cmdAction = cmd.getAction();
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
                                 @NotNull CommandNode node,
                                 CommandState editorState) {
    // The user entered a valid command. Create the command and add it to the stack
    final EditorActionHandlerBase myAction = node.getActionHolder().getAction();
    Command cmd = new Command(count, myAction, myAction.getType(), myAction.getFlags(), keys);
    currentCmd.push(cmd);

    if (currentArg != null && !checkArgumentCompatibility(node)) return;

    if (myAction.getArgumentType() == null || stopMacroRecord(node, editorState)) {
      state = State.READY;
    }
    else {
      currentArg = myAction.getArgumentType();
      startWaitingForArgument(editor, context, key.getKeyChar(), currentArg, editorState, myAction);
      partialReset(editor);
    }

    // TODO In the name of God, get rid of EX_STRING, FLAG_COMPLETE_EX and all the related staff
    if (currentArg == Argument.Type.EX_STRING && myAction.getFlags().contains(CommandFlags.FLAG_COMPLETE_EX)) {
      EditorActionHandlerBase action;
      if (forwardSearch) {
        action = new SearchEntryFwdAction();
      }
      else {
        action = new SearchEntryRevAction();
      }

      String text = VimPlugin.getProcess().endSearchCommand(editor);
      currentCmd.pop();

      Argument arg = new Argument(text);
      cmd = new Command(count, action, action.getType(), action.getFlags(), keys);
      cmd.setArgument(arg);
      currentCmd.push(cmd);
      CommandState.getInstance(editor).popState();
    }
  }

  private boolean stopMacroRecord(CommandNode node, @NotNull CommandState editorState) {
    return editorState.isRecording() && node.getActionHolder().getAction() instanceof ToggleRecordingAction;
  }

  private void startWaitingForArgument(Editor editor,
                                       DataContext context,
                                       char key,
                                       @NotNull Argument.Type argument,
                                       CommandState editorState,
                                       EditorActionHandlerBase action) {
    switch (argument) {
      case CHARACTER:
      case DIGRAPH:
        digraph = new DigraphSequence();
        state = State.CHAR_OR_DIGRAPH;
        break;
      case MOTION:
        if (CommandState.getInstance(editor).isDotRepeatInProgress() && VimRepeater.Extension.INSTANCE.getArgumentCaptured() != null) {
          currentCmd.peek().setArgument(VimRepeater.Extension.INSTANCE.getArgumentCaptured());
          state = State.READY;
        }
        editorState.pushState(editorState.getMode(), editorState.getSubMode(), MappingMode.OP_PENDING);
        break;
      case EX_STRING:
        forwardSearch = !(action instanceof SearchEntryRevAction);

        VimPlugin.getProcess().startSearchCommand(editor, context, count, key);
        state = State.NEW_COMMAND;
        editorState.pushState(CommandState.Mode.CMD_LINE, CommandState.SubMode.NONE, MappingMode.CMD_LINE);
        currentCmd.pop();
    }
  }

  private boolean checkArgumentCompatibility(@NotNull CommandNode node) {
    if (currentArg == Argument.Type.MOTION &&
        node.getActionHolder().getAction().getType() != Command.Type.MOTION) {
      state = State.BAD_COMMAND;
      return false;
    }
    return true;
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public static boolean executeAction(@NotNull String name, @NotNull DataContext context) {
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
  public void partialReset(@Nullable Editor editor) {
    count = 0;
    keys = new ArrayList<>();
    CommandState editorState = CommandState.getInstance(editor);
    editorState.stopMappingTimer();
    editorState.getMappingKeys().clear();
    editorState.setCurrentNode(VimPlugin.getKey().getKeyRoot(editorState.getMappingMode()));
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument
   *
   * @param editor The editor to reset.
   */
  public void reset(@Nullable Editor editor) {
    partialReset(editor);
    state = State.NEW_COMMAND;
    currentCmd.clear();
    currentArg = null;
    digraph = null;
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  public void fullReset(@Nullable Editor editor) {
    VimPlugin.clearError();
    CommandState.getInstance(editor).reset();
    reset(editor);
    lastChar = 0;
    lastWasBS = false;
    VimPlugin.getRegister().resetRegister();
    if (editor != null) {
      VisualGroupKt.updateCaretState(editor);
      editor.getSelectionModel().removeSelection();
    }
  }

  // This method is copied from com.intellij.openapi.editor.actionSystem.EditorAction.getProjectAwareDataContext
  @NotNull
  private static DataContext getProjectAwareDataContext(@NotNull final Editor editor,
                                                        @NotNull final DataContext original) {
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

  // This class is copied from com.intellij.openapi.editor.actionSystem.DialogAwareDataContext.DialogAwareDataContext
  private final static class DialogAwareDataContext implements DataContext {
    private static final DataKey[] keys = {PROJECT, PROJECT_FILE_DIRECTORY, EDITOR, VIRTUAL_FILE, PSI_FILE};
    private final Map<String, Object> values = new HashMap<>();

    DialogAwareDataContext(DataContext context) {
      for (DataKey key : keys) {
        values.put(key.getName(), key.getData(context));
      }
    }

    @Nullable
    @Override
    public Object getData(@NotNull @NonNls String dataId) {
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
    ActionRunner(Editor editor, DataContext context, Command cmd, KeyStroke key) {
      this.editor = editor;
      this.context = context;
      this.cmd = cmd;
      this.key = key;
    }

    @Override
    public void run() {
      CommandState editorState = CommandState.getInstance(editor);
      boolean wasRecording = editorState.isRecording();

      KeyHandler.getInstance().state = State.NEW_COMMAND;
      executeVimAction(editor, cmd.getAction(), context);
      if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
        VimPlugin.getChange().processCommand(editor, cmd);
      }

      // Now that the command has been executed let's clean up a few things.

      // By default the "empty" register is used by all commands so we want to reset whatever the last register
      // selected by the user was to the empty register - unless we just executed the "select register" command.
      if (cmd.getType() != Command.Type.SELECT_REGISTER) {
        VimPlugin.getRegister().resetRegister();
      }

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.getSubMode() == CommandState.SubMode.SINGLE_COMMAND &&
          (!cmd.getFlags().contains(CommandFlags.FLAG_EXPECT_MORE))) {
        editorState.popState();
      }

      KeyHandler.getInstance().reset(editor);

      if (wasRecording && editorState.isRecording()) {
        VimPlugin.getRegister().recordKeyStroke(key);
      }
    }

    private final Editor editor;
    private final DataContext context;
    private final Command cmd;
    private final KeyStroke key;
  }

  private enum State {
    /** Awaiting a new command */
    NEW_COMMAND,
    // TODO  This should be probably processed in some better way
    /** Awaiting for char or digraph input. In this mode mappings doesn't work (even for <C-K>) */
    CHAR_OR_DIGRAPH,
    READY,
    BAD_COMMAND
  }

  private int count;
  private List<KeyStroke> keys = new ArrayList<>();
  private State state = State.NEW_COMMAND;
  @NotNull private final Stack<Command> currentCmd = new Stack<>();
  @Nullable private Argument.Type currentArg;
  private TypedActionHandler origHandler;
  @Nullable private DigraphSequence digraph = null;
  private char lastChar;
  private boolean lastWasBS;

  private boolean forwardSearch = true;

  private static KeyHandler instance;
}
