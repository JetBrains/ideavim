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

package com.maddyhome.idea.vim.group;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretStateTransferableData;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData;
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkAction;
import com.maddyhome.idea.vim.action.motion.search.SearchAgainNextAction;
import com.maddyhome.idea.vim.action.motion.search.SearchAgainPreviousAction;
import com.maddyhome.idea.vim.action.motion.search.SearchEntryFwdAction;
import com.maddyhome.idea.vim.action.motion.search.SearchEntryRevAction;
import com.maddyhome.idea.vim.action.motion.text.MotionParagraphNextAction;
import com.maddyhome.idea.vim.action.motion.text.MotionParagraphPreviousAction;
import com.maddyhome.idea.vim.action.motion.text.MotionSentenceNextStartAction;
import com.maddyhome.idea.vim.action.motion.text.MotionSentencePreviousStartAction;
import com.maddyhome.idea.vim.action.motion.updown.MotionPercentOrMatchAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.ClipboardHandler;
import kotlin.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This group works with command associated with copying and pasting text
 */
public class RegisterGroup {
  private static final String WRITABLE_REGISTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-*+_/\"";
  private static final String READONLY_REGISTERS = ":.%#=/";
  private static final String RECORDABLE_REGISTER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String PLAYBACK_REGISTER = RECORDABLE_REGISTER + "\".*+";
  private static final String VALID_REGISTERS = WRITABLE_REGISTERS + READONLY_REGISTERS;
  private static final List<Character> CLIPBOARD_REGISTERS = ImmutableList.of('*', '+');
  private static final Logger logger = Logger.getInstance(RegisterGroup.class.getName());

  private char defaultRegister = '"';
  private char lastRegister = defaultRegister;
  @NotNull private final HashMap<Character, Register> registers = new HashMap<>();
  private char recordRegister = 0;
  @Nullable private List<KeyStroke> recordList = null;

  public RegisterGroup() {
    final ListOption clipboardOption = OptionsManager.INSTANCE.getClipboard();
    clipboardOption.addOptionChangeListener(event -> {
      if (clipboardOption.contains("unnamed")) {
        defaultRegister = '*';
      }
      else if (clipboardOption.contains("unnamedplus")) {
        defaultRegister = '+';
      }
      else {
        defaultRegister = '"';
      }
      lastRegister = defaultRegister;
    });
  }

  /**
   * Check to see if the last selected register can be written to.
   */
  private boolean isRegisterWritable() {
    return READONLY_REGISTERS.indexOf(lastRegister) < 0;
  }

