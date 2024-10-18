package io.jenkins.plugins.railflow.jenkins.testresults;

import com.google.common.collect.ImmutableMap;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.railflow.TestRailParameters;
import io.jenkins.plugins.railflow.UploadParameters;
import io.jenkins.plugins.railflow.commons.SearchMode;
import io.jenkins.plugins.railflow.commons.UploadMode;
import io.jenkins.plugins.railflow.commons.statistics.ServerType;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfig;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfigFactory;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfigFactoryImpl;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfigValidator;
import io.jenkins.plugins.railflow.jenkins.admin.GlobalConfigValidatorImpl;
import io.jenkins.plugins.railflow.jenkins.admin.TestRailServerConfig;
import io.jenkins.plugins.railflow.jenkins.cli.RailflowJenkinsCli;
import io.jenkins.plugins.railflow.jenkins.util.JenkinsSecretPasswordEncrypter;
import io.jenkins.plugins.railflow.jenkins.util.PasswordEncrypter;
import io.jenkins.plugins.railflow.jenkins.util.ProjectUtils;
import io.jenkins.plugins.railflow.jenkins.util.RailflowUtils;
import io.jenkins.plugins.railflow.testreport.model.ReportFormat;
import io.jenkins.plugins.railflow.jenkins.Messages;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Publisher for Railflow.
 *
 * @author Sergey Oplavin
 * @author Liu Yang
 */
public class RailflowPublisher extends Recorder implements SimpleBuildStep {

	private static final Map<ReportFormat, String> TEST_REPORT_FORMATS = ImmutableMap.<ReportFormat, String> builder()
			.put(ReportFormat.ALLURE, Messages.reportFormatAllure())
			.put(ReportFormat.CUCUMBER, Messages.reportFormatCucumber())
			.put(ReportFormat.JUNIT, Messages.reportFormatJunit())
			.put(ReportFormat.NUNIT, Messages.reportFormatNunit())
			.put(ReportFormat.NUNIT_SPECFLOW, Messages.reportFormatNunitSpecflow())
			.put(ReportFormat.PYTEST_RAILFLOW, Messages.reportFormatPytestRailflow())
			.put(ReportFormat.ROBOT, Messages.reportFormatRobot())
			.put(ReportFormat.TESTNG, Messages.reportFormatTestng())
			.put(ReportFormat.TESTNG_STEPS, Messages.reportFormatTestngSteps())
			.put(ReportFormat.TRX, Messages.reportFormatTrx())
			.put(ReportFormat.XUNIT, Messages.reportFormatXunit())
			.put(ReportFormat.PLAYWRIGHT, Messages.reportFormatPlaywright()).build();
	private static final Map<SearchMode, String> SEARCH_MODES = ImmutableMap.<SearchMode, String> builder()
			.put(SearchMode.PATH, Messages.searchModePath())
			.put(SearchMode.NAME, Messages.searchModeName()).build();
	private static final Map<UploadMode, String> UPLOAD_MODES = ImmutableMap.<UploadMode, String> builder()
			.put(UploadMode.CREATE_NO_UPDATE, Messages.createNoUpdate())
			.put(UploadMode.CREATE_UPDATE, Messages.createUpdate())
			.put(UploadMode.NO_CREATE_UPDATE, Messages.noCreateUpdate())
			.put(UploadMode.NO_CREATE_NO_UPDATE, Messages.noCreateNoUpdate()).build();
	private GlobalConfigFactory globalConfigFactory = GlobalConfigFactoryImpl.THE_INSTANCE;
	private TestRailParametersFactory testRailParametersFactory = TestRailParametersFactoryImpl.THE_INSTANCE;
	private UploadParametersFactory uploadParametersFactory = UploadParametersFactoryImpl.THE_INSTANCE;
	private GlobalConfigValidator globalConfigValidator = GlobalConfigValidatorImpl.THE_INSTANCE;
	private RailflowCallableFactory railflowCallableFactory = new RailflowCallableFactoryImpl();
	private PasswordEncrypter passwordEncrypter = JenkinsSecretPasswordEncrypter.THE_INSTANCE;
	private String testRailServerName;
	private String testRailProjectName;
	private boolean failIfUploadFailed;
	private String overriddenUserName;
	private Secret overriddenPassword;
	private List<RailflowUploadJobConfiguration> jobConfigurations;
	private boolean debugLogEnabled;

