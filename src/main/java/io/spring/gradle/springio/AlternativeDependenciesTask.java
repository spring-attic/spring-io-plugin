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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} that checks {@Configuration Configurations} for dependencies with
 * preferred alternatives.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class AlternativeDependenciesTask extends DefaultTask {

	private File reportFile = getProject().file(String.valueOf(getProject().getBuildDir())
			+ "/spring-io/alternative-dependencies.log");

	@Input
	@Optional
	private Map<String, String> alternatives;

	private Collection<Configuration> configurations;

	/**
	 * Performs the alternative dependencies check.
	 */
	@TaskAction
	public void check() {
		this.reportFile.getParentFile().mkdirs();

		if (this.alternatives == null) {
			this.alternatives = loadAlternatives();
		}

		if (this.configurations == null) {
			this.configurations = new ArrayList<>();
			for (Configuration configuration : getProject().getConfigurations()) {
				if (!configuration.getName().toLowerCase().contains("test")) {
					this.configurations.add(configuration);
				}
			}
		}

		Map<String, List<String>> problemsByConfiguration = new HashMap<>();
		for (Configuration configuration : this.configurations) {
			List<String> problems = new ArrayList<>();
			for (Dependency dependency : configuration.getDependencies()) {
				if (dependency instanceof ExternalModuleDependency) {
					String problem = checkDependency(dependency.getGroup(),
							dependency.getName());
					if (problem != null) {
						problems.add(problem);
					}
				}
			}
			if (!problems.isEmpty()) {
				problemsByConfiguration.put(configuration.getName(), problems);
			}
		}

		if (!problemsByConfiguration.isEmpty()) {
			PrintWriter reportWriter = null;
			try {
				reportWriter = new PrintWriter(new FileWriter(this.reportFile, true));
				reportWriter.println(getProject().getName());
				Set<Map.Entry<String, List<String>>> entries = problemsByConfiguration
						.entrySet();
				for (Map.Entry<String, List<String>> entry : entries) {
					reportWriter.println("    Configuration: " + entry.getKey());
					for (String problem : entry.getValue()) {
						reportWriter.println("        " + problem);
					}
				}
				throw new IllegalStateException(
						"Found dependencies that have better alternatives. See "
								+ this.reportFile + " for a detailed report");
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
	 * Returns the file to which the alternative dependencies report will be written.
	 *
	 * @return the report file
	 */
	public File getReportFile() {
		return this.reportFile;
	}

	/**
	 * Sets the file to which the alternative dependencies report will be written.
	 *
	 * @param reportFile the report file
	 */
	public void setReportFile(File reportFile) {
		this.reportFile = reportFile;
	}

	/**
	 * Returns the map of alternative dependencies to check for. The maps is of the form
	 * {@code artifactId:groupId -> artifactId:groupId} where the keys are dependencies
	 * and the values are alternatives.
	 *
	 * @return the alternatives
	 */
	public Map<String, String> getAlternatives() {
		return this.alternatives;
	}

	/**
	 * Sets the map of alternative dependencies to check for. The maps is of the form
	 * {@code artifactId:groupId -> artifactId:groupId} where the keys are dependencies
	 * and the values are alternatives.
	 *
	 * @param alternatives the alternatives
	 */
	public void setAlternatives(Map<String, String> alternatives) {
		this.alternatives = alternatives;
	}

	/**
	 * Returns the {@link Configuration Configurations} that will be checked.
	 *
	 * @return the configurations
	 */
	public Collection<Configuration> getConfigurations() {
		return this.configurations;
	}

	/**
	 * Sets the {@link Configuration Configurations} that will be checked.
	 *
	 * @param configurations the configurations
	 */
	public void setConfigurations(Collection<Configuration> configurations) {
		this.configurations = configurations;
	}

	private Map<String, String> loadAlternatives() {
		InputStream stream = getClass()
				.getResourceAsStream("spring-io-alternatives.properties");
		Properties properties = new Properties();
		try {
			properties.load(stream);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException ex) {
				// Continue
			}
		}
		Map<String, String> alternativesMap = new HashMap<>();
		for (String property : properties.stringPropertyNames()) {
			alternativesMap.put(property, properties.getProperty(property));
		}
		return alternativesMap;
	}

	private String checkDependency(String groupId, String artifactId) {
		String id = groupId + ":" + artifactId;
		String alternative = this.alternatives.get(id);
		if (alternative != null) {
			return "Please depend on " + alternative + " instead of " + id;
		}
		return null;
	}

}