  /**
   * Store which register the user wishes to work with.
   *
   * @param reg The register name
   * @return true if a valid register name, false if not
   */
  public boolean selectRegister(char reg) {
    if (VALID_REGISTERS.indexOf(reg) != -1) {
      lastRegister = reg;
      if (logger.isDebugEnabled()) logger.debug("register selected: " + lastRegister);

      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Reset the selected register back to the default register.
   */
  public void resetRegister() {
    lastRegister = defaultRegister;
    logger.debug("Last register reset to default register");
  }

  public void resetRegisters() {
    registers.clear();
  }

  /**
   * Store text into the last register.
   *
   * @param editor   The editor to get the text from
   * @param range    The range of the text to store
   * @param type     The type of copy
   * @param isDelete is from a delete
   * @return true if able to store the text into the register, false if not
   */
  public boolean storeText(@NotNull Editor editor, @NotNull TextRange range, @NotNull SelectionType type,
                           boolean isDelete) {
    if (isRegisterWritable()) {
      String text = EditorHelper.getText(editor, range);

      return storeTextInternal(editor, range, text, type, lastRegister, isDelete);
    }

    return false;
  }

  public boolean storeTextInternal(@NotNull Editor editor, @NotNull TextRange range, @NotNull String text,
                                   @NotNull SelectionType type, char register, boolean isDelete) {
    // Null register doesn't get saved
    if (lastRegister == '_') return true;

    int start = range.getStartOffset();
    int end = range.getEndOffset();
    // Normalize the start and end
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    // If this is an uppercase register, we need to append the text to the corresponding lowercase register
    final List<TextBlockTransferableData> transferableData = start != -1 ? getTransferableData(editor, range, text) : new ArrayList<>();
    final String processedText = start != -1 ? preprocessText(editor, range, text, transferableData) : text;
    if (logger.isDebugEnabled()) {
      final String transferableClasses =
        transferableData.stream().map(it -> it.getClass().getName()).collect(Collectors.joining(","));
      logger.debug("Copy to '" + lastRegister + "' with transferable data: " + transferableClasses);
    }
    if (Character.isUpperCase(register)) {
      char lreg = Character.toLowerCase(register);
      Register r = registers.get(lreg);
      // Append the text if the lowercase register existed
      if (r != null) {
        r.addTextAndResetTransferableData(processedText);
      }
      // Set the text if the lowercase register didn't exist yet
      else {
        registers.put(lreg, new Register(lreg, type, processedText, new ArrayList<>(transferableData)));
        if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + processedText + "\"");
      }
    }
    // Put the text in the specified register
    else {
      registers.put(register, new Register(register, type, processedText, new ArrayList<>(transferableData)));
      if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + processedText + "\"");
    }

    if (CLIPBOARD_REGISTERS.contains(register)) {
      ClipboardHandler.setClipboardText(processedText, new ArrayList<>(transferableData), text);
    }

    // Also add it to the default register if the default wasn't specified
    if (register != defaultRegister && ".:/".indexOf(register) == -1) {
      registers.put(defaultRegister, new Register(defaultRegister, type, processedText, new ArrayList<>(transferableData)));
      if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + processedText + "\"");
    }

    if (isDelete) {
      boolean smallInlineDeletion = type == SelectionType.CHARACTER_WISE &&
                       editor.offsetToLogicalPosition(start).line == editor.offsetToLogicalPosition(end).line;

      // Deletes go into numbered registers only if text is smaller than a line, register is used or it's a special case
      if (!smallInlineDeletion || register != defaultRegister || isSmallDeletionSpecialCase(editor)) {
        // Old 1 goes to 2, etc. Old 8 to 9, old 9 is lost
        for (char d = '8'; d >= '1'; d--) {
          Register t = registers.get(d);
          if (t != null) {
            t.rename((char)(d + 1));
            registers.put((char)(d + 1), t);
          }
        }
        registers.put('1', new Register('1', type, processedText, new ArrayList<>(transferableData)));
      }

      // Deletes smaller than one line and without specified register go the the "-" register
      if (smallInlineDeletion && register == defaultRegister) {
        registers.put('-', new Register('-', type, processedText, new ArrayList<>(transferableData)));
      }
    }
    // Yanks also go to register 0 if the default register was used
    else if (register == defaultRegister) {
      registers.put('0', new Register('0', type, processedText, new ArrayList<>(transferableData)));
      if (logger.isDebugEnabled()) logger.debug("register '" + '0' + "' contains: \"" + processedText + "\"");
    }

    if (start != -1) {
      VimPlugin.getMark().setChangeMarks(editor, new TextRange(start, Math.max(end - 1, 0)));
    }

