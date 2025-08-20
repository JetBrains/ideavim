[![TeamCity Build][teamcity-build-status-svg]][teamcity-build-status]

IdeaVim is an open source project created by 130+ contributors. Would you like to make it even better? That’s wonderful!

This page is created to help you start contributing. And who knows, maybe in a few days this project will be brighter than ever!

# Awards for Quality Contributions

In February 2025, we’re starting a program to award one-year All Products Pack subscriptions to the implementers of quality contributions to the IdeaVim project. The program will continue for all of 2025 and may be prolonged.

Subscriptions can be awarded for merged pull requests that meet the following requirements:


- The change should be non-trivial, though there might be exceptions — for example, where a trivial fix requires a complicated investigation.
- The change should fully implement a feature or fix the root cause of a bug. Workarounds or hacks are not accepted.
- If applicable, the change should be properly covered with unit tests.
- The work should be performed by the contributor, though the IdeaVim team is happy to review it and give feedback.
- The change should fix an issue or implement a feature filed by another user. If you want to file an issue and provide a solution to it, your request for a license should be explicitly discussed with the IdeaVim team in the ticket comments.


We'd like to make sure this award program is helpful and fair. Since we just started it and still fine-tuning the details, the final say on giving licenses remains with the IdeaVim team and the requirements might evolve over time.


Also, a few notes:


