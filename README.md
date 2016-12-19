# auderis-gradle-tools
Small but useful additions for Gradle build system

## Changelog

### 1.0.1 
  * `SemanticVersion` class added
  * Build system change (Gradle instead of Maven)

### 1.0.0 Initial release
  * Test support plugin introduced

## Test Support Plugin

*Motivation:* Typically, software projects focus only on production code part, occasionally a test part
is included, too. However support for library objects' testing/validation in client environment is - as it
seems - a novel idea, which leads to a too common "reinventing the wheel". Basically, a developer who wants
to perform certain tests (also known as *assertions*) on library's objects is left to his own devices, which
results in reader-unfriendly, cumbersome assertion constructs. Compare the following test code:

    Customer customer = ...
    assertNotNull( customer.getLastOrder() );
    assertTrue( !customer.getLastOrder().isClosed() );
 
with alternative
 
    Customer customer = ...
    assertThat( customer, hasLastOrderOpen() );
      
The difference is that author of the hypothetical `Customer` class spend some time providing an appropriate Hamcrest
matcher for a certain property (of course provided that the usefulness of the property warrants a dedicated matcher).

Obviously, the matchers and other components supporting tests must be published to be useful outside of their project,
but their inclusion into a production code bundle is deemed a bad design. **The solution and the raison d'etre of this
plugin is an idea that a library-type project should have 3 parts:**

  * `src/main`: the library with production code
  * `src/test-support`: a module that facilitates unit testing of the project's objects in a client environment,
    e.g. Hamcrest matchers, JavaBeans property editors, JUnitParams annotations and converters etc. 
  * `src/test`: normal unit test code that has access to both production and test-support code
  
Of the mentioned 3 project parts, the library itself and the test support module would be published as artifacts.
The test support is distinguished by having artifact filename "appendix" set to `test_support`.
For example, the produced artifacts of a "FooBar" project would be:

  * Production: `foobar-1.0.0.jar`
  * Test support: `foobar-test_support-1.0.0.jar`

Plugin usage

    // build.gradle
    buildscript {
        // Define where to find the plugin module, e.g. mavenCentral()
    }
    apply plugin: 'cz.auderis.TestSupport'
    
    dependencies {
        ...
        testSupportCompile 'org.hamcrest:hamcrest-all:1.3'
        ...
    }

## Semantic versioning

As specified in Gradle documentation, project/module version may be an object of any type. Gradle uses its
`toString()` to obtain the version string. However often there is a need for other, more detailed inspection
of version number structure.

When adopting [Semantic Versioning](http://http://semver.org/) approach, a convenient class is made available
by this library for Gradle scripts. The version may be defined either directly in the script or loaded from
an external resource, such as a file or even URL. 

    import cz.auderis.tools.gradle.SemanticVersion

    // build.gradle
    buildscript {
        // Define where to find the plugin module, e.g. mavenCentral()
    }
    
    // Use direct specification
    project.version = SemanticVersion.is("1.0.0-SNAPSHOT")
    
    // Load version specification from a file
    project version = SemanticVersion.from("version.txt")

An instance of `SemanticVersion` class has, among others, the following properties:

  * `majorVersion` (with convenient alias `major`) returns first numeric part X from version X.Y.Z
  * `minorVersion` (with convenient alias `minor`) returns numeric part Y from version X.Y.Z
  * `patchRevision` (with convenient aliases `patch` and `patchLevel`) returns numeric part Z from version X.Y.Z
  * `prerelease` is a boolean flag that indicates that either major version is 0 or there are pre-release identifiers
    present (part P in version string X.Y.Z-P). There is an alias `preRelease` available as well.
  * `stable` is a boolean flag that is effectively a negation of `prerelease` property
  * `snapshot` is a boolean flag that indicates whether a special pre-release ID `SNAPSHOT` is present
    in the version string (i.e. "1.4.2-SNAPSHOT")
