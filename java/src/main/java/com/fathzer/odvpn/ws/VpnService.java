package com.fathzer.odvpn.ws;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fathzer.odvpn.AbstractOnDemandVPNManager;
import com.fathzer.odvpn.LocalDiskOnDemandVPNManager;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;

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

    private final Map<String, AbstractOnDemandVPNManager> storage = new ConcurrentHashMap<>();
    private final VPNRepositorySettings settings;

    public VpnService(VPNRepositorySettings validatedSettings) throws IOException {
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
        final String id = dir.getFileName().toString();
        if (!AbstractOnDemandVPNManager.isValidId(id)) {
            logger.warn("Ignoring directory {}. Its name is not a valid VPN ID", dir.getFileName());
            return;
        }
        try {
            Path configFile = dir.resolve("config.json");
            if (Files.exists(configFile)) {
                LocalDiskOnDemandVPNManager manager = new LocalDiskOnDemandVPNManager(settings, id);
                storage.put(manager.id(), manager);
                logger.info("Loaded VPN configuration from {}", dir);
            } else {
                logger.warn("Configuration file {} not found in directory {}", configFile, dir);
            }
        } catch (Exception e) {
            logger.error("Failed to load VPN configuration from " + dir, e);
        }
    }

    private Vpn toVpn(AbstractOnDemandVPNManager manager) {
        Vpn vpn = new Vpn(manager.id(), manager.settings());
        try {
            if (manager.isServerRunning()) {
                vpn.setStatus(VPNStatus.RUNNING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return vpn;
    }   

    public Vpn create(String id, InstanceParameters dto, Path openVPNConfigPath, boolean force) throws IOException {
        AbstractOnDemandVPNManager manager = new LocalDiskOnDemandVPNManager(settings, dto, id);
        manager.init(openVPNConfigPath, force);
        storage.put(id, manager);
        return toVpn(manager);
    }

    public List<Vpn> findAll() {
        return storage.values().stream().map(this::toVpn).toList();
    }

    public boolean exists(String id) {
        return storage.containsKey(id);
    }
    
    public Vpn findVpnById(String id) {
        return toVpn(getManager(id));
    }

    private AbstractOnDemandVPNManager getManager(String id) {
        final AbstractOnDemandVPNManager manager = storage.get(id);
        if (manager == null) throw new VpnException(HttpStatus.NOT_FOUND, "VPN " + id + " not found");
        return manager;
    }

    public void delete(String id, boolean force) throws IOException {
        getManager(id).delete(force);
        storage.remove(id);
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
