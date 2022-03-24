/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
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
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.api.VimRegisterGroupBase;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.OptionChangeListener;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.ui.ClipboardHandler;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import kotlin.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This group works with command associated with copying and pasting text
 */
@State(name = "VimRegisterSettings", storages = {
  @Storage(value = "$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)
})
public class RegisterGroup extends VimRegisterGroupBase implements PersistentStateComponent<Element> {

  private static final Logger logger = Logger.getInstance(RegisterGroup.class);

  public RegisterGroup() {

    VimPlugin.getOptionService().addListener(
      OptionConstants.clipboardName,
      new OptionChangeListener<VimDataType>() {
        @Override
        public void processGlobalValueChange(@Nullable VimDataType oldValue) {
          String clipboardOptionValue = ((VimString) VimPlugin.getOptionService()
            .getOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.clipboardName, OptionConstants.clipboardName)).getValue();
          if (clipboardOptionValue.contains("unnamed")) {
            defaultRegister = '*';
          }
          else if (clipboardOptionValue.contains("unnamedplus")) {
            defaultRegister = '+';
          }
          else {
            defaultRegister = UNNAMED_REGISTER;
          }
          lastRegister = defaultRegister;
        }
      },
      true
    );
  }

  /**
   * Stores text, character wise, in the given special register
   *
   * <p>This method is intended to support writing to registers when the text cannot be yanked from an editor. This is
   * expected to only be used to update the search and command registers. It will not update named registers.</p>
   *
   * <p>While this method allows setting the unnamed register, this should only be done from tests, and only when it's
   * not possible to yank or cut from the fixture editor. This method will skip additional text processing, and won't
   * update other registers such as the small delete register or reorder the numbered registers. It is much more
   * preferable to yank from the fixture editor.</p>
   *
   * @param register  The register to use for storing the text. Cannot be a normal text register
   * @param text      The text to store, without further processing
   * @return          True if the text is stored, false if the passed register is not supported
   */
  public boolean storeTextSpecial(char register, @NotNull String text) {
    if (READONLY_REGISTERS.indexOf(register) == -1 && register != LAST_SEARCH_REGISTER
        && register != UNNAMED_REGISTER) {
      return false;
    }
    myRegisters.put(register, new Register(register, SelectionType.CHARACTER_WISE, text, new ArrayList<>()));
    if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + text + "\"");
    return true;
  }

  @Override
  public @NotNull List<?> getTransferableData(@NotNull VimEditor vimEditor, @NotNull TextRange textRange, @NotNull String text) {
    Editor editor = ((IjVimEditor)vimEditor).getEditor();
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

  @Override
  public @NotNull String preprocessText(@NotNull VimEditor vimEditor,
                                        @NotNull TextRange textRange,
                                        @NotNull String text,
                                        @NotNull List<?> transferableData) {
    Editor editor = ((IjVimEditor)vimEditor).getEditor();
    final Project project = editor.getProject();
    if (project == null) return text;

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return text;
    String rawText = TextBlockTransferable.convertLineSeparators(text, "\n",
                                                                 (Collection<? extends TextBlockTransferableData>)transferableData);


    if (VimPlugin.getOptionService().isSet(OptionScope.GLOBAL.INSTANCE, OptionConstants.ideacopypreprocessName, OptionConstants.ideacopypreprocessName)) {
      String escapedText;
      for (CopyPastePreProcessor processor : CopyPastePreProcessor.EP_NAME.getExtensionList()) {
        escapedText = processor.preprocessOnCopy(file, textRange.getStartOffsets(), textRange.getEndOffsets(), rawText);
        if (escapedText != null) {
          return escapedText;
        }
      }
    }


    return text;
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  public @Nullable Register getLastRegister() {
    return getRegister(lastRegister);
  }

  public @Nullable Register getPlaybackRegister(char r) {
    if (PLAYBACK_REGISTERS.indexOf(r) != 0) {
      return getRegister(r);
    }
    else {
      return null;
    }
  }

  public @Nullable Register getRegister(char r) {
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(r)) {
      r = Character.toLowerCase(r);
    }
    return CLIPBOARD_REGISTERS.indexOf(r) >= 0 ? refreshClipboardRegister(r) : myRegisters.get(r);
  }

  public void saveRegister(char r, Register register) {
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(r)) {
      r = Character.toLowerCase(r);
    }
    if (CLIPBOARD_REGISTERS.indexOf(r) >= 0) {
      String text = register.getText();
      String rawText = register.getRawText();
      if (text != null && rawText != null) {
        VimInjectorKt.getInjector()
          .getClipboardManager()
          .setClipboardText(text, rawText, new ArrayList<>(register.getTransferableData()));
      }
    }
    myRegisters.put(r, register);
  }

  public @NotNull List<Register> getRegisters() {
    final List<Register> res = new ArrayList<>(myRegisters.values());
    for (int i = 0; i < CLIPBOARD_REGISTERS.length(); i++) {
      final char r = CLIPBOARD_REGISTERS.charAt(i);
      final Register register = refreshClipboardRegister(r);
      if (register != null) {
        res.add(register);
      }
    }
    res.sort(Register.KeySorter.INSTANCE);
    return res;
  }

  public boolean startRecording(Editor editor, char register) {
    if (RECORDABLE_REGISTERS.indexOf(register) != -1) {
      CommandState.getInstance(new IjVimEditor(editor)).setRecording(true);
      recordRegister = register;
      recordList = new ArrayList<>();
      return true;
    }
    else {
      return false;
    }
  }

  public void recordText(@NotNull String text) {
    if (recordRegister != 0 && recordList != null) {
      recordList.addAll(StringHelper.stringToKeys(text));
    }
  }

  public void setKeys(char register, @NotNull List<KeyStroke> keys) {
    myRegisters.put(register, new Register(register, SelectionType.CHARACTER_WISE, keys));
  }

  public void setKeys(char register, @NotNull List<KeyStroke> keys, SelectionType type) {
    myRegisters.put(register, new Register(register, type, keys));
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
          myRegisters.put(Character.toLowerCase(recordRegister), reg);
        }
        else {
          reg.addKeys(recordList);
        }
      }
      CommandState.getInstance(new IjVimEditor(editor)).setRecording(false);
    }

    recordRegister = 0;
  }

  public void saveData(final @NotNull Element element) {
    logger.debug("Save registers data");
    final Element registersElement = new Element("registers");
    if (logger.isTraceEnabled()) {
      logger.trace("Saving " + myRegisters.size() + " registers");
    }
    for (Character key : myRegisters.keySet()) {
      final Register register = myRegisters.get(key);
      if (logger.isTraceEnabled()) {
        logger.trace("Saving register '" + key + "'");
      }
      final Element registerElement = new Element("register");
      registerElement.setAttribute("name", String.valueOf(key));
      registerElement.setAttribute("type", Integer.toString(register.getType().getValue()));
      final String text = register.getText();
      if (text != null) {
        logger.trace("Save register as 'text'");
        final Element textElement = new Element("text");
        StringHelper.setSafeXmlText(textElement, text);
        registerElement.addContent(textElement);
      }
      else {
        logger.trace("Save register as 'keys'");
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
    logger.debug("Finish saving registers data");
  }

  public void readData(final @NotNull Element element) {
    logger.debug("Read registers data");
    final Element registersElement = element.getChild("registers");
    if (registersElement != null) {
      logger.trace("'registers' element is not null");
      final List<Element> registerElements = registersElement.getChildren("register");
      if (logger.isTraceEnabled()) {
        logger.trace("Detected " + registerElements.size() + " register elements");
      }
      for (Element registerElement : registerElements) {
        final char key = registerElement.getAttributeValue("name").charAt(0);
        if (logger.isTraceEnabled()) {
          logger.trace("Read register '" + key + "'");
        }
        final Register register;
        final Element textElement = registerElement.getChild("text");
        final String typeText = registerElement.getAttributeValue("type");
        final SelectionType type = SelectionType.fromValue(Integer.parseInt(typeText));
        if (textElement != null) {
          logger.trace("Register has 'text' element");
          final String text = StringHelper.getSafeXmlText(textElement);
          if (text != null) {
            logger.trace("Register data parsed");
            register = new Register(key, type, text, Collections.emptyList());
          }
          else {
            logger.trace("Cannot parse register data");
            register = null;
          }
        }
        else {
          logger.trace("Register has 'keys' element");
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
        logger.trace("Save register to vim registers");
        myRegisters.put(key, register);
      }
    }
    logger.debug("Finish reading registers data");
  }

  private @Nullable Register refreshClipboardRegister(char r) {
    final Pair<String, List<TextBlockTransferableData>> clipboardData = ClipboardHandler.getClipboardTextAndTransferableData();
    if (clipboardData == null) return null;
    final Register currentRegister = myRegisters.get(r);
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

  private @NotNull SelectionType guessSelectionType(@NotNull String text) {
    if (text.endsWith("\n")) {
      return SelectionType.LINE_WISE;
    }
    else {
      return SelectionType.CHARACTER_WISE;
    }
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("registers");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }
}
