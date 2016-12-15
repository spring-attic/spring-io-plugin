package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.dsl.GeneratedPomCustomizationHandler;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

import java.io.File;

/**
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class SpringIoPlugin implements Plugin<Project> {

	private static final String CHECK_TASK_NAME = "springIoCheck";

	private static final String TEST_TASK_NAME = "springIoTest";

	private static final String INCOMPLETE_EXCLUDES_TASK_NAME = "springIoIncompleteExcludesCheck";

	private static final String ALTERNATIVE_DEPENDENCIES_TASK_NAME = "springIoAlternativeDependenciesCheck";

	private static final String CHECK_DEPENDENCY_VERSION_MAPPING_TASK_NAME = "springIoDependencyVersionMappingCheck";

	@Override
	public void apply(final Project project) {
		project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
			@Override
			public void execute(JavaPlugin javaPlugin) {
				applyJavaProject(project);
			}
		});
	}

	private void applyJavaProject(final Project project) {
		if (project.getPlugins().findPlugin(DependencyManagementPlugin.class) == null) {
			project.getPlugins().apply(DependencyManagementPlugin.class);
		}

		DependencyManagementExtension dependencyManagement = project.getExtensions().findByType(DependencyManagementExtension.class);
		dependencyManagement.setOverriddenByDependencies(false);
		dependencyManagement.setApplyMavenExclusions(false);
		dependencyManagement.generatedPomCustomization(new Action<GeneratedPomCustomizationHandler>() {
			public void execute(GeneratedPomCustomizationHandler handler) {
				handler.setEnabled(false);
			}
		});

		final Configuration springIoTestRuntimeConfiguration = project.getConfigurations().create("springIoTestRuntime", new Action<Configuration>() {
			@Override
			public void execute(Configuration springIoTestRuntimeConfiguration) {
				springIoTestRuntimeConfiguration.extendsFrom(project.getConfigurations().getByName("testRuntime"));
			}
		});

		final Task springIoTest = project.getTasks().create(TEST_TASK_NAME);
		final SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
		SourceSet springIoTestSourceSet = sourceSets.create("springIoTest", new Action<SourceSet>() {
			@Override
			public void execute(SourceSet springIoTestSourceSet) {
				SourceSet testSourceSet = sourceSets.findByName("test");
				springIoTestSourceSet.setCompileClasspath(project.files(sourceSets.getByName("main").getOutput(),
						springIoTestRuntimeConfiguration));
				springIoTestSourceSet.setRuntimeClasspath(project.files(sourceSets.getByName("main").getOutput(),
						springIoTestSourceSet.getOutput(), springIoTestRuntimeConfiguration));
				springIoTestSourceSet.getJava().setSrcDirs(testSourceSet.getJava().getSrcDirs());
				springIoTestSourceSet.getResources().setSrcDirs(testSourceSet.getResources().getSrcDirs());
			}
		});

		maybeCreateJdkTest(project, springIoTestRuntimeConfiguration, "Jdk7", springIoTest, springIoTestSourceSet);
		maybeCreateJdkTest(project, springIoTestRuntimeConfiguration, "Jdk8", springIoTest, springIoTestSourceSet);

		final Task incompleteExcludesCheck = project.getTasks().create(INCOMPLETE_EXCLUDES_TASK_NAME, IncompleteExcludesTask.class);
		final Task alternativeDependenciesCheck = project.getTasks().create(ALTERNATIVE_DEPENDENCIES_TASK_NAME, AlternativeDependenciesTask.class);
		final DependencyVersionMappingCheckTask dependencyVersionMappingCheck = project.getTasks().create(CHECK_DEPENDENCY_VERSION_MAPPING_TASK_NAME, DependencyVersionMappingCheckTask.class);
		dependencyVersionMappingCheck.setManagedVersions(dependencyManagement.getManagedVersionsForConfiguration(springIoTestRuntimeConfiguration));

		project.getTasks().create(CHECK_TASK_NAME, new Action<Task>() {
			@Override
			public void execute(Task task) {
				task.dependsOn(dependencyVersionMappingCheck);
				task.dependsOn(springIoTest);
				task.dependsOn(incompleteExcludesCheck);
				task.dependsOn(alternativeDependenciesCheck);
			}
		});
	}

	private void maybeCreateJdkTest(final Project project, final Configuration springioTestRuntimeConfig,
			final String jdk, Task springIoTest, final SourceSet springIoTestSourceSet) {
		final String whichJdk = jdk.toUpperCase() + "_HOME";
		if (!project.hasProperty(whichJdk)) {
			return;
		}

		Object jdkHome = project.getProperties().get(whichJdk);
		final File exec = new File(jdkHome instanceof File ? ((File)jdkHome): new File(jdkHome.toString()), createRelativeJavaExec(isWindows()));
		if (!exec.exists()) {
			throw new IllegalStateException("The path " + String.valueOf(exec) + " does not exist! Please provide a valid JDK home as a command-line argument using -P" + whichJdk + "=<path>");
		}

		Test springIoJdkTest = project.getTasks().create("springIo" + jdk + "Test", Test.class, new Action<Test>() {
			@Override
			public void execute(Test test) {
				test.setClasspath(springIoTestSourceSet.getRuntimeClasspath());
				test.setTestClassesDir(springIoTestSourceSet.getOutput().getClassesDir());
				test.getReports().getHtml().setDestination(project.file(project.getBuildDir() + "/reports/spring-io-" + jdk.toLowerCase() + "-tests"));
				test.getReports().getJunitXml().setDestination(project.file(project.getBuildDir() + "/spring-io-" + jdk.toLowerCase() + "-test-results"));
				test.executable(exec);
			}
		});
		springIoTest.dependsOn(springIoJdkTest);
	}

	String createRelativeJavaExec(boolean isWindows) {
		return isWindows ? "/bin/java.exe" : "/bin/java";
	}

	private Boolean isWindows() {
		return File.pathSeparatorChar == ';';
	}

}
