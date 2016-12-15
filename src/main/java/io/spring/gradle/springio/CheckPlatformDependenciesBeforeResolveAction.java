package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementHandler;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckPlatformDependenciesBeforeResolveAction implements Action<ResolvableDependencies> {

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
		final CheckingDependencyResolveDetailsAction action = new CheckingDependencyResolveDetailsAction(configuration, managedVersions);
		configuration.getResolutionStrategy().eachDependency(action);
		configuration.getIncoming().afterResolve(new Action<ResolvableDependencies>() {
			@Override
			public void execute(ResolvableDependencies resolvableDependencies) {
				String message = null;
				if (failOnUnmappedDirectDependency && !action.getUnmappedDirectDependencies().isEmpty()) {
					message = "The following direct dependencies do not have Spring IO versions: \n";
					for (ModuleVersionSelector unmappedDirectDependency: action.getUnmappedDirectDependencies()) {
						message += "     - " + unmappedDirectDependency.getGroup() + ":" + unmappedDirectDependency.getName() + "\n";
					}
				}
				if (failOnUnmappedTransitiveDependency && !action.getUnmappedTransitiveDependencies().isEmpty()) {
					message = message == null ? "": message;
					message += "The following transitive dependencies do not have Spring IO versions: \n";
					for (ModuleVersionSelector unmappedTransitiveDependency: action.getUnmappedTransitiveDependencies()) {
						message += "     - " + unmappedTransitiveDependency.getGroup() + ":" + unmappedTransitiveDependency.getName() + "\n";
					}
				}
				if (message != null) {
					message += "\nPlease refer to the plugin's README for further instructions: https://github.com/spring-gradle-plugins/spring-io-plugin/blob/master/README.adoc";
					throw new InvalidUserDataException(message);
				}
			}
		});
	}

	private static class CheckingDependencyResolveDetailsAction implements Action<DependencyResolveDetails> {

		private final Configuration configuration;

		private final Map<String, String> managedVersions;

		private final List<ModuleVersionSelector> unmappedDirectDependencies = new ArrayList<ModuleVersionSelector>();

		private final List<ModuleVersionSelector> unmappedTransitiveDependencies = new ArrayList<ModuleVersionSelector>();

		CheckingDependencyResolveDetailsAction(Configuration configuration,
				Map<String, String> managedVersions) {
			this.configuration = configuration;
			this.managedVersions = managedVersions;
		}

		@SuppressWarnings("unchecked")
		public void execute(DependencyResolveDetails details) {
			ModuleVersionSelector requested = details.getRequested();
			String id = details.getRequested().getGroup() + ":" + details.getRequested().getName();
			if (!managedVersions.containsKey(id)) {
				if (isDirectDependency(requested)) {
					unmappedDirectDependencies.add(requested);
				}
				else {
					unmappedTransitiveDependencies.add(requested);
				}
			}
		}

		private boolean isDirectDependency(ModuleVersionSelector selector) {
			for (Dependency dependency : configuration.getAllDependencies()) {
				if (dependency.getGroup().equals(selector.getGroup()) && dependency.getName().equals(selector.getName())) {
					return true;
				}
			}
			return false;
		}

		private List<ModuleVersionSelector> getUnmappedDirectDependencies() {
			return unmappedDirectDependencies;
		}

		private List<ModuleVersionSelector> getUnmappedTransitiveDependencies() {
			return unmappedTransitiveDependencies;
		}

	}

}
