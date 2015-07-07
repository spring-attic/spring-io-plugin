package io.spring.gradle.springio

import io.spring.gradle.dependencymanagement.DependencyManagementExtension

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.ResolvableDependencies

class CheckPlatformDependenciesBeforeResolveAction implements Action<ResolvableDependencies> {

	Configuration configuration

	def dependencyManagement

	boolean failOnUnmappedDirectDependency = true

	boolean failOnUnmappedTransitiveDependency = false

	@Override
	public void execute(ResolvableDependencies resolvableDependencies) {
		CheckingDependencyResolveDetailsAction checkingAction = new CheckingDependencyResolveDetailsAction(
				configuration: configuration, dependencyManagement: dependencyManagement)

		configuration.resolutionStrategy.eachDependency checkingAction
		configuration.incoming.afterResolve {
			String message
			if (failOnUnmappedDirectDependency && checkingAction.unmappedDirectDependencies) {
				message = "The following direct dependencies do not have Spring IO versions: \n" + checkingAction.unmappedDirectDependencies.collect { "    - $it.group:$it.name" }.join("\n")
			}

			if (failOnUnmappedTransitiveDependency && checkingAction.unmappedTransitiveDependencies) {
				message = message ? message + ". " : ""
				message += "The following transitive dependencies do not have Spring IO versions: \n" + checkingAction.unmappedTransitiveDependencies.collect { "    - $it.group:$it.name" }.join("\n")
			}

			if (message) {
				message += "\nPlease refer to the plugin's README for further instructions: https://github.com/spring-projects/gradle-plugins/tree/master/spring-io-plugin#dealing-with-unmapped-dependencies"
				throw new InvalidUserDataException(message)
			}
		}
	}

	private static class CheckingDependencyResolveDetailsAction implements Action<DependencyResolveDetails> {

		Configuration configuration

		def dependencyManagement

		List<ModuleVersionSelector> unmappedDirectDependencies = []

		List<ModuleVersionSelector> unmappedTransitiveDependencies = []

		void execute(DependencyResolveDetails details) {
			ModuleVersionSelector requested = details.requested
			Map managedVersions = dependencyManagement.managedVersions
			String id = "${details.requested.group}:${details.requested.name}"
			if (!managedVersions[id]) {
				if (isDirectDependency(requested)) {
					unmappedDirectDependencies << requested
				} else {
					unmappedTransitiveDependencies << requested
				}
			}
		}

		private boolean isDirectDependency(ModuleVersionSelector selector) {
			for (Dependency dependency: configuration.allDependencies) {
				if (dependency.group == selector.group && dependency.name == selector.name) {
					return true
				}
			}
			return false
		}
	}

}
