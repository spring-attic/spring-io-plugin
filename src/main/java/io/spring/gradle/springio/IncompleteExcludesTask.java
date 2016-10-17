package io.spring.gradle.springio;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class IncompleteExcludesTask extends DefaultTask {

	private Collection<Configuration> configurations;

	private File reportFile = getProject().file(String.valueOf(getProject().getBuildDir()) + "/spring-io/incomplete-excludes.log");

	@TaskAction
	public void check() {
		this.reportFile.getParentFile().mkdirs();

		if (this.configurations == null) {
			this.configurations = new ArrayList<Configuration>();
			for (Configuration configuration: getProject().getConfigurations()) {
				if (!configuration.getName().toLowerCase().contains("test")) {
					this.configurations.add(configuration);
				}
			}
		}

		Map<String, Map<Dependency, List<String>>>  problemsByConfiguration = new HashMap<String, Map<Dependency, List<String>>>();
		for (Configuration configuration: this.configurations) {
			Map<Dependency, List<String>> problemsByDependency = new HashMap<Dependency, List<String>>();
			for (Dependency dependency: configuration.getDependencies()) {
				if (dependency instanceof ExternalModuleDependency) {
					List<String> problems = new ArrayList<String>();
					for (ExcludeRule excludeRule: ((ExternalModuleDependency) dependency).getExcludeRules()) {
						if (excludeRule.getGroup() == null || excludeRule.getGroup().length() == 0) {
							problems.add("Exclude for module " + excludeRule.getModule() + " does not specify a group. The exclusion will not be included in generated POMs");
						}
						else if (excludeRule.getModule() == null || excludeRule.getModule().length() == 0) {
							problems.add("Exclude for group " + excludeRule.getGroup() + " does not specify a module. The exclusion will not be included in generated POMs");
						}
					}
					if (!problems.isEmpty()) {
						problemsByDependency.put(dependency, problems);
					}
				}
			}
			if (!problemsByDependency.isEmpty()) {
				problemsByConfiguration.put(configuration.getName(), problemsByDependency);
			}
		}
		if (!problemsByConfiguration.isEmpty()) {
			PrintWriter reportWriter = null;
			try {
				reportWriter = new PrintWriter(new FileWriter(this.reportFile, true));
				reportWriter.println(getProject().getName());
				for (Map.Entry<String, Map<Dependency, List<String>>> configurationEntry : problemsByConfiguration.entrySet()) {
					reportWriter.println("    Configuration: " + configurationEntry.getKey());
					for (Map.Entry<Dependency, List<String>> dependencyEntry: configurationEntry.getValue().entrySet()) {
						Dependency dependency = dependencyEntry.getKey();
						reportWriter.println("        " + dependency.getGroup() + ":" + dependency.getName() + ":" +
								dependency.getVersion());
						for (String problem : dependencyEntry.getValue()) {
							reportWriter.println("            " + problem);
						}
					}
				}
				throw new IllegalStateException("Found incomplete dependency exclusions. See " + this.reportFile
						+ " for a detailed report");
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			} finally {
				reportWriter.close();
			}
		}

	}

	public Collection<Configuration> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(Collection<Configuration> configurations) {
		this.configurations = configurations;
	}

	public File getReportFile() {
		return reportFile;
	}

	public void setReportFile(File reportFile) {
		this.reportFile = reportFile;
	}

}
