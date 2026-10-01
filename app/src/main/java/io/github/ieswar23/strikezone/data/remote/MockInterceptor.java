package io.github.ieswar23.strikezone.data.remote;

import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Serves JSON fixtures from {@code assets/api} instead of hitting the network, with a small
 * artificial latency so loading states are visible. Short-circuits the chain, so no socket is
 * ever opened.
 */
public final class MockInterceptor implements Interceptor {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Map<String, String> ROUTES = new HashMap<>();

    static {
        ROUTES.put("/v1/tournament/teams", "api/teams.json");
        ROUTES.put("/v1/challenges/daily", "api/challenges.json");
    }

    private final AssetManager assets;
    private final long minDelayMs;
    private final long maxDelayMs;
    private final Random random = new Random();

    public MockInterceptor(AssetManager assets, long minDelayMs, long maxDelayMs) {
        this.assets = assets;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        simulateLatency();
        String path = request.url().encodedPath();
        String asset = ROUTES.get(path);
        if (asset == null) {
            return build(request, 404, "Not Found", "{\"error\":\"No mock route for " + path + "\"}");
        }
        return build(request, 200, "OK", readAsset(asset));
    }

    private void simulateLatency() throws IOException {
        long delay = minDelayMs + (long) (random.nextDouble() * (maxDelayMs - minDelayMs));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private String readAsset(String name) throws IOException {
        try (InputStream in = assets.open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static Response build(Request request, int code, String message, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .header("Content-Type", "application/json")
                .body(ResponseBody.create(body, JSON))
                .build();
    }
}
