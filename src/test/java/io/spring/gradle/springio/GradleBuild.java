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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A {@link TestRule} for running a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild implements TestRule {

	private final TemporaryFolder temp = new TemporaryFolder();

	private File projectDir;

	private String script;

	private String gradleVersion;

	@Override
	public Statement apply(final Statement base, Description description) {
		URL scriptUrl = findDefaultScript(description);
		if (scriptUrl != null) {
			script(scriptUrl.getFile());
		}
		return this.temp.apply(new Statement() {

			@Override
			public void evaluate() throws Throwable {
				before();
				try {
					base.evaluate();
				}
				finally {
					after();
				}
			}

		}, description);
	}

	private URL findDefaultScript(Description description) {
		URL scriptUrl = getScriptForTestMethod(description);
		if (scriptUrl != null) {
			return scriptUrl;
		}
		return getScriptForTestClass(description.getTestClass());
	}

	private URL getScriptForTestMethod(Description description) {
		String name = description.getTestClass().getSimpleName() + "-"
				+ removeGradleVersion(description.getMethodName()) + ".gradle";
		return description.getTestClass().getResource(name);
	}

	private String removeGradleVersion(String methodName) {
		return methodName.replaceAll("\\[Gradle .+\\]", "").trim();
	}

	private URL getScriptForTestClass(Class<?> testClass) {
		return testClass.getResource(testClass.getSimpleName() + ".gradle");
	}

	private void before() throws IOException {
		this.projectDir = this.temp.newFolder();
	}

	private void after() {
		GradleBuild.this.script = null;
	}

	private String pluginClasspath() {
		return absolutePath("bin") + "," + absolutePath("build/classes/java/main") + ","
				+ absolutePath("build/resources/main") + ","
				+ pathOfJarContaining(DependencyManagementPlugin.class);
	}

	private String absolutePath(String path) {
		return new File(path).getAbsolutePath();
	}

	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public GradleBuild script(String script) {
		this.script = script;
		return this;
	}

	public BuildResult build(String... arguments) {
		try {
			return prepareRunner(arguments).build();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public BuildResult buildAndFail(String... arguments) {
		try {
			return prepareRunner(arguments).buildAndFail();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public GradleRunner prepareRunner(String... arguments) throws IOException {
		Files.readAllBytes(new File(this.script).toPath());
		Files.copy(new File(this.script).toPath(),
				new File(this.projectDir, "build.gradle").toPath(),
				StandardCopyOption.REPLACE_EXISTING);
		GradleRunner gradleRunner = GradleRunner.create().withProjectDir(this.projectDir)
				.forwardOutput();
		if (this.gradleVersion != null) {
			gradleRunner.withGradleVersion(this.gradleVersion);
		}
		List<String> allArguments = new ArrayList<>();
		allArguments.add("-PpluginClasspath=" + pluginClasspath());
		allArguments.add("--stacktrace");
		allArguments.addAll(Arrays.asList(arguments));
		System.out.println(allArguments);
		return gradleRunner.withArguments(allArguments);
	}

	public File getProjectDir() {
		return this.projectDir;
	}

	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}

	public GradleBuild gradleVersion(String version) {
		this.gradleVersion = version;
		return this;
	}

	public String getGradleVersion() {
		return this.gradleVersion;
	}

}
