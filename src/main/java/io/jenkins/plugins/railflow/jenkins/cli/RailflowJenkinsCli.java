package io.jenkins.plugins.railflow.jenkins.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.jenkins.plugins.railflow.JiraParameters;
import io.jenkins.plugins.railflow.LicenseInfo;
import io.jenkins.plugins.railflow.ReportFilesProvider;
import io.jenkins.plugins.railflow.TestRailParameters;
import io.jenkins.plugins.railflow.UploadParameters;
import io.jenkins.plugins.railflow.commons.http.ProxySettings;
import io.jenkins.plugins.railflow.commons.statistics.ServerType;
import io.jenkins.plugins.railflow.testrail.ResultsUploader.UploadResult;

public class RailflowJenkinsCli {

    private static final Logger LOGGER = Logger.getLogger(RailflowJenkinsCli.class.getName());
    private static final Gson GSON = (new GsonBuilder()).create();

    private static boolean debug = false;

    private RailflowJenkinsCli() {
    }

    public static void enableDebugLogging(final boolean enabled) {
        RailflowJenkinsCli.debug = enabled;
    }

    public static Optional<LicenseInfo> getLicense(final String licenseKeyOrContent, final ProxySettings proxySettings)
        throws IOException {
        final List<String> args = new ArrayList<>();
        args.add("--license");
        args.add(licenseKeyOrContent);
        
        if (proxySettings != null) {
            args.add("--proxy");
            args.add(GSON.toJson(proxySettings));
        }

        final String license = exec("check-license", args, "LICENSE");
        return (license == null) ? Optional.empty() : Optional.of(GSON.fromJson(license, LicenseInfo.class));
    }

