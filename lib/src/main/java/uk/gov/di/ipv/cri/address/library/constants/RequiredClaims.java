package uk.gov.di.ipv.cri.address.library.constants;

public enum RequiredClaims {
    NBF("nbf"),
    EXP("exp"),
    SUB("sub");

    public final String name;

    RequiredClaims(String name) {
        this.name = name;
    }
}
