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
import com.fathzer.odvpn.AbstractOnDemandVPNManager.DetailedStatus;
import com.fathzer.odvpn.OpenVPNManager.User;
import com.fathzer.odvpn.LocalDiskOnDemandVPNManager;
import com.fathzer.odvpn.StartProgressListener;
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    public void create(String id, InstanceParameters dto, Path openVPNConfigPath, boolean force) throws IOException {
        AbstractOnDemandVPNManager manager = new LocalDiskOnDemandVPNManager(settings, id, dto);
        manager.init(openVPNConfigPath, force);
        storage.put(id, manager);
    }

    public List<String> findAll() {
        return storage.values().stream().map(AbstractOnDemandVPNManager::id).toList();
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

    public DetailedStatus getStatus(String id) throws IOException {
        return getManager(id).getStatus();
    }

    public void start(String id) {
        final AbstractOnDemandVPNManager manager = getManager(id);
        Thread thread = new Thread(() -> {
            try {
                manager.start(new StartProgressListener() {});
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        thread.start();
    }

    public void stop(String id) throws IOException {
//TODO
        Vpn vpn = findVpnById(id);
        if (vpn.status() == VPNStatus.STOPPED) return;

        vpn.setStatus(VPNStatus.STOPPING);
        // Simuler un arrêt
        vpn.setStatus(VPNStatus.STOPPED);
    }

    public List<User> listUsers(String id) throws IOException {
        final AbstractOnDemandVPNManager manager = getManager(id);
        return manager.getUsers();
    }

    public byte[] getUserConfig(String id, String user) throws IOException {
        final AbstractOnDemandVPNManager manager = getManager(id);
        return manager.getUserConfig(user);
    }

    public void createUser(String id, String user) throws IOException {
        final AbstractOnDemandVPNManager manager = getManager(id);
        manager.addUser(user);
    }

    public void deleteUser(String id, String user) throws IOException {
        final AbstractOnDemandVPNManager manager = getManager(id);
        manager.deleteUser(user);
    }
}