- If you have any doubts about whether your change or fix is eligible for the award, get in touch with us in the comments on YouTrack or in any other way.
- Please mention this program in the pull request text. This is not an absolute requirement, but it will help ensure we know you would like to be considered for an award, but this is not required.
- During 2025, a single person may only receive a single subscription. Even if you make multiple contributions, you will not be eligible for multiple awards.
- Any delays caused by the IdeaVim team will not affect eligibility for an award if the other requirements are met.
- Draft pull requests will not be reviewed unless explicitly requested.
- Tickets with the [ideavim-bounty](https://youtrack.jetbrains.com/issues?q=tag:%20%7BIdeaVim-bounty%7D) tag are good candidates for this award.


## Before you begin

- The project is primarily written in Kotlin with a few Java files. When contributing to the project, use Kotlin unless
you’re working in areas where Java is explicitly used.

- If you come across some IntelliJ Platform code, these links may prove helpful:

    * [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
    * [IntelliJ architectural overview](https://plugins.jetbrains.com/docs/intellij/fundamentals.html)
    * [IntelliJ Platform community space](https://platform.jetbrains.com/)

- Having any difficulties?
Ask any questions in [GitHub discussions](https://github.com/JetBrains/ideavim/discussions) or [IntelliJ Platform community space](https://platform.jetbrains.com/).

OK, ready to do some coding?

## Yes, I'm ready for some coding

* Fork the repository and clone it to the local machine.
* Open the project with IntelliJ IDEA.

Yoo hoo! You’re all set to begin contributing.
We've prepared some useful configurations for you:

- `Start IJ with IdeaVim`
- `IdeaVim tests`
- `IdeaVim full verification`

![Prepared configurations light](assets/contributing/configs-light.png#gh-light-mode-only)![Prepared configurations dark](assets/contributing/configs-dark.png#gh-dark-mode-only)

And here are useful gradle commands:

* `./gradlew runIde` — start the dev version of IntelliJ IDEA with IdeaVim installed.
* `./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test` — run tests.
* `./gradlew buildPlugin` — build the plugin. The result will be located in `build/distributions`. This file can be
installed by using `Settings | Plugin | >Gear Icon< | Install Plugin from Disk...`. You can stay with your personal build
for a few days or send it to a friend for testing.

## Warmup

 - Pick a few relatively simple tasks that are tagged with 
[#patch_welcome](https://youtrack.jetbrains.com/issues/VIM?q=%23patch_welcome%20%23Unresolved%20sort%20by:%20votes%20)
 in the issue tracker.
 - Read the javadoc for the `@VimBehaviorDiffers` annotation in the source code and fix the corresponding functionality.
 - Implement one of the requested [#vim plugin](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved%20tag:%20%7Bvim%20plugin%7D%20sort%20by:%20votes%20)s.

> :small_orange_diamond: You may leave a comment in the YouTrack ticket or open a draft PR if you’d like early feedback
> or want to let maintainers know you’ve started working on an issue. Otherwise, simply open a PR.

## Where to start in the codebase

If you are looking for:

- Vim commands (`w`, `<C-O>`, `p`, etc.):
    - Any particular command:
      - [Commands common for Fleet and IdeaVim](vim-engine/src/main/resources/ksp-generated/engine_commands.json)
      - [IdeaVim only commands](src/main/resources/ksp-generated/intellij_commands.json)
    - How commands are executed in common: `EditorActionHandlerBase`.
    - Key mapping: `KeyHandler.handleKey()`.

- Ex commands (`:set`, `:s`, `:nohlsearch`):
    - Any particular command:
        - [Commands common for Fleet and IdeaVim](vim-engine/src/main/resources/ksp-generated/engine_ex_commands.json)
        - [IdeaVim only commands](src/main/resources/ksp-generated/intellij_ex_commands.json)
    - Vim script grammar: `Vimscript.g4`.
    - Vim script parsing: package `com.maddyhome.idea.vim.vimscript.parser`.
    - Vim script executor: `Executor`.

- Extensions:
    - Extensions handler: `VimExtensionHandler`.
    - Available extensions: package `com/maddyhome/idea/vim/extension`.

- Common features:
    - State machine. How every particular keystroke is parsed in IdeaVim: `KeyHandler.handleKey()`.
    - Options (`incsearch`, `iskeyword`, `relativenumber`): `VimOptionGroup`.
    - Plugin startup: `PluginStartup`.
    - Notifications: `NotificationService`.
    - Status bar icon: `StatusBar.kt`.
    - On/off switch: `VimPlugin.setEnabled()`.


## Testing

Here are some guides for testing:

1. Read the javadoc for the `@VimBehaviorDiffers` annotation in the source code.

2. Please avoid senseless text like "dhjkwaldjwa", "asdasdasd", "123 123 123 123", etc. Use a few lines of code or
the following template:
```text
Lorem Ipsum

Lorem ipsum dolor sit amet,
consectetur adipiscing elit
Sed in orci mauris.
Cras id tellus in ex imperdiet egestas.
```

3. Don't forget to test your functionality with line start, line end, file start, file end, empty line, multiple
carets, dollar motion, etc.
   
##### Neovim
IdeaVim has an integration with neovim in tests. Tests that are performed with `doTest` also executed in
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


## Fleet

The IdeaVim plugin is divided into two main modules: IdeaVim and vim-engine.
IdeaVim serves as a plugin for JetBrains IDEs, while vim-engine is an IntelliJ Platform-independent Vim engine.
This engine is utilized in both the Vim plugin for Fleet and IdeaVim.

If you develop a plugin that depends on IdeaVim: We have an instrument to check that our changes don't affect
the plugins in the marketplace.
If you still encounter any issues with the newer versions of IdeaVim, please [contact maintainers](https://github.com/JetBrains/ideavim#contact-maintainers).


-----

### I read the whole page but something is still unclear.

Oh no! No cookies for the maintainers today! Please [tell us](https://github.com/JetBrains/ideavim#contact-maintainers) about it so we can help.


### I’ve found a bug in this documentation.

No beer in the bar for us unless it's fixed. [Let us know](https://github.com/JetBrains/ideavim#contact-maintainers) situation so we might be able to fix it.


### The lack of documentation or a javadoc/ktdoc makes it difficult to start contributing.

This is just terrible. [You know what to do](https://github.com/JetBrains/ideavim#contact-maintainers).

### Resources:

* [Continuous integration builds](https://ideavim.teamcity.com/)
* [Bug tracker](https://youtrack.jetbrains.com/issues/VIM)
* [IntelliJ Platform community space](https://platform.jetbrains.com/)
* [Chat on gitter](https://gitter.im/JetBrains/ideavim)
* [IdeaVim Channel](https://jb.gg/bi6zp7) on [JetBrains Server](https://discord.gg/jetbrains)
* [Plugin homepage](https://plugins.jetbrains.com/plugin/164-ideavim)
* [Changelog](CHANGES.md)
* [Contributors listing](AUTHORS.md)

[teamcity-build-status]: https://ideavim.teamcity.com/viewType.html?buildTypeId=Ideavim_IdeaVimTests_Latest_EAP&guest=1
[teamcity-build-status-svg]: https://ideavim.teamcity.com/app/rest/builds/buildType:(id:Ideavim_IdeaVimTests_Latest_EAP)/statusIcon.svg?guest=1
