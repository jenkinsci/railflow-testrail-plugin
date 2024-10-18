package io.jenkins.plugins.railflow.jenkins.testresults;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.util.Secret;
import io.jenkins.plugins.railflow.JiraParameters;
import io.jenkins.plugins.railflow.TestRailParameters;
import io.jenkins.plugins.railflow.UploadParameters;
import io.jenkins.plugins.railflow.jenkins.cli.RailflowJenkinsCli;
import io.jenkins.plugins.railflow.jenkins.log.RailflowLogger;
import io.jenkins.plugins.railflow.testrail.ResultsUploader;
import io.jenkins.plugins.railflow.testrail.client.model.Run;

/**
 * Callable for running export into TestRail on a remote agent.
 *
 * @author Sergey Oplavin
 */
public class RailflowCallable implements Callable<List<String>, Exception>, Serializable {

	private static final long serialVersionUID = 4875516836673710685L;

	private final Secret licenseKeyOrContent;
	private final TestRailParameters testRailParameters;
	private final UploadParameters uploadParameters;
	private final String reportFilePattern;
	private final JiraParameters jiraParameters;
	private final TaskListener listener;
	private final String runId;
	private final FilePath workspace;
	private final boolean debugLogEnabled;

	public RailflowCallable(final String licenseKeyOrContent, final TestRailParameters testRailParameters, final UploadParameters uploadParameters,
			final String reportFilePattern, final JiraParameters jiraParameters, final TaskListener listener, final String runId, final FilePath workspace,
			final boolean debugLogEnabled) {
		this.licenseKeyOrContent = Secret.fromString(licenseKeyOrContent);
		this.testRailParameters = testRailParameters;
		this.uploadParameters = uploadParameters;
		this.reportFilePattern = reportFilePattern;
		this.jiraParameters = jiraParameters;
		this.listener = listener;
		this.runId = runId;
		this.workspace = workspace;
		this.debugLogEnabled = debugLogEnabled;
	}

	@Override
	public List<String> call() throws Exception {
		final List<String> runUrls = new ArrayList<>();
		RailflowJenkinsCli.enableDebugLogging(this.debugLogEnabled);
		try (final RailflowLogger logger = new RailflowLogger(this.listener, this.runId, this.workspace, this.debugLogEnabled)) {
			final ResultsUploader.UploadResult uploadResult = RailflowJenkinsCli
					.uploadTestReport(Secret.toString(this.licenseKeyOrContent), this.testRailParameters, this.uploadParameters,
							new ReportFilesProviderImpl(this.reportFilePattern, this.workspace), this.jiraParameters);
			if (uploadResult != null) {
				final List<Run> runs = uploadResult.getRuns();
				if (runs != null) {
					runs.forEach(r -> runUrls.add(r.getUrl()));
				}
			}
		}
		return runUrls;
	}

	@Override
	@SuppressWarnings("lgtm[jenkins/callable-without-role-check]")
	public void checkRoles(final RoleChecker checker) throws SecurityException {
	}
}
