import { SessionItem } from "../types/session";

export interface SessionService {
    getSession(sessionId: string): Promise<SessionItem>;
}
