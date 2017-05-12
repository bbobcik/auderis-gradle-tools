package cz.auderis.tools.gradle.semver;

import cz.auderis.test.category.SlowTest;
import cz.auderis.test.category.UnitTest;
import cz.auderis.test.category.IntegrationTest;
import cz.auderis.test.rule.WorkFolder;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Boleslav Bobcik
 * @version 1.0.0
 */
@RunWith(JUnitParamsRunner.class)
@Category({UnitTest.class, SlowTest.class})
public class SemanticVersionExtensionTest {

    @Rule
    public WorkFolder folder = WorkFolder.basic();

    File homeDir;
    File projectDir;
    Project project;
    SemanticVersionExtension extension;

    @Before
    public void initializeProject() throws Exception {
        // Create directories
        homeDir = folder.newFolder("home");
        projectDir = folder.newFolder("project");

        // Prepare project instance
        final ProjectBuilder builder = ProjectBuilder.builder();
        builder.withGradleUserHomeDir(homeDir);
        builder.withProjectDir(projectDir);
        builder.withName("test-project");
        project = builder.build();

        // Apply the semantic version plugin
        final PluginManager pluginManager = project.getPluginManager();
        pluginManager.apply("cz.auderis.SemanticVersion");

        // Get extension instance
        final ExtensionContainer extensions = project.getExtensions();
        extension = extensions.findByType(SemanticVersionExtension.class);
    }

    @Test
    @Category(IntegrationTest.class)
    public void shouldEnableExtension() throws Exception {
        // Given
        final ExtensionContainer extensions = project.getExtensions();

        // When
        final Object extByName = extensions.findByName(SemanticVersionPlugin.EXTENSION_NAME);
        final SemanticVersionExtension extByClass = extensions.findByType(SemanticVersionExtension.class);

        // Then
        assertThat(extByName, is(instanceOf(SemanticVersionExtension.class)));
        assertThat(extByClass, is(sameInstance(extByName)));
    }

    @Test
    @Category(IntegrationTest.class)
    @Parameters({
            "0.0.1-SNAPSHOT",
            "1.2.16",
            "4.0.7-ALPHA+server1"
    })
    public void shouldUseExtensionForVersionParsing(String versionSpec) throws Exception {
        // Given
        final SemanticVersion referenceVersion = SemanticVersion.is(versionSpec);

        // When
        final SemanticVersion parsedVersion = extension.parse(versionSpec);

        // Then
        assertThat(parsedVersion, is(referenceVersion));
    }

}
