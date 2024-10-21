import { CanonicalAddress } from "./canonical-address";

export type Address = {
    addresses: CanonicalAddress[];
    context?: string;
};
