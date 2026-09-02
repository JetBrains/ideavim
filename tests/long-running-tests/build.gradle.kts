import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  intellijPlatform {
    defaultRepositories()
  }
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project
  testImplementation("org.junit.vintage:junit-vintage-engine:6.1.2")

  intellijPlatform {
    // Snapshots don't use installers
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions-installers
    val useInstaller = "EAP-SNAPSHOT" !in ideaVersion

    create(ideaType, ideaVersion) { this.useInstaller = false }
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
  }
}

intellijPlatform {
  buildSearchableOptions = false
}


tasks {
  // This task is disabled because it should be excluded from `gradle test` run (because it's slow)
  // I didn't find a better way to exclude except disabling and defining a new task with a different name
  // Note that useJUnitTestPlatform() is required to prevent red code
  test {
    useJUnitPlatform()

    // Without this the test IDE starts every plugin bundled with the product; a startup
    // error in any of them is rethrown as a failure of whichever IdeaVim test ran first.
    systemProperty("idea.load.plugins.id", "com.intellij,IdeaVIM")

    // Their .properties files shadow, rather than complement, the originals they translate.
    classpath -= classpath.filter { it.name.startsWith("localization-") && it.name.endsWith(".jar") }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
