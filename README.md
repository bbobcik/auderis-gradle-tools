# auderis-gradle-tools
Small but useful additions for Gradle build system

## Changelog

### 1.0.1 
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
