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

/**
 * Defines an abstract provider of runtime version specification override.
 * This override may originate for example in project properties, system
 * environment etc.
 */
public interface VersionOverrideSource {

    /**
     * Returns human-readable description of the version override source. This is useful
     * for diagnostics and error reporting.
     *
     * @return human-readable source description
     */
    String getName();

    /**
     * Indicates whether this version override source is actually able to override the version;
     * it implies that {@link #getVersionSpecification()} will return a meaningful value.
     * For example, in case of system environment-based source, {@code true} may inform that
     * the appropriate environment variable exists.
     *
     * @return {@code true} when
     */
    boolean isActive();

    /**
     * Returns the string with version specification override or {@code null} when this
     * source is not active (see {@link #isActive()}).
     *
     * @return actual text that overrides version; {@code null} when the source is not currently applicable
     */
    String getVersionSpecification();

}
