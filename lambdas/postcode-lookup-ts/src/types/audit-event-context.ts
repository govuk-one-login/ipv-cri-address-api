import type { SessionItem } from "@govuk-one-login/cri-types";

export interface AuditEventContext {
    personIdentity: {
        addresses: {
            postalCode: string;
        }[];
    };
    requestHeaders: Record<string, string>;
    session: SessionItem;
}
