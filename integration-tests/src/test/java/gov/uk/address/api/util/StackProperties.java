package gov.uk.address.api.util;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;

import java.security.InvalidParameterException;

public final class StackProperties {

    private static final AmazonCloudFormation cloudFormation =
            AmazonCloudFormationClientBuilder.standard()
                    .withRegion(System.getenv("AWS_REGION"))
                    .build();

    public static String getParameter(String stackName, String parameterName) {
        return getStack(stackName).getParameters().stream()
                .filter(parameter -> parameter.getParameterKey().equals(parameterName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new InvalidParameterException(
                                        String.format(
                                                "Could not get parameter %s from stack %s",
                                                parameterName, stackName)))
                .getParameterValue();
    }

    public static String getOutput(String stackName, String outputName) {
        return getStack(stackName).getOutputs().stream()
                .filter(output -> output.getOutputKey().equals(outputName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new InvalidParameterException(
                                        String.format(
                                                "Could not get output %s from stack %s",
                                                outputName, stackName)))
                .getOutputValue();
    }

    private static Stack getStack(String stackName) {
        return cloudFormation
                .describeStacks(new DescribeStacksRequest().withStackName(stackName))
                .getStacks()
                .get(0);
    }
}
