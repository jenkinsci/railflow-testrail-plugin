package io.jenkins.plugins.railflow.jenkins.testresults;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import hudson.util.Secret;
import io.jenkins.plugins.railflow.TestRailParameters;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfig;
import io.jenkins.plugins.railflow.jenkins.admin.TestRailServerConfig;
import io.jenkins.plugins.railflow.jenkins.util.RailflowProxySettingsFactory;
import jenkins.model.Jenkins;

/**
 * Default implementation of {@link TestRailParametersFactory}.
 *
 * @author Sergey Oplavin
 */
public enum TestRailParametersFactoryImpl implements TestRailParametersFactory {
	THE_INSTANCE;

	@Override
	public TestRailParameters create(final GlobalConfig globalConfig, final String testRailServerName, final String overriddenUserName,
			final Secret overriddenPassword) {
		if (testRailServerName == null) {
			throw new NullPointerException(io.jenkins.plugins.railflow.jenkins.Messages.testRailServerNameIsNull());
		}
		final List<TestRailServerConfig> testRailServers = globalConfig.getTestRailServers();
		if (testRailServers == null) {
			throw new NullPointerException(io.jenkins.plugins.railflow.jenkins.Messages.testRailServersListIsNull());
		}
		final TestRailServerConfig testRailServerConfig = testRailServers.stream().filter(config -> testRailServerName.equals(config.getName())).findFirst()
				.orElseThrow(() -> new RuntimeException(io.jenkins.plugins.railflow.jenkins.Messages.testRailDoesNotExist(testRailServerName)));
		final String testRailUrl = testRailServerConfig.getTestRailUrl();
		if (StringUtils.isEmpty(testRailUrl)) {
			throw new NullPointerException(io.jenkins.plugins.railflow.jenkins.Messages.testRailUrlIsNull(testRailServerName));
		}
		final String url = testRailServerConfig.getTestRailUrl();
		String userName = testRailServerConfig.getTestRailUserName();
		Secret password = testRailServerConfig.getTestRailPassword();
		if (StringUtils.isNotEmpty(overriddenUserName)) {
			userName = overriddenUserName;
		}
		if (overriddenPassword != null && StringUtils.isNotEmpty(overriddenPassword.getPlainText())) {
			password = overriddenPassword;
		}
		if (StringUtils.isEmpty(userName)) {
			throw new RuntimeException(io.jenkins.plugins.railflow.jenkins.Messages.testRailCredentialsNotSet(testRailServerName));
		}

		final TestRailParameters params = TestRailParameters
				.builder(url, userName, password.getPlainText())
				.proxySettings(RailflowProxySettingsFactory.THE_INSTANCE.create(Jenkins.get().proxy))
				.build();
		return params;
	}
}
