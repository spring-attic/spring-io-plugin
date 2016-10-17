package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.DependencyManagementHandler;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CheckPlatformDependenciesBeforeResolveAction}
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class CheckPlatformDependenciesBeforeResolveActionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Project project;

	private Configuration configuration;

	private DependencyManagementHandler dependencyManagement;

	@Before
	public void setup() {
		this.project = ProjectBuilder.builder().withName("project").build();
		this.project.getRepositories().mavenCentral();
		this.configuration = this.project.getConfigurations().create("configuration");
		this.dependencyManagement = mock(DependencyManagementHandler.class);
		given(this.dependencyManagement.getOwnManagedVersions()).willReturn(new HashMap<String, String>());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executionSucceedsWithMappedDirectDependency() {
		((Map<String, String>)this.dependencyManagement.getOwnManagedVersions())
				.put("commons-logging:commons-logging", "1.2");
		this.project.getDependencies().add("configuration", "commons-logging:commons-logging:1.2");
		new CheckPlatformDependenciesBeforeResolveAction(this.configuration, this.dependencyManagement, true, false)
				.execute(mock(ResolvableDependencies.class));
		this.configuration.resolve();
	}

	@Test
	public void executionFailsWithMappedDirectDependency() {
		this.project.getDependencies().add("configuration", "commons-logging:commons-logging");
		this.thrown.expect(InvalidUserDataException.class);
		this.thrown.expectMessage("commons-logging");
		new CheckPlatformDependenciesBeforeResolveAction(this.configuration, this.dependencyManagement, true, false)
				.execute(mock(ResolvableDependencies.class));
		this.configuration.resolve();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executionSucceedsWithUnmappedTransitiveDependency() {
		((Map<String, String>)this.dependencyManagement.getOwnManagedVersions())
				.put("org.springframework:spring-core", "4.3.3.RELEASE");
		this.project.getDependencies().add("configuration", "org.springframework:spring-core:4.3.3.RELEASE");
		new CheckPlatformDependenciesBeforeResolveAction(this.configuration, this.dependencyManagement, true, false)
				.execute(mock(ResolvableDependencies.class));
		this.configuration.resolve();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executionCanBeConfiguredToFailWithUnmappedTransitiveDependency() {
		((Map<String, String>)this.dependencyManagement.getOwnManagedVersions())
				.put("org.springframework:spring-core", "4.3.3.RELEASE");
		this.project.getDependencies().add("configuration", "org.springframework:spring-core:4.3.3.RELEASE");
		this.thrown.expect(InvalidUserDataException.class);
		this.thrown.expectMessage("commons-logging");
		new CheckPlatformDependenciesBeforeResolveAction(this.configuration, this.dependencyManagement, true, true)
				.execute(mock(ResolvableDependencies.class));
		this.configuration.resolve();
	}

	@Test
	public void executionCanBeConfiguredToSucceedWithUnmappedDirectDependency() {
		this.project.getDependencies().add("configuration", "commons-logging:commons-logging:1.2");
		new CheckPlatformDependenciesBeforeResolveAction(this.configuration, this.dependencyManagement, false, false)
				.execute(mock(ResolvableDependencies.class));
		this.configuration.resolve();
	}
	
}