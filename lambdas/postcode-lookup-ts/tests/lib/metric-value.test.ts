import { describe, expect, it } from "vitest";
import { cleanMetricValue } from "../../src/lib/metric-value";

describe("cleanMetricValue", () => {
    it.each([null, undefined, "", "   ", "\t\n"])("returns no_content for %s", (value) => {
        expect(cleanMetricValue(value)).toBe("no_content");
    });

    it("removes prefixed colons", () => {
        expect(cleanMetricValue(":::metric-value")).toBe("metric-value");
    });

    it("returns no_content when the value contains only colons", () => {
        expect(cleanMetricValue(":::")).toBe("no_content");
    });

    it("returns no_content when removing colons leaves whitespace", () => {
        expect(cleanMetricValue(":::   ")).toBe("no_content");
    });

    it("replaces each whitespace character with an underscore", () => {
        expect(cleanMetricValue("metric  value")).toBe("metric__value");
    });

    it("converts leading and trailing whitespace to underscores", () => {
        expect(cleanMetricValue(" metric value ")).toBe("_metric_value_");
    });

    it("replaces non-ASCII characters with underscores", () => {
        expect(cleanMetricValue("métric")).toBe("m_tric");
    });

    it("replaces non-printable characters with underscores", () => {
        expect(cleanMetricValue("metric\u0001value")).toBe("metric_value");
    });

    it("limits the cleaned value to 250 characters", () => {
        expect(cleanMetricValue("a".repeat(251))).toBe("a".repeat(250));
    });

    it("retains printable ASCII characters", () => {
        expect(cleanMetricValue("metric:value-123")).toBe("metric:value-123");
    });
});
