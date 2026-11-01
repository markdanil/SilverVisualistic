package ru.pvpoverlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores remembered SilverVisible device sessions, never account passwords. */
public final class SilverVisibleProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("silvervisible-profiles.json");
    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    private static String activeLogin = "";
    private static boolean loaded;

    private SilverVisibleProfileStore() { }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(FILE)) return;
        try {
            StoreData data = GSON.fromJson(Files.readString(FILE), StoreData.class);
            if (data == null) return;
            activeLogin = data.activeLogin == null ? "" : data.activeLogin;
            if (data.profiles != null) {
                for (Profile profile : data.profiles) {
                    if (profile == null || profile.login == null || profile.token == null || profile.token.isBlank()) continue;
                    PROFILES.put(key(profile.login), profile);
                }
            }
        } catch (IOException | JsonParseException error) {
            System.err.println("[SilverVisualistic] Не удалось загрузить профили SilverVisible: " + error.getMessage());
        }
    }

    public static synchronized Profile active() {
        load();
        return PROFILES.get(key(activeLogin));
    }

    public static synchronized List<Profile> all() {
        load();
        return List.copyOf(PROFILES.values());
    }

    public static synchronized void save(Profile profile, boolean activate) {
        load();
        PROFILES.put(key(profile.login), profile);
        if (activate) activeLogin = profile.login;
        persist();
    }

    public static synchronized void activate(String login) {
        load();
        if (PROFILES.containsKey(key(login))) {
            activeLogin = PROFILES.get(key(login)).login;
            persist();
        }
    }

    public static synchronized void remove(String login) {
        load();
        PROFILES.remove(key(login));
        if (activeLogin.equalsIgnoreCase(login)) activeLogin = "";
        persist();
    }

    public static synchronized void clearActive() {
        load();
        activeLogin = "";
        persist();
    }

    private static String key(String login) { return login == null ? "" : login.toLowerCase(java.util.Locale.ROOT); }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            StoreData data = new StoreData();
            data.activeLogin = activeLogin;
            data.profiles = new ArrayList<>(PROFILES.values());
            Files.writeString(FILE, GSON.toJson(data));
            try {
                Files.setPosixFilePermissions(FILE, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {
                // Windows uses the user's profile-folder ACLs.
            }
        } catch (IOException error) {
            System.err.println("[SilverVisualistic] Не удалось сохранить профили SilverVisible: " + error.getMessage());
        }
    }

    private static final class StoreData {
        String activeLogin = "";
        List<Profile> profiles = new ArrayList<>();
    }

    public static final class Profile {
        public String id;
        public String login;
        public String nickname;
        public String role;
        public String token;
        public long expiresAt;

        public Profile() { }
        public Profile(String id, String login, String nickname, String role, String token, long expiresAt) {
            this.id = id;
            this.login = login;
            this.nickname = nickname;
            this.role = role;
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }
}
