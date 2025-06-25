package com.fathzer.terravpn.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fathzer.terravpn.repository.InstanceParameters;

import org.springframework.stereotype.Service;

@Service
public class VpnService {
    private final Map<String, Vpn> storage = new ConcurrentHashMap<>();

    public Vpn save(InstanceParameters dto) {
        Vpn vpn = new Vpn(dto.protocol(), dto.hostName(), dto.port());
        storage.put(vpn.id(), vpn);
        return vpn;
    }

    public List<Vpn> findAll() {
        return new ArrayList<>(storage.values());
    }

    public Optional<Vpn> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<Vpn> update(Long id, InstanceParameters dto) {
        Vpn existing = storage.get(id);
        if (existing == null) return Optional.empty();
        //TODO
        return Optional.of(existing);
    }

    public boolean delete(Long id) {
        return storage.remove(id) != null;
    }

    public Optional<VPNStatus> getStatus(Long id) {
        return Optional.ofNullable(storage.get(id)).map(Vpn::status);
    }

    public Optional<VPNStatus> startVpn(Long id) {
        Vpn vpn = storage.get(id);
        if (vpn == null || vpn.status() == VPNStatus.RUNNING) return Optional.empty();

        vpn.setStatus(VPNStatus.STARTING);
        // Simuler un démarrage (ici synchro pour simplicité)
        vpn.setStatus(VPNStatus.RUNNING);
        return Optional.of(vpn.status());
    }

    public Optional<VPNStatus> stopVpn(Long id) {
        Vpn vpn = storage.get(id);
        if (vpn == null || vpn.status() == VPNStatus.STOPPED) return Optional.empty();

        vpn.setStatus(VPNStatus.STOPPING);
        // Simuler un arrêt
        vpn.setStatus(VPNStatus.STOPPED);
        return Optional.of(vpn.status());
    }
}
