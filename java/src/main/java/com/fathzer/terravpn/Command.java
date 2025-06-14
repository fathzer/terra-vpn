package com.fathzer.terravpn;

import java.nio.file.Path;

record Command(String command, String name, Path configPath, boolean force) {
    public Command {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }
}
