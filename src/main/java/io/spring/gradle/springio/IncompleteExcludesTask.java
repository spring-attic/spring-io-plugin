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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} that checks that dependency exclusions are not incomplete. An exclusion
 * is deemed to be incomplete if it does not specify both a group and a module.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class IncompleteExcludesTask extends DefaultTask {

	private Collection<Configuration> configurations;

	private File reportFile = getProject().file(String.valueOf(getProject().getBuildDir())
			+ "/spring-io/incomplete-excludes.log");

	/**
	 * Performs the incomplete exlusions check.
	 */
	@TaskAction
	public void check() {
		this.reportFile.getParentFile().mkdirs();
		if (this.configurations == null) {
			this.configurations = new ArrayList<>();
			for (Configuration configuration : getProject().getConfigurations()) {
				if (!configuration.getName().toLowerCase().contains("test")) {
					this.configurations.add(configuration);
				}
			}
		}
		Map<String, Map<Dependency, List<String>>> problemsByConfiguration = new HashMap<>();
		for (Configuration configuration : this.configurations) {
			Map<Dependency, List<String>> problemsByDependency = new HashMap<>();
			for (Dependency dependency : configuration.getDependencies()) {
				if (dependency instanceof ExternalModuleDependency) {
					List<String> problems = new ArrayList<>();
					for (ExcludeRule excludeRule : ((ExternalModuleDependency) dependency)
							.getExcludeRules()) {
						if (excludeRule.getGroup() == null
								|| excludeRule.getGroup().length() == 0) {
							problems.add("Exclude for module " + excludeRule.getModule()
									+ " does not specify a group. The exclusion will not be included in generated POMs");
						}
						else if (excludeRule.getModule() == null
								|| excludeRule.getModule().length() == 0) {
							problems.add("Exclude for group " + excludeRule.getGroup()
									+ " does not specify a module. The exclusion will not be included in generated POMs");
						}
					}
					if (!problems.isEmpty()) {
						problemsByDependency.put(dependency, problems);
					}
				}
			}
			if (!problemsByDependency.isEmpty()) {
				problemsByConfiguration.put(configuration.getName(),
						problemsByDependency);
			}
		}
		if (!problemsByConfiguration.isEmpty()) {
			PrintWriter reportWriter = null;
			try {
				reportWriter = new PrintWriter(new FileWriter(this.reportFile, true));
				reportWriter.println(getProject().getName());
				for (Map.Entry<String, Map<Dependency, List<String>>> configurationEntry : problemsByConfiguration
						.entrySet()) {
					reportWriter
							.println("    Configuration: " + configurationEntry.getKey());
					for (Map.Entry<Dependency, List<String>> dependencyEntry : configurationEntry
							.getValue().entrySet()) {
						Dependency dependency = dependencyEntry.getKey();
						reportWriter.println("        " + dependency.getGroup() + ":"
								+ dependency.getName() + ":" + dependency.getVersion());
						for (String problem : dependencyEntry.getValue()) {
							reportWriter.println("            " + problem);
						}
					}
				}
				throw new IllegalStateException(
						"Found incomplete dependency exclusions. See " + this.reportFile
								+ " for a detailed report");
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
			finally {
				reportWriter.close();
			}
		}
	}

	/**
	 * Returns the {@link Configuration Configurations} that will be checked for
	 * incomplete dependency exclusions.
	 *
	 * @return the configurations
	 */
	public Collection<Configuration> getConfigurations() {
		return this.configurations;
	}

	/**
	 * Sets the {@link Configuration Configurations} that will be checked for incomplete
	 * dependency exclusions.
	 *
	 * @param configurations the configurations
	 */
	public void setConfigurations(Collection<Configuration> configurations) {
		this.configurations = configurations;
	}

	/**
	 * Returns the file to which the incomplete exclusions report will be written.
	 *
	 * @return the report file
	 */
	public File getReportFile() {
		return this.reportFile;
	}

	/**
	 * Sets the file to which the incomplete exclusions report will be written.
	 *
	 * @param reportFile the report file
	 */
	public void setReportFile(File reportFile) {
		this.reportFile = reportFile;
	}

}
