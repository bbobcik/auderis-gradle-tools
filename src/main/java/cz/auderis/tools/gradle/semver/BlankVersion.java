package cz.auderis.tools.gradle.semver;

import org.gradle.api.Project;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Instance of this class is set as an initial value to {@code project.version}. Its {@link #toString()}
 * returns {@code Project.DEFAULT_VERSION} so there should be little visible difference from normal
 * unset project property, however it offers useful member methods for easier setting of actual version value.
 * <p>
 * Version can be set directly:
 * <pre>
 *   project.version.is('1.3.9-SNAPSHOT')
 * </pre>
 * Alternatively, version specification can be obtained from an external resource:
 * <pre>
 *   project.version.from('version.txt')
 * </pre>
 *
 * @author Boleslav Bobcik
 * @version 1.0.0
 */
public class BlankVersion {

    private final WeakReference<Project> ownerRef;

    BlankVersion(Project project) {
        this.ownerRef = new WeakReference<Project>(project);
    }

    @Override
    public String toString() {
        return Project.DEFAULT_VERSION;
    }

    /**
     * Replaces reference in {@code project.version} from itself to a concrete version instance
     *
     * @param versionSpecification semantic version text representation
     */
    public void is(String versionSpecification) {
        final SemanticVersion version = SemanticVersion.is(versionSpecification);
        getOwner().setVersion(version);
    }

    /**
     * Replaces reference in {@code project.version} from itself to a concrete version instance
     * read from the provided stream
     *
     * @param stream stream that contains semantic version specification
     */
    public void from(InputStream stream) {
        final SemanticVersion version = SemanticVersion.from(stream);
        getOwner().setVersion(version);
    }

    /**
     * Replaces reference in {@code project.version} from itself to a concrete version instance
     * read from the given file
     *
     * @param versionHolder file that contains semantic version specification
     */
    public void from(File versionHolder) {
        final SemanticVersion version = SemanticVersion.from(versionHolder);
        getOwner().setVersion(version);
    }

    /**
     * Replaces reference in {@code project.version} from itself to a concrete version instance
     * obtained from the provided URL
     *
     * @param url URL of a semantic version specification
     */
    public void from(URL url) {
        final SemanticVersion version = SemanticVersion.from(url);
        getOwner().setVersion(version);
    }

    /**
     * Replaces reference in {@code project.version} from itself to a concrete version instance
     * obtained from the provided source; it may be text representation of a URL or a file path.
     *
     * @param source source location where a semantic version can be read
     */
    public void from(String source) {
        final SemanticVersion version = SemanticVersion.from(source);
        getOwner().setVersion(version);
    }

    private Project getOwner() {
        final Project project = ownerRef.get();
        if (null == project) {
            throw new IllegalStateException("Project reference was destroyed");
        }
        return project;
    }

}
