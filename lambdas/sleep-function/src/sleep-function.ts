import { LambdaInterface } from "@aws-lambda-powertools/commons";

export class SleepFunction implements LambdaInterface {
    public async handler(event: { ms: number }, _context: unknown) {
        await new Promise((resolve) => setTimeout(resolve, event.ms));
    }
}

const handlerClass = new SleepFunction();
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
