plugins {
    java
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

// group 'org.jetbrains.ideavim'
// version 'SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10")

    compileOnly("org.jetbrains:annotations:23.0.0")
}

tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            apiVersion = "1.5"
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }
}

// --- Linting

ktlint {
    disabledRules.add("no-wildcard-imports")
    version.set("0.43.0")
}
