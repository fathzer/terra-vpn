package com.fathzer.odvpn;

public class Constants {
    /** The OpenVPN image to use */
    public static final String OPENVPN_IMAGE = "kylemanna/openvpn";
    /** The OpenVPN configuration folder on the VPS */
    public static final String OPENVPN_VPS_FOLDER = "/etc/openvpn";

    /** The Terraform instance type variable name */
    public static final String INSTANCE_TYPE_VAR = "instance_type";
    /** The Terraform location variable name */
    public static final String REGION_VAR = "region";
    /** The Terraform root volume size variable name */
    public static final String ROOT_VOLUME_SIZE_GB_VAR = "root_volume_size_gb";

    /** The Terraform ssh user variable name */
    public static final String SSH_USER_VAR = "ssh_user";

    /** The Terraform protocol variable name */
    public static final String PROTOCOL_VAR = "protocol";
    /** The Terraform port variable name */
    public static final String PORT_VAR = "port";
        
    private Constants() {
    }
}
