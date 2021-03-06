/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.plugins.internal

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

import static org.gradle.util.TextUtil.toPlatformLineSeparators

class BuildScriptBuilderTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def outputFile = tmpDir.file("build.gradle")
    def builder = new BuildScriptBuilder(outputFile)

    def "generates basic build script"() {
        when:
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 */

""")
    }

    def "can add file comment"() {
        when:
        builder.fileComment("""This is a sample
see more at gradle.org""")
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 *
 * This is a sample
 * see more at gradle.org
 */

""")
    }

    def "can add plugins"() {
        when:
        builder.plugin("Add support for the Java language", "java")
        builder.plugin("Add support for building applications", "application")
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// Add support for the Java language
apply plugin: 'java'

// Add support for building applications
apply plugin: 'application'

""")
    }

    def "can add compile dependencies"() {
        when:
        builder.compileDependency("Use slf4j", "org.slf4j:slf4j-api:2.7", "org.slf4j:slf4j-simple:2.7")
        builder.compileDependency("Use Scala to compile", "org.scala-lang:scala-library:2.10")
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Use slf4j
    compile 'org.slf4j:slf4j-api:2.7'
    compile 'org.slf4j:slf4j-simple:2.7'

    // Use Scala to compile
    compile 'org.scala-lang:scala-library:2.10'
}

""")
    }

    def "can add test compile and runtime dependencies"() {
        when:
        builder.testCompileDependency("use some test kit", "org:test:1.2", "org:test-utils:1.2")
        builder.testRuntimeDependency("needs some libraries at runtime", "org:test-runtime:1.2")
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // use some test kit
    testCompile 'org:test:1.2'
    testCompile 'org:test-utils:1.2'

    // needs some libraries at runtime
    testRuntime 'org:test-runtime:1.2'
}

""")
    }

    def "can add further configuration"() {
        when:
        builder.configuration("Enable some test option", """test {
    flag = true
}""")
        builder.configuration("Switch off a thing", "thing.off()")
        builder.create().generate()

        then:
        outputFile.file
        outputFile.text == toPlatformLineSeparators("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// Enable some test option
test {
    flag = true
}

// Switch off a thing
thing.off()

""")
    }

}
