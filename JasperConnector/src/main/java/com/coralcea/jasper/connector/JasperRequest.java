package com.coralcea.jasper.connector;

import java.util.Map;

public class JasperRequest {

	private String version;
	private String method;
	private String ruri;
	private String rule;
	private Map<String, String> headers;
	private Object parameters;
	private byte[] payload;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getRuri() {
		return ruri;
	}

	public void setRuri(String ruri) {
		this.ruri = ruri;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Object getParameters() {
		return parameters;
	}

	public void setParameters(Object parameters) {
		this.parameters = parameters;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

}
