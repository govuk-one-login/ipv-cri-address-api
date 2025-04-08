import { LambdaInterface } from "@aws-lambda-powertools/commons";

export class TimeFunction implements LambdaInterface {
    public async handler(_event: unknown, _context: unknown): Promise<number> {
        return Math.floor(Date.now() / 1000);
    }
}

const handlerClass = new TimeFunction();
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
