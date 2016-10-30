# auderis-gradle-tools
Small but useful additions for Gradle build system

## Changelog

### 1.0.0 Initial release
  * Test support plugin introduced

## Test Support Plugin
The main idea is that a library-type project should have 3 parts:

  * the library with production code (`src/main`)
  * a module that facilitates unit testing of the project's objects in a client environment,
    e.g. Hamcrest matchers (`src/test-support`)
  * normal unit test code (`src/test`)

Of these 3 parts, the library itself and the test support module would be published as artifacts.
The test support is distinguished by having artifact filename "appendix" set to `test_support`.
For example, the produced artifacts of a "FooBar" project would be:

  * Production: `foobar-1.0.0.jar`
  * Test support: `foobar-test_support-1.0.0.jar`

Plugin usage

    // build.gradle
    buildscript {
        // Define where to find the plugin module
    }
    apply plugin: 'cz.auderis.TestSupport'
    
    dependencies {
        ...
        testSupportCompile 'org.hamcrest:hamcrest-all:1.3'
        ...
    }
