/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.springio;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
		assertThat(this.task.getAlternatives()).containsEntry(
				"org.apache.geronimo.specs:geronimo-jta_1.1_spec",
				"javax.transaction:javax.transaction-api");
	}

	@Test
	public void failsWhenDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("compile", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.thrown.expect(IllegalStateException.class);
		this.task.check();
	}

	@Test
	public void succeedsWhenTestCompileDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("testCompile", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.task.check();
	}

	@Test
	public void succeedsWhenTestRuntimeDependencyHasPreferredAlternative() {
		this.project.getDependencies().add("testRuntime", "asm:asm:3.3.1");
		Map<String, String> alternatives = new HashMap<>();
		alternatives.put("asm:asm", "Please use some alternative");
		this.task.setAlternatives(alternatives);
		this.task.check();
	}

	private void applyPlugin(Class<?> pluginClass) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("plugin", pluginClass);
		this.project.apply(arguments);
	}

}
