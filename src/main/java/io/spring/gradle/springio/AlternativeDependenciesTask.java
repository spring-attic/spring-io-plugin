package io.spring.gradle.springio;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.util.*;

/**
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class AlternativeDependenciesTask extends DefaultTask {

    private File reportFile = getProject().file(String.valueOf(getProject().getBuildDir()) + "/spring-io/alternative-dependencies.log");

    @Input
    @org.gradle.api.tasks.Optional
    private Map<String, String> alternatives;

    private Collection<Configuration> configurations;

    @TaskAction
    public void check() {
        this.reportFile.getParentFile().mkdirs();

        if (this.alternatives == null) {
            this.alternatives = loadAlternatives();
        }

        if (this.configurations == null) {
            this.configurations = new ArrayList<Configuration>();
            for (Configuration configuration: getProject().getConfigurations()) {
                if (!configuration.getName().toLowerCase().contains("test")) {
                    this.configurations.add(configuration);
                }
            }
        }

        Map<String, List<String>> problemsByConfiguration = new HashMap<String, List<String>>();
        for (Configuration configuration: this.configurations) {
            List<String> problems = new ArrayList<String>();
            for (Dependency dependency: configuration.getDependencies()) {
                if (dependency instanceof ExternalModuleDependency) {
                    String problem = checkDependency(dependency.getGroup(), dependency.getName());
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
                Set<Map.Entry<String, List<String>>> entries = problemsByConfiguration.entrySet();
                for (Map.Entry<String, List<String>> entry : entries) {
                    reportWriter.println("    Configuration: " + entry.getKey());
                    for (String problem : entry.getValue()) {
                        reportWriter.println("        " + problem);
                    }
                }
                throw new IllegalStateException("Found dependencies that have better alternatives. See "
                        + this.reportFile + " for a detailed report");
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            } finally {
                reportWriter.close();
            }
        }
    }

    public File getReportFile() {
        return reportFile;
    }

    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }

    public Map<String, String> getAlternatives() {
        return this.alternatives;
    }

    public void setAlternatives(Map<String, String> alternatives) {
        this.alternatives = alternatives;
    }

    public Collection<Configuration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<Configuration> configurations) {
        this.configurations = configurations;
    }

    private Map<String, String> loadAlternatives() {
        InputStream stream = getClass().getResourceAsStream("spring-io-alternatives.properties");
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
            } catch (IOException ex) {
                // Continue
            }
        }
        Map<String, String> alternativesMap = new HashMap<String, String>();
        for (String property: properties.stringPropertyNames()) {
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
