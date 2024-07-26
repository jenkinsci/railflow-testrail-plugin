package io.railflow.jenkins.admin;

/**
 * Factory for producing {@link GlobalConfig} objects.
 *
 * @author Sergey Oplavin
 */
@FunctionalInterface
public interface GlobalConfigFactory {
	GlobalConfig create();
}
