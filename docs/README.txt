@NAME@ - Version @VERSION@

This plugin attempts to emulate the functionality of VIM within IDEA. It
actually emulates 'gvim' more than 'vim' since IDEA is a graphical IDE (of
course).


Installation

UNIX/Linux

Copy @NAME@.jar to $IDEA_HOME/plugins where IDEA_HOME is where IntelliJ IDEA
has been installed.

Copy vim.xml to $HOME/.IntelliJIdea/config/keymaps

Windows

Copy @NAME@.jar to %IDEA_HOME%/plugins where IDEA_HOME is where IntelliJ IDEA
has been installed.

Copy vim.xml to C:\Documents and Settings\<user>\.IntelliJIdea\config\keymaps


Starting

Once the files have been installed into their proper locations, start IDEA
normally and then perform the following steps. These only need to be done the
first time:

1) Select the "Tools" menu. There should be a new menu option labeled
   "VimPlugin Toggle". This should have a checkmark next to it. If not, please
   select this menu to check it. If this menu option is not available, you did
   not copy @NAME@.jar into the correct location.
2) Select the "Options|Keymaps" menu. There should be a new keymap labeled "vim"
   listed in the top listbox. If there is no such keymap listed you did not
   install the vim.xml file in the proper location. Please highlight "vim" and
   click on the "Set Active" button. Click "OK" to save these changes.

At this point you must use VIM keystrokes in all editors.


Disabling the @NAME@ Plugin

If you wish to disable the plugin, select the "Tools|VimPlugin Toggle" menu so
it is unchecked. You must also select "Options|Keymaps" and make a keymap other
than "vim" the active keymap. At this point IDEA will work with it's regular
keyboard shortcuts.


Changes to IDEA

Undo/Redo

The @NAME@ plugin uses it's own undo/redo functionality so it is important
that you use the standard VIM keys 'u' and 'Ctrl-R' for undo/redo instead of
the built in undo/redo. An exception might be if you wish to undo the creation
of a new class. For this you must select the Edit|Undo menu since @NAME@
doesn't support this feature. Using the built in undo/redo while editing a
file will result in strange behavior and you will most likely lose changes.

Escape

In IDEA, the Escape key is used during editing to cancel code completion
windows and parameter tooltips. While in VIM Insert mode, Escape is used to
return back to Normal mode. If you are typing in Insert mode and a code
completion window is popped up, pressing Escape will both cancel the window
and exit Insert mode. If a parameter tooltip appears, pressing Escape will not
make the tooltip go away whether in Insert or Normal mode. The only way to make
the tooltip disappear is to move the cursor outside of the parameter area of
the method call. (I would love to receive solutions for both of these issues.)

Menu Changes

In order to emulate the keystrokes used by VIM, several of the default hotkeys
used by IDEA had to be changed. Below is a list of IDEA menus, their default
keyboard shortcuts, and their new VIM keystrokes.

File
     Save All                 Ctrl-S              :w

Edit
     Undo                     Ctrl-Z              u
     Redo                     Ctrl-Shift-Z        Ctrl-R
     Cut                      Ctrl-X              "+x
     Copy                     Ctrl-C              "+y
     Paste                    Ctrl-V              "+P
     Select All               Ctrl-A              ggVG

Search
     Find                     Ctrl-F              /
     Replace                  Ctrl-R              :s
     Find Next                F3                  n
     Find Previous            Shift-F3            N

View
     Quick JavaDoc            Ctrl-Q              K
     Parameter Info           Ctrl-P              Ctrl-Shift-P
     Swap Panels              Ctrl-U              <None>
     Recent Files...          Ctrl-E              <None>
     Type Hierarchy           Ctrl-H              Ctrl-Alt-Shift-H

Goto
     Class...                 Ctrl-N              Alt-Shift-N
     Line...                  Ctrl-G              G
     Declaration              Ctrl-B              gd
     Super Method             Ctrl-U              Ctrl-Shift-U

Code
     Override Methods...      Ctrl-O              Ctrl-Shift-O
     Implement Methods...     Ctrl-I              Ctrl-Shift-I
     Complete Code                                (Only in Insert mode)
          Basic               Ctrl-Space          Ctrl-Space or Ctrl-N or Ctrl-P
          Smart Type          Ctrl-Shift-Space    Ctrl-Shift-Space
          Class Name          Ctrl-Alt-Space      Ctrl-Alt-Space
     Insert Live Template     Ctrl-J              Ctrl-]

Tools
     Version Control
          Check In Project    Ctrl-K              <None>


Summary of Supported/Unsupported VIM Features

Supported

Motion keys
Deletion/Changing
Insert mode commands
Marks
Registers
VIM undo/redo
Visual mode commands
Some Ex commands

Not Supported (yet)
Keymaps
Diagraphs
Various, lesser used (by me anyway), commands
Jumplists
macros
Window commands
Command line history
Search history
Search and Search/Replace still use the standard IDEA dialogs. This will be the
case until either the JDK regexp library supported backward searches or the
IDEA openapi provides access to their builtin search and replace.

Please see the file 'index.txt' for a complete list of supported, soon-to-be
supported, and never-to-be supported commands.
