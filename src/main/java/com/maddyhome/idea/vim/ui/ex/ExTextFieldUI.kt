/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import javax.swing.plaf.basic.BasicTextFieldUI
import javax.swing.text.JTextComponent
import javax.swing.text.Position
import javax.swing.text.View

internal class ExTextFieldUI(private val editorKit: ExEditorKit) : BasicTextFieldUI() {
  override fun getEditorKit(tc: JTextComponent?) = editorKit

  override fun installKeyboardActions() {
    // Install the default Keymap, but don't load InputMap or ActionMap. This gives us the default action handler which
    // accepts typed characters, but no other actions.
    // The difference between Keymap and InputMap/ActionMap is a little confusing. Keymap is an older way of mapping
    // keystrokes to actions. It's specific to text components and has essentially been superseded by InputMap and
    // ActionMap, which are more general and apply to other components too. InputMap maps keystrokes to action names,
    // and ActionMap maps action names to actions. Replacing key bindings is now easier because you can just replace
    // InputMap without changing the list of available actions in ActionMap. Furthermore, InputMap and ActionMap have a
    //  hierarchy to allow for additional or replaced key bindings or actions.
    // Keymap is still very important, though, because it also maintains the default action handler. If there is no
    // matching action for a keystroke, and if it's a typed key (i.e., has a valid keyChar), then the default action
    // handler is invoked, and this modifies the text component's content. Without this default action handler, the
    // component doesn't change.
    // To marry the two different sets of maps, the Keymap is wrapped as an InputMap and added to the hierarchy. So if
    // a keystroke doesn't exist in the InputMap, it eventually gets to the Keymap's default action.
    // This method is responsible for loading the Keymap, InputMap and ActionMap. The Keymap is a shared, named instance
    // that is empty apart from the default action handler. We could override it in createKeymap(). The InputMap is
    // loaded from Swing defaults, and we can't override it without changing the property prefix and requiring a whole
    // new set of defaults. The ActionMap is a combination of the text component's getActions() (which defers to the
    // editor kit's list of default actions) and various hardcoded actions like cut, copy and paste. Even if we overrode
    // the component's getActions method, we'd have several hardcoded actions that we don't need.
    // To get an empty InputMap and ActionMap, it's easier to just not call the super class and rely on defaults. We
    // still need to set the Keymap, though, to make sure we have the default action handler. (And without setting the
    // keymap, it doesn't install itself into the InputMap chain).
    // (The UndoManager unexpectedly adds a couple of custom actions, but there's little we can do about that)
    component.keymap = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP)
  }

  fun setPromptCharacter(promptCharacter: Char) {
    // We only ever want one prompt view, so remove it if it's there. Alternatively, we could find the current prompt
    // and update it
    clearPromptCharacter()

    val paragraphView = getParagraphView()

    val caretOffset = component.caret.dot
    val childViewIndex = paragraphView.getViewIndex(caretOffset, Position.Bias.Forward)
    val childView = paragraphView.getView(childViewIndex) ?: return  // Unexpected

    val promptView = ExFakePromptView(promptCharacter, childView.element, caretOffset)

    when {
      childView.startOffset == caretOffset -> {
        // Insert before the current view
        paragraphView.insert(childViewIndex, promptView)
      }

      childView.endOffset - 1 == caretOffset -> { // End offset is exclusive
        // Append to the current view (insert before the next view)
        paragraphView.insert(childViewIndex + 1, promptView)
      }

      else -> {
        // Careful! Fragments are dangerous. Normal views match 1:1 with an element, and defer the start/end offset to
        // it. Fragments have specified start/end offsets that don't update with changes to the document. This can lead
        // to "stale view" exceptions, e.g. by trying to calculate the preferred span of a GlyphView after deleting
        // text. There doesn't seem to be a good way to guard against this, especially when it can be called
        // non-deterministically to update the flashing caret.
        val pre = childView.createFragment(childView.startOffset, caretOffset)  // End offset is exclusive
        val post = childView.createFragment(caretOffset, childView.endOffset)
        paragraphView.replace(childViewIndex, 1, arrayOf(pre, promptView, post))
      }
    }

    paragraphView.preferenceChanged(paragraphView, true, false)
    component.repaint()
  }

  fun clearPromptCharacter() {
    val paragraphView = getParagraphView()

    for (i in 0 until paragraphView.viewCount) {
      val childView = paragraphView.getView(i)
      if (childView is ExFakePromptView) {

        // Fragments are very helpful for inserting an additional view, but can be problematic when the document is
        // edited, so replace consecutive fragments with a simple view
        if (i > 0 && i < paragraphView.viewCount - 1
          && paragraphView.getView(i - 1).element == childView.element
          && paragraphView.getView(i + 1).element == childView.element) {

          val viewFactory = childView.viewFactory
          val elementView = viewFactory.create(childView.element)
          paragraphView.replace(i - 1, 3, arrayOf(elementView))
        }
        else {
          paragraphView.remove(i)
        }

        paragraphView.preferenceChanged(paragraphView, true, false)
        component.repaint()

        // There is (hopefully) only one PromptView and removing while iterating could be awkward
        break
      }
    }
  }

  private fun getParagraphView(): View {
    val rootView = getRootView(component)
    val sectionView = rootView.getView(0) // Only one section (ScrollingInlineCompositeView)
    return sectionView.getView(0)  // Only one paragraph (InlineCompositeView)
  }
}
