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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolvableDependencies;

/**
 * An {@link Action} that checks {@link ResolvableDependencies} to ensure that each
 * dependency is part of the Spring IO Platform.
 *
 * @author Andy Wilkinson
 */
class CheckPlatformDependenciesBeforeResolveAction
		implements Action<ResolvableDependencies> {

	private final Configuration configuration;

	private final Map<String, String> managedVersions;

	private final boolean failOnUnmappedDirectDependency;

	private final boolean failOnUnmappedTransitiveDependency;

	CheckPlatformDependenciesBeforeResolveAction(Configuration configuration,
			Map<String, String> managedVersions, boolean failOnUnmappedDirectDependency,
			boolean failOnUnmappedTransitiveDependency) {
		this.configuration = configuration;
		this.managedVersions = managedVersions;
		this.failOnUnmappedDirectDependency = failOnUnmappedDirectDependency;
		this.failOnUnmappedTransitiveDependency = failOnUnmappedTransitiveDependency;
	}

	@Override
	public void execute(ResolvableDependencies resolvableDependencies) {
		final CheckingDependencyResolveDetailsAction action = new CheckingDependencyResolveDetailsAction(
				this.configuration, this.managedVersions);
		this.configuration.getResolutionStrategy().eachDependency(action);
		this.configuration.getIncoming()
				.afterResolve(new Action<ResolvableDependencies>() {

					@Override
					public void execute(ResolvableDependencies resolvableDependencies) {
						String message = null;
						if (CheckPlatformDependenciesBeforeResolveAction.this.failOnUnmappedDirectDependency
								&& !action.getUnmappedDirectDependencies().isEmpty()) {
							message = "The following direct dependencies do not have Spring IO versions: \n";
							for (ModuleVersionSelector unmappedDirectDependency : action
									.getUnmappedDirectDependencies()) {
								message += "     - " + unmappedDirectDependency.getGroup()
										+ ":" + unmappedDirectDependency.getName() + "\n";
							}
						}
						if (CheckPlatformDependenciesBeforeResolveAction.this.failOnUnmappedTransitiveDependency
								&& !action.getUnmappedTransitiveDependencies()
										.isEmpty()) {
							message = message == null ? "" : message;
							message += "The following transitive dependencies do not have Spring IO versions: \n";
							for (ModuleVersionSelector unmappedTransitiveDependency : action
									.getUnmappedTransitiveDependencies()) {
								message += "     - "
										+ unmappedTransitiveDependency.getGroup() + ":"
										+ unmappedTransitiveDependency.getName() + "\n";
							}
						}
						if (message != null) {
							message += "\nPlease refer to the plugin's README for further instructions: https://github.com/spring-gradle-plugins/spring-io-plugin/blob/master/README.adoc";
							throw new InvalidUserDataException(message);
						}
					}

				});
	}

	private static class CheckingDependencyResolveDetailsAction
			implements Action<DependencyResolveDetails> {

		private final Configuration configuration;

		private final Map<String, String> managedVersions;

		private final List<ModuleVersionSelector> unmappedDirectDependencies = new ArrayList<>();

		private final List<ModuleVersionSelector> unmappedTransitiveDependencies = new ArrayList<>();

		CheckingDependencyResolveDetailsAction(Configuration configuration,
				Map<String, String> managedVersions) {
			this.configuration = configuration;
			this.managedVersions = managedVersions;
		}

		@Override
		public void execute(DependencyResolveDetails details) {
			ModuleVersionSelector requested = details.getRequested();
			String id = details.getRequested().getGroup() + ":"
					+ details.getRequested().getName();
			if (!this.managedVersions.containsKey(id)) {
				if (isDirectDependency(requested)) {
					this.unmappedDirectDependencies.add(requested);
				}
				else {
					this.unmappedTransitiveDependencies.add(requested);
				}
			}
		}

		private boolean isDirectDependency(ModuleVersionSelector selector) {
			for (Dependency dependency : this.configuration.getAllDependencies()) {
				if (dependency instanceof ExternalModuleDependency
						&& dependency.getGroup().equals(selector.getGroup())
						&& dependency.getName().equals(selector.getName())) {
					return true;
				}
			}
			return false;
		}

		private List<ModuleVersionSelector> getUnmappedDirectDependencies() {
			return this.unmappedDirectDependencies;
		}

		private List<ModuleVersionSelector> getUnmappedTransitiveDependencies() {
			return this.unmappedTransitiveDependencies;
		}

	}

}
