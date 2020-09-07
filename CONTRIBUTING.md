[![TeamCity Build][teamcity-build-status-svg]][teamcity-build-status]

IdeaVim is an open source project created by 60+ contributors. Would you like to make it even better? That’s wonderful!

This page is created to help you start contributing. And who knows, maybe in a few days this project will be brighter than ever!

## Before you begin

- The project is written in Kotlin and Java. Choose whichever language you feel more comfortable with,
or maybe one that you’d like to get to know better (why not start [learning Kotlin](https://kotlinlang.org/docs/tutorials/) right now?).

- If you come across some IntelliJ Platform code, these links may prove helpful:

    * [IntelliJ architectural overview](https://www.jetbrains.org/intellij/sdk/docs/platform/fundamentals.html)
    * [IntelliJ plugin development resources](https://www.jetbrains.org/intellij/sdk/docs/welcome.html)

- Having any difficulties?
Join the brand new
[![Join the chat at https://gitter.im/JetBrains/ideavim](https://badges.gitter.im/JetBrains/ideavim.svg)](https://gitter.im/JetBrains/ideavim?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
for IdeaVim developers and contributors! 

OK, ready to do some coding?

## Yes, I'm ready for some coding

* Fork the repository and clone it to the local machine.
* Open the project with IntelliJ IDEA.

Yoo hoo! You’re all set to begin contributing. Here are some useful gradle commands:

* `./gradlew runIde` — start the dev version of IntelliJ IDEA with IdeaVim installed.
* `./gradlew test` — run tests.
* `./gradlew buildPlugin` — build the plugin. The result will be located in `build/distributions`. This file can be
installed by using `Settings | Plugin | >Gear Icon< | Install Plugin from Disk...`. You can stay with your personal build
for a few days or send it to a friend for testing.

## Warmup

 - Pick a few relatively simple tasks that are tagged with 
[#patch_welcome](https://youtrack.jetbrains.com/issues/VIM?q=%23patch_welcome%20%23Unresolved%20sort%20by:%20votes%20)
 in the issue tracker.
 - Read the javadoc for the `@VimBehaviorDiffers` annotation in the source code and fix the corresponding functionality.
 - Implement one of the requested [#vim plugin](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved%20tag:%20%7Bvim%20plugin%7D%20sort%20by:%20votes%20)s.

> :small_orange_diamond: Selected an issue to work on? Leave a comment in a YouTrack ticket or create a draft PR
> to indicate that you've started working on it so that you might get additional guidance and feedback from the maintainers.

## Where to start in the codebase

If you are looking for:

- Vim commands (`w`, `<C-O>`, `p`, etc.):
    - Any particular command: `package-info.java`.
    - How commands are executed in common: `EditorActionHandlerBase`.
    - Key mapping: `KeyHandler.handleKey()`.

- Ex commands (`:set`, `:s`, `:nohlsearch`):
    - Any particular ex command: package `com.maddyhome.idea.vim.ex.handler`.
    - Ex command executor: `CommandHandler`.

- Extensions:
    - Extensions handler: `VimExtensionHandler`.
    - Available extensions: package `com/maddyhome/idea/vim/extension`.

- Common features:
    - State machine. How every particular keystroke is parsed in IdeaVim: `KeyHandler.handleKey()`.
    - Options (`incsearch`, `iskeyword`, `relativenumber`): `OptionsManager`.
    - Plugin startup: `PluginStartup`.
    - Notifications: `NotificationService`.
    - Status bar icon: `StatusBar.kt`.
    - On/off switch: `VimPlugin.setEnabled()`.


## Testing

Here are some guides for testing:

1. Read the javadoc for the `@VimBehaviorDiffers` annotation in the source code.

2. Please avoid senseless text like "dhjkwaldjwa", "asdasdasd", "123 123 123 123", etc. Try to choose an example
text that is easy to read and understand what is wrong if the test fails. For example, take a few lines from your
favorite poem, or use Vladimir Nabokov’s “A Discovery" if you don't have one.

3. Don't forget to test your functionality with line start, line end, file start, file end, empty line, multiple
carets, dollar motion, etc.
   
##### Neovim
IdeaVim has an experimental integration with neovim in tests. Tests that are performed with `doTest` also executed in
neovim instance, and the state of IdeaVim is asserted to be the same as the state of neovim.
- Only tests that use `doTest` are checked with neovim.
- Tests with `@VimBehaviorDiffers` or `@TestWithoutNeovim` annotations don't use neovim.

#### Property-based tests
Property-based tests are located under `propertybased` package. These tests a flaky by nature
although in most cases they are stable. If the test fails on your TeamCity run, try to check the test output and understand 
if the fail is caused by your changes. If it's not, just ignore the test.


## A common direction

We’re trying to make IdeaVim close to the original Vim both in terms of functionality and architecture.

- Vim motions can be [either inclusive, exclusive, or linewise](http://vimdoc.sourceforge.net/htmldoc/motion.html#inclusive).
In IdeaVim, you can use `MotionType` for that.
- Have you read the [interesting things](https://github.com/JetBrains/ideavim#some-facts-about-vim) about IdeaVim?
Do you remember how `dd`, `yy`, and other similar commands work? `DuplicableOperatorAction` will help you with that.
And we also translate it to `d_` and `y_`: `KeyHandler.mapOpCommand()`.
- All IdeaVim extensions use the same command names as the originals (e.g. `<Plug>(CommentMotion)`, `<Plug>ReplaceWithRegisterLine`),
so you can reuse your `.vimrc` settings. 
We also support proper command mappings (functions are mapped to `<Plug>...`), the operator function (`OperatorFunction`), and so on.
- Magic is supported as well. See `Magic`.

-----

### I read the whole page but something is still unclear.

Oh no! No cookies for the maintainers today! Please [tell us](https://github.com/JetBrains/ideavim#contact-maintainers) about it so we can help.


### I’ve found a bug in this documentation.

No beer in the bar for us unless it's fixed. [Let us know](https://github.com/JetBrains/ideavim#contact-maintainers) situation so we might be able to fix it.


### The lack of documentation or a javadoc/ktdoc makes it difficult to start contributing.

This is just terrible. [You know what to do](https://github.com/JetBrains/ideavim#contact-maintainers).

### Resources:

* [Continuous integration builds](https://teamcity.jetbrains.com/project.html?projectId=IdeaVim&guest=1)
* [Bug tracker](https://youtrack.jetbrains.com/issues/VIM)
* [Chat on gitter](https://gitter.im/JetBrains/ideavim)
* [Unofficial discord server](https://jb.gg/bi6zp7)
* [Plugin homepage](https://plugins.jetbrains.com/plugin/164-ideavim)
* [Changelog](CHANGES.md)
* [Contributors listing](AUTHORS.md)

[teamcity-build-status]: https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20201&guest=1
[teamcity-build-status-svg]: https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20201)/statusIcon.svg?guest=1
