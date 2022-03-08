package uk.gov.di.ipv.cri.address.library.constants;

public class OrdnanceSurveyConstants {

    private OrdnanceSurveyConstants() {
        throw new IllegalStateException(
                "This class is not meant to be instantiated, it only holds constants");
    }

    public static final String POSTCODE_LOOKUP_API = "https://api.os.uk/search/places/v1/postcode";

    public static final String LOG_RESPONSE_PREFIX = "Ordnance Survey Responded with ";
}
