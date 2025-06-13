package com.fathzer.terravpn;

import static com.fathzer.terravpn.Provider.readResource;
import java.util.List;

public interface VPSProvider extends Provider {
    default List<String> getTerraformProvider() {
        return readResource(this, "-providers.tf", "Terraform provider");
    }
    default List<String> getTerraformScript() {
        return readResource(this, "-script.tf", "Terraform script");
    }
    String getCompletedResource();
}
