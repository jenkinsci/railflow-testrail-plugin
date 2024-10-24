package io.jenkins.plugins.railflow.jenkins.admin;

import hudson.Extension;
import hudson.Util;
import hudson.util.HttpResponses;
import hudson.util.Secret;
import io.jenkins.plugins.railflow.LicenseInfo;
import io.jenkins.plugins.railflow.commons.http.ProxySettings;
import io.jenkins.plugins.railflow.commons.statistics.ServerType;
import io.jenkins.plugins.railflow.jenkins.cli.RailflowJenkinsCli;
import io.jenkins.plugins.railflow.jenkins.util.JenkinsSecretPasswordEncrypter;
import io.jenkins.plugins.railflow.jenkins.util.LicenseInfoProvider;
import io.jenkins.plugins.railflow.jenkins.util.PasswordEncrypter;
import io.jenkins.plugins.railflow.jenkins.util.ProjectUtils;
import io.jenkins.plugins.railflow.jenkins.util.RailflowLicenseInfoProvider;
import io.jenkins.plugins.railflow.jenkins.util.RailflowProxySettingsFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

/**
 * Global configuration page.
 *
 * @author Sergey Oplavin
 * @author Liu Yang
 */
@Symbol("railflowGlobalConfig")
@Extension
public class GlobalConfig extends GlobalConfiguration {

