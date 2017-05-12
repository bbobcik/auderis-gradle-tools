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

import java.util.Map;

class StartParameterVersionOverride implements VersionOverrideSource {

    public static final String DEFAULT_PARAMETER_NAME = "versionOverride";

    final StartParameter startParameter;
    final String propertyName;

    StartParameterVersionOverride(StartParameter startParameter, String propertyName) {
        assert null != startParameter : "Start parameter reference is undefined";
        assert null != propertyName : "Property name is undefined";
        assert !propertyName.trim().isEmpty() : "Property name is blank";
        this.startParameter = startParameter;
        this.propertyName = propertyName;
    }

    @Override
    public String getName() {
        return "parameter '" + propertyName + '\'';
    }

    @Override
    public boolean isActive() {
        final Map<String, String> properties = startParameter.getProjectProperties();
        return properties.containsKey(propertyName);
    }

    @Override
    public String getVersionSpecification() {
        final Map<String, String> properties = startParameter.getProjectProperties();
        return properties.get(propertyName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("parameter '");
        sb.append(propertyName);
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
