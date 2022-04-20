package uk.gov.di.ipv.cri.address.library.domain.verifiablecredential;

public class VerifiableCredentialConstants {
    public static final String VC_CONTEXT = "@context";
    public static final String W3_BASE_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    public static final String DI_CONTEXT =
            "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld";
    public static final String VC_TYPE = "type";
    public static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    public static final String ADDRESS_CREDENTIAL_TYPE = "AddressCredential";
    public static final String CREDENTIAL_SUBJECT_NAME = "name";
    public static final String CREDENTIAL_SUBJECT_ADDRESS = "address";
    public static final String VC_CREDENTIAL_SUBJECT = "credentialSubject";
    public static final String VC_CLAIM = "vc";
    public static final String VC_ADDRESS_KEY = "address";

    private VerifiableCredentialConstants() {}
}
