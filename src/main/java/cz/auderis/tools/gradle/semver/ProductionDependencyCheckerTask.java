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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.api.tasks.TaskAction;

import java.util.HashSet;
import java.util.Set;

@ParallelizableTask
public class ProductionDependencyCheckerTask extends DefaultTask {

    private final Set<String> configurationNames;
    private boolean failOnError;

    public ProductionDependencyCheckerTask() {
        this.configurationNames = new HashSet<>(4);
        this.configurationNames.add(Dependency.DEFAULT_CONFIGURATION);
        this.failOnError = true;
    }

    @Input
    @Optional
    public Set<String> getConfigurations() {
        return configurationNames;
    }

    public void setConfigurations(Set<String> newConfigurations) {
        if (null == newConfigurations) {
            throw new NullPointerException();
        }
        this.configurationNames.clear();
        this.configurationNames.addAll(newConfigurations);
    }

    public void configurations(Object... newConfigurations) {
        this.configurationNames.clear();
        for (final Object newCfg : newConfigurations) {
            if (null == newCfg) {
                // Ignored
            } else if (newCfg instanceof Configuration) {
                final String cfgName = ((Configuration) newCfg).getName();
                configurationNames.add(cfgName);
            } else {
                configurationNames.add(newCfg.toString());
            }
        }
    }

    public void configuration(String configName) {
        if (null == configName) {
            throw new NullPointerException();
        }
        this.configurationNames.add(configName);
    }

    public void configuration(Configuration configInstance) {
        if (null == configInstance) {
            throw new NullPointerException();
        }
        this.configurationNames.add(configInstance.getName());
    }

    @Input
    @Optional
    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @TaskAction
    protected void performDependencyVersionCheck() {
        boolean didWork = false;
        final Logger logger = getLogger();
        final Project project = getProject();
        Set<Dependency> invalidDependencies = null;
        final ConfigurationContainer projectConfigurations = project.getConfigurations();
        for (final String cfgName : configurationNames) {
            final Configuration cfg = projectConfigurations.getByName(cfgName);
            final DependencySet cfgDependencies = cfg.getAllDependencies();
            for (final Dependency dependency : cfgDependencies) {
                final boolean productionGrade = isDependencyProductionGrade(dependency);
                if (!productionGrade) {
                    logger.info("Checking release status of {} in {}: FAIL (version {})", dependency, cfg, dependency.getVersion());
                    if (null == invalidDependencies) {
                        invalidDependencies = new HashSet<>(16);
                    }
                    invalidDependencies.add(dependency);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Checking release status of {} in {}: {}", dependency, cfg, "OK");
                }
                didWork = true;
            }
            if (failOnError && (null != invalidDependencies)) {
                setDidWork(true);
                final String errorMessage = prepareErrorMsg(cfg, invalidDependencies);
                throw new GradleException(errorMessage);
            }
        }
        setDidWork(didWork);
    }

    protected boolean isDependencyProductionGrade(Dependency dep) {
        final String versionSpec = dep.getVersion();
        final boolean production;
        if (null == versionSpec) {
            // Ignore this dependency
            production = true;
        } else if (SemanticVersion.isValid(versionSpec)) {
            final SemanticVersion semVer = SemanticVersion.parse(versionSpec);
            production = semVer.isStable();
        } else {
            production = !(versionSpec.startsWith("0.") || versionSpec.contains(SemanticVersion.SNAPSHOT_ID));
        }
        return production;
    }

    protected String prepareErrorMsg(Configuration cfg, Set<Dependency> invalidDependencies) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Non-production grade dependencies detected in ");
        sb.append(cfg);
        String separator = ": ";
        for (final Dependency dep : invalidDependencies) {
            sb.append(separator);
            separator = ", ";
            final String depDescription = dep.toString();
            final String depVersion = dep.getVersion();
            sb.append(dep);
            if (!depDescription.contains(depVersion)) {
                sb.append(" (version ");
                sb.append(depVersion);
                sb.append(')');
            }
        }
        return sb.toString();
    }

}