	private static final Logger LOGGER = Logger.getLogger(GlobalConfig.class.getName());
	private static final String PERPETUAL = "Perpetual";
	private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal
			.withInitial(() -> new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH));
	private static final int KEY_LENGTH = 23;

	private transient final PasswordEncrypter passwordEncrypter;
	private transient final LicenseInfoProvider licenseInfoProvider;
	private List<TestRailServerConfig> testRailServers = Collections.emptyList();
	private boolean onlineActivation;
	private Secret licenseContent;
	private String licenseExpirationDate;
	private boolean uploadStatistics = true;
	private boolean trial;

	public GlobalConfig() {
		this(JenkinsSecretPasswordEncrypter.THE_INSTANCE, RailflowLicenseInfoProvider.THE_INSTANCE);
	}

	GlobalConfig(final PasswordEncrypter passwordEncrypter, final LicenseInfoProvider licenseInfoProvider) {
		this.passwordEncrypter = passwordEncrypter;
		this.licenseInfoProvider = licenseInfoProvider;
		this.load();
	}

	@Nonnull
	@Override
	public String getDisplayName() {
		return io.jenkins.plugins.railflow.jenkins.Messages.pluginDisplayName();
	}

	@POST
	@Override
	public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
		Jenkins.get().checkPermission(Jenkins.ADMINISTER);
		this.testRailServers = Collections.emptyList();
		req.bindJSON(this, json);
		this.save();

		if ((this.uploadStatistics || this.isTrial()) && this.licenseContent != null) {
			this.collectConfig(this.passwordEncrypter.decrypt(this.licenseContent));
		}
		return true;
	}

	@POST
	public HttpResponse doUploadLicenseFile(final StaplerRequest req) throws ServletException, IOException {
		Jenkins.get().checkPermission(Jenkins.ADMINISTER);
		final FileItem licenseFile = req.getFileItem("licenseFile");
		if (licenseFile == null) {
			return new ErrorResponse("License file is not selected", false);
		}
		return this.handleLicense(licenseFile.getString());
	}

	@POST
	public HttpResponse doActivateLicenseKey(@QueryParameter final String licenseKey) {
		Jenkins.get().checkPermission(Jenkins.ADMINISTER);
		final String licenseKeyTrim = Util.fixEmptyAndTrim(licenseKey);
		if (licenseKeyTrim == null) {
			return new ErrorResponse("License key is empty", false);
		}
		return this.handleLicense(licenseKeyTrim);
	}

	public List<TestRailServerConfig> getTestRailServers() {
		return this.testRailServers;
	}

	@DataBoundSetter
	public void setTestRailServers(final List<TestRailServerConfig> testRailServers) {
		this.testRailServers = testRailServers;
	}
	
	public boolean isOnlineActivation() {
		return this.onlineActivation;
	}

	@DataBoundSetter
	public void setOnlineActivation(final boolean onlineActivation) {
		this.onlineActivation = onlineActivation;
	}

	public Secret getLicenseContent() {
		return this.licenseContent;
	}

	@DataBoundSetter
	public void setLicenseContent(final Secret licenseContent) {
		this.licenseContent = licenseContent;
	}

	public boolean isUploadStatistics() {
		return this.uploadStatistics;
	}

	@DataBoundSetter
	public void setUploadStatistics(final boolean uploadStatistics) {
		this.uploadStatistics = uploadStatistics;
	}

	public boolean isTrial() {
		return this.trial;
	}

	@DataBoundSetter
	public void setTrial(final boolean trial) {
		this.trial = trial;
	}

	public String getLicenseExpirationDate() {
		if (this.licenseExpirationDate == null && this.licenseContent != null) {
			try {
				final Optional<LicenseInfo> licenseInfo = this.licenseInfoProvider.getLicenseInfo(this.passwordEncrypter.decrypt(this.licenseContent),
						Jenkins.get().proxy);
				this.licenseExpirationDate = licenseInfo.map(this::getExpirationDetails).orElse(null);
			} catch (final Exception ex) {
				LOGGER.log(Level.WARNING, "Could not get license information", ex);
			}
		}
		return this.licenseExpirationDate;
	}

	private String getExpirationDetails(final LicenseInfo licenseInfo) {
		if (licenseInfo.isPerpetual()) {
			return PERPETUAL;
		}
		return DATE_FORMAT.get().format(licenseInfo.getExpirationDate());
	}

	/**
	 * Used from the Jelly.
	 *
	 * @return license expiration date as sting to be displayed in the UI.
	 */
	public String getLicenseExpirationDateString() {
		final String expirationDate = getLicenseExpirationDate();
		return expirationDate != null ? expirationDate : "N/A";
	}

	private HttpResponse handleLicense(final String licenseKeyOrContent) {
		try {
			final Optional<LicenseInfo> license = this.licenseInfoProvider.getLicenseInfo(licenseKeyOrContent, Jenkins.get().proxy);
			if (!license.isPresent()) {
				return new ErrorResponse("The license key/file is not valid", false);
			}
			final LicenseInfo licenseInfo = license.get();
			if (licenseInfo.isFree()) {
				return new ErrorResponse("[Jenkins Plugin] is a paid feature. Upgrade to TestRail Enterprise or Professional to unlock all Railflow features",
						false);
			}
			final String expirationDate = this.getExpirationDetails(licenseInfo);
			if (licenseInfo.isExpired()) {
				return new ErrorResponse("The license key/file is expired. Expiration date: " + expirationDate, false);
			}
			this.setLicenseFields(licenseInfo);
			this.setOnlineActivation(licenseKeyOrContent.length() <= KEY_LENGTH);
			this.setTrial(licenseInfo.isTrial());

			final Map<String, String> returnValues = new HashMap<>();
			returnValues.put("expirationDate", expirationDate);
			returnValues.put("licenseContent", new String(licenseInfo.getLicenseKey(), StandardCharsets.UTF_8));
			returnValues.put("message", io.jenkins.plugins.railflow.jenkins.Messages.licenseActivatedSuccess());
			returnValues.put("onlineActivation", String.valueOf(this.onlineActivation));
			returnValues.put("trial", String.valueOf(this.trial));
			return HttpResponses.okJSON(returnValues);
		} catch (final Throwable ex) {
			return new ErrorResponse(ex, true);
		}
	}

	private void setLicenseFields(final LicenseInfo licenseInfo) {
		this.licenseExpirationDate = getExpirationDetails(licenseInfo);
		this.setLicenseContent(this.passwordEncrypter.encrypt(new String(licenseInfo.getLicenseKey(), StandardCharsets.UTF_8)));
	}

	private void collectConfig(final String licenseKeyOrContent) {
		try {
			final ProxySettings proxySettings = RailflowProxySettingsFactory.THE_INSTANCE.create(Jenkins.get().proxy);
			RailflowJenkinsCli.collectConfigEvent(ServerType.Jenkins, ProjectUtils.getPluginVersion(), Jenkins.VERSION, licenseKeyOrContent,
					proxySettings);
		} catch (final Exception e) {
			// do nothing
		}
	}

	static class ErrorResponse extends org.kohsuke.stapler.HttpResponses.HttpResponseException {

		public static final int STATUS_CODE = 500;
		public static final String CONTENT_TYPE = "text/plain;charset=UTF-8";
		private static final long serialVersionUID = 5259566539107436395L;
		private final Throwable cause;
		private final boolean printStackTrace;

		public ErrorResponse(final String message, final boolean printStackTrace) {
			this(new Exception(message), printStackTrace);
		}

		public ErrorResponse(final Throwable cause, final boolean printStackTrace) {
			this.cause = cause;
			this.printStackTrace = printStackTrace;
		}

		@Override
		public void generateResponse(final StaplerRequest req, final StaplerResponse rsp, final Object node) throws IOException, ServletException {
			rsp.setStatus(STATUS_CODE);
			rsp.setContentType(CONTENT_TYPE);
			if (this.cause != null) {
				try (final PrintWriter w = new PrintWriter(rsp.getWriter())) {
					if (this.printStackTrace) {
						this.cause.printStackTrace(w);
					} else {
						w.append(this.cause.getMessage());
					}
				}
			}
		}
	}

}
