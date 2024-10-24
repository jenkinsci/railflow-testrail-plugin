package io.jenkins.plugins.railflow.jenkins.admin;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.railflow.jenkins.util.RailflowUtils;

/**
 * Holds configuration for a single TestRail server.
 *
 * @author Sergey Oplavin
 */
public class TestRailServerConfig extends AbstractDescribableImpl<TestRailServerConfig> {
	private String name;
	private String testRailUrl;
	private String testRailUserName;
	private Secret testRailPassword;

	@DataBoundConstructor
	public TestRailServerConfig() {
	}

	public String getName() {
		return this.name;
	}

	@DataBoundSetter
	public void setName(final String name) {
		this.name = Util.fixEmptyAndTrim(name);
	}

	public String getTestRailUrl() {
		return this.testRailUrl;
	}

	@DataBoundSetter
	public void setTestRailUrl(final String testRailUrl) {
		this.testRailUrl = Util.fixEmptyAndTrim(testRailUrl);
	}

	public String getTestRailUserName() {
		return this.testRailUserName;
	}

	@DataBoundSetter
	public void setTestRailUserName(final String testRailUserName) {
		this.testRailUserName = testRailUserName;
	}

	public Secret getTestRailPassword() {
		return this.testRailPassword;
	}

	@DataBoundSetter
	public void setTestRailPassword(final Secret testRailPassword) {
		this.testRailPassword = testRailPassword;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<TestRailServerConfig> {

		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckName(@QueryParameter final String name) {
			return RailflowUtils.validateRequiredField(name);
		}

		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckTestRailUrl(@QueryParameter final String testRailUrl) {
			final String preparedUrl = Util.fixEmptyAndTrim(testRailUrl);
			if (preparedUrl == null) {
				return FormValidation.error(io.jenkins.plugins.railflow.jenkins.Messages.testRailUrlIsRequired());
			}
			try {
				new URL(testRailUrl);
			} catch (final MalformedURLException e) {
				return FormValidation.error(io.jenkins.plugins.railflow.jenkins.Messages.malformedTestRailUrl(e.getMessage()));
			}
			return FormValidation.ok();
		}

		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckTestRailUserName(@QueryParameter final String testRailUserName) {
			return RailflowUtils.validateRequiredField(testRailUserName);
		}

		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckTestRailPassword(@QueryParameter final String testRailPassword) {
			return RailflowUtils.validateRequiredField(testRailPassword);
		}
	}
}