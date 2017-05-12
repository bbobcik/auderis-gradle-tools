/*
 * Copyright 2017 Boleslav Bobcik - Auderis
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

package cz.auderis.tools.gradle.semver;

import org.gradle.StartParameter;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.util.regex.Matcher;

import static cz.auderis.tools.gradle.semver.SemanticVersion.PATTERN;
import static cz.auderis.tools.gradle.semver.SemanticVersion.parseSpecification;

public class SemanticVersionExtension {

    final Project project;
    final VersionOverrideSourceList overrideSources;

    public SemanticVersionExtension(Project project) {
        this.project = project;
        final VersionOverrideSource defaultOverride = new StartParameterVersionOverride(
                project.getGradle().getStartParameter(),
                StartParameterVersionOverride.DEFAULT_PARAMETER_NAME
        );
        this.overrideSources = new VersionOverrideSourceList(defaultOverride);
    }

    /**
     * Creates an instance of semantic version directly from the provided version string.
     * Version specification override mechanism is not used in this method.
     *
     * @param specification text representation of semantic version
     * @return instance of semantic version object
     * @throws NullPointerException when {@code specification} is {@code null}
     * @throws InvalidUserDataException when {@code specification} does not conform to semantic version rules
     */
    public SemanticVersion parse(String specification) {
        if (null == specification) {
            throw new NullPointerException("Semantic version is not specified");
        }
        final Matcher specMatcher = PATTERN.matcher(specification);
        if (!specMatcher.matches()) {
            throw new InvalidUserDataException("Invalid semantic version specification: " + specification);
        }
        final SemanticVersion semanticVersion = parseSpecification(specMatcher);
        return semanticVersion;
    }

    /**
     * Creates an instance of semantic version from a version string that is resolved using the following
     * order:
     * <ol>
     *     <li>The first active version specification override source is used to obtain version string</li>
     *     <li>If no override source is active, the provided argument {@code specification} is used</li>
     * </ol>
     * The method handles the resolved version specification string exactly as {@link #parse(String)}.
     *
     * @param specification fall-back text representation of semantic version
     * @return instance of semantic version object that corresponds to the resolved version specification
     * @throws NullPointerException when resolved version specification is {@code null}
     * @throws InvalidUserDataException when resolved version specification does not conform to semantic version rules
     */
    public SemanticVersion is(String specification) {
        final SemanticVersion override = getVersionOverride();
        if (null != override) {
            return override;
        }
        return parse(specification);
    }

    public SemanticVersion of(Dependency dep) {
        final String versionSpec = (null != dep) ? dep.getVersion() : null;
        if (null == versionSpec) {
            return null;
        }
        return null;
    }

    public void allowOverrideFromEnvironment(String environmentVariableName) {
        if (null == environmentVariableName) {
            throw new NullPointerException("undefined environment variable name");
        } else if (environmentVariableName.isEmpty()) {
            throw new IllegalArgumentException("invalid environment variable name: ''");
        }
        final EnvironmentVersionOverride envOverride = new EnvironmentVersionOverride(environmentVariableName);
        overrideSources.add(envOverride);
    }

    public void allowOverrideFromParameter(String propertyName) {
        if (null == propertyName) {
            throw new NullPointerException("undefined start parameter name");
        } else if (propertyName.isEmpty()) {
            throw new IllegalArgumentException("invalid start parameter name: ''");
        }
        final StartParameter params = project.getGradle().getStartParameter();
        final StartParameterVersionOverride paramOverride = new StartParameterVersionOverride(params, propertyName);
        overrideSources.add(paramOverride);
    }

    SemanticVersion getVersionOverride() {
        VersionOverrideSource overrideSource = null;
        for (final VersionOverrideSource overrideCandidate : overrideSources) {
            if (overrideCandidate.isActive()) {
                overrideSource = overrideCandidate;
                break;
            }
        }
        if (null == overrideSource) {
            return null;
        }
        final String overrideSpec = overrideSource.getVersionSpecification();
        if (null == overrideSpec) {
            return null;
        }
        if (!SemanticVersion.isValid(overrideSpec)) {
            throw new InvalidUserDataException("Invalid semantic version: " + overrideSource);
        }
        project.getLogger().info("Using project version override: {}", overrideSource);
        return SemanticVersion.parse(overrideSpec);
    }

}
