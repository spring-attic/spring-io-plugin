/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.springio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link IncompleteExcludesTask}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class IncompleteExcludesTaskTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Project project;

	private IncompleteExcludesTask task;

	@Before
	public void setup() {
		this.project = ProjectBuilder.builder().withName("project").build();
		applyPlugin(this.project, JavaPlugin.class);
		this.task = this.project.getTasks().create("springIoIncompleteExcludesCheck",
				IncompleteExcludesTask.class);

	}

	@Test
	public void failsWithGroupOnlyExclusion() {
		Dependency dependency = this.project.getDependencies().add("compile",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("group", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.thrown.expect(IllegalStateException.class);
		this.task.check();
	}

	@Test
	public void failsWithModuleOnlyExclusion() {
		Dependency dependency = this.project.getDependencies().add("compile",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("module", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.thrown.expect(IllegalStateException.class);
		this.task.check();
	}

	@Test
	public void succeedsWithGroupAndModuleExclusion() {
		Dependency dependency = this.project.getDependencies().add("compile",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("group", "commons-logging");
		exclusion.put("module", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.task.check();
	}

	@Test
	public void succeedsWithIncompleteExcludeInTestCompileConfiguration() {
		Dependency dependency = this.project.getDependencies().add("testCompile",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("module", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.task.check();
	}

	@Test
	public void succeedsWithIncompleteExcludeInTestRuntimeConfiguration() {
		Dependency dependency = this.project.getDependencies().add("testRuntime",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("module", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.task.check();
	}

	@Test
	public void succeedsWithIncompleteExcludeInConfigruationThatIsNotChecked() {
		this.task.setConfigurations(new HashSet<>(this.project.getConfigurations()));
		this.project.getConfigurations().create("notChecked");
		Dependency dependency = this.project.getDependencies().add("notChecked",
				"org.springframework:spring-core:3.2.0.RELEASE");
		Map<String, String> exclusion = new HashMap<>();
		exclusion.put("module", "commons-logging");
		((ExternalModuleDependency) dependency).exclude(exclusion);
		this.task.check();
	}

	private void applyPlugin(Project project, Class<?> pluginClass) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("plugin", pluginClass);
		project.apply(arguments);
	}

}
