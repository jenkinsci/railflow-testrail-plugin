package io.jenkins.plugins.railflow;

import java.util.Optional;

import hudson.util.Secret;

/**
 * Default implementation of {@link JiraParametersImpl}.
 *
 * @author Liu Yang
 */
public class JiraParametersImpl implements JiraParameters {
	private static final long serialVersionUID = 5609571937668955392L;

	private String jiraUrl;
	private String email;
	private Secret token;
	private Secret projectKey;
	private int timeout;
	private String todoStatusName;
	private String doneStatusName;
	private String issueTypeName;
	private String lookupFieldName;

	public JiraParametersImpl(final String jiraUrl, final String email, final String token, final String projectKey) {
		this.jiraUrl = jiraUrl;
		this.email = email;
		this.token = Secret.fromString(token);
		this.projectKey = Secret.fromString(projectKey);
	}

	@Override
	public String getJiraUrl() {
		return this.jiraUrl;
	}

	public void setJiraUrl(final String jiraUrl) {
		this.jiraUrl = jiraUrl;
	}

	@Override
	public String getEmail() {
		return this.email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	@Override
	public String getToken() {
		return Secret.toString(this.token);
	}

	public void setToken(final String token) {
		this.token = Secret.fromString(token);
	}

	@Override
	public String getProjectKey() {
		return Secret.toString(this.projectKey);
	}

	public void setProjectKey(final String projectKey) {
		this.projectKey = Secret.fromString(projectKey);
	}

	@Override
	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	@Override
	public Optional<String> getTodoStatusName() {
		return Optional.ofNullable(this.todoStatusName);
	}

	public void setTodoStatusName(final String todoStatusName) {
		this.todoStatusName = todoStatusName;
	}

	@Override
	public Optional<String> getDoneStatusName() {
		return Optional.ofNullable(this.doneStatusName);
	}

	public void setDoneStatusName(final String doneStatusName) {
		this.doneStatusName = doneStatusName;
	}

	@Override
	public Optional<String> getIssueTypeName() {
		return Optional.ofNullable(this.issueTypeName);
	}

	public void setIssueTypeName(final String issueTypeName) {
		this.issueTypeName = issueTypeName;
	}

	@Override
	public Optional<String> getLookupFieldName() {
		return Optional.ofNullable(this.lookupFieldName);
	}

	public void setLookupFieldName(final String lookupFieldName) {
		this.lookupFieldName = lookupFieldName;
	}
}
