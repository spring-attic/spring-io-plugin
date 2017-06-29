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
import java.util.Map;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link DependencyVersionMappingCheckTask}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class DependencyVersionMappingCheckTaskTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final Map<String, String> managedVersions = new HashMap<>();

	private Project project;

	private Configuration configuration;

	private DependencyVersionMappingCheckTask task;

	@Before
	public void setup() {
		this.project = ProjectBuilder.builder().withName("project").build();
		this.project.getRepositories().mavenCentral();
		this.task = this.project.getTasks().create("dependencyVersionMappingCheck",
				DependencyVersionMappingCheckTask.class);
		this.configuration = this.project.getConfigurations().create("configuration");
		this.task.setConfiguration(this.configuration);
		this.task.setManagedVersions(this.managedVersions);
	}

	@Test
	public void executionSucceedsWithMappedDirectDependency() {
		this.managedVersions.put("commons-logging:commons-logging", "1.2");
		this.project.getDependencies().add("configuration",
				"commons-logging:commons-logging:1.2");
		this.task.checkVersionMapping();
	}

	@Test
	public void selfResolvingDependenciesAreHandledCorrectlyWhenExaminingUnmappedDependency() {
		this.project.getDependencies().add("configuration",
				this.project.files("foo.jar"));
		this.project.getDependencies().add("configuration",
				"commons-logging:commons-logging:1.2");
		this.thrown.expect(InvalidUserDataException.class);
		this.thrown.expectMessage("commons-logging");
		this.task.checkVersionMapping();
	}

	@Test
	public void executionFailsWithUnmappedDirectDependency() {
		this.project.getDependencies().add("configuration",
				"commons-logging:commons-logging:1.2");
		this.thrown.expect(InvalidUserDataException.class);
		this.thrown.expectMessage("commons-logging");
		this.task.checkVersionMapping();
	}

	@Test
	public void executionSucceedsWithUnmappedTransitiveDependency() {
		this.managedVersions.put("org.springframework:spring-core", "4.3.9.RELEASE");
		this.project.getDependencies().add("configuration",
				"org.springframework:spring-core:4.3.9.RELEASE");
		this.task.checkVersionMapping();
	}

	@Test
	public void executionCanBeConfiguredToFailWithUnmappedTransitiveDependency() {
		this.managedVersions.put("org.springframework:spring-core", "4.3.3.RELEASE");
		this.project.getDependencies().add("configuration",
				"org.springframework:spring-core:4.3.3.RELEASE");
		this.task.setFailOnUnmappedTransitiveDependency(true);
		this.thrown.expect(InvalidUserDataException.class);
		this.thrown.expectMessage("commons-logging");
		this.task.checkVersionMapping();
	}

	@Test
	public void executionCanBeConfiguredToSucceedWithUnmappedDirectDependency() {
		this.project.getDependencies().add("configuration",
				"commons-logging:commons-logging:1.2");
		this.task.setFailOnUnmappedDirectDependency(false);
		this.task.checkVersionMapping();
	}

}
