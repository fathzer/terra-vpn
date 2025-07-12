package com.fathzer.odvpn.providers.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SshKey(String id, String name) {}