package com.maddyhome.idea.vim;

import javax.swing.*;

/**
 * @author dhleong
 */
public interface GetCharListener {

  void onCharTyped(KeyStroke key, char chKey);
}
