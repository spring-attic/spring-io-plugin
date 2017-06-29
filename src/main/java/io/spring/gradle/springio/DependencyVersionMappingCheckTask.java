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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} that checks that each of a {@link Configuration Configuration's}
 * dependencies is part of the Spring IO Platform.
 *
 * @author Andy Wilkinson
 */
public class DependencyVersionMappingCheckTask extends ConventionTask {

	private Configuration configuration;

	private Map<String, String> managedVersions;

	private boolean failOnUnmappedDirectDependency = true;

	private boolean failOnUnmappedTransitiveDependency = false;

	/**
	 * Creates a new {@code DependencyVersionMappingCheckTask}.
	 */
	public DependencyVersionMappingCheckTask() {
		getOutputs().upToDateWhen(Specs.SATISFIES_ALL);
	}

	/**
	 * Performs the dependency versions mapping check.
	 */
	@TaskAction
	public void checkVersionMapping() {
		Set<String> unmappedDirectDependencies = new LinkedHashSet<>();
		Set<String> unmappedTransitiveDependencies = new LinkedHashSet<>();
		for (ResolvedArtifact resolvedArtifact : this.configuration
				.getResolvedConfiguration().getResolvedArtifacts()) {
			ModuleVersionIdentifier module = resolvedArtifact.getModuleVersion().getId();
			String id = module.getGroup() + ":" + module.getName();
			if (!this.managedVersions.containsKey(id)) {
				if (isDirectDependency(module)) {
					if (this.failOnUnmappedDirectDependency) {
						unmappedDirectDependencies.add(id);
					}
				}
				else if (this.failOnUnmappedTransitiveDependency) {
					unmappedTransitiveDependencies.add(id);
				}
			}
		}
		String message = "";
		if (!unmappedDirectDependencies.isEmpty()) {
			message += "The following direct dependencies do not have Spring IO versions: \n";
			for (String dependency : unmappedDirectDependencies) {
				message += "    - " + dependency + "\n";
			}
		}
		if (!unmappedTransitiveDependencies.isEmpty()) {
			message = "The following transitive dependencies do not have Spring IO versions: \n";
			for (String dependency : unmappedTransitiveDependencies) {
				message += "    - " + dependency + "\n";
			}
		}
		if (message.length() > 0) {
			throw new InvalidUserDataException(message);
		}
	}

	/**
	 * Returns the {@link Configuration} that will be checked by this task.
	 *
	 * @return the configuration
	 */
	@Input
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
	@Input
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
	@Input
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
	@Input
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

	private boolean isDirectDependency(ModuleVersionIdentifier module) {
		for (Dependency dependency : this.configuration.getAllDependencies()) {
			if (dependency instanceof ExternalModuleDependency
					&& dependency.getGroup().equals(module.getGroup())
					&& dependency.getName().equals(module.getName())) {
				return true;
			}
		}
		return false;
	}

}
