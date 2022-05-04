package uk.gov.di.ipv.cri.address.library.util;

import java.util.List;

public class ListUtil {
    public <T> T getOneItemOrThrowError(List<T> list) {
        if (list.size() == 1) return list.get(0);

        throw new IllegalArgumentException(
                String.format("Parameter must have exactly one value: %s", list));
    }
}
