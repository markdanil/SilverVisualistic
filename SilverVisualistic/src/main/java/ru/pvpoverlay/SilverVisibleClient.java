package ru.pvpoverlay;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Authenticated presence integration for SilverVisible's public player directory. */
public final class SilverVisibleClient {
    public static final String BASE_URL = "https://silvervisible-api.silvervisible.workers.dev";
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    private static final long PLAYERS_REFRESH_INTERVAL_MS = 20_000L;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final String MOD_VERSION = FabricLoader.getInstance().getModContainer("pvpoverlay")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("2.0.0");

    private static final ExecutorService HTTP_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "SilverVisible-HTTP");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .executor(HTTP_EXECUTOR).connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_2).build();
    private static final AtomicBoolean HEARTBEAT_IN_FLIGHT = new AtomicBoolean(false);
    private static final AtomicBoolean PLAYERS_IN_FLIGHT = new AtomicBoolean(false);

    private static volatile List<PlayerInfo> players = List.of();
    private static volatile int onlineCount;
    private static volatile String status = "Требуется вход";
    private static volatile boolean serviceReachable;

    private static UUID sessionId;
    private static volatile String sessionLogin = "";
    private static volatile String sessionToken = "";
    private static boolean wasInWorld;
    private static long nextHeartbeatAt;
    private static long nextPlayersRefreshAt;

    private SilverVisibleClient() { }

    public static void tick(MinecraftClient client) {
        SilverVisibleProfileStore.Profile profile = SilverVisibleProfileStore.active();
        if (profile == null || profile.token == null || profile.token.isBlank()) {
            endSession();
            status = "Войди в SilverVisible";
            onlineCount = 0;
            players = List.of();
            serviceReachable = false;
            nextPlayersRefreshAt = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (profile.expiresAt <= now / 1000L) {
            endSession();
            status = "Сессия истекла — войди снова";
            onlineCount = 0;
            players = List.of();
            serviceReachable = true;
            return;
        }
        boolean inWorld = client.player != null && client.world != null;
        if (!sessionLogin.equalsIgnoreCase(profile.login) || !sessionToken.equals(profile.token)) {
            endSession();
            sessionLogin = profile.login;
            sessionToken = profile.token;
            status = "Аккаунт подключён";
        }

        if (inWorld) {
            if (!wasInWorld || sessionId == null) {
                sessionId = UUID.randomUUID();
                nextHeartbeatAt = 0L;
            }
            if (now >= nextHeartbeatAt) {
                nextHeartbeatAt = now + HEARTBEAT_INTERVAL_MS;
                sendHeartbeat(profile, sessionId, currentServerAddress(client));
            }
        } else if (wasInWorld && sessionId != null) {
            sendGoodbye(sessionId, sessionToken);
            sessionId = null;
        }
        wasInWorld = inWorld;

        if (now >= nextPlayersRefreshAt) {
            nextPlayersRefreshAt = now + PLAYERS_REFRESH_INTERVAL_MS;
            refreshPlayers();
        }
    }

    public static void resetSession() {
        endSession();
        nextPlayersRefreshAt = 0L;
        status = "Аккаунт переключён";
    }

    private static void endSession() {
        if (sessionId != null && !sessionToken.isBlank()) sendGoodbye(sessionId, sessionToken);
        sessionId = null;
        sessionLogin = "";
        sessionToken = "";
        wasInWorld = false;
        nextHeartbeatAt = 0L;
    }

    public static boolean isAuthenticated() {
        SilverVisibleProfileStore.Profile profile = SilverVisibleProfileStore.active();
        return profile != null && profile.token != null && !profile.token.isBlank()
                && profile.expiresAt > System.currentTimeMillis() / 1000L;
    }

    public static SilverVisibleProfileStore.Profile activeProfile() { return SilverVisibleProfileStore.active(); }

    public static void refreshNow() {
        nextPlayersRefreshAt = System.currentTimeMillis() + PLAYERS_REFRESH_INTERVAL_MS;
        refreshPlayers();
    }

    public static List<PlayerInfo> players() { return players; }
    public static int onlineCount() { return onlineCount; }
    public static String status() { return status; }
    public static boolean isServiceReachable() { return serviceReachable; }

    public static String currentServerAddress(MinecraftClient client) {
        if (client.isInSingleplayer()) return "Одиночный мир";
        if (client.getCurrentServerEntry() == null) return "Неизвестный сервер";
        String value = client.getCurrentServerEntry().address;
        if (value == null || value.isBlank()) return "Неизвестный сервер";
        String safe = value.trim().replaceAll("[\\p{Cntrl}<>]", "");
        return safe.substring(0, Math.min(160, safe.length()));
    }

    private static void sendHeartbeat(SilverVisibleProfileStore.Profile profile, UUID id, String serverAddress) {
        if (!HEARTBEAT_IN_FLIGHT.compareAndSet(false, true)) return;
        JsonObject body = new JsonObject();
        body.addProperty("modVersion", MOD_VERSION);
        body.addProperty("sessionId", id.toString());
        body.addProperty("serverAddress", serverAddress);
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/heartbeat"))
                .timeout(REQUEST_TIMEOUT).header("Content-Type", "application/json")
                .header("Accept", "application/json").header("Authorization", "Bearer " + profile.token)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    HEARTBEAT_IN_FLIGHT.set(false);
                    if (error != null) {
                        if (profile.login.equalsIgnoreCase(sessionLogin)) status = "Сервис недоступен";
                        return;
                    }
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        if (profile.login.equalsIgnoreCase(sessionLogin)) status = "Ты отображаешься в каталоге";
                    } else {
                        String message = readErrorMessage(response.body());
                        if (profile.login.equalsIgnoreCase(sessionLogin)) status = message;
                        if (response.statusCode() == 401) serviceReachable = true;
                    }
                });
    }

    private static void refreshPlayers() {
        if (!PLAYERS_IN_FLIGHT.compareAndSet(false, true)) return;
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/players?limit=200"))
                .timeout(REQUEST_TIMEOUT).header("Accept", "application/json").GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    PLAYERS_IN_FLIGHT.set(false);
                    if (error != null) {
                        serviceReachable = false;
                        status = "Сервис недоступен";
                        return;
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        serviceReachable = false;
                        status = "Ошибка API: " + response.statusCode();
                        return;
                    }
                    try {
                        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                        int count = Math.max(0, root.has("onlineCount") ? root.get("onlineCount").getAsInt() : 0);
                        JsonArray entries = root.has("players") && root.get("players").isJsonArray()
                                ? root.getAsJsonArray("players") : new JsonArray();
                        List<PlayerInfo> parsed = new ArrayList<>();
                        for (int i = 0; i < entries.size(); i++) {
                            if (!entries.get(i).isJsonObject()) continue;
                            JsonObject entry = entries.get(i).getAsJsonObject();
                            String nickname = entry.has("nickname") ? entry.get("nickname").getAsString() : "";
                            if (nickname.isBlank()) continue;
                            String version = entry.has("modVersion") ? entry.get("modVersion").getAsString() : "";
                            String server = entry.has("serverAddress") ? entry.get("serverAddress").getAsString() : "Не указан";
                            String role = entry.has("role") ? entry.get("role").getAsString() : "member";
                            parsed.add(new PlayerInfo(nickname, version, server, role));
                        }
                        players = List.copyOf(parsed);
                        onlineCount = count;
                        serviceReachable = true;
                        if (isAuthenticated()) status = "Сервис доступен";
                    } catch (Exception malformedResponse) {
                        serviceReachable = false;
                        status = "Некорректный ответ API";
                    }
                });
    }

    private static void sendGoodbye(UUID id, String token) {
        JsonObject body = new JsonObject();
        body.addProperty("sessionId", id.toString());
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/goodbye"))
                .timeout(REQUEST_TIMEOUT).header("Content-Type", "application/json")
                .header("Accept", "application/json").header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private static String readErrorMessage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("message")) return json.get("message").getAsString();
        } catch (Exception ignored) { }
        return "Ошибка SilverVisible";
    }

    public record PlayerInfo(String nickname, String modVersion, String serverAddress, String role) { }
}
