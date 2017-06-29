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

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyVersionMappingCheckTask}.
 *
 * @author Andy Wilkinson
 */
public class DependencyVersionMappingCheckTaskIntegrationTests {

	@Rule
	public GradleBuild gradleBuild = new GradleBuild();

	@Test
	public void failsWithNoConfiguration() {
		assertThat(this.gradleBuild.buildAndFail("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.FAILED);
	}

	@Test
	public void failsWithNoManagedVersions() {
		assertThat(this.gradleBuild.buildAndFail("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.FAILED);
	}

	@Test
	public void succeedsWithConfigurationAndManagedVersions() {
		assertThat(this.gradleBuild.build("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void upToDateWhenBuiltTwice() {
		assertThat(this.gradleBuild.build("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@Test
	public void notUpToDateWhenDependenciesChange() {
		assertThat(
				this.gradleBuild.build("-PmanageVerions", "dependencyVersionMappingCheck")
						.task(":dependencyVersionMappingCheck").getOutcome())
								.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild
				.build("-PmanageVersions", "-PspringVersion=4.3.9.RELEASE",
						"dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void notUpToDateWhenManagedVersionsChange() {
		assertThat(this.gradleBuild.build("dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild
				.build("-PmanageVersions", "dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void notUpToDateWhenFailOnUnmappedDirectChanges() {
		assertThat(this.gradleBuild
				.build("-PfailDirect=true", "dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild
				.build("-PfailDirect=false", "dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void notUpToDateWhenFailOnUnmappedTransitiveChanges() {
		assertThat(this.gradleBuild
				.build("-PfailTransitive=false", "dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild
				.build("-PfailTransitive=true", "dependencyVersionMappingCheck")
				.task(":dependencyVersionMappingCheck").getOutcome())
						.isEqualTo(TaskOutcome.SUCCESS);
	}

}
