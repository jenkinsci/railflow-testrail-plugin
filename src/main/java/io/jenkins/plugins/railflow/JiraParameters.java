package io.jenkins.plugins.railflow;

import java.io.Serializable;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Holds the parameters for connecting to Atlassian.
 *
 * @author Liu Yang
 */
public interface JiraParameters extends Serializable {

	/**
	 * New builder.
	 *
	 * @param jiraUrl
	 *            the Atlassian Jira URL.
	 * @param email
	 *            the Atlassian user email address.
	 * @param token
	 *            the token of the Atlassian user.
	 * @param projectKey
	 *            the project key.
	 * @return builder.
	 */
	static JiraParameters.Builder builder(
			final String jiraUrl, final String email,
			final String token, final String projectKey) {

		return new JiraParameters.Builder(jiraUrl, email, token, projectKey);
	}

	/**
	 * Gets the Atlassian jira url.
	 *
	 * @return the Atlassian jira url.
	 */
	@NonNull
	String getJiraUrl();

	/**
	 * Gets the Atlassian account email.
	 *
	 * @return the Atlassian account email.
	 */
	@NonNull
	String getEmail();

	/**
	 * Gets the token of the Atlassian account email.
	 *
	 * @return the token of the Atlassian account email.
	 */
	@NonNull
	String getToken();

	/**
	 * Gets the project key.
	 *
	 * @return the project key.
	 */
	@NonNull
	String getProjectKey();

	/**
	 * Gets the connection timeout to the Atlassian REST API service.
	 *
	 * @return the connection timeout in seconds
	 */
	int getTimeout();

	/**
	 * Gets the TO DO status name.
	 *
	 * @return the TO DO status name.
	 */
	Optional<String> getTodoStatusName();

	/**
	 * Gets the DONE status name.
	 *
	 * @return the DONE status name.
	 */
	Optional<String> getDoneStatusName();

	/**
	 * Gets the issue type name.
	 *
	 * @return the issue type name.
	 */
	Optional<String> getIssueTypeName();

	/**
	 * Gets the lookup field name.
	 *
	 * @return the lookup field name.
	 */
	Optional<String> getLookupFieldName();

	class Builder {
		private final JiraParametersImpl jiraParameters;

		private Builder(final String jiraUrl, final String email,
				final String password, final String projectKey) {

			this.jiraParameters = new JiraParametersImpl(
					jiraUrl, email, password, projectKey);
		}

		public JiraParameters.Builder timeout(final int timeout) {
			this.jiraParameters.setTimeout(timeout);
			return this;
		}

		public JiraParameters.Builder todoStatusName(final String todoStatusName) {
			this.jiraParameters.setTodoStatusName(todoStatusName);
			return this;
		}

		public JiraParameters.Builder doneStatusName(final String doneStatusName) {
			this.jiraParameters.setDoneStatusName(doneStatusName);
			return this;
		}

		public JiraParameters.Builder issueTypeName(final String issueTypeName) {
			this.jiraParameters.setIssueTypeName(issueTypeName);
			return this;
		}

		public JiraParameters.Builder lookupFieldName(final String lookupFieldName) {
			this.jiraParameters.setLookupFieldName(lookupFieldName);
			return this;
		}

		public JiraParameters build() {
			return this.jiraParameters;
		}
	}
}
