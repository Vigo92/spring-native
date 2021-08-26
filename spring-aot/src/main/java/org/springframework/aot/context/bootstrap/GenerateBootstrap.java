/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aot.context.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import org.springframework.aot.ApplicationStructure;
import org.springframework.aot.BootstrapCodeGenerator;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.nativex.AotOptions;
import org.springframework.util.StringUtils;

@Command(mixinStandardHelpOptions = true,
		description = "Generate the Java source for the Spring Bootstrap class.")
public class GenerateBootstrap implements Callable<Integer> {

	@Parameters(index = "0", arity = "0..1", description = "The main application class, auto-detected if not provided.")
	private String mainClass;

	@Option(names = {"--sources-out"}, required = true, description = "Output path for the generated sources.")
	private Path sourceOutputPath;

	@Option(names = {"--resources-out"}, required = true, description = "Output path for the generated resources.")
	private Path resourcesOutputPath;

	@Option(names = {"--classes"}, required = true, split = "${sys:path.separator}", description = "Paths to the application compiled classes.")
	private List<Path> classesPaths;

	@Option(names = {"--resources"}, required = true, split = "${sys:path.separator}", description = "Paths to the application compiled resources.")
	private Set<Path> resourcesPaths;

	@Option(names = {"--debug"}, description = "Enable debug logging.")
	private boolean isDebug;

	@Option(names = {"--remove-yaml"}, description = "Remove Yaml support.")
	private boolean removeYaml;

	@Option(names = {"--remove-jmx"}, description = "Remove JMX support.")
	private boolean removeJmx;

	@Option(names = {"--remove-xml"}, description = "Remove XML support.")
	private boolean removeXml;

	@Option(names = {"--remove-spel"}, description = "Remove SpEL support.")
	private boolean removeSpel;

	@Option(names = {"--props"}, split = ",", description = "Build time properties checks.")
	private List<String> propertiesCheck = Collections.emptyList();

	@Override
	public Integer call() throws Exception {
		AotOptions aotOptions = new AotOptions();
		aotOptions.setDebugVerify(this.isDebug);
		aotOptions.setRemoveYamlSupport(this.removeYaml);
		aotOptions.setRemoveJmxSupport(this.removeJmx);
		aotOptions.setRemoveXmlSupport(this.removeXml);
		aotOptions.setRemoveSpelSupport(this.removeSpel);
		aotOptions.setBuildTimePropertiesChecks(propertiesCheck.toArray(new String[0]));

		String[] classPath = StringUtils.tokenizeToStringArray(System.getProperty("java.class.path"), File.pathSeparator);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (!this.isDebug) {
			LoggingSystem loggingSystem = LoggingSystem.get(classLoader);
			loggingSystem.beforeInitialize();
		}
		BootstrapCodeGenerator generator = new BootstrapCodeGenerator(aotOptions);
		ApplicationStructure applicationStructure = new ApplicationStructure(this.sourceOutputPath, this.resourcesOutputPath, this.resourcesPaths,
				this.classesPaths.get(0), this.mainClass, Arrays.asList(classPath), classLoader);
		generator.generate(applicationStructure);
		return 0;
	}

	public static void main(String[] args) throws IOException {
		int exitCode = new CommandLine(new GenerateBootstrap()).execute(args);
		System.exit(exitCode);
	}
}