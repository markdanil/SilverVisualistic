package ru.pvpoverlay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** SilverVisible website account authentication for the in-game settings gate. */
public final class SilverVisibleAuthClient {
    private static final String BASE_URL = SilverVisibleClient.BASE_URL;
    private static final Duration TIMEOUT = Duration.ofSeconds(12);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "SilverVisible-Auth");
        thread.setDaemon(true);
        return thread;
    });
    // Keep HttpClient's internal work off the single-thread auth task executor.
    // login()/verify()/logout() call synchronous HTTP.send() from EXECUTOR; sharing
    // that executor with HttpClient can starve its own response/body processing.
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).build();

    private SilverVisibleAuthClient() { }

    public static void login(String login, char[] password, Consumer<Result> callback) {
        EXECUTOR.execute(() -> {
            try {
                JsonObject challenge = postJson("/v1/auth/challenge", json("login", login));
                String salt = requiredString(challenge, "salt");
                int iterations = challenge.get("iterations").getAsInt();
                if (iterations < 100_000 || iterations > 2_000_000 || salt.length() != 32) {
                    throw new IllegalStateException("Сервис вернул некорректные параметры защиты пароля.");
                }
                String verifier = deriveVerifier(password, salt, iterations);
                JsonObject payload = json("login", login);
                payload.addProperty("verifier", verifier);
                JsonObject response = postJson("/v1/auth/mod-login", payload);
                JsonObject account = response.getAsJsonObject("account");
                String token = requiredString(response, "token");
                long expiresAt = response.get("expiresAt").getAsLong();
                SilverVisibleProfileStore.Profile profile = toProfile(account, token, expiresAt);
                if (profile.nickname == null || profile.nickname.isBlank()) {
                    throw new IllegalStateException("К аккаунту не привязан игровой ник. Открой сайт SilverVisible и укажи ник.");
                }
                SilverVisibleProfileStore.save(profile, true);
                dispatch(callback, Result.ok(profile));
            } catch (Exception error) {
                dispatch(callback, Result.fail(message(error)));
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
    }

    public static void verify(SilverVisibleProfileStore.Profile profile, Consumer<Result> callback) {
        EXECUTOR.execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/auth/me"))
                        .timeout(TIMEOUT).header("Authorization", "Bearer " + profile.token)
                        .header("Accept", "application/json").GET().build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException(errorMessage(response.body(), "Сессия истекла. Войди снова."));
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject account = root.getAsJsonObject("account");
                SilverVisibleProfileStore.Profile refreshed = toProfile(account, profile.token, profile.expiresAt);
                SilverVisibleProfileStore.save(refreshed, true);
                dispatch(callback, Result.ok(refreshed));
            } catch (Exception error) {
                dispatch(callback, Result.fail(message(error)));
            }
        });
    }

    public static void logout(SilverVisibleProfileStore.Profile profile, boolean forgetLocally, Runnable callback) {
        EXECUTOR.execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/auth/logout"))
                        .timeout(TIMEOUT).header("Authorization", "Bearer " + profile.token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)).build();
                HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception error) {
                System.err.println("[SilverVisualistic] Не удалось завершить сессию SilverVisible на сервере: " + error.getMessage());
            } finally {
                if (forgetLocally) SilverVisibleProfileStore.remove(profile.login);
                dispatch(callback, null);
            }
        });
    }

    private static SilverVisibleProfileStore.Profile toProfile(JsonObject account, String token, long expiresAt) {
        return new SilverVisibleProfileStore.Profile(
                optionalString(account, "id"), requiredString(account, "login"),
                optionalString(account, "nickname"), optionalString(account, "role"), token, expiresAt);
    }

    private static String deriveVerifier(char[] password, String saltHex, int iterations) throws GeneralSecurityException {
        byte[] salt = HexFormat.of().parseHex(saltHex);
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(encoded);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }

    private static JsonObject postJson(String path, JsonObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(TIMEOUT).header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(errorMessage(response.body(), "Сервер отклонил запрос (" + response.statusCode() + ")."));
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static JsonObject json(String key, String value) {
        JsonObject object = new JsonObject();
        object.addProperty(key, value);
        return object;
    }

    private static String requiredString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) throw new IllegalStateException("В ответе сервиса отсутствует поле " + key + ".");
        return object.get(key).getAsString();
    }

    private static String optionalString(JsonObject object, String key) {
        return object == null || !object.has(key) || object.get(key).isJsonNull() ? "" : object.get(key).getAsString();
    }

    private static String errorMessage(String body, String fallback) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("message")) return json.get("message").getAsString();
        } catch (Exception ignored) { }
        return fallback;
    }

    private static String message(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "Не удалось подключиться к SilverVisible.";
        if (error instanceof java.net.ConnectException || error instanceof java.net.http.HttpTimeoutException) return "SilverVisible недоступен. Проверь подключение к интернету.";
        return message;
    }

    private static void dispatch(Consumer<Result> callback, Result result) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> callback.accept(result));
    }

    public record Result(boolean success, SilverVisibleProfileStore.Profile profile, String message) {
        static Result ok(SilverVisibleProfileStore.Profile profile) { return new Result(true, profile, ""); }
        static Result fail(String message) { return new Result(false, null, message); }
    }
}
