package com.fathzer.odvpn.repository;

import java.util.Map;
import java.util.stream.Collectors;

public class ObjectConfig<T> {
	private final T provider;
	private final Map<String, String> rawConfig;
	private Map<String, String> config; // Lazy initialized
	
	public ObjectConfig(T provider, Map<String, String> rawConfig) {
		super();
		this.provider = provider;
		this.rawConfig = rawConfig.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public T provider() {
		return provider;
	}

	public Map<String, String> rawConfig() {
		return rawConfig;
	}

	public Map<String, String> config() {
		if (config == null) {
			config = rawConfig.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> resolve(e.getValue())));
		}
		return config;
	}

	protected String resolve(String value) {
		if (value.startsWith("${") && value.endsWith("}")) {
			final String variable = value.substring(2, value.length() - 1);
			final String resolved = get(variable);
			if (resolved != null) {
				return resolved;
			}
		}
		return value;
	}

	protected String get(String variable) {
		return System.getenv(variable);
	}
}
