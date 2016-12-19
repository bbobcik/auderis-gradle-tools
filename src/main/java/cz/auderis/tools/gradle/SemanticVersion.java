/*
 * Copyright 2016 Boleslav Bobcik - Auderis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.auderis.tools.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * As specified in Gradle documentation, project/module version may be an object of any type.
 * Gradle uses its {@code toString()} to obtain the version string. However often there is
 * a need for other, more detailed inspection of version number structure.
 * <p>
 * When adopting <a href="http://http://semver.org">Semantic Versioning</a> approach,
 * a convenient class is made available by this library for Gradle scripts. The version may
 * be defined either directly in the script or loaded from an external resource, such as
 * a file or even URL.
 * <pre>
 * import cz.auderis.tools.gradle.SemanticVersion
 *
 * // build.gradle
 * buildscript {
 * // Define where to find the plugin module, e.g. mavenCentral()
 * }
 *
 * // Use direct specification
 * project.version = SemanticVersion.is("1.0.0-SNAPSHOT")
 *
 * // Load version specification from a file
 * project version = SemanticVersion.from("version.txt")
 * </pre>
 *
 * An instance of {@code SemanticVersion} class has, among others, the following properties:
 * <ul>
 * <li>{@code majorVersion} (with convenient alias {@code major}) returns first
 * numeric part X from version X.Y.Z</li>
 * <li>{@code minorVersion} (with convenient alias {@code minor}) returns numeric
 * part Y from version X.Y.Z</li>
 * <li> {@code patchRevision} (with convenient aliases {@code patch} and {@code patchLevel})
 * returns numeric part Z from version X.Y.Z</li>
 * <li>{@code prerelease} is a boolean flag that indicates that either major version is 0
 * or there are pre-release identifiers present (part P in version string X.Y.Z-P).
 * There is an alias {@code preRelease} available as well.</li>
 * <li>{@code stable} is a boolean flag that is effectively a negation of {@code prerelease} property</li>
 * <li>{@code snapshot} is a boolean flag that indicates whether a special pre-release ID
 * {@code SNAPSHOT} is present in the version string (i.e. "1.4.2-SNAPSHOT")</li>
 * </ul>
 */
public class SemanticVersion implements Serializable, Comparable<SemanticVersion> {
    private static final long serialVersionUID = 1764473113703074347L;

    public static final Pattern PATTERN = Pattern.compile(
            "^\\s*("                                           // Optional leading whitespace
            + "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"  // Main part, dot-separated major-minor-patch
            + "(?:-([0-9A-Z-]+(?:\\.[0-9A-Z-]+)*))?"           // Optional pre-release identifiers preceded by -
            + "(?:\\+([0-9A-Z-]+(?:\\.[0-9A-Z-]+)*))?"         // Optional build metadata identifiers preceded by +
            + ")\\s*$",                                        // Optional trailing whitespace
            Pattern.CASE_INSENSITIVE
    );
    static final String IDENTIFIER_PATTERN_ALTERNATIVES =
            "0"                                // "0" is the only numeric ID that can start with zero
            + "|[1-9]\\d*"                     // no 0s allowed in non-zero numeric IDs
            + "|[A-Z](?:[A-Z0-9-]*[A-Z0-9])?"  // non-numeric IDs starting with a letter
            + "|\\d(?:[A-Z0-9]|-(?!\\Z))*";    // non-numeric IDs starting with a digit

