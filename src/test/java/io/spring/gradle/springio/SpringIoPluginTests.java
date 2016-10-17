package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringIoPlugin}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class SpringIoPluginTests {

	private final Project project = ProjectBuilder.builder().build();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File jdk7Home;

	private File jdk8Home;

	private File java7;

	private File java8;

	@Before
	public void setup() throws IOException {
		this.jdk7Home = this.tempFolder.newFolder();
		this.java7 = new File(jdk7Home, "bin/java");
		this.java7.getParentFile().mkdirs();
		this.java7.createNewFile();

		this.jdk8Home = this.tempFolder.newFolder();
		this.java8 = new File(jdk8Home, "bin/java");
		this.java8.getParentFile().mkdirs();
		this.java8.createNewFile();
	}

	@Test
	public void pluginCanBeAppliedToNonJavaProject() {
		applyPlugin(SpringIoPlugin.class);
		assertThat(project.getConfigurations().findByName("springIoTestRuntime")).isNull();
	}

	@Test
	public void pluginCanBeAppliedToGroovyProject() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(GroovyPlugin.class);
		project.getConfigurations().getByName("springIoTestRuntime");
	}

	@Test
	public void pluginSetsUpSpringIoTestRuntimeConfiguration() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Configuration configuration = project.getConfigurations().getByName("springIoTestRuntime");
		assertThat(configuration.getExtendsFrom()).contains(project.getConfigurations().getByName("testRuntime"));
	}

	@Test
	public void pluginAppliesTheDependencyManagementPlugin() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(project.getPlugins().findPlugin(DependencyManagementPlugin.class)).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pluginCreatesTheSpringIoCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Task task = project.getTasks().findByName("springIoCheck");
		assertThat(task).isNotNull();
		assertThat((Set<Task>)task.getTaskDependencies().getDependencies(task)).containsExactlyInAnyOrder(
				project.getTasks().findByName("springIoTest"),
				project.getTasks().findByName("springIoAlternativeDependenciesCheck"),
				project.getTasks().findByName("springIoDependencyVersionMappingCheck"),
				project.getTasks().findByName("springIoIncompleteExcludesCheck"));
	}

	@Test
	public void pluginCreatesSpringIoJdk7TestTaskWhenJdk7IsAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) this.project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		org.gradle.api.tasks.testing.Test springIoJdk7Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk7Test");
		assertThat(springIoJdk7Test).isNotNull();
		assertThat(springIoJdk7Test.getExecutable()).isEqualTo(this.java7.getAbsolutePath());
		assertThat(this.project.getTasks().findByName("springIoJdk8Test")).isNull();
	}

	@Test
	public void pluginCreatesSpringIoJdk8TestTaskWhenJdk8IsAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK8_HOME", this.jdk8Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		org.gradle.api.tasks.testing.Test springIoJdk8Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk8Test");
		assertThat(springIoJdk8Test).isNotNull();
		assertThat(springIoJdk8Test.getExecutable()).isEqualTo(this.java8.getAbsolutePath());
		assertThat(this.project.getTasks().findByName("springIoJdk7Test")).isNull();
	}

	@Test
	public void pluginCreatesSpringIoJdk7And8TestTasksWhenJdk7And8AreAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		ext.set("JDK8_HOME", this.jdk8Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		org.gradle.api.tasks.testing.Test springIoJdk7Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk7Test");
		assertThat(springIoJdk7Test).isNotNull();
		assertThat(springIoJdk7Test.getExecutable()).isEqualTo(this.java7.getAbsolutePath());
		org.gradle.api.tasks.testing.Test springIoJdk8Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk8Test");
		assertThat(springIoJdk8Test).isNotNull();
		assertThat(springIoJdk8Test.getExecutable()).isEqualTo(this.java8.getAbsolutePath());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pluginCreatesSpringIoTestTask() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		ext.set("JDK8_HOME", this.jdk8Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Task springIoTest = project.getTasks().findByName("springIoTest");
		assertThat(springIoTest.getTaskDependencies().getDependencies(springIoTest))
				.containsExactlyInAnyOrder(this.project.getTasks().findByName("springIoJdk7Test"),
						this.project.getTasks().findByName("springIoJdk8Test"));
	}

	@Test
	public void pluginCreatesSpringIoIncompleteExcludesCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoIncompleteExcludesCheck")).isNotNull();
	}

	@Test
	public void pluginCreatesSpringIoAlternativeDependenciesCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoAlternativeDependenciesCheck")).isNotNull();
	}

	@Test
	public void pluginCreatesSpringIoDependencyVersionMappingCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoDependencyVersionMappingCheck")).isNotNull();
	}

	@Test
	public void usesCorrectPathForJavaExecutableOnWindows() {
		assertThat(new SpringIoPlugin().createRelativeJavaExec(true)).isEqualTo("/bin/java.exe");
	}

	private void applyPlugin(Class<?> pluginClass) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("plugin", pluginClass);
		this.project.apply(arguments);
	}

}
