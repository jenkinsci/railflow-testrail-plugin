package io.jenkins.plugins.railflow.jenkins.admin;

import io.jenkins.plugins.railflow.jenkins.Messages;

/**
 * Implementation of {@link GlobalConfigValidator} which checks required params
 * and throws an exception if anything if missing.
 *
 * @author Sergey Oplavin
 */
public enum GlobalConfigValidatorImpl implements GlobalConfigValidator {
	THE_INSTANCE;

	@Override
	public void validate(GlobalConfig globalConfig) {
		if (globalConfig == null) {
			throw new IllegalArgumentException(Messages.globalConfigurationIsNull());
		}
		if (globalConfig.getLicenseContent() == null) {
			throw new IllegalArgumentException(Messages.licenseMissing());
		}
		if (globalConfig.getTestRailServers() == null) {
			throw new IllegalArgumentException(Messages.testRailServersListIsNull());
		}
		if (globalConfig.getTestRailServers().isEmpty()) {
			throw new IllegalArgumentException(Messages.testRailServersListIsEmpty());
		}
	}
}
