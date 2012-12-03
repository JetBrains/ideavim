IdeaVim
=======

IdeaVim is a Vim emulation plug-in for IDEs based on the IntelliJ platform.
IdeaVim can be used with IntelliJ IDEA, RubyMine, PyCharm, PhpStorm, WebStorm
and AppCode.

Resources:

* [Plugin homepage](http://plugins.intellij.net/plugin/?id=164)
* [Changelog](https://github.com/JetBrains/ideavim/blob/master/CHANGES.md)
* [Continuous integration builds](http://teamcity.jetbrains.com/project.html?projectId=project55)


Installation
------------

Use the IDE's plugin manager to install the latest version of the plugin.
Start the IDE normally and enable the Vim emulation using "Tools | VIM
Emulator" menu item. At this point you must use Vim keystrokes in all editors.

If you wish to disable the plugin, select the "Tools | VIM Emulator" menu so
it is unchecked. At this point IDE will work with it's regular keyboard
shortcuts.


Summary of Supported Vim Features
---------------------------------

Supported:

* Motion keys
* Deletion/Changing
* Insert mode commands
* Marks
* Registers
* Undo/redo
* Visual mode commands
* Some Ex commands
* Some [:set options](https://github.com/JetBrains/ideavim/blob/master/doc/set-commands.md)
* Full Vim regexps for search and search/replace
* Macros
* Digraphs
* Command line and search history
* Vim web help

Not supported (yet):

* Key mappings
* Various less used commands
* Jump lists
* Window commands

Please see the file [index.txt](https://github.com/JetBrains/ideavim/blob/master/index.txt)
for a list of commands covered with tests.


Changes to the IDE
------------------

### Undo/Redo

The IdeaVim plugin uses the undo/redo functionality of the IntelliJ platform,
so the behaviour of the `u` and `<C-R>` commands may differ from the original
Vim. Vim compatibility of undo/redo may be improved in the future releases.

### Escape

In the IDE, the `<Esc>` key is used during editing to cancel code completion
windows, dialog windows, and parameter tooltips. While in the Vim insert mode,
`<Esc>` is used to return back to the normal mode. If you are typing in the
insert mode and a code completion window is popped up, pressing `<Esc>` cancel
the window without exiting the insert mode.

Using `<Esc>` in dialog windows remains problematic. For most dialog windows
the Vim emulator is put into the insert mode without the possibility to switch to
the normal mode. In some dialog windows the normal mode is on by default. The
usage of the Vim emulator in dialog windows is an area for improvements.

### Menu Changes

In order to emulate the keystrokes used by Vim, several of the default hotkeys
used by the IDE had to be changed. Below is a list of IDE menus, their default
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


Authors
-------

See [AUTHORS.md](https://github.com/JetBrains/ideavim/blob/master/AUTHORS.md)
for a list of authors and contributors.


License
-------

IdeaVim is licensed under the terms of the GNU Public license version 2.

