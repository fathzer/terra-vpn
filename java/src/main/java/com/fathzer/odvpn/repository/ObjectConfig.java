package com.fathzer.odvpn.repository;

import java.util.Map;


public class ObjectConfig<T> {
	private final T provider;
	private final Map<String, String> config;
	
	public ObjectConfig(T provider, Map<String, String> config) {
		super();
		this.provider = provider;
		this.config = config;
	}

	public T provider() {
		return provider;
	}

	public Map<String, String> config() {
		return config;
	}
}
