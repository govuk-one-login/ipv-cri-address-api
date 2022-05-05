package uk.gov.di.ipv.cri.address.library.exception;

import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;

@ExcludeFromGeneratedCoverageReport
public class SqsException extends Exception {
    public SqsException(Throwable e) {
        super(e);
    }

    public SqsException(String errorMessage) {
        super(errorMessage);
    }
}