    public static void collectConfigEvent(final ServerType serverType,
        final String pluginVersion, final String serverVersion,
        final String licenseKeyOrContent, final ProxySettings proxySettings) {

        try {
            final List<String> args = new ArrayList<>();
            args.add("--server-type");
            args.add(serverType.name());
            args.add("--plugin-version");
            args.add(pluginVersion);
            args.add("--jenkins-version");
            args.add(serverVersion);
            args.add("--license");
            args.add(licenseKeyOrContent);
            
            if (proxySettings != null) {
                args.add("--proxy");
                args.add(GSON.toJson(proxySettings));
            }

            exec("collect-config-event", args, null);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "[RailflowJenkinsCLI] Failed to collect config event: " + e.getMessage(), e);
        }
    }

    public static void collectExportEvent(final ServerType serverType,
        final boolean onlineActivation, final String licenseKeyOrContent,
		final TestRailParameters testRailParameters, final UploadParameters uploadParameters,
        final JiraParameters jiraParameters) {

        try {
            final List<String> args = new ArrayList<>();
            args.add("--server-type");
            args.add(serverType.name());

            if (onlineActivation) {
                args.add("--online-activation");
            }

            args.add("--license");
            args.add(licenseKeyOrContent);
            args.add("--testrail-parameters");
            args.add(GSON.toJson(testRailParameters));
            args.add("--upload-parameters");
            args.add(GSON.toJson(uploadParameters));

            if (jiraParameters != null) {
                args.add("--jira-parameters");
                args.add(GSON.toJson(jiraParameters));
            }

            exec("collect-export-event", args, null);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "[RailflowJenkinsCLI] Failed to collect export event: " + e.getMessage(), e);
        }
    }

    public static void collectExportEvent(final ServerType serverType,
        final String pluginVersion, final String serverVersion,
        final boolean onlineActivation, final String licenseKeyOrContent,
        final TestRailParameters testRailParameters, final UploadParameters uploadParameters,
        final JiraParameters jiraParameters, final Throwable throwable) {

        try {
            final List<String> args = new ArrayList<>();
            args.add("--server-type");
            args.add(serverType.name());
            args.add("--plugin-version");
            args.add(pluginVersion);
            args.add("--jenkins-version");
            args.add(serverVersion);
            
            if (onlineActivation) {
                args.add("--online-activation");
            }

            args.add("--license");
            args.add(licenseKeyOrContent);
            args.add("--testrail-parameters");
            args.add(GSON.toJson(testRailParameters));
            args.add("--upload-parameters");
            args.add(GSON.toJson(uploadParameters));

            if (jiraParameters != null) {
                args.add("--jira-parameters");
                args.add(GSON.toJson(jiraParameters));
            }

            args.add("--exception");
            args.add(throwable.getMessage());

            exec("collect-export-event", args, null);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "[RailflowJenkinsCLI] Failed to collect export event: " + e.getMessage(), e);
        }
    }

	public static UploadResult uploadTestReport(final String licenseKeyOrContent,
        final TestRailParameters testRailParameters, final UploadParameters uploadParameters,
        final ReportFilesProvider reportFilesProvider, final JiraParameters jiraParameters)
        throws IOException {

        final List<String> args = new ArrayList<>();
        args.add("--license");
        args.add(licenseKeyOrContent);
        args.add("--testrail-parameters");
        args.add(GSON.toJson(testRailParameters));
        args.add("--upload-parameters");
        args.add(GSON.toJson(uploadParameters));
        args.add("--report-files");
        args.add(GSON.toJson(new ReportFilesHolder(reportFilesProvider)));

        if (jiraParameters != null) {
            args.add("--jira-parameters");
            args.add(GSON.toJson(jiraParameters));
        }

        final String result = exec("upload-test-report", args, "UPLOAD");
        return (result == null) ? null : GSON.fromJson(result, UploadResultHolder.class).map();
    }

    private static String exec(final String command, final List<String> args, final String resultKey)
        throws IOException {
        final String jar = "railflow-jenkins-cli-1.0.jar";
        final File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        final File target = new File(tmpdir, jar);
        if (!target.exists()) {
            final URL url = new URL("https://api.railflow.io/storage/v1/object/public/jenkins/" + jar);
            LOGGER.info("[RailflowJenkinsCLI] " + jar + " not found, downloading from " + url.toExternalForm());

            final URLConnection connection = url.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla");
            connection.setReadTimeout(300_000);
            connection.setConnectTimeout(60_000);
            
            try {
                final InputStream stream = connection.getInputStream();
                Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
                LOGGER.info("[RailflowJenkinsCLI] " + jar + " downloaded to " + target.toPath().toString());
            } catch (AccessDeniedException e) {
                LOGGER.severe("[RailflowJenkinsCLI] Write access denied to " + target.toPath().toString());
                LOGGER.info("This version of Jenkins plugin is not compatible. Please download Railflow Jenkins plugin from the Railflow Jenkins downloads page.");
                throw e;
            }
        }

        final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        final List<String> params = new ArrayList<>();
        params.add(isWindows ? "java.exe" : "java");
        params.add("-jar");
        params.add(target.toPath().toString());
        params.add(command);
        params.addAll(args);

        if (debug) {
            LOGGER.info("[RailflowJenkinsCLI] Running with:");
            for (final String param : params) {
                LOGGER.info("    " + param);
            }
        } else {
            LOGGER.info("[RailflowJenkinsCLI] Running");
        }

        try {
            final ProcessBuilder builder = new ProcessBuilder(params);
            builder.redirectErrorStream(true);

            final Process process = builder.start();
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                final Scanner scanner = new Scanner(process.getInputStream(), "UTF-8").useDelimiter("\r?\n|\r");
                while (scanner.hasNext()) {
                    final String line = scanner.next();
                    LOGGER.info("[RailflowJenkinsCLI] " + line);
                }
                
                scanner.close();
                throw new RuntimeException("[RailflowJenkinsCLI] Terminated with exit code: " + exitCode);
            }

            final Scanner scanner = new Scanner(process.getInputStream(), "UTF-8").useDelimiter("\r?\n|\r");
            while (scanner.hasNext()) {
                final String line = scanner.next();
                LOGGER.info("[RailflowJenkinsCLI] " + line);

                if ((resultKey != null) && (line.startsWith(resultKey))) {
                    scanner.close();            
                    return line.substring(resultKey.length() + 2);
                }

                if (line.startsWith("EXCEPTION: ")) {
                    scanner.close();
                    throw new RuntimeException("[RailflowJenkinsCLI] " + line.substring("EXCEPTION: ".length()));
                }
            }

            scanner.close();
            
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
	}
}
