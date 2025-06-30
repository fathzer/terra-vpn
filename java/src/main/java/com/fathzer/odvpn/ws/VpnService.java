package com.fathzer.odvpn.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;

@Service
public class VpnService {
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

    public VpnService(VPNRepositorySettings validatedSettings) {
        System.out.println(validatedSettings); //TODO
    }

    public Vpn create(String id, InstanceParameters dto) {
        if (id == null || !id.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new VpnException(HttpStatus.BAD_REQUEST, "ID must contain only letters, numbers, underscores (_), hyphens (-), and periods (.)");
        }
        final Vpn vpn = new Vpn(id, dto.protocol(), dto.hostName(), dto.port());
        if (storage.putIfAbsent(id, vpn) != null) throw new VpnException(HttpStatus.CONFLICT, "VPN " + id + " already exists");
        return vpn;
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
