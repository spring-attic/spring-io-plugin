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

import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} that checks that each of a {@link Configuration Configuration's}
 * dependencies is part of the Spring IO Platform.
 *
 * @author Andy Wilkinson
 */
public class DependencyVersionMappingCheckTask extends DefaultTask {

	private Configuration configuration;

	private Map<String, String> managedVersions;

	@Input
	@Optional
	private boolean failOnUnmappedDirectDependency = true;

	@Input
	@Optional
	private boolean failOnUnmappedTransitiveDependency = false;

	/**
	 * Performs the dependency versions mapping check.
	 */
	@TaskAction
	public void checkVersionMapping() {
		if (this.configuration == null) {
			this.configuration = getProject().getConfigurations()
					.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
		}
		this.configuration.getIncoming()
				.beforeResolve(new CheckPlatformDependenciesBeforeResolveAction(
						this.configuration, this.managedVersions,
						this.failOnUnmappedDirectDependency,
						this.failOnUnmappedTransitiveDependency));
		this.configuration.resolve();
	}

	/**
	 * Returns the {@link Configuration} that will be checked by this task.
	 *
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Sets the {@link Configuration} that will be checked by this task.
	 *
	 * @param configuration the configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Returns the managed versions ({@code groupId:artifactId -> version} that are part
	 * of the Spring IO Platform.
	 *
	 * @return the managed versions
	 */
	public Map<String, String> getManagedVersions() {
		return this.managedVersions;
	}

	/**
	 * Sets the managed versions ({@code groupId:artifactId -> version} that are part of
	 * the Spring IO Platform.
	 *
	 * @param managedVersions the managed versions
	 */
	public void setManagedVersions(Map<String, String> managedVersions) {
		this.managedVersions = managedVersions;
	}

	/**
	 * Returns whether the task should fail when a direct dependency is found that is not
	 * part of the Spring IO Platform.
	 *
	 * @return {@code true} if the task should fail, {@code false} if it should not
	 */
	public boolean isFailOnUnmappedDirectDependency() {
		return this.failOnUnmappedDirectDependency;
	}

	/**
	 * Sets whether the task should fail when a direct dependency is found that is not
	 * part of the Spring IO Platform.
	 *
	 * @param failOnUnmappedDirectDependency {@code true} if the task should fail,
	 * {@code false} if it should not
	 */
	public void setFailOnUnmappedDirectDependency(
			boolean failOnUnmappedDirectDependency) {
		this.failOnUnmappedDirectDependency = failOnUnmappedDirectDependency;
	}

	/**
	 * Returns whether the task should fail when a transitive dependency is found that is
	 * not part of the Spring IO Platform.
	 *
	 * @return {@code true} if the task should fail, {@code false} if it should not
	 */
	public boolean isFailOnUnmappedTransitiveDependency() {
		return this.failOnUnmappedTransitiveDependency;
	}

	/**
	 * Sets whether the task should fail when a transitive dependency is found that is not
	 * part of the Spring IO Platform.
	 *
	 * @param failOnUnmappedTransitiveDependency {@code true} if the task should fail,
	 * {@code false} if it should not
	 */
	public void setFailOnUnmappedTransitiveDependency(
			boolean failOnUnmappedTransitiveDependency) {
		this.failOnUnmappedTransitiveDependency = failOnUnmappedTransitiveDependency;
	}

}
