jest.mock("@aws-sdk/client-ssm", () => {
    const originalModule = jest.requireActual("@aws-sdk/client-ssm");
    return {
        __esModule: true,
        ...originalModule,
        GetParameterCommand: jest.fn(),
    };
});

import { ConfigService } from "../../../src/services/config-service";
import { SsmClient } from "../../../src/lib/param-store-client";
import { GetParameterCommand } from "@aws-sdk/client-ssm";

const mockSsmClient = jest.mocked(SsmClient);
const mockGetParameterCommand = jest.mocked(GetParameterCommand);

describe("Config Service", () => {
    const OLD_ENV = process.env;
    let configService: ConfigService;

    beforeAll(() => {
        process.env.AWS_STACK_NAME = "test";
    });

    afterAll(() => {
        process.env = { ...OLD_ENV };
    });

    beforeEach(() => {
        jest.resetAllMocks();
        configService = new ConfigService(SsmClient);
    });

    it("should set the correct parameter when init is called", async () => {
        mockSsmClient.send = jest.fn().mockResolvedValue({
            Parameter: { Value: "myParameter" },
        });

        await configService.init();

        expect(mockGetParameterCommand).toHaveBeenCalledWith({ Name: "/test/AddressLookupTableName" });
        expect(configService.config.AddressLookupTableName).toEqual("myParameter");
    });

    it("should not set the parameter when its not found", async () => {
        try {
            process.env.AWS_STACK_NAME = "test";
            mockSsmClient.send = jest.fn().mockResolvedValue({});

            expect.assertions(5);
            await configService.init();
        } catch (err) {
            expect(mockGetParameterCommand).toHaveBeenCalledWith({ Name: "/test/AddressLookupTableName" });
            expect(configService.config.AddressLookupTableName).toEqual(undefined);
            expect(err).toBeDefined();
            expect(typeof err).toBe("object");
            const errorObj = err as Error;
            expect(errorObj.message).toContain("Missing Parameter - AddressLookupTableName");
        }
    });
});
