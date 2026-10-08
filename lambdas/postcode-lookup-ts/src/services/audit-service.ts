export enum AuditEventType {
    REQUEST_SENT = "REQUEST_SENT",
    RESPONSE_RECEIVED = "RESPONSE_RECEIVED",
}

export interface AuditService {
    sendAuditEvent(eventType: AuditEventType, context: AuditEventContext): Promise<void>;
}
