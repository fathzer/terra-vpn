package com.fathzer.odvpn.ws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VpnService {
    final Logger logger = LoggerFactory.getLogger(VpnService.class);

    public static class VpnException extends RuntimeException {
        private static final long serialVersionUID = 1L;
		private final HttpStatus status;
        public VpnException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
        public HttpStatus getStatus() {
            return status;
        }
    }

    private final Map<String, Vpn> storage = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final VPNRepositorySettings settings;

    public VpnService(VPNRepositorySettings validatedSettings, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.settings = Objects.requireNonNull(validatedSettings);
        logger.info("Loading On Demand VPN definitions from {}", settings.dataPath());
        
        Path dataPath = settings.dataPath();
        if (Files.isDirectory(dataPath)) {
            try (Stream<Path> paths = Files.list(dataPath)) {
                paths.filter(Files::isDirectory)
                     .forEach(this::loadVpnFromDirectory);
            }
        } else {
            logger.warn("Data path {} is not a directory or does not exist", dataPath);
        }
    }
    
    private void loadVpnFromDirectory(Path dir) {
        if (!isValidId(dir.getFileName().toString())) {
            logger.warn("Ignoring directory {}. Its name is not a valid VPN ID", dir.getFileName());
            return;
        }
        try {
            Path configFile = dir.resolve("config.json");
            if (Files.exists(configFile)) {
                InstanceParameters params = objectMapper.readValue(configFile.toFile(), InstanceParameters.class);
                Vpn vpn = new Vpn(dir.getFileName().toString(), params);
                storage.put(vpn.id(), vpn);
                logger.info("Loaded VPN configuration from {}", dir);
            } else {
                logger.warn("Configuration file {} not found in directory {}", configFile, dir);
            }
        } catch (Exception e) {
            logger.error("Failed to load VPN configuration from " + dir, e);
        }
    }

    public Vpn create(String id, InstanceParameters dto) {
        if (!isValidId(id)) {
            throw new VpnException(HttpStatus.BAD_REQUEST, "ID must start with a letter or number and can only contain letters, numbers, underscores (_), hyphens (-), and periods (.)");
        }
        final Vpn vpn = new Vpn(id, dto);
        if (storage.putIfAbsent(id, vpn) != null) throw new VpnException(HttpStatus.CONFLICT, "VPN " + id + " already exists");
        return vpn;
    }

    public static boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    }

    public List<Vpn> findAll() {
        return new ArrayList<>(storage.values());
    }

    public Vpn findVpnById(String id) {
        final Vpn vpn = storage.get(id);
        if (vpn == null) throw new VpnException(HttpStatus.NOT_FOUND, "VPN " + id + " not found");
        return vpn;
    }

    public Vpn update(String id, InstanceParameters dto) {
        Vpn existing = findVpnById(id);
        //TODO
        return existing;
    }

    public void delete(String id) {
        if (storage.remove(id) == null) throw new VpnException(HttpStatus.NOT_FOUND, "VPN " + id + " not found");
    }

    public VPNStatus getStatus(String id) {
        Vpn vpn = findVpnById(id);
        return vpn.status();
    }

    public VPNStatus startVpn(String id) {
        Vpn vpn = findVpnById(id);
        if (vpn.status() == VPNStatus.RUNNING) return VPNStatus.RUNNING;

        vpn.setStatus(VPNStatus.STARTING);
        // Simuler un démarrage (ici synchro pour simplicité)
        vpn.setStatus(VPNStatus.RUNNING);
        return vpn.status();
    }

    public VPNStatus stopVpn(String id) {
        Vpn vpn = findVpnById(id);
        if (vpn.status() == VPNStatus.STOPPED) return VPNStatus.STOPPED;

        vpn.setStatus(VPNStatus.STOPPING);
        // Simuler un arrêt
        vpn.setStatus(VPNStatus.STOPPED);
        return vpn.status();
    }

    public List<String> listUsers(String id) {
        Vpn vpn = findVpnById(id);
        return List.of("TODO");
    }

    public void createUser(String id, String user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createUser'");
    }

    public void deleteUser(String id, String user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }
}
