export interface OrdnanceSurveyPostcodeResponse {
    results?: Result[];
}

export interface Result {
    DPA?: Dpa;
}

export interface Dpa {
    UPRN?: string;
    ORGANISATION_NAME?: string;
    DEPARTMENT_NAME?: string;
    SUB_BUILDING_NAME?: string;
    BUILDING_NAME?: string;
    BUILDING_NUMBER?: string;
    DEPENDENT_THOROUGHFARE_NAME?: string;
    THOROUGHFARE_NAME?: string;
    DOUBLE_DEPENDENT_LOCALITY?: string;
    DEPENDENT_LOCALITY?: string;
    POST_TOWN?: string;
    POSTCODE?: string;
}
