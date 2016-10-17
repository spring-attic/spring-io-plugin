package io.spring.gradle.springio;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests for {@link AlternativeDependenciesTask}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class AlternativeDependenciesTaskTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Project project;

	private AlternativeDependenciesTask task;

	@Before
	public void setup() {
		this.project = ProjectBuilder.builder().withName("project").build();
		applyPlugin(JavaPlugin.class);
		this.task = this.project.getTasks().create("springIoAlternativeDependenciesCheck",
				AlternativeDependenciesTask.class);
	}


	@Test
	public void defaultAlternativesAreLoaded() {
		this.task.check();
		this.task.getAlternatives();
		assertThat(this.task.getAlternatives()).containsEntry("org.apache.geronimo.specs:geronimo-jta_1.1_spec",
				"javax.transaction:javax.transaction-api");
	}

	@Test
	public void failsWhenDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("compile", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<String, String>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.thrown.expect(IllegalStateException.class);
		this.task.check();
	}

	@Test
	public void succeedsWhenTestCompileDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("testCompile", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<String, String>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.task.check();
	}

	@Test
	public void succeedsWhenTestRuntimeDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("testRuntime", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<String, String>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.task.check();
	}

	private void applyPlugin(Class<?> pluginClass) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("plugin", pluginClass);
		this.project.apply(arguments);
	}

}