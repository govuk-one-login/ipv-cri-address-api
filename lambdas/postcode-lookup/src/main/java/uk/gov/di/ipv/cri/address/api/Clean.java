package uk.gov.di.ipv.cri.address.api;

public class Clean {
    public static String clean(String metricValue) {
        if (metricValue == null || metricValue.isEmpty()) {
            return "no_content";
        }
        return metricValue.replace(" ", "_").trim();
    }
}
