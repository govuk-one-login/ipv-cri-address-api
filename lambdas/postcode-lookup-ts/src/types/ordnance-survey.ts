export interface OrdnanceSurveyPostcodeResponse {
    results?: Result[];
}

export interface Result {
    DPA?: Dpa;
}

export interface Dpa {
    UPRN?: string;
    ADDRESS?: string;
    POSTCODE?: string;
}
