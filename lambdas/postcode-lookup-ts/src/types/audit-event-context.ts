import { SessionItem } from "./session";

export interface AuditEventContext {
    personIdentity: {
        addresses: {
            postalCode: string;
        }[];
    };
    requestHeaders: Record<string, string>;
    session: SessionItem;
}
