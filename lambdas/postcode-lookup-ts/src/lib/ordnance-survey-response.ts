import { CanonicalAddress } from "../types/canonical-address";
import { Dpa, OrdnanceSurveyPostcodeResponse } from "../types/ordnance-survey";

export const toCanonicalAddress = (dpa: Dpa): CanonicalAddress => ({
    uprn: dpa.UPRN,
    organisationName: dpa.ORGANISATION_NAME,
    departmentName: dpa.DEPARTMENT_NAME,
    subBuildingName: dpa.SUB_BUILDING_NAME,
    buildingName: dpa.BUILDING_NAME,
    buildingNumber: dpa.BUILDING_NUMBER,
    dependentStreetName: dpa.DEPENDENT_THOROUGHFARE_NAME,
    streetName: dpa.THOROUGHFARE_NAME,
    doubleDependentAddressLocality: dpa.DOUBLE_DEPENDENT_LOCALITY,
    dependentAddressLocality: dpa.DEPENDENT_LOCALITY,
    addressLocality: dpa.POST_TOWN,
    postalCode: dpa.POSTCODE,
    addressCountry: "GB",
});

export const processOrdnanceSurveySuccessResponse = (responseBody: string): CanonicalAddress[] => {
    if (responseBody.trim() === "") {
        return [];
    }

    const response = JSON.parse(responseBody) as OrdnanceSurveyPostcodeResponse;

    return (
        response.results
            ?.map((result) => result.DPA)
            .filter((dpa): dpa is Dpa => dpa !== undefined)
            .map(toCanonicalAddress) ?? []
    );
};
