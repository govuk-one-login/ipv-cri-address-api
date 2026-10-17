export const decodePostcode = (postcode: string): string => decodeURIComponent(postcode.replaceAll("+", " "));
