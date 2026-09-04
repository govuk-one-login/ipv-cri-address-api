import {
  APIGatewayProxyEvent,
  APIGatewayProxyResult,
  Context,
} from "aws-lambda";

export function main(
  event: APIGatewayProxyEvent,
  context: Context,
): APIGatewayProxyResult {
  const body = {
    message: "hello",
    method: event.httpMethod,
    contextMethod: event.requestContext.httpMethod,
    body: event.body,
  };

  return {
    statusCode: 200,
    body: JSON.stringify(body),
  };
}
