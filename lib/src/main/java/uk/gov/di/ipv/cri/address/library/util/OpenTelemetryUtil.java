package uk.gov.di.ipv.cri.address.library.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class OpenTelemetryUtil {
    private static final OpenTelemetry OPEN_TELEMETRY = GlobalOpenTelemetry.get();

    private OpenTelemetryUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static OpenTelemetry getOpenTelemetry() {
        return OPEN_TELEMETRY;
    }

    public static Tracer getTracer(String instrumentationScopeName) {
        return getOpenTelemetry().getTracer(instrumentationScopeName);
    }

    public static Span createSpan(
            Class<?> clazz, String functionName, String httpMethod, String url) {
        return getTracer(clazz.getName())
                .spanBuilder(String.format("%s %s", httpMethod, functionName))
                .setSpanKind(SpanKind.CLIENT)
                .setStartTimestamp(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS)
                .setAttribute("http.method", httpMethod)
                .setAttribute("url.full", url)
                .setAttribute("function", functionName)
                .startSpan();
    }

    public static void endSpan(Span span) {
        span.end(0, TimeUnit.MILLISECONDS);
    }
}
