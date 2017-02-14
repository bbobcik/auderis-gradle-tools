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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class SemanticVersionPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "semanticVersion";
    public static final String DEFAULT_CHECK_TASK = "ensureProductionDependencies";

    @Override
    public void apply(Project target) {
        createSemanticVersionExtension(target);
        createVersionCheckTask(target);
        initializeProjectVersion(target);
    }

    private void createSemanticVersionExtension(Project project) {
        final ExtensionContainer extensions = project.getExtensions();
        final SemanticVersionExtension ext = extensions.create(EXTENSION_NAME, SemanticVersionExtension.class, project);
    }

    private void createVersionCheckTask(Project project) {
        final TaskContainer tasks = project.getTasks();
        final ProductionDependencyCheckerTask task = tasks.create(DEFAULT_CHECK_TASK, ProductionDependencyCheckerTask.class);
        task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
        task.setDescription("Ensures that all dependencies of default configuration have stable versions");
        task.onlyIf(new DefaultConfigurationPresentSpec());
    }

    private void initializeProjectVersion(Project project) {
        final Object version = project.getVersion();
        if ((null == version) || Project.DEFAULT_VERSION.equals(version)) {
            final BlankVersion blankVersion = new BlankVersion(project);
            project.setVersion(blankVersion);
            project.getLogger().debug("Version of {} set to blank semantic version", project);
        } else if (version instanceof CharSequence) {
            final SemanticVersion versionInstance = SemanticVersion.parse(version.toString());
            project.setVersion(versionInstance);
            project.getLogger().debug("Version of {} changed to semantic version instance: {}", project, versionInstance);
        }
    }

    private static final class DefaultConfigurationPresentSpec implements Spec<Task> {
        @Override
        public boolean isSatisfiedBy(Task task) {
            final Project project = task.getProject();
            final ConfigurationContainer configurations = project.getConfigurations();
            final Configuration defaultCfg = configurations.findByName(Dependency.DEFAULT_CONFIGURATION);
            return null != defaultCfg;
        }
    }

}
