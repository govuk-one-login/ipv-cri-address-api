package gov.uk.address.api.util;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

public class SsmParamsHelper {

    public static String getParaValue(String paraName) {
        Region region = Region.EU_WEST_2;
        SsmClient ssmClient = SsmClient.builder().region(region).build();

        getParaValue(paraName);
        ssmClient.close();

        String parameterValue = null;
        try {
            GetParameterRequest parameterRequest =
                    GetParameterRequest.builder().name(paraName).build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            parameterValue = parameterResponse.parameter().value();
            System.out.println("The parameter value is " + parameterValue);

        } catch (SsmException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return parameterValue;
    }
}
