package io.jenkins.plugins.railflow.jenkins.admin;

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
			throw new IllegalArgumentException(io.jenkins.plugins.railflow.jenkins.Messages.globalConfigurationIsNull());
		}
		if (globalConfig.getLicenseContent() == null) {
			throw new IllegalArgumentException(io.jenkins.plugins.railflow.jenkins.Messages.licenseMissing());
		}
		if (globalConfig.getTestRailServers() == null) {
			throw new IllegalArgumentException(io.jenkins.plugins.railflow.jenkins.Messages.testRailServersListIsNull());
		}
		if (globalConfig.getTestRailServers().isEmpty()) {
			throw new IllegalArgumentException(io.jenkins.plugins.railflow.jenkins.Messages.testRailServersListIsEmpty());
		}
	}
}
