package io.jenkins.plugins.railflow.jenkins.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import hudson.Util;
import hudson.util.FormValidation;
import io.jenkins.plugins.railflow.RunId;

/**
 * @author Sergey Oplavin
 */
public final class RailflowUtils {
	private static final String NAME_VALUE_DELIMITER = "=";
	private static final String NEWLINE_DELIMITER = "\n";
	private static final String COMMA_DELIMITER = ",";

	private RailflowUtils() {
	}

	/**
	 * Validate required field.
	 *
	 * @param fieldValue
	 *            the field value
	 * @return the form validation
	 */
	public static FormValidation validateRequiredField(final String fieldValue) {
		final String preparedVal = Util.fixEmptyAndTrim(fieldValue);
		if (preparedVal == null) {
			return FormValidation.error(io.jenkins.plugins.railflow.jenkins.Messages.thisFieldIsRequired());
		}
		return FormValidation.ok();
	}

	public static Map<String, Object> getCustomFields(final String strCustomFields) {
		return convert(strCustomFields, value -> value);
	}

	public static List<String> getLines(final String linesString) {
		return StringUtils.isNotEmpty(linesString) ? Arrays.asList(linesString.split(NEWLINE_DELIMITER)) : Collections.emptyList();
	}

	public static List<String> getAssignments(final String smartTestFailureAssignment) {
		return StringUtils.isNotEmpty(smartTestFailureAssignment)
				? Arrays.stream(smartTestFailureAssignment.split(COMMA_DELIMITER)).map(String::trim).collect(Collectors.toList())
				: Collections.emptyList();
	}

	private static <T> Map<String, T> convert(final String content, final Function<String, T> convertFunction) {
		if (StringUtils.isEmpty(content)) {
			return Collections.emptyMap();
		}
		final Map<String, T> mappings = new LinkedHashMap<>();
		final String[] entries = content.split(NEWLINE_DELIMITER);
		for (final String entry : entries) {
			final String[] keyValuePair = entry.trim().split(NAME_VALUE_DELIMITER, 2);
			mappings.put(keyValuePair[0].trim(), keyValuePair.length > 1 ? convertFunction.apply(keyValuePair[1].trim()) : null);
		}
		return mappings;
	}

	public static Level getSl4jToJULMapping(final org.slf4j.event.Level level) {
		if (null == level) {
			throw new NullPointerException("level is null");
		}
		switch (level) {
		case TRACE:
			return Level.FINEST;
		case DEBUG:
			return Level.FINE;
		case INFO:
			return Level.INFO;
		case WARN:
			return Level.WARNING;
		case ERROR:
			return Level.SEVERE;
		default:
			throw new IllegalArgumentException("level is invalid");
		}
	}

	public static FormValidation validateMilestonePath(final String milestonePath) {
		return isMilestonePathValid(milestonePath) ? FormValidation.ok() : FormValidation.error(io.jenkins.plugins.railflow.jenkins.Messages.milestonePathInvalid());
	}

	public static boolean isMilestonePathValid(final String milestonePath) {
		final String preparedVal = Util.fixEmptyAndTrim(milestonePath);
		return preparedVal == null || preparedVal.split("/").length <= 2;
	}

	public static FormValidation validateRunId(final String runId) {
		try {
			RunId.create(runId);
		} catch (final Exception e) {
			return FormValidation.error(io.jenkins.plugins.railflow.jenkins.Messages.runIdInvalid());
		}
		return FormValidation.ok();
	}
}
