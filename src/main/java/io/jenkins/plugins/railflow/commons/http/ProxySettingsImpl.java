package io.jenkins.plugins.railflow.commons.http;

import hudson.util.Secret;

/**
 * Default implementation of {@link ProxySettings}
 * 
 * @author Sergey Oplavin
 */
public class ProxySettingsImpl implements ProxySettings {
	private static final long serialVersionUID = -8473948203948501820L;

	private String protocol;
	private String host;
	private int port;
	private String userName;
	private Secret password;

	public ProxySettingsImpl(final String protocol, final String host, final int port) {
		this(protocol, host, port, null, null);
	}

	public ProxySettingsImpl(final String protocol, final String host, final int port, final String userName, final String password) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.userName = userName;
		this.password = Secret.fromString(password);
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	@Override
	public String getUserName() {
		return this.userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	@Override
	public String getPassword() {
		return Secret.toString(this.password);
	}

	public void setPassword(final String password) {
		this.password = Secret.fromString(password);
	}
}
