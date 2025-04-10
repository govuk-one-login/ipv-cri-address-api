import { LambdaInterface } from "@aws-lambda-powertools/commons";
import { Logger } from "@aws-lambda-powertools/logger";
import * as zlib from "zlib";
import { CloudWatchLogsDecodedData } from "aws-lambda";
import { CloudWatchLogsEvent } from "aws-lambda/trigger/cloudwatch-logs";
import {
    CloudWatchLogsClient,
    PutLogEventsCommand,
    PutLogEventsCommandOutput,
    CreateLogStreamCommand,
} from "@aws-sdk/client-cloudwatch-logs";
import { redactPII } from "./pii-redactor";

const logger = new Logger();
const cloudwatch = new CloudWatchLogsClient();

export class PiiRedactHandler implements LambdaInterface {
    public async handler(event: CloudWatchLogsEvent, _context: unknown): Promise<object> {
        try {
            logger.info("Received " + JSON.stringify(event));

            const logDataBase64 = event.awslogs.data;
            const logDataBuffer = Buffer.from(logDataBase64, "base64");
            const decompressedData = zlib.unzipSync(logDataBuffer).toString("utf-8");
            const logEvents: CloudWatchLogsDecodedData = JSON.parse(decompressedData);
            const piiRedactLogGroup = logEvents.logGroup + "-redacted";
            const logStream = logEvents.logStream;

            try {
                await cloudwatch.send(
                    new CreateLogStreamCommand({
                        logGroupName: piiRedactLogGroup,
                        logStreamName: logStream,
                    }),
                );
            } catch (error: unknown) {
                const message = error instanceof Error ? error.message : String(error);
                if (!message.includes("The specified log stream already exists")) {
                    throw error;
                }
                logger.info(logStream + " already exists");
            }

            for (const logEvent of logEvents.logEvents) {
                logEvent.message = redactPII(logEvent.message);
            }

            logger.info("Putting redacted logs into " + piiRedactLogGroup);

            try {
                const response: PutLogEventsCommandOutput = await cloudwatch.send(
                    new PutLogEventsCommand({
                        logGroupName: piiRedactLogGroup,
                        logStreamName: logStream,
                        logEvents: logEvents.logEvents.map((event) => ({
                            id: event.id,
                            message: JSON.stringify(JSON.parse(event.message), null, 2),
                            timestamp: event.timestamp,
                            extractedFields: event.extractedFields,
                        })),
                    }),
                );
                logger.info(JSON.stringify(response));
            } catch (error) {
                logger.error(`Error putting log events into ${piiRedactLogGroup}: ${error}`);
                throw error;
            }

            return {};
        } catch (error: unknown) {
            const message = error instanceof Error ? error.message : String(error);
            logger.error(`Error in PiiRedactHandler: ${message}`);
            throw error;
        }
    }
}

const handlerClass = new PiiRedactHandler();
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
