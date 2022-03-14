package uk.gov.di.ipv.cri.address.library.helpers;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RequestHelper {

    private RequestHelper() {}

    public static String getHeaderByKey(Map<String, String> headers, String headerKey) {
        if (Objects.isNull(headers)) {
            return null;
        }
        var values =
                headers.entrySet().stream()
                        .filter(e -> headerKey.equalsIgnoreCase(e.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
        if (values.size() == 1) {
            var value = values.get(0);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public static Map<String, String> parseRequestBody(String body) {
        Map<String, String> queryPairs = new HashMap<>();

        for (NameValuePair pair : URLEncodedUtils.parse(body, Charset.defaultCharset())) {
            queryPairs.put(pair.getName(), pair.getValue());
        }

        return queryPairs;
    }
}
