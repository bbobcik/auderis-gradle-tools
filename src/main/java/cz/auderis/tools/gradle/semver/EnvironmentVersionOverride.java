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

import java.util.Map;

/**
 * Defines a source for version overrides based on system environment. This is useful for
 * various CI products.
 */
class EnvironmentVersionOverride implements VersionOverrideSource {

    final String envName;

    EnvironmentVersionOverride(String envName) {
        assert null != envName : "Environment variable name is undefined";
        assert !envName.trim().isEmpty() : "Environment variable name is empty";
        this.envName = envName;
    }

    @Override
    public String getName() {
        return "environment '" + envName + '\'';
    }

    @Override
    public boolean isActive() {
        final Map<String, String> environment = System.getenv();
        return environment.containsKey(envName);
    }

    @Override
    public String getVersionSpecification() {
        final Map<String, String> environment = System.getenv();
        return environment.get(envName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("environment '");
        sb.append(envName);
        sb.append('\'');
        if (isActive()) {
            final String versionSpecification = getVersionSpecification();
            if (null != versionSpecification) {
                sb.append(" with value ");
                sb.append(versionSpecification);
                sb.append('\'');
            } else {
                sb.append(" with undefined value");
            }
        } else {
            sb.append(" (not active)");
        }
        return sb.toString();
    }

}
