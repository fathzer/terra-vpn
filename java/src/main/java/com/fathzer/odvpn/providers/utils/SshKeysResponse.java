package com.fathzer.odvpn.providers.utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SshKeysResponse(@JsonProperty("ssh_keys") List<SshKey> sshKeys) {}
