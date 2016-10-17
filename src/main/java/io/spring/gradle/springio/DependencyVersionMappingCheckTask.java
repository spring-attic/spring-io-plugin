package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.DependencyManagementHandler;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public class DependencyVersionMappingCheckTask extends DefaultTask {

	private Configuration configuration;

	private DependencyManagementHandler dependencyManagement;

	@Input
	@Optional
	private boolean failOnUnmappedDirectDependency = true;

	@Input
	@Optional
	private boolean failOnUnmappedTransitiveDependency = false;

	@TaskAction
	public void checkVersionMapping() {
		if (this.configuration == null) {
			configuration = getProject().getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
		}
		configuration.getIncoming().beforeResolve(new CheckPlatformDependenciesBeforeResolveAction(this.configuration,
				this.dependencyManagement, this.failOnUnmappedDirectDependency,
				this.failOnUnmappedTransitiveDependency));
		configuration.resolve();
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public DependencyManagementHandler getDependencyManagement() {
		return this.dependencyManagement;
	}

	public void setDependencyManagement(DependencyManagementHandler dependencyManagement) {
		this.dependencyManagement = dependencyManagement;
	}

	public boolean isFailOnUnmappedDirectDependency() {
		return this.failOnUnmappedDirectDependency;
	}

	public void setFailOnUnmappedDirectDependency(boolean failOnUnmappedDirectDependency) {
		this.failOnUnmappedDirectDependency = failOnUnmappedDirectDependency;
	}

	public boolean isFailOnUnmappedTransitiveDependency() {
		return this.failOnUnmappedTransitiveDependency;
	}

	public void setFailOnUnmappedTransitiveDependency(boolean failOnUnmappedTransitiveDependency) {
		this.failOnUnmappedTransitiveDependency = failOnUnmappedTransitiveDependency;
	}

}
