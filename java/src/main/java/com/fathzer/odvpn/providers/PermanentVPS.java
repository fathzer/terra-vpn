package com.fathzer.odvpn.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.utils.IPv4Validator;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Permanent VPS provider implementation.
 * This provider allows deploying OpenVPN servers on a permanent VPS (One that is not managed by Terraform).
 */
@Registerable(
        value = "permanent",
        classes = {VPSProvider.class}
)
public class PermanentVPS extends VPSProvider<PermanentVPS.PermanentSettings> {
	
	public static class PermanentSettings {
		private String ip;
        private String sshUser;

		public String getIp() {
			return ip;
		}

        public String getSshUser() {
            return sshUser;
        }
	}

    @Override
    public String name() {
        return "Permanent VPS";
    }

    @Override
    public Class<PermanentSettings> getConfigClass() {
        return PermanentSettings.class;
    }   

    @Override
    public List<String> checkConfiguration() {
        final List<String> errors = new LinkedList<>();
        if (settings == null || settings.ip==null) {
            errors.add("Missing IP");
        } else if (!IPv4Validator.isValid(settings.ip)) {
        	errors.add(String.format("IP %s is not valid", settings.ip));
        }
        return errors;
    }

    @Override
    public VPSState createVPS(Consumer<VPSState> progress) {
        return new VPSState("permanentServer", settings.ip, Status.READY);
    }

    @Override
    public boolean exists(String id) {
        return true;
    }

    @Override
    public void deleteVPS(String id) {
        // Do nothing, as the VPS is supposed to be permanent
    }

    @Override
    public String getSSHUser() {
        final String sshUser = resolve(settings.getSshUser());
        return sshUser != null ? sshUser : super.getSSHUser();
    }
    
}
