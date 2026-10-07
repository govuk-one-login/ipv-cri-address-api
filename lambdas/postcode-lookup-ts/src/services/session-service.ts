import { SessionItem } from "../types/session";

export interface SessionService {
    validateSessionId(sessionId: string): Promise<SessionItem>;
}
