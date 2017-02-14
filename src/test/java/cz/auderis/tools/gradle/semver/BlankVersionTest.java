package cz.auderis.tools.gradle.semver;

import cz.auderis.test.rule.WorkFolder;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginManager;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Boleslav Bobcik
 * @version 1.0.0
 */
@RunWith(JUnitParamsRunner.class)
public class BlankVersionTest {

    @Rule
    public WorkFolder folder = WorkFolder.basic();

    File homeDir;
    File projectDir;
    Project project;

    @Before
    public void initializeProject() throws Exception {
        // Create directories
        homeDir = folder.newFolder("home");
        projectDir = folder.newFolder("project");
        folder.newResourceCopy("project/build.gradle", "base-build-script.gradle");

        // Prepare project instance
        final ProjectBuilder builder = ProjectBuilder.builder();
        builder.withGradleUserHomeDir(homeDir);
        builder.withProjectDir(projectDir);
        builder.withName("test-project");
        project = builder.build();

        // Apply the semantic version plugin
        final PluginManager pluginManager = project.getPluginManager();
        pluginManager.apply("cz.auderis.SemanticVersion");
    }


    @Test
    public void shouldInitializeProjectVersion() throws Exception {
        // Given / When - no action
        // Then
        final Object version = project.getVersion();
        assertThat(version, is(instanceOf(BlankVersion.class)));
    }

    @Test
    @Parameters({
            "1.0.0",
            "0.2.3",
            "3.14.15-SNAPSHOT"
    })
    public void shouldSetProjectVersionDirectly(String versionSpec) throws Exception {
        // Given
        final SemanticVersion referenceVersion = SemanticVersion.parse(versionSpec);

        // When
        ((BlankVersion) project.getVersion()).is(versionSpec);

        // Then
        final Object version = project.getVersion();
        assertThat(version, is(referenceVersion));
    }

    @Test
    @Parameters({
            "1.0.0",
            "0.2.3",
            "3.14.15-SNAPSHOT"
    })
    public void shouldSetProjectVersionFromFile(String versionSpec) throws Exception {
        // Given
        final SemanticVersion referenceVersion = SemanticVersion.parse(versionSpec);
        final File versionFile = folder.newFile("project/version.txt", withContents(versionSpec));

        // When
        ((BlankVersion) project.getVersion()).from(versionFile);

        // Then
        final Object version = project.getVersion();
        assertThat(version, is(referenceVersion));
    }

    @Test
    @Parameters({
            "1.0.0            | VERSION",
            "0.2.3            | version.txt",
            "3.14.15-SNAPSHOT | release.txt"
    })
    public void shouldSetProjectVersionFromFilePath(String versionSpec, String versionFilename) throws Exception {
        // Given
        final SemanticVersion referenceVersion = SemanticVersion.parse(versionSpec);

        folder.newFile(projectDir.getName() + '/' + versionFilename, withContents(versionSpec));
        final String versionPath = project.file(versionFilename).getPath();

        // When
        ((BlankVersion) project.getVersion()).from(versionPath);

        // Then
        final Object version = project.getVersion();
        assertThat(version, is(referenceVersion));
    }


    private static InputStream withContents(String text) {
        final byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(textBytes);
    }

}