    return true;
  }

  @NotNull
  public List<TextBlockTransferableData> getTransferableData(@NotNull Editor editor,
                                                              @NotNull TextRange textRange,
                                                              String text) {
    final List<TextBlockTransferableData> transferableDatas = new ArrayList<>();
    final Project project = editor.getProject();
    if (project == null) return new ArrayList<>();

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return new ArrayList<>();
    DumbService.getInstance(project).withAlternativeResolveEnabled(() -> {
      for (CopyPastePostProcessor<? extends TextBlockTransferableData> processor : CopyPastePostProcessor.EP_NAME
        .getExtensionList()) {
        try {
          transferableDatas.addAll(processor.collectTransferableData(file, editor, textRange.getStartOffsets(), textRange.getEndOffsets()));
        }
        catch (IndexNotReadyException ignore) {
        }
      }
    });
    transferableDatas.add(new CaretStateTransferableData(new int[]{0}, new int[]{text.length()}));

    // These data provided by {@link com.intellij.openapi.editor.richcopy.TextWithMarkupProcessor} doesn't work with
    //   IdeaVim and I don't see a way to fix it
    // See https://youtrack.jetbrains.com/issue/VIM-1785
    // See https://youtrack.jetbrains.com/issue/VIM-1731
    transferableDatas.removeIf(it -> (it instanceof RtfTransferableData) || (it instanceof HtmlTransferableData));
    return transferableDatas;
  }

  private String preprocessText(@NotNull Editor editor, @NotNull TextRange textRange, String text, List<TextBlockTransferableData> transferableDatas) {
    final Project project = editor.getProject();
    if (project == null) return text;

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return text;
    String rawText = TextBlockTransferable.convertLineSeparators(text, "\n", transferableDatas);
    String escapedText;
    for (CopyPastePreProcessor processor : CopyPastePreProcessor.EP_NAME.getExtensionList()) {
      escapedText = processor.preprocessOnCopy(file, textRange.getStartOffsets(), textRange.getEndOffsets(), rawText);
      if (escapedText != null) {
        return escapedText;
      }
    }
    return text;
  }

  private boolean isSmallDeletionSpecialCase(Editor editor) {
    Command currentCommand = CommandState.getInstance(editor).getCommand();
    if (currentCommand != null) {
      Argument argument = currentCommand.getArgument();
      if (argument != null) {
        Command motionCommand = argument.getMotion();
        EditorActionHandlerBase action = motionCommand.getAction();
        return action instanceof MotionPercentOrMatchAction || action instanceof MotionSentencePreviousStartAction
          || action instanceof MotionSentenceNextStartAction || action instanceof MotionGotoFileMarkAction
          || action instanceof SearchEntryFwdAction || action instanceof SearchEntryRevAction
          || action instanceof SearchAgainNextAction || action instanceof SearchAgainPreviousAction
          || action instanceof MotionParagraphNextAction || action instanceof MotionParagraphPreviousAction;
      }
    }

    return false;
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  @Nullable
  public Register getLastRegister() {
    return getRegister(lastRegister);
  }

  @Nullable
  public Register getPlaybackRegister(char r) {
    if (PLAYBACK_REGISTER.indexOf(r) != 0) {
      return getRegister(r);
    }
    else {
      return null;
    }
  }

  @Nullable
  public Register getRegister(char r) {
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(r)) {
      r = Character.toLowerCase(r);
    }
    return CLIPBOARD_REGISTERS.contains(r) ? refreshClipboardRegister(r) : registers.get(r);
  }

  /**
   * Gets the last register name selected by the user
   *
   * @return The register name
   */
  public char getCurrentRegister() {
    return lastRegister;
  }

  /**
   * The register key for the default register.
   */
  public char getDefaultRegister() {
    return defaultRegister;
  }

  @NotNull
  public List<Register> getRegisters() {
    final List<Register> res = new ArrayList<>(registers.values());
    for (Character r : CLIPBOARD_REGISTERS) {
      final Register register = refreshClipboardRegister(r);
      if (register != null) {
        res.add(register);
      }
    }
    res.sort(new Register.KeySorter());
    return res;
  }

  public boolean startRecording(Editor editor, char register) {
    if (RECORDABLE_REGISTER.indexOf(register) != -1) {
      CommandState.getInstance(editor).setRecording(true);
      recordRegister = register;
      recordList = new ArrayList<>();
      return true;
    }
    else {
      return false;
    }
  }

  public void recordKeyStroke(KeyStroke key) {
    if (recordRegister != 0 && recordList != null) {
      recordList.add(key);
    }
  }

  public void recordText(@NotNull String text) {
    if (recordRegister != 0 && recordList != null) {
      recordList.addAll(StringHelper.stringToKeys(text));
    }
  }

  public void setKeys(char register, @NotNull List<KeyStroke> keys) {
    registers.put(register, new Register(register, SelectionType.CHARACTER_WISE, keys));
  }

  public void finishRecording(Editor editor) {
    if (recordRegister != 0) {
      Register reg = null;
      if (Character.isUpperCase(recordRegister)) {
        reg = getRegister(recordRegister);
      }

      if (recordList != null) {
        if (reg == null) {
          reg = new Register(Character.toLowerCase(recordRegister), SelectionType.CHARACTER_WISE, recordList);
          registers.put(Character.toLowerCase(recordRegister), reg);
        }
        else {
          reg.addKeys(recordList);
        }
      }
      CommandState.getInstance(editor).setRecording(false);
    }

    recordRegister = 0;
  }

  public void saveData(@NotNull final Element element) {
    logger.debug("saveData");
    final Element registersElement = new Element("registers");
    for (Character key : registers.keySet()) {
      final Register register = registers.get(key);
      final Element registerElement = new Element("register");
      registerElement.setAttribute("name", String.valueOf(key));
      registerElement.setAttribute("type", Integer.toString(register.getType().getValue()));
      final String text = register.getText();
      if (text != null) {
        final Element textElement = new Element("text");
        StringHelper.setSafeXmlText(textElement, text);
        registerElement.addContent(textElement);
      }
      else {
        final Element keys = new Element("keys");
        final List<KeyStroke> list = register.getKeys();
        for (KeyStroke stroke : list) {
          final Element k = new Element("key");
          k.setAttribute("char", Integer.toString(stroke.getKeyChar()));
          k.setAttribute("code", Integer.toString(stroke.getKeyCode()));
          k.setAttribute("mods", Integer.toString(stroke.getModifiers()));
          keys.addContent(k);
        }
        registerElement.addContent(keys);
      }
      registersElement.addContent(registerElement);
    }

    element.addContent(registersElement);
  }

  public void readData(@NotNull final Element element) {
    logger.debug("readData");
    final Element registersElement = element.getChild("registers");
    if (registersElement != null) {
      final List<Element> registerElements = registersElement.getChildren("register");
      for (Element registerElement : registerElements) {
        final char key = registerElement.getAttributeValue("name").charAt(0);
        final Register register;
        final Element textElement = registerElement.getChild("text");
        final String typeText = registerElement.getAttributeValue("type");
        final SelectionType type = SelectionType.fromValue(Integer.parseInt(typeText));
        if (textElement != null) {
          final String text = StringHelper.getSafeXmlText(textElement);
          if (text != null) {
            register = new Register(key, type, text, Collections.emptyList());
          }
          else {
            register = null;
          }
        }
        else {
          final Element keysElement = registerElement.getChild("keys");
          final List<Element> keyElements = keysElement.getChildren("key");
          final List<KeyStroke> strokes = new ArrayList<>();
          for (Element keyElement : keyElements) {
            final int code = Integer.parseInt(keyElement.getAttributeValue("code"));
            final int modifiers = Integer.parseInt(keyElement.getAttributeValue("mods"));
            final char c = (char)Integer.parseInt(keyElement.getAttributeValue("char"));
            //noinspection MagicConstant
            strokes.add(c == KeyEvent.CHAR_UNDEFINED ?
                        KeyStroke.getKeyStroke(code, modifiers) :
                        KeyStroke.getKeyStroke(c));
          }
          register = new Register(key, type, strokes);
        }
        registers.put(key, register);
      }
    }
  }

  @Nullable
  private Register refreshClipboardRegister(char r) {
    final Pair<String, List<TextBlockTransferableData>> clipboardData = ClipboardHandler.getClipboardTextAndTransferableData();
    final Register currentRegister = registers.get(r);
    final String text = clipboardData.getFirst();
    final List<TextBlockTransferableData> transferableData = clipboardData.getSecond();
    if (text != null) {
      if (currentRegister != null && text.equals(currentRegister.getText())) {
        return currentRegister;
      }
      return new Register(r, guessSelectionType(text), text, transferableData);
    }
    return null;
  }

  @NotNull
  private SelectionType guessSelectionType(@NotNull String text) {
    if (text.endsWith("\n")) {
      return SelectionType.LINE_WISE;
    }
    else {
      return SelectionType.CHARACTER_WISE;
    }
  }
}
