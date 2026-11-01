package ru.pvpoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;

/** Prevents opening mod settings until a SilverVisible account is authenticated. */
public final class SilverVisibleAuthScreen extends Screen {
    private static final int BG = 0xFF0D0A18;
    private static final int PANEL = 0xFF19142B;
    private static final int CARD = 0xFF241D3A;
    private static final int PURPLE = 0xFFB39BFF;
    private static final int MUTED = 0xFFB9B0D2;
    private static final int TEXT = 0xFFF5F1FF;

    private final Screen parent;
    private final boolean autoOpenOnValidToken;
    private TextFieldWidget loginField;
    private TextFieldWidget passwordField;
    private String message = "Войди в SilverVisible, чтобы открыть настройки мода.";
    private boolean busy;
    private boolean verifying;

    private SilverVisibleAuthScreen(Screen parent, boolean autoOpenOnValidToken) {
        super(Text.literal("SilverVisible — авторизация"));
        this.parent = parent;
        this.autoOpenOnValidToken = autoOpenOnValidToken;
    }

    public static Screen gate(Screen parent) { return new SilverVisibleAuthScreen(parent, true); }
    public static Screen switcher(Screen parent) { return new SilverVisibleAuthScreen(parent, false); }

    @Override
    protected void init() {
        int center = width / 2;
        int formWidth = Math.min(280, width - 40);
        int left = center - formWidth / 2;
        int top = Math.max(62, height / 2 - 112);
        loginField = addDrawableChild(new TextFieldWidget(textRenderer, left, top + 48, formWidth, 20, Text.literal("Логин SilverVisible")));
        loginField.setMaxLength(24);
        loginField.setPlaceholder(Text.literal("Логин SilverVisible"));
        passwordField = addDrawableChild(new TextFieldWidget(textRenderer, left, top + 88, formWidth, 20, Text.literal("Пароль")));
        passwordField.setMaxLength(128);
        passwordField.setPlaceholder(Text.literal("Пароль"));
        passwordField.setRenderTextProvider((value, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString("•".repeat(value.length()), Style.EMPTY));

        addDrawableChild(ButtonWidget.builder(Text.literal("Войти и открыть настройки"), button -> doLogin())
                .dimensions(left, top + 124, formWidth, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Регистрация / личный кабинет"), button -> openWebsite())
                .dimensions(left, top + 152, formWidth, 20).build());

        List<SilverVisibleProfileStore.Profile> profiles = SilverVisibleProfileStore.all();
        if (!profiles.isEmpty()) {
            int listY = top + 194;
            for (SilverVisibleProfileStore.Profile profile : profiles) {
                String label = profile.nickname + "  ·  " + profile.login + ("author".equalsIgnoreCase(profile.role) ? "  ✦ Автор" : "");
                addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> selectSavedProfile(profile))
                        .dimensions(left, listY, formWidth, 20).build());
                listY += 24;
            }
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> client.setScreen(parent))
                .dimensions(left, height - 34, formWidth, 20).build());

        if (autoOpenOnValidToken && !verifying) {
            SilverVisibleProfileStore.Profile active = SilverVisibleProfileStore.active();
            if (active != null && !busy) {
                verifying = true;
                busy = true;
                message = "Проверяем сохранённую сессию…";
                SilverVisibleAuthClient.verify(active, result -> {
                    verifying = false;
                    busy = false;
                    if (client.currentScreen != this) return;
                    if (result.success()) openSettings(result.profile());
                    else message = result.message();
                });
            }
        }
        setInitialFocus(loginField);
    }

    private void doLogin() {
        if (busy) return;
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        if (login.length() < 3 || password.length() < 12) {
            message = "Проверь логин и пароль (не менее 12 символов).";
            return;
        }
        busy = true;
        message = "Проверяем пароль и подключаем аккаунт…";
        char[] passwordChars = password.toCharArray();
        passwordField.setText("");
        SilverVisibleAuthClient.login(login, passwordChars, result -> {
            busy = false;
            if (client.currentScreen != this) return;
            if (result.success()) openSettings(result.profile());
            else message = result.message();
        });
    }

    private void selectSavedProfile(SilverVisibleProfileStore.Profile profile) {
        if (busy) return;
        busy = true;
        message = "Переключаемся на " + profile.login + "…";
        SilverVisibleAuthClient.verify(profile, result -> {
            busy = false;
            if (client.currentScreen != this) return;
            if (result.success()) openSettings(result.profile());
            else message = result.message();
        });
    }

    private void openSettings(SilverVisibleProfileStore.Profile profile) {
        if (profile.nickname == null || profile.nickname.isBlank()) {
            message = "Сначала привяжи игровой ник в личном кабинете SilverVisible.";
            return;
        }
        SilverVisibleProfileStore.activate(profile.login);
        PvPOverlayConfig.silverVisibleEnabled = true;
        PvPOverlayConfig.save();
        SilverVisibleClient.resetSession();
        client.setScreen(new PvPOverlayScreen(parent));
    }

    private void openWebsite() {
        Util.getOperatingSystem().open(URI.create(SilverVisibleClient.BASE_URL + "/account"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        long time = System.currentTimeMillis();
        int glow = 24 + (int) (18 * (0.5 + 0.5 * Math.sin(time / 650.0)));
        context.fillGradient(0, 0, width, height / 2, 0xFF1E1734, BG);
        int center = width / 2;
        int formWidth = Math.min(320, width - 30);
        int top = Math.max(62, height / 2 - 130);
        context.fill(center - formWidth / 2, top - 16, center + formWidth / 2, Math.min(height - 10, top + 256 + SilverVisibleProfileStore.all().size() * 24), PANEL);
        context.fill(center - formWidth / 2, top - 16, center + formWidth / 2, top - 13, (glow << 24) | 0x00A78BFA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("SilverVisible"), center, top, TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Единый аккаунт для SilverVisualistic"), center, top + 18, MUTED);
        context.drawText(textRenderer, Text.literal("Логин"), center - formWidth / 2 + 20, top + 39, MUTED, false);
        context.drawText(textRenderer, Text.literal("Пароль"), center - formWidth / 2 + 20, top + 79, MUTED, false);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(message), center, height - 51, message.startsWith("Проверяем") || message.startsWith("Переключаем") ? PURPLE : MUTED);
        super.render(context, mouseX, mouseY, delta);
    }
}