    public static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "\\A" + IDENTIFIER_PATTERN_ALTERNATIVES + "\\Z",
            Pattern.CASE_INSENSITIVE
    );

    public static final String SNAPSHOT_ID = "SNAPSHOT";

    int major;
    int minor;
    int patch;
    List<String> preReleaseIdentifiers;
    List<String> buildMetadataIdentifiers;
    private transient String stringRepresentation;

    /**
     * Creates an instance of semantic version directly from the provided version string
     *
     * @param specification text representation of semantic version
     * @return instance of semantic version object
     * @throws NullPointerException when {@code specification} is {@code null}
     * @throws InvalidUserDataException when {@code specification} does not conform to semantic version rules
     */
    public static SemanticVersion is(String specification) {
        if (null == specification) {
            throw new NullPointerException("Version holder file not specified");
        }
        final Matcher specMatcher = PATTERN.matcher(specification);
        if (!specMatcher.matches()) {
            throw new InvalidUserDataException("Invalid semantic version specification: " + specification);
        }
        final SemanticVersion semanticVersion = parseSpecification(specMatcher);
        return semanticVersion;
    }

    /**
     * Loads a version string from the provided stream and parses it into an instance of semantic version.
     * <p>
     * The provided stream is expected to adhere to certain rules:
     * <ol>
     * <li>It must be a text file (as understood by {@link BufferedReader#readLine()}</li>
     * <li>At the beginning of the file (i.e. until a semantic version string is encountered),
     * all blank lines and comment lines (lines where first non-whitespace characters are either
     * {@code #} or {@code //}) are skipped.</li>
     * <li>The first line that is neither blank nor comment must specify a valid semantic version.</li>
     * <li>After the semantic version line is found, the parser terminates and the rest of the stream
     * is ignored</li>
     * </ol>
     * If these rules are not satisfied, the parser throws a {@code InvalidUserDataException} exception.
     *
     * @param stream source to be parsed
     * @return instance of semantic version object
     * @throws NullPointerException when {@code stream} is {@code null}
     * @throws InvalidUserDataException when contents of {@code stream} does not adhere to the rules or when
     * the parsed version specification does not conform to semantic version rules
     */
    public static SemanticVersion from(InputStream stream) {
        if (null == stream) {
            throw new NullPointerException("Version specification stream not defined");
        }
        try (final InputStreamReader streamReader = new InputStreamReader(stream, "UTF-8");
             final BufferedReader reader = new BufferedReader(streamReader, 512)) {
            final SemanticVersion result = readVersion(reader);
            return result;
        } catch (GradleException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUserDataException("Cannot parse version specification stream " + stream, e);
        }
    }

    /**
     * Loads a version string from a file and parses it into an instance of semantic version.
     * <p>
     * The provided stream is expected to adhere to rules defined in {@link #from(InputStream)}.
     *
     * @param versionHolder source to be parsed
     * @return instance of semantic version object
     * @throws NullPointerException when {@code versionHolder} is {@code null}
     * @throws InvalidUserDataException when contents of {@code versionHolder} file does not adhere
     * to the rules or when the parsed version specification does not conform to semantic version rules
     */
    public static SemanticVersion from(File versionHolder) {
        if (null == versionHolder) {
            throw new NullPointerException("Version holder file not specified");
        }
        if (!versionHolder.canRead() || !versionHolder.isFile()) {
            throw new InvalidUserDataException("Unable to read version holder file " + versionHolder);
        }
        try (final FileReader fileReader = new FileReader(versionHolder);
             final BufferedReader reader = new BufferedReader(fileReader, 512)) {
            final SemanticVersion result = readVersion(reader);
            return result;
        } catch (GradleException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUserDataException("Cannot parse version holder file " + versionHolder, e);
        }
    }

    /**
     * Loads a version string from the provided URL and parses it into an instance of semantic version.
     * <p>
     * The provided stream is expected to adhere to rules defined in {@link #from(InputStream)}.
     *
     * @param url source object to be parsed
     * @return instance of semantic version object
     * @throws NullPointerException when {@code url} is {@code null}
     * @throws InvalidUserDataException when {@code url} is not accessible, contents of {@code url}
     * does not adhere to the rules or when the parsed version specification does not conform to
     * semantic version rules
     */
    public static SemanticVersion from(URL url) {
        if (null == url) {
            throw new NullPointerException("Version specification URL not specified");
        }
        try (final InputStream urlStream = url.openStream();
             final InputStreamReader urlReader = new InputStreamReader(urlStream, "UTF-8");
             final BufferedReader reader = new BufferedReader(urlReader, 512)
        ) {
            final SemanticVersion result = readVersion(reader);
            return result;
        } catch (GradleException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUserDataException("Cannot parse version specification URL " + url, e);
        }
    }

    /**
     * Tries to interpret the {@code source} argument as URL and uses it to parse a version string from its contents;
     * if the argument is not a valid URL, it is considered to be a local file path.
     *
     * @param source text specification that must be either a valid URL or file path
     * @return instance of semantic version object
     * @throws NullPointerException when {@code source} is {@code null}
     * @throws InvalidUserDataException when {@code source} is not accessible, its contents
     * does not adhere to the rules or when the parsed version specification does not conform to
     * semantic version rules
     */
    public static SemanticVersion from(String source) {
        if (null == source) {
            throw new NullPointerException("Version specification source not defined");
        }
        // Try to interpret source as an URL
        try {
            final URL sourceUrl = new URL(source);
            return from(sourceUrl);
        } catch (MalformedURLException e) {
            // Not an URL, fall back to next case
        }
        // Try local file
        final File sourceFile = new File(source);
        if (sourceFile.isFile()) {
            return from(sourceFile);
        }
        throw new InvalidUserDataException("Cannot determine nature of version specification source: " + source);
    }

    private SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        preReleaseIdentifiers = new ArrayList<>(1);
        buildMetadataIdentifiers = new ArrayList<>(1);
    }

    SemanticVersion() {
        // Used for serialization
    }

    // Convenience property alias
    @Deprecated
    public int getMajor() {
        return major;
    }

    public int getMajorVersion() {
        return major;
    }

    // Convenience property alias
    @Deprecated
    public int getMinor() {
        return minor;
    }

    public int getMinorVersion() {
        return minor;
    }

    // Convenience property alias
    @Deprecated
    public int getPatch() {
        return patch;
    }

    // Convenience property alias
    @Deprecated
    public int getPatchLevel() {
        return patch;
    }

    public int getPatchRevision() {
        return patch;
    }

    // Convenience alias to prevent errors when referencing properties
    public boolean isPrerelease() {
        return isPreRelease();
    }

    public boolean isPreRelease() {
        return (0 == major) || !preReleaseIdentifiers.isEmpty();
    }

    public boolean isStable() {
        return (0 != major) && preReleaseIdentifiers.isEmpty();
    }

    public boolean isSnapshot() {
        return preReleaseIdentifiers.contains(SNAPSHOT_ID);
    }

    public List<String> getPreReleaseIdentifiers() {
        return Collections.unmodifiableList(preReleaseIdentifiers);
    }

    public boolean hasPreReleaseIdentifier(String id) {
        if (null == id) {
            throw new NullPointerException();
        }
        return preReleaseIdentifiers.contains(id);
    }

    public SemanticVersion withPreReleaseIdentifier(String id) {
        if (null == id) {
            throw new NullPointerException("Pre-release identifier not specified");
        } else if (!validId(id)) {
            throw new InvalidUserDataException("Invalid pre-release ID: " + id);
        } else if (preReleaseIdentifiers.contains(id)) {
            return this;
        }
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.preReleaseIdentifiers.add(id);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        return extendedVersion;
    }

    public SemanticVersion withOptionalPreReleaseIdentifier(String id) {
        if ((null == id) || preReleaseIdentifiers.contains(id)) {
            return this;
        } else if (!validId(id)) {
            throw new InvalidUserDataException("Invalid pre-release ID: " + id);
        }
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.preReleaseIdentifiers.add(id);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        return extendedVersion;
    }

    public SemanticVersion withPreReleaseIdentifiers(String... ids) {
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        for (final String id : ids) {
            if ((null != id) && !extendedVersion.preReleaseIdentifiers.contains(id)) {
                if (!validId(id)) {
                    throw new InvalidUserDataException("Invalid pre-release ID: " + id);
                }
                extendedVersion.preReleaseIdentifiers.add(id);
            }
        }
        return extendedVersion;
    }

    public SemanticVersion stripPreReleaseIdentifiers() {
        final SemanticVersion stripped = new SemanticVersion(major, minor, patch);
        stripped.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        return stripped;
    }

    protected List<String> getPreReleaseIds() {
        return preReleaseIdentifiers;
    }

    public List<String> getBuildMetadataIdentifiers() {
        return Collections.unmodifiableList(buildMetadataIdentifiers);
    }

    public boolean hasBuildMetadataIdentifier(String id) {
        if (null == id) {
            throw new NullPointerException();
        }
        return buildMetadataIdentifiers.contains(id);
    }

    public SemanticVersion withBuildMetadataIdentifier(String id) {
        if (null == id) {
            throw new NullPointerException("Build metadata identifier not specified");
        } else if (!validId(id)) {
            throw new InvalidUserDataException("Invalid build metadata ID: " + id);
        } else if (buildMetadataIdentifiers.contains(id)) {
            return this;
        }
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        extendedVersion.buildMetadataIdentifiers.add(id);
        return extendedVersion;
    }

    public SemanticVersion withOptionalBuildMetadataIdentifier(String id) {
        if ((null == id) || buildMetadataIdentifiers.contains(id)) {
            return this;
        } else if (!validId(id)) {
            throw new InvalidUserDataException("Invalid build metadata ID: " + id);
        }
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        extendedVersion.buildMetadataIdentifiers.add(id);
        return extendedVersion;
    }

    public SemanticVersion withBuildMetadataIdentifiers(String... ids) {
        final SemanticVersion extendedVersion = new SemanticVersion(major, minor, patch);
        extendedVersion.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        extendedVersion.buildMetadataIdentifiers.addAll(buildMetadataIdentifiers);
        for (final String id : ids) {
            if ((null != id) && !extendedVersion.buildMetadataIdentifiers.contains(id)) {
                if (!validId(id)) {
                    throw new InvalidUserDataException("Invalid build metadata ID: " + id);
                }
                extendedVersion.buildMetadataIdentifiers.add(id);
            }
        }
        return extendedVersion;
    }

    public SemanticVersion stripBuildMetadataIdentifiers() {
        final SemanticVersion stripped = new SemanticVersion(major, minor, patch);
        stripped.preReleaseIdentifiers.addAll(preReleaseIdentifiers);
        return stripped;
    }

    protected List<String> getBuildMetadataIds() {
        return buildMetadataIdentifiers;
    }

    public SemanticVersion stripAllIdentifiers() {
        final SemanticVersion stripped = new SemanticVersion(major, minor, patch);
        return stripped;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SemanticVersion)) {
            return false;
        }
        final SemanticVersion other = (SemanticVersion) obj;
        if (major != other.getMajorVersion()) {
            return false;
        } else if (minor != other.getMinorVersion()) {
            return false;
        } else if (patch != other.getPatchRevision()) {
            return false;
        } else if (!preReleaseIdentifiers.equals(other.getPreReleaseIds())) {
            return false;
        } else if (!buildMetadataIdentifiers.equals(other.getBuildMetadataIds())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (29 * major + minor) * 47 + patch + 6353 * preReleaseIdentifiers.hashCode();
    }

    @Override
    public int compareTo(SemanticVersion other) {
        final int otherMajor = other.getMajorVersion();
        if (major != otherMajor) {
            return (major < otherMajor) ? -1 : 1;
        }
        final int otherMinor = other.getMinorVersion();
        if (minor != otherMinor) {
            return (minor < otherMinor) ? -1 : 1;
        }
        final int otherPatch = other.getPatchRevision();
        if (patch != otherPatch) {
            return (patch < otherPatch) ? -1 : 1;
        }
        final List<String> otherPreReleaseIds = other.getPreReleaseIds();
        final int idCmp = compareIdLists(preReleaseIdentifiers, otherPreReleaseIds);
        return idCmp;
    }

    @Override
    public String toString() {
        if (null == stringRepresentation) {
            final int ids = preReleaseIdentifiers.size() + buildMetadataIdentifiers.size();
            final StringBuilder str = new StringBuilder(16 + 8 * ids);
            str.append(major);
            str.append('.').append(minor);
            str.append('.').append(patch);
            if (!preReleaseIdentifiers.isEmpty()) {
                char separator = '-';
                for (final String id : preReleaseIdentifiers) {
                    str.append(separator);
                    if ('-' == separator) {
                        separator = '.';
                    }
                    str.append(id);
                }
            }
            if (!buildMetadataIdentifiers.isEmpty()) {
                char separator = '+';
                for (final String id : buildMetadataIdentifiers) {
                    str.append(separator);
                    if ('+' == separator) {
                        separator = '.';
                    }
                    str.append(id);
                }
            }
            stringRepresentation = str.toString();
        }
        return stringRepresentation;
    }


    public enum OrderBy implements Comparator<SemanticVersion> {
        NORMAL {
            @Override
            public int compare(SemanticVersion sv1, SemanticVersion sv2) {
                if ((null == sv1) || (null == sv2)) {
                    throw new NullPointerException();
                }
                // Compare 3 key parts - major, minor, patch
                int cmp = Integer.compare(sv1.getMajorVersion(), sv2.getMajorVersion());
                if (0 != cmp) return cmp;
                cmp = Integer.compare(sv1.getMinorVersion(), sv2.getMinorVersion());
                if (0 != cmp) return cmp;
                cmp = Integer.compare(sv1.getPatchRevision(), sv2.getPatchRevision());
                if (0 != cmp) return cmp;
                // Compare pre-release status
                final List<String> ids1 = sv1.getPreReleaseIds();
                final List<String> ids2 = sv2.getPreReleaseIds();
                cmp = compareIdLists(ids1, ids2);
                return cmp;
            }
        }
    }


    static SemanticVersion readVersion(BufferedReader reader) throws IOException {
        final Pattern ignoredLinePattern = Pattern.compile("^\\s*(?:(?:#|//).*)?$");
        final Matcher ignoredLineMatcher = ignoredLinePattern.matcher("");
        final Matcher versionMatcher = PATTERN.matcher("");
        SemanticVersion result = null;
        String line;
        FILE_SCAN:
        while (null != (line = reader.readLine())) {
            versionMatcher.reset(line);
            if (versionMatcher.matches()) {
                result = parseSpecification(versionMatcher);
                break FILE_SCAN;
            }
            ignoredLineMatcher.reset(line);
            if (!ignoredLineMatcher.matches()) {
                throw new InvalidUserDataException("Invalid contents of version holder file: " + line);
            }
        }
        if (null == result) {
            throw new InvalidUserDataException("Cannot find valid semantic version specification");
        }
        return result;
    }

    static SemanticVersion parseSpecification(Matcher versionMatcher) {
        assert null != versionMatcher;
        assert versionMatcher.groupCount() >= 4;
        final String wholeSpec = versionMatcher.group(1);
        final int major = Integer.parseInt(versionMatcher.group(2));
        final int minor = Integer.parseInt(versionMatcher.group(3));
        final int patch = Integer.parseInt(versionMatcher.group(4));
        final SemanticVersion result = new SemanticVersion(major, minor, patch);
        if (versionMatcher.groupCount() > 4) {
            // Parse optional components
            try {
                final String preReleasePart = versionMatcher.group(5);
                parseIdentifiers(preReleasePart, result.preReleaseIdentifiers);
            } catch (Exception e) {
                throw new InvalidUserDataException("Invalid pre-release part in semantic version: " + wholeSpec, e);
            }
            try {
                final String buildMetadataPart = versionMatcher.group(6);
                parseIdentifiers(buildMetadataPart, result.buildMetadataIdentifiers);
            } catch (Exception e) {
                throw new InvalidUserDataException("Invalid build metadata part in semantic version: " + wholeSpec, e);
            }
        }
        return result;
    }

    static void parseIdentifiers(String idPart, List<? super String> targetList) {
        if ((null == idPart) || idPart.isEmpty()) {
            return;
        }
        final Pattern identifierPattern = Pattern.compile("(?:\\A|\\G\\.)(0|\\d.*\\D.*|[1-9]\\d*|[a-zA-Z][a-zA-Z0-9-]*)?+([^.]*)(?=\\Z|\\.)");
        final Matcher identifierMatcher = identifierPattern.matcher(idPart);
        while (identifierMatcher.find()) {
            final String identifier = identifierMatcher.group(1);
            final String invalidPart = identifierMatcher.group(2);
            if (!invalidPart.isEmpty()) {
                throw new IllegalArgumentException("Malformed identifier: " + identifier);
            }
            targetList.add(identifier);
        }
        if (!identifierMatcher.hitEnd()) {
            throw new IllegalArgumentException("Malformed identifier sequence: " + idPart);
        }
    }

    static int compareIdLists(List<String> ids1, List<String> ids2) {
        assert null != ids1;
        assert null != ids2;
        final boolean empty1 = ids1.isEmpty();
        final boolean empty2 = ids2.isEmpty();
        if (empty1) {
            return empty2 ? 0 : 1;
        } else if (empty2) {
            return -1;
        }
        final Iterator<String> iter1 = ids1.iterator();
        final Iterator<String> iter2 = ids2.iterator();
        int cmp = 0;
        while ((0 == cmp) && iter1.hasNext() && iter2.hasNext()) {
            final String id1 = iter1.next();
            final String id2 = iter2.next();
            cmp = compareIds(id1, id2);
        }
        if (0 == cmp) {
            if (iter1.hasNext()) {
                assert !iter2.hasNext();
                cmp = 1;
            } else if (iter2.hasNext()) {
                cmp = -1;
            }
        }
        return cmp;
    }

    static int compareIds(String id1, String id2) {
        assert null != id1;
        assert null != id2;
        final boolean id1IsNumber = isNumber(id1);
        final boolean id2IsNumber = isNumber(id2);
        if (id1IsNumber) {
            if (!id2IsNumber) {
                return -1;
            }
            final int n1 = Integer.parseInt(id1);
            final int n2 = Integer.parseInt(id2);
            return Integer.compare(n1, n2);
        } else if (id2IsNumber) {
            return 1;
        }
        // Both identifiers are non-numeric
        return id1.compareTo(id2);
    }

    static boolean isNumber(String id) {
        assert null != id;
        assert !id.isEmpty();
        final int length = id.length();
        for (int i=0; i<length; ++i) {
            final char c = id.charAt(i);
            if ((c < '0') || (c > '9')) {
                return false;
            }
        }
        return true;
    }

    static boolean validId(String id) {
        assert null != id;
        final Matcher matcher = IDENTIFIER_PATTERN.matcher(id);
        return matcher.matches();
    }

}
