package uk.gov.di.ipv.cri.address.library.domain;

public enum AuditEventTypes {
    IPV_ADDRESS_CRI_START, // Before a session is written to the Session table
    IPV_ADDRESS_CRI_REQUEST_SENT, // When an address is added in the PUT /address API call
    IPV_ADDRESS_CRI_VC_ISSUED // When the final VC is created in the issue credential lambda
}
