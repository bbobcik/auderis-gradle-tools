package cz.auderis.tools.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CONFIGURATION_NAME;

/**
 * Adds support for test support source set to a Gradle project.
 */
public class TestSupportPlugin implements Plugin<Project> {

    public static final String SUPPORT_SOURCE_SET_NAME = "testSupport";
    public static final String SUPPORT_CONFIGURATION_NAME = "testSupport";
    public static final String SUPPORT_COMPILE_CLASSPATH_CONFIGURATION_NAME = "testSupportCompileClasspath";
    public static final String SUPPORT_RUNTIME_CONFIGURATION_NAME = "testSupportRuntime";
    public static final String SUPPORT_JAR_TASK_NAME = SUPPORT_CONFIGURATION_NAME + "Jar";


    public static final String SUPPORT_SOURCE_BASE = "src/test-support";
    public static final String SUPPORT_JAVA_SOURCES = SUPPORT_SOURCE_BASE + "/java";
    public static final String SUPPORT_RESOURCES = SUPPORT_SOURCE_BASE + "/resources";

    public static final String SUPPORT_JAR_CLASSIFIER = "test_support";


    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        final JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);

        configureSourceSet(javaConvention);
        configureConfigurations(javaConvention);
        configureArchives(javaConvention);
    }

    private void configureSourceSet(JavaPluginConvention convention) {
        // Declare test support source set
        final SourceSetContainer sourceSets = convention.getSourceSets();
        final SourceSet supportSet = sourceSets.create(SUPPORT_SOURCE_SET_NAME);
        defineSourceSetPaths(supportSet, convention.getProject());

        // Support depends on main source set
        final SourceSet mainSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final Project project = convention.getProject();
        supportSet.setCompileClasspath(project.files(mainSet.getOutput(), project.getConfigurations().getByName(SUPPORT_COMPILE_CLASSPATH_CONFIGURATION_NAME)));
        supportSet.setRuntimeClasspath(project.files(supportSet.getOutput(), mainSet.getOutput(), project.getConfigurations().getByName(SUPPORT_RUNTIME_CONFIGURATION_NAME)));

        // Test depends on support, among others
        final SourceSet testSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        testSet.setCompileClasspath(testSet.getCompileClasspath().plus(supportSet.getOutput()));
        testSet.setRuntimeClasspath(testSet.getRuntimeClasspath().plus(supportSet.getOutput()));
    }

    private void defineSourceSetPaths(SourceSet supportSet, final Project project) {
        // Define source paths
        supportSet.getJava().setSrcDirs(Collections.singleton(SUPPORT_JAVA_SOURCES));
        supportSet.getResources().setSrcDirs(Collections.singleton(SUPPORT_RESOURCES));

        // Define output paths
        final ConventionMapping supportConventionMapping = ((IConventionAware) supportSet.getOutput()).getConventionMapping();
        supportConventionMapping.map("classesDir", new Callable<Object>() {
            @Override public Object call() throws Exception {
                return new File(project.getBuildDir(), "classes/test-support");
            }
        });
        supportConventionMapping.map("resourcesDir", new Callable<Object>() {
            @Override public Object call() throws Exception {
                return new File(project.getBuildDir(), "resources/test-support");
            }
        });
    }

    private void configureConfigurations(JavaPluginConvention pluginConvention) {
        final Project project = pluginConvention.getProject();
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration supportConfiguration = configurations.create(SUPPORT_CONFIGURATION_NAME);

        // Make the new configuration extend a configuration created by JavaBasePlugin
        final SourceSet supportSet = pluginConvention.getSourceSets().getByName(SUPPORT_SOURCE_SET_NAME);
        final Configuration supportRuntimeConfiguration = configurations.getByName(supportSet.getRuntimeConfigurationName());
        supportConfiguration.extendsFrom(supportRuntimeConfiguration);

        // Establish dependencies on main configurations
        final Configuration compileConfiguration = configurations.getByName(COMPILE_CONFIGURATION_NAME);
        final Configuration runtimeConfiguration = configurations.getByName(RUNTIME_CONFIGURATION_NAME);
        final Configuration supportCompileConfiguration = configurations.getByName(supportSet.getCompileConfigurationName());
        supportCompileConfiguration.extendsFrom(compileConfiguration);
        supportRuntimeConfiguration.extendsFrom(runtimeConfiguration);
    }

    private void configureArchives(JavaPluginConvention pluginConvention) {
        final Project project = pluginConvention.getProject();
        final Jar supportJarTask = project.getTasks().create(SUPPORT_JAR_TASK_NAME, Jar.class);
        supportJarTask.setDescription("Assembles a jar containing test support classes");
        supportJarTask.setGroup(BasePlugin.BUILD_GROUP);
        supportJarTask.setClassifier(SUPPORT_JAR_CLASSIFIER);

        // Define JAR contents
        final SourceSet supportSet = pluginConvention.getSourceSets().getByName(SUPPORT_SOURCE_SET_NAME);
        supportJarTask.from(supportSet.getOutput());

        // Set up publishing
        final ArchivePublishArtifact supportArtifact = new ArchivePublishArtifact(supportJarTask);
        final Configuration supportConfiguration = project.getConfigurations().getByName(SUPPORT_CONFIGURATION_NAME);
        supportConfiguration.getArtifacts().add(supportArtifact);
        final JavaLibrary supportLibrary = new JavaLibrary(supportArtifact, supportConfiguration.getAllDependencies());
        project.getComponents().add(supportLibrary);

        // Make a prerequisite for 'assemble' task
        final Task assembleTask = project.getTasks().getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME);
        assembleTask.dependsOn(supportJarTask);
    }

}
