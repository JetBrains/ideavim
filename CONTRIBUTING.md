<div>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20183&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20183)/statusIcon.svg?guest=1"/>
  </a>
  <span>2018.3 Tests</span>
</div>
<div>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20191&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20191)/statusIcon.svg?guest=1"/>
  </a>
  <span>2019.1 Tests</span>
</div>
<div>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20192&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20192)/statusIcon.svg?guest=1"/>
  </a>
  <span>2019.2 Tests</span>
</div>
<div>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20193&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20193)/statusIcon.svg?guest=1"/>
  </a>
  <span>2019.3 Tests</span>
</div>
<div>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20201&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20201)/statusIcon.svg?guest=1"/>
  </a>
  <span>2020.1 Tests</span>
</div>


### Where to Start

In order to contribute to IdeaVim, you should have some understanding of [Kotlin](https://kotlinlang.org/) or Java.

See also these docs on the IntelliJ API:

* [IntelliJ architectural overview](https://www.jetbrains.org/intellij/sdk/docs/platform/fundamentals.html)
* [IntelliJ plugin development resources](https://www.jetbrains.org/intellij/sdk/docs/welcome.html)

You can start by:

 - Picking relatively simple tasks that are tagged with
[#patch_welcome](https://youtrack.jetbrains.com/issues/VIM?q=%23patch_welcome%20%23Unresolved%20sort%20by:%20votes%20)
in the issue tracker.
 - Read about the `@VimBehaviorDiffers` annotation and fix the corresponding functionality.


### Development Environment

1. Fork IdeaVim on GitHub and clone the repository on your local machine.

2. Import the project from the existing sources in IntelliJ IDEA 2018.1 or newer (Community or
   Ultimate), by selecting "File | New | Project from Existing Sources..." or selecting "Import
   Project" from the Welcome screen.

    * In the project wizard, select "Import project from external model | Gradle".

    * Select your Java 8+ JDK as the Gradle JVM; leave other parameters unchanged.

3. Run your IdeaVim plugin within IntelliJ via a Gradle task:

    * Select the "View | Tool Windows | Gradle" tool window.
    
    * Launch "ideavim | intellij | runIde" from the tool window.

4. Run IdeaVim tests via a Gradle task:

    * Select the "View | Tool Windows | Gradle" tool window.
    
    * Launch "ideavim | verification | test" from the tool window.

5. Build the plugin distribution by running `./gradlew clean buildPlugin` in the
   terminal in your project root.

    * The resulting distribution file will be located at build/distributions/IdeaVim-VERSION.zip

    * You can install this file by selecting "Settings | Plugins | Install plugin
      from disk...".
       
### Testing

1. Read about the `@VimBehaviorDiffers` annotation.

2. Please avoid senseless text like "dhjkwaldjwa", "asdasdasd",
"123 123 123 123", etc. Try to choose an example text that is easy to
read and understand what is wrong if the test fails.
For example, take a few lines from your favorite poem, or use
"Vladimir Nabokov – A Discovery" if you don't have one.

3. Test your functionality properly.
Especially check whether your command works with:
line start, line end, file start, file end, empty line, multiple carets, dollar motion, etc.
