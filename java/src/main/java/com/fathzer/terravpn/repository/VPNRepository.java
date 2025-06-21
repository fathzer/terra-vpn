package com.fathzer.terravpn.repository;

import java.nio.file.Path;

/**
 * Repository for VPNs.
 */
public class VPNRepository {
    private final Path root;
    private final Path sshKeysDir;

    public VPNRepository(Path root, Path sshKeysDir) {
        this.root = root;
        this.sshKeysDir = sshKeysDir;
    }



}
