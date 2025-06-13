package com.fathzer.terravpn;

import java.util.List;

/**
 * Interface for Dynamic DNS provider implementations.
 * Implementations should be annotated with @DdnsProvider.
 */
public interface DynamicDNSProvider extends Provider {
    default List<String> getDnsUpdateScript() {
        return Provider.readResource(this, "-dnsUpdate.sh", "Dynamic DNS update shell script");
    }
    String getAuthentArguments();
}
