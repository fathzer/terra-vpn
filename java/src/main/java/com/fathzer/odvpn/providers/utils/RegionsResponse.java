package com.fathzer.odvpn.providers.utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegionsResponse(List<Region> regions) {

}
