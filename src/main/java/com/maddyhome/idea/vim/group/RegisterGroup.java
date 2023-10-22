/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.state.mode.SelectionType;
import com.maddyhome.idea.vim.register.Register;
import com.maddyhome.idea.vim.register.VimRegisterGroupBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
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
      registerElement.setAttribute("type", register.getType().name());
      final String text = register.getText();
      if (text != null) {
        logger.trace("Save register as 'text'");
        final Element textElement = new Element("text");
        VimPlugin.getXML().setSafeXmlText(textElement, text);
        registerElement.addContent(textElement);
      } else {
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
        SelectionType type;
        try {
          type = SelectionType.valueOf(typeText);
        } catch (IllegalArgumentException e) {
          // This whole `if` keeps compatibility with the mode when SelectionType had numbers
          if (Integer.toString(1 << 1).equals(typeText)) {
            type = SelectionType.CHARACTER_WISE;
          } else if (Integer.toString(1 << 2).equals(typeText)) {
            type = SelectionType.LINE_WISE;
          } else if (Integer.toString(1 << 3).equals(typeText)) {
            type = SelectionType.BLOCK_WISE;
          } else {
            type = SelectionType.CHARACTER_WISE;
          }
        }
        if (textElement != null) {
          logger.trace("Register has 'text' element");
          final String text = VimPlugin.getXML().getSafeXmlText(textElement);
          if (text != null) {
            logger.trace("Register data parsed");
            register = new Register(key, type, text, Collections.emptyList());
          } else {
            logger.trace("Cannot parse register data");
            register = null;
          }
        } else {
          logger.trace("Register has 'keys' element");
          final Element keysElement = registerElement.getChild("keys");
          final List<Element> keyElements = keysElement.getChildren("key");
          final List<KeyStroke> strokes = new ArrayList<>();
          for (Element keyElement : keyElements) {
            final int code = Integer.parseInt(keyElement.getAttributeValue("code"));
            final int modifiers = Integer.parseInt(keyElement.getAttributeValue("mods"));
            final char c = (char) Integer.parseInt(keyElement.getAttributeValue("char"));
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