	@DataBoundConstructor
	public RailflowPublisher(final String testRailServerName, final String testRailProjectName, final String overriddenUserName,
			final Secret overriddenPassword, final boolean failIfUploadFailed, final List<RailflowUploadJobConfiguration> jobConfigurations,
			final boolean debugLogEnabled) {
		this.testRailServerName = Util.fixEmptyAndTrim(testRailServerName);
		this.testRailProjectName = Util.fixEmptyAndTrim(testRailProjectName);
		this.failIfUploadFailed = failIfUploadFailed;
		this.jobConfigurations = jobConfigurations;
		this.overriddenUserName = Util.fixEmptyAndTrim(overriddenUserName);
		setOverriddenPasswordSecret(overriddenPassword);
		this.debugLogEnabled = debugLogEnabled;
	}

	@Override
	public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener)
			throws IOException {
		listener.getLogger().println(" ____         _  _   __  _                 \n"
				+ "|  _ \\  __ _ (_)| | / _|| |  ___ __      __\n"
				+ "| |_) |/ _` || || || |_ | | / _ \\\\ \\ /\\ / /\n"
				+ "|  _ <| (_| || || ||  _|| || (_) |\\ V  V / \n"
				+ "|_| \\_\\\\__,_||_||_||_|  |_| \\___/  \\_/\\_/  \n"
				+ "                                           ");
		listener.getLogger().println(Messages.pluginVersion() + ProjectUtils.getPluginVersion());
		listener.getLogger().println(Messages.startUpload());
		if (!this.doUpdate(run, launcher, listener, workspace)) {
			throw new AbortException(Messages.logFailIfUploadTrue());
		}
	}

	private boolean doUpdate(final Run<?, ?> run, final Launcher launcher, final TaskListener listener, final FilePath workspace) throws AbortException {
		final GlobalConfig globalConfig = this.globalConfigFactory.create();
		this.globalConfigValidator.validate(globalConfig);
		final TestRailParameters testRailParameters = this.testRailParametersFactory.create(globalConfig, this.testRailServerName, this.overriddenUserName,
				this.overriddenPassword);
		if (this.jobConfigurations == null || this.jobConfigurations.isEmpty()) {
			listener.getLogger().println(Messages.noUploadConfigs());
			return true;
		}
		final VirtualChannel channel = launcher.getChannel();
		if (channel == null) {
			throw new AbortException(Messages.virtualChannelIsNull());
		}

		final String licenseContent = this.passwordEncrypter.decrypt(globalConfig.getLicenseContent());
		boolean success = true;
		for (final RailflowUploadJobConfiguration jobConfiguration : this.jobConfigurations) {
			UploadParameters uploadParameters = null;
			try {
				final EnvVars environment = run.getEnvironment(listener);
				uploadParameters = this.uploadParametersFactory.create(this.testRailProjectName, jobConfiguration, environment);
				final List<String> runUrls = channel
						.call(this.railflowCallableFactory
								.create(licenseContent, testRailParameters, uploadParameters, environment.expand(jobConfiguration.getResultFilePattern()),
										run.getId(), listener, workspace, this.debugLogEnabled));
				runUrls.forEach(url -> run.addAction(new RailflowRunLinkAction(url)));

				final UploadParameters finalUploadParameters = uploadParameters;
				collectExportEvent(globalConfig.isUploadStatistics() || globalConfig.isTrial(), t -> RailflowJenkinsCli.collectExportEvent(
						ServerType.Jenkins, globalConfig.isOnlineActivation(), licenseContent, testRailParameters, finalUploadParameters, null));
			} catch (final Exception e) {
				listener.getLogger().println(Messages.uploadErrorMessage() + e.getMessage());
				e.printStackTrace(listener.getLogger());
				if (this.failIfUploadFailed) {
					listener.getLogger().println(Messages.logFailIfUploadTrue());
					success = false;
				} else {
					listener.getLogger().println(Messages.logFailIfUploadFalse());
				}

				// If uploadParameters is null, it means the exception is thrown
				// when executing Run.getEnvironment. So we will not collect
				// this exporting.
				final UploadParameters finalUploadParameters = uploadParameters;
				collectExportEvent((globalConfig.isUploadStatistics() || globalConfig.isTrial()) && uploadParameters != null,
						t -> RailflowJenkinsCli.collectExportEvent(ServerType.Jenkins, ProjectUtils.getPluginVersion(), Jenkins.VERSION,
								globalConfig.isOnlineActivation(), licenseContent, testRailParameters, finalUploadParameters, null, e));
			}
		}

		return success;
	}

	public List<RailflowUploadJobConfiguration> getJobConfigurations() {
		return this.jobConfigurations;
	}

	@DataBoundSetter
	public void setJobConfigurations(final List<RailflowUploadJobConfiguration> jobConfigurations) {
		this.jobConfigurations = jobConfigurations;
	}

	public String getTestRailServerName() {
		return this.testRailServerName;
	}

	@DataBoundSetter
	public void setTestRailServerName(final String testRailServerName) {
		this.testRailServerName = testRailServerName;
	}

	public String getTestRailProjectName() {
		return this.testRailProjectName;
	}

	@DataBoundSetter
	public void setTestRailProjectName(final String testRailProjectName) {
		this.testRailProjectName = testRailProjectName;
	}

	public String getOverriddenUserName() {
		return this.overriddenUserName;
	}

	@DataBoundSetter
	public void setOverriddenUserName(final String overriddenUserName) {
		this.overriddenUserName = overriddenUserName;
	}

	public Secret getOverriddenPassword() {
		return this.overriddenPassword;
	}

	@DataBoundSetter
	public void setOverriddenPassword(final Secret overriddenPassword) {
		setOverriddenPasswordSecret(overriddenPassword);
	}

	public boolean isFailIfUploadFailed() {
		return this.failIfUploadFailed;
	}

	@DataBoundSetter
	public void setFailIfUploadFailed(final boolean failIfUploadFailed) {
		this.failIfUploadFailed = failIfUploadFailed;
	}

	public boolean isDebugLogEnabled() {
		return this.debugLogEnabled;
	}

	@DataBoundSetter
	public void setDebugLogEnabled(final boolean debugLogEnabled) {
		this.debugLogEnabled = debugLogEnabled;
	}

	void setGlobalConfigFactory(final GlobalConfigFactory globalConfigFactory) {
		this.globalConfigFactory = globalConfigFactory;
	}

	void setTestRailParametersFactory(final TestRailParametersFactory testRailParametersFactory) {
		this.testRailParametersFactory = testRailParametersFactory;
	}

	void setUploadParametersFactory(final UploadParametersFactory uploadParametersFactory) {
		this.uploadParametersFactory = uploadParametersFactory;
	}

	void setGlobalConfigValidator(final GlobalConfigValidator globalConfigValidator) {
		this.globalConfigValidator = globalConfigValidator;
	}

	void setRailflowCallableFactory(final RailflowCallableFactory railflowCallableFactory) {
		this.railflowCallableFactory = railflowCallableFactory;
	}

	void setPasswordEncrypter(final PasswordEncrypter passwordEncrypter) {
		this.passwordEncrypter = passwordEncrypter;
	}

	private void collectExportEvent(final boolean collectable, final Consumer<Void> consumer) {
		try {
			if (collectable) {
				consumer.accept(null);
			}
		} catch (final Exception ex) {
			// do nothing
		}
	}

	private void setOverriddenPasswordSecret(final Secret passwordSecret) {
		final String decrypted = this.passwordEncrypter.decrypt(passwordSecret);
		this.overriddenPassword = StringUtils.isNotEmpty(decrypted) ? passwordSecret : null;
	}

	@Extension
	@Symbol("railflow")
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private static final String JOB_CONFIGURATIONS_KEY = "jobConfigurations";
		private static final String SEARCH_MODE_KEY = "searchMode";

		public DescriptorImpl() {
			super(RailflowPublisher.class);
			this.load();
		}

		@Nonnull
		@Override
		public String getDisplayName() {
			return Messages.postBuildDisplayName();
		}

		@Override
		public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public Publisher newInstance(@Nullable final StaplerRequest req, @Nonnull final JSONObject formData) {
			if (req == null) {
				throw new IllegalArgumentException("Stapler request is null");
			}

			final Object configObject = formData.get(JOB_CONFIGURATIONS_KEY);
			if (configObject instanceof JSONObject) {
				final JSONObject jobConfigurations = (JSONObject) configObject;
				removeSearchModeKeyFromConfigsIfEmpty(jobConfigurations);
			} else if (configObject instanceof JSONArray) {
				final JSONArray jobConfigurationsArray = (JSONArray) configObject;
				for (int i = 0; i < jobConfigurationsArray.size(); i++) {
					final JSONObject jobConfigurations = jobConfigurationsArray.getJSONObject(i);
					removeSearchModeKeyFromConfigsIfEmpty(jobConfigurations);
				}
			}

			return req.bindJSON(RailflowPublisher.class, formData);
		}

		public void removeSearchModeKeyFromConfigsIfEmpty(final JSONObject jobConfigurations) {
			if (jobConfigurations != null && jobConfigurations.size() > 0) {
				final String searchMode = jobConfigurations.getString(SEARCH_MODE_KEY);
				if (StringUtils.isEmpty(searchMode)) {
					jobConfigurations.remove(SEARCH_MODE_KEY);
				}
			}
		}

		public List<TestRailServerConfig> getTestRailServerNames() {
			return GlobalConfigFactoryImpl.THE_INSTANCE.create().getTestRailServers();
		}

		public Map<ReportFormat, String> getReportFormats() {
			return TEST_REPORT_FORMATS;
		}

		public Map<SearchMode, String> getSearchModes() {
			return SEARCH_MODES;
		}

		public Map<UploadMode, String> getUploadModes() {
			return UPLOAD_MODES;
		}

		/**
		 * Do check project name.
		 *
		 * @param testRailProjectName
		 *            the test rail project
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckTestRailProjectName(@QueryParameter final String testRailProjectName) {
			return RailflowUtils.validateRequiredField(testRailProjectName);
		}

		/**
		 * Do check test path.
		 *
		 * @param testCasePath
		 *            the test path
		 * @param testReportFormat
		 *            report format
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckTestCasePath(@QueryParameter final String testCasePath, @QueryParameter final ReportFormat testReportFormat) {
			final String testPathTrimmed = Util.fixEmptyAndTrim(testCasePath);
			if (StringUtils.isEmpty(testPathTrimmed)) {
				return FormValidation.error(Messages.thisFieldIsRequired());
			}

			if (ReportFormat.TRX == testReportFormat) {
				final String[] testPathArray = testPathTrimmed.split(Pattern.quote("/"));
				if (testPathArray.length < 2) {
					return FormValidation.warning(Messages.sectionNameInTestPathIsRequiredInTrxReportFormat());
				}

			}
			return FormValidation.ok();
		}

		/**
		 * Do check search mode.
		 *
		 * @param searchMode
		 *            the searchMode
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckSearchMode(@QueryParameter final String searchMode) {
			return RailflowUtils.validateRequiredField(searchMode);
		}

		/**
		 * Do check project name.
		 *
		 * @param resultFilePattern
		 *            result pattern
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckResultFilePattern(@QueryParameter final String resultFilePattern) {
			return RailflowUtils.validateRequiredField(resultFilePattern);
		}

		/**
		 * Do check milestone path.
		 *
		 * @param milestonePath
		 *            milestonePath
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckMilestonePath(@QueryParameter final String milestonePath) {
			return RailflowUtils.validateMilestonePath(milestonePath);
		}

		/**
		 * Do check JIRA project key.
		 *
		 * @param jiraProjectKey
		 *            the JIRA project key.
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckJiraProjectKey(@QueryParameter final String jiraProjectKey) {
			return RailflowUtils.validateRequiredField(jiraProjectKey);
		}

		/**
		 * Do check run id.
		 *
		 * @param runId
		 *            the run id.
		 * @return the form validation
		 */
		@SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
		public FormValidation doCheckRunId(@QueryParameter final String runId) {
			return RailflowUtils.validateRunId(runId);
		}
	}
}
