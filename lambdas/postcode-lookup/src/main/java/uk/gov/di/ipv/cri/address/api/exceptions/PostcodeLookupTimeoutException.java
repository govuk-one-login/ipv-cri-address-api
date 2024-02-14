package uk.gov.di.ipv.cri.address.api.exceptions;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

public class PostcodeLookupTimeoutException extends RuntimeException {
    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupTimeoutException() {
        super();
    }

    public PostcodeLookupTimeoutException(String message) {
        super(message);
    }

    public PostcodeLookupTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupTimeoutException(Throwable cause) {
        super(cause);
    }
}
