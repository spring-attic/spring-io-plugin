package io.spring.gradle.springio

import io.spring.gradle.dependencymanagement.DependencyManagementExtension

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class DependencyVersionMappingCheckTask extends DefaultTask {

	Configuration configuration

	def dependencyManagement

	@Input
	@Optional
	boolean failOnUnmappedDirectDependency = true

	@Input
	@Optional
	boolean failOnUnmappedTransitiveDependency = false

	@TaskAction
	void checkVersionMapping() {
		if (!configuration) {
			configuration = project.configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME)
		}

		configuration.incoming.beforeResolve(
			new CheckPlatformDependenciesBeforeResolveAction(configuration: configuration,
				dependencyManagement: dependencyManagement,
				failOnUnmappedDirectDependency: failOnUnmappedDirectDependency,
				failOnUnmappedTransitiveDependency: failOnUnmappedTransitiveDependency))

		configuration.resolvedConfiguration
	}

	void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
}
