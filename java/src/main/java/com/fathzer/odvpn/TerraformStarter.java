package com.fathzer.odvpn;

import java.io.IOException;
import java.nio.file.Path;

public class TerraformStarter {

    public static void main(String[] args) throws IOException, InterruptedException {
        new OnDemandVPNManager(Path.of("data/myvpn"), Path.of("ssh/id_rsa")).start();
    }
}
