package ru.pvpoverlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Category-based settings screen with independent, per-feature controls. */
public final class PvPOverlayScreen extends Screen {
    private static final int BG = 0xFF0D0A18;
    private static final int PANEL = 0xFF19142B;
    private static final int CARD = 0xFF241D3A;
    private static final int BORDER = 0xFF44375F;
    private static final int INK = 0xFFF5F1FF;
    private static final int MUTED = 0xFFB9B0D2;
    private static final int ACCENT = 0xFFB39BFF;
    private static final int GREEN = 0xFF51C99A;
    private static final int OFF = 0xFF5A526F;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_GAP = 1;
    private static final float DESCRIPTION_SCALE = 0.82f;

    private final Screen parent;
    private final List<Page> pages = List.of(
            new Page("Эффекты", "Панель активных эффектов зелий", "EFFECTS"),
            new Page("Противник", "Информация о последней атакованной цели", "TARGET"),
            new Page("Подсветка врага", "Грань куба, кольца с туманом и орбитальные шары с Trails", "ENEMY_HIGHLIGHT"),
            new Page("Время суток", "Виджет текущего времени игрового мира", "TIME"),
            new Page("SilverVisible", "Онлайн-сообщество игроков SilverVisualistic 2.0.0+", "SILVER_VISIBLE"),
            new Page("HitFeedback", "Подсветка игрока после твоего удара", "HIT_FEEDBACK"),
            new Page("Яркость и GPS", "Ночное зрение, координаты и чистый экран в огне", "VISIBILITY"),
            new Page("Туман", "Цвет и дистанция появления тумана", "FOG"),
            new Page("Свободный обзор", "Настройки камеры и клавиши V", "LOOK"),
            new Page("Движение", "Автоматический спринт при удержании W", "MOVEMENT"),
            new Page("Игровой процесс", "Стримерский HUD с ником и адресом сервера", "GAMEPLAY"),
            new Page("Оптимизация FPS", "Независимое управление графическими упрощениями", "OPTIMIZATION"),
            new Page("Прочность брони", "Предупреждения о низкой прочности", "ARMOR"),
            new Page("Соотношение экрана", "Меняет перспективу мира и область видимости камеры; размер окна и HUD не меняются", "ASPECT"),
            new Page("Better Minecraft", "Кинематографичная атмосфера, мягкий свет и плавные анимации интерфейса", "BETTER"),
            new Page("Прицел", "Настройка собственного прицела по центру экрана", "CROSSHAIR"),
            new Page("Язык", "Выберите язык интерфейса: Русский или English", "LANGUAGE"),
            new Page("Об Авторе", "Автор SilverVisualistic и контакты для связи", "AUTHOR")
    );

    private int selectedPage;
    private int scrollIndex;
    private int sidebarScrollIndex;
    private int sidebarX, sidebarWidth;
    private int contentX, contentY, contentWidth, contentBottom;
    private Option draggingSlider;
    private int silverVisibleScrollIndex;
    private int silverVisibleRefreshX, silverVisibleRefreshY;
    private int silverVisibleRowsTop, silverVisibleRowsBottom;
    private int silverVisibleSwitchX, silverVisibleSwitchY, silverVisibleLogoutX, silverVisibleLogoutY;

    public PvPOverlayScreen(Screen parent) {
        super(Text.literal("SilverVisualistic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        sidebarX = 16;
        sidebarWidth = Math.max(118, Math.min(158, width / 4));
        contentX = sidebarX + sidebarWidth + 18;
        contentY = 98;
        contentWidth = Math.max(160, width - contentX - 18);
        // Reserve only a narrow bottom margin. The Save button is in the header,
        // which leaves room for eight compact settings cards on typical screens.
        contentBottom = Math.max(contentY + 80, height - 54);
        scrollIndex = MathHelper.clamp(scrollIndex, 0, maxScrollIndex());
        sidebarScrollIndex = MathHelper.clamp(sidebarScrollIndex, 0, maxSidebarScrollIndex());
        // Do not move the sidebar when widgets are rebuilt after a wheel scroll.
        // The selected entry is already visible when chosen; preserving this index
        // is what makes the category rail scroll immediately without a click first.
        addVisibleFields();
    }

    private int sidebarListTop() {
        return 106;
    }

    private int sidebarListBottom() {
        // Keep a small footer area free so the list never touches the frame.
        return Math.max(sidebarListTop() + 23, height - 38);
    }

    private int sidebarVisibleRows() {
        int available = Math.max(23, sidebarListBottom() - sidebarListTop());
        return Math.max(1, Math.min(8, (available - 18) / 23 + 1));
    }

    private int maxSidebarScrollIndex() {
        return Math.max(0, pages.size() - sidebarVisibleRows());
    }

    private int sidebarPageY(int pageIndex) {
        return sidebarListTop() + (pageIndex - sidebarScrollIndex) * 23;
    }

    private int rowHeight(Option option) {
        // Keep the cards dense enough to show eight complete settings on a normal
        // 720p GUI while calculating height from wrapped text (never cut a word).
        int reserve = option.kind == Kind.TOGGLE ? 92
                : option.kind == Kind.SLIDER ? 138
                : option.kind == Kind.CYCLE ? 146 : 150;
        int textWidth = Math.max(110, contentWidth - reserve - 18);
        int titleLines = Math.max(1, textRenderer.wrapLines(Text.literal(UiLanguage.tr(option.title)), textWidth).size());
        int descriptionWidth = Math.max(120, (int) (textWidth / DESCRIPTION_SCALE));
        int descriptionLines = Math.max(1, textRenderer.wrapLines(Text.literal(UiLanguage.tr(option.description)), descriptionWidth).size());
        int descriptionHeight = (int) Math.ceil(descriptionLines * textRenderer.fontHeight * DESCRIPTION_SCALE);
        return Math.max(25, 3 + titleLines * textRenderer.fontHeight + descriptionHeight);
    }

    private int rowY(int index) {
        int y = contentY;
        List<Option> list = options();
        for (int i = scrollIndex; i < index && i < list.size(); i++) y += rowHeight(list.get(i)) + ROW_GAP;
        return y;
    }

    private int visibleRows() {
        List<Option> list = options();
        int available = Math.max(0, contentBottom - contentY);
        int used = 0;
        int rows = 0;
        for (int i = scrollIndex; i < list.size() && rows < MAX_VISIBLE_ROWS; i++) {
            int next = rowHeight(list.get(i)) + (rows == 0 ? 0 : ROW_GAP);
            if (used + next > available && rows > 0) break;
            if (used + next > available) break;
            used += next;
            rows++;
        }
        return rows;
    }

    private int maxScrollIndex() {
        List<Option> list = options();
        int used = 0;
        int start = list.size();
        int rows = 0;
        int available = Math.max(0, contentBottom - contentY);
        for (int i = list.size() - 1; i >= 0 && rows < MAX_VISIBLE_ROWS; i--) {
            int next = rowHeight(list.get(i)) + (rows == 0 ? 0 : ROW_GAP);
            if (used + next > available) break;
            used += next;
            start = i;
            rows++;
        }
        return Math.max(0, start);
    }

    private void addVisibleFields() {
        List<Option> list = options();
        int end = Math.min(list.size(), scrollIndex + visibleRows());
        for (int i = scrollIndex; i < end; i++) {
            Option option = list.get(i);
            if (option.kind != Kind.INT_FIELD && option.kind != Kind.HEX_FIELD && option.kind != Kind.STRING_FIELD) continue;

            int row = i - scrollIndex;
            int y = rowY(i);
            int fieldWidth = Math.min(132, Math.max(92, contentWidth / 3));
            int x = contentX + contentWidth - fieldWidth - 12;
            TextFieldWidget field = new TextFieldWidget(textRenderer, x, y + Math.max(4, (rowHeight(option) - 20) / 2), fieldWidth, 20, Text.literal(UiLanguage.tr(option.title)));
            field.setMaxLength(option.kind == Kind.HEX_FIELD ? 7 : option.kind == Kind.STRING_FIELD ? 16 : 8);
            field.setText(option.fieldValue());
            field.setChangedListener(option::acceptFieldValue);
            addDrawableChild(field);
        }
    }

    private List<Option> options() {
        return switch (pages.get(selectedPage).key) {
            case "EFFECTS" -> effectsOptions();
            case "TARGET" -> targetOptions();
            case "ENEMY_HIGHLIGHT" -> enemyHighlightOptions();
            case "TIME" -> timeOptions();
            case "SILVER_VISIBLE" -> silverVisibleOptions();
            case "HIT_FEEDBACK" -> hitFeedbackOptions();
            case "VISIBILITY" -> visibilityOptions();
            case "FOG" -> fogOptions();
            case "LOOK" -> lookOptions();
            case "MOVEMENT" -> movementOptions();
            case "GAMEPLAY" -> gameplayOptions();
            case "OPTIMIZATION" -> optimizationOptions();
            case "ARMOR" -> armorOptions();
            case "CROSSHAIR" -> crosshairOptions();
            case "ASPECT" -> aspectOptions();
            case "BETTER" -> betterMinecraftOptions();
            case "LANGUAGE" -> languageOptions();
            case "AUTHOR" -> List.of();
            default -> List.of();
        };
    }

    private List<Option> effectsOptions() {
        return List.of(
                Option.toggle("Показывать эффекты зелий", "Включает или скрывает панель активных эффектов.", () -> PvPOverlayConfig.showEffects, v -> PvPOverlayConfig.showEffects = v),
                Option.toggle("Ванильные иконки эффектов", "Показывает стандартные иконки Minecraft рядом с названиями.", () -> PvPOverlayConfig.showEffectIcons, v -> PvPOverlayConfig.showEffectIcons = v),
                Option.slider("Размер иконок", "Размер иконок в панели эффектов.", () -> PvPOverlayConfig.effectsIconSize, v -> PvPOverlayConfig.effectsIconSize = v, 12, 36, " px"),
                Option.slider("Скругление углов", "Радиус скругления фона панели.", () -> PvPOverlayConfig.effectsCornerRadius, v -> PvPOverlayConfig.effectsCornerRadius = v, 0, 20, " px"),
                Option.slider("Расстояние между строками", "Вертикальный отступ между эффектами.", () -> PvPOverlayConfig.effectsRowGap, v -> PvPOverlayConfig.effectsRowGap = v, 2, 12, " px"),
                Option.slider("Масштаб панели", "100% — обычный размер, 50% — вдвое меньше.", () -> PvPOverlayConfig.effectsScale, v -> PvPOverlayConfig.effectsScale = v, 25, 200, "%"),
                Option.intField("Позиция X", "Расстояние от левого края экрана.", () -> PvPOverlayConfig.effectsX, v -> PvPOverlayConfig.effectsX = v, 0, 2000),
                Option.intField("Позиция Y", "Расстояние от верхнего края экрана.", () -> PvPOverlayConfig.effectsY, v -> PvPOverlayConfig.effectsY = v, 0, 2000),
                Option.hexInt("Цвет фона", "Hex-цвет фона панели, например #202020.", () -> PvPOverlayConfig.effectsBg, v -> PvPOverlayConfig.effectsBg = v),
                Option.hexInt("Цвет текста", "Hex-цвет текста, например #FFFFFF.", () -> PvPOverlayConfig.effectsTextColor, v -> PvPOverlayConfig.effectsTextColor = v),
                Option.slider("Прозрачность фона", "0% — прозрачный, 100% — непрозрачный.", () -> PvPOverlayConfig.effectsOpacity, v -> PvPOverlayConfig.effectsOpacity = v, 0, 100, "%")
        );
    }

    private List<Option> targetOptions() {
        return List.of(
                Option.toggle("Показывать информацию о цели", "Включает карточку игрока, по которому вы попали.", () -> PvPOverlayConfig.showTarget, v -> PvPOverlayConfig.showTarget = v),
                Option.toggle("Показывать здоровье", "Добавляет здоровье цели в карточку.", () -> PvPOverlayConfig.showTargetHealth, v -> PvPOverlayConfig.showTargetHealth = v),
                Option.toggle("Показывать пинг", "Добавляет сетевую задержку цели, если её можно получить.", () -> PvPOverlayConfig.showTargetPing, v -> PvPOverlayConfig.showTargetPing = v),
                Option.toggle("Показывать прочность брони", "Отображает состояние экипировки цели.", () -> PvPOverlayConfig.showTargetArmor, v -> PvPOverlayConfig.showTargetArmor = v),
                Option.intField("Смещение X", "Сдвиг карточки относительно центра. Допускаются отрицательные числа.", () -> PvPOverlayConfig.targetOffsetX, v -> PvPOverlayConfig.targetOffsetX = v, -1000, 1000),
                Option.intField("Положение Y", "Расстояние от верхнего края экрана.", () -> PvPOverlayConfig.targetHudY, v -> PvPOverlayConfig.targetHudY = v, 0, 1000),
                Option.slider("Время отображения", "Сколько миллисекунд показывать цель после удара.", () -> PvPOverlayConfig.targetHoldMs, v -> PvPOverlayConfig.targetHoldMs = v, 500, 15000, " мс"),
                Option.slider("Масштаб карточки", "100% — обычный размер.", () -> PvPOverlayConfig.targetScale, v -> PvPOverlayConfig.targetScale = v, 25, 200, "%"),
                Option.hexInt("Цвет текста", "Hex-цвет текста карточки.", () -> PvPOverlayConfig.targetColor, v -> PvPOverlayConfig.targetColor = v),
                Option.hexInt("Цвет фона", "Hex-цвет фона карточки.", () -> PvPOverlayConfig.targetBg, v -> PvPOverlayConfig.targetBg = v),
                Option.slider("Прозрачность фона", "0% — прозрачный, 100% — непрозрачный.", () -> PvPOverlayConfig.targetOpacity, v -> PvPOverlayConfig.targetOpacity = v, 0, 100, "%"),
                Option.hexInt("Цвет прочности брони", "Hex-цвет показателя прочности брони.", () -> PvPOverlayConfig.armorColor, v -> PvPOverlayConfig.armorColor = v)
        );
    }

    private List<Option> enemyHighlightOptions() {
        List<Option> result = new java.util.ArrayList<>();
        result.add(Option.toggle("Включить подсветку врага", "Показывает эффект только вокруг одной выбранной цели: игрока или любого живого моба. Игроки вашей команды исключаются.", () -> PvPOverlayConfig.enemyHighlightEnabled, v -> PvPOverlayConfig.enemyHighlightEnabled = v));
        result.add(Option.choice("Стиль подсветки", "Маркер — квадратная грань куба, обращённая к камере, без вращения; кольца — три уровня с мягким цилиндрическим туманом; шары — полупрозрачные сферы на отдельных наклонных орбитах с Trails.", () -> PvPOverlayConfig.enemyHighlightStyle, v -> { PvPOverlayConfig.enemyHighlightStyle = v; scrollIndex = 0; clearAndInit(); }, new String[]{"Маркер (грань куба)", "Кольца + туман", "Светящиеся шары"}));
        result.add(Option.choice("Выбор цели", "Под прицелом — текущий моб или игрок. Последняя атакованная — последняя выбранная живая цель.", () -> PvPOverlayConfig.enemyHighlightTargetMode, v -> PvPOverlayConfig.enemyHighlightTargetMode = v, new String[]{"Под прицелом", "Последняя атакованная"}));
        result.add(Option.choice("Режим цветов", "HEX-градиент использует заданные цвета. Rainbow плавно проходит семь цветов радуги.", () -> PvPOverlayConfig.enemyHighlightColorMode, v -> { PvPOverlayConfig.enemyHighlightColorMode = v; scrollIndex = 0; clearAndInit(); }, new String[]{"HEX-градиент", "Rainbow"}));
        if (PvPOverlayConfig.enemyHighlightColorMode == 0) {
            result.add(Option.slider("Количество HEX-цветов", "Используется от одного до четырёх цветов. В стиле шаров число сфер равно числу цветов; Rainbow создаёт семь сфер.", () -> PvPOverlayConfig.enemyHighlightColorCount, v -> PvPOverlayConfig.enemyHighlightColorCount = v, 1, 4, " цвета"));
            result.add(Option.stringField("Цвет 1 (HEX)", "Первый цвет палитры, формат #RRGGBB.", () -> PvPOverlayConfig.enemyHighlightColor1, v -> { if (validHex(v)) PvPOverlayConfig.enemyHighlightColor1 = PvPOverlayConfig.normalizeColorHex(v, "#FF4D5A"); }));
            if (PvPOverlayConfig.enemyHighlightColorCount >= 2) result.add(Option.stringField("Цвет 2 (HEX)", "Второй цвет палитры, формат #RRGGBB.", () -> PvPOverlayConfig.enemyHighlightColor2, v -> { if (validHex(v)) PvPOverlayConfig.enemyHighlightColor2 = PvPOverlayConfig.normalizeColorHex(v, "#FFC36B"); }));
            if (PvPOverlayConfig.enemyHighlightColorCount >= 3) result.add(Option.stringField("Цвет 3 (HEX)", "Третий цвет палитры, формат #RRGGBB.", () -> PvPOverlayConfig.enemyHighlightColor3, v -> { if (validHex(v)) PvPOverlayConfig.enemyHighlightColor3 = PvPOverlayConfig.normalizeColorHex(v, "#7CE7D2"); }));
            if (PvPOverlayConfig.enemyHighlightColorCount >= 4) result.add(Option.stringField("Цвет 4 (HEX)", "Четвёртый цвет палитры, формат #RRGGBB.", () -> PvPOverlayConfig.enemyHighlightColor4, v -> { if (validHex(v)) PvPOverlayConfig.enemyHighlightColor4 = PvPOverlayConfig.normalizeColorHex(v, "#8C75FF"); }));
        }
        result.add(Option.slider("Скорость градиента", "Скорость плавного переливания цветов. Rainbow использует непрерывный переход по всем семи цветам.", () -> PvPOverlayConfig.enemyHighlightGradientSpeed, v -> PvPOverlayConfig.enemyHighlightGradientSpeed = v, 1, 100, "%"));
        result.add(Option.slider("Прозрачность подсветки", "Насколько ярко и плотно видны элементы эффекта.", () -> PvPOverlayConfig.enemyHighlightOpacity, v -> PvPOverlayConfig.enemyHighlightOpacity = v, 10, 100, "%"));
        // Shared controls are always visible regardless of the selected effect.
        result.add(Option.slider("Размер эффекта", "Масштаб углового маркера, кольцевого цилиндра или орбитальных шаров относительно игрока.", () -> PvPOverlayConfig.enemyHighlightScale, v -> PvPOverlayConfig.enemyHighlightScale = v, 50, 180, "%"));
        result.add(Option.slider("Время подсветки", "Сколько держать выбранную цель после того, как прицел ушёл с неё или атака завершилась.", () -> PvPOverlayConfig.enemyHighlightHoldMs, v -> PvPOverlayConfig.enemyHighlightHoldMs = v, 500, 15000, " мс"));
        result.add(Option.slider("Максимальная дистанция", "Не отображать эффект, если цель дальше выбранного расстояния.", () -> PvPOverlayConfig.enemyHighlightMaxDistance, v -> PvPOverlayConfig.enemyHighlightMaxDistance = v, 8, 96, " блоков"));

        // Show only controls belonging to the selected visual style.
        switch (PvPOverlayConfig.enemyHighlightStyle) {
            case 0 -> result.add(Option.slider("Ширина маркера", "Толщина четырёх угловых скобок. Работает в стиле «Маркер».", () -> PvPOverlayConfig.enemyHighlightMarkerThickness, v -> PvPOverlayConfig.enemyHighlightMarkerThickness = v, 1, 8, " px"));
            case 1 -> result.add(Option.slider("Плотность тумана колец", "Прозрачность градиентного цилиндра между тремя кольцами.", () -> PvPOverlayConfig.enemyHighlightCylinderOpacity, v -> PvPOverlayConfig.enemyHighlightCylinderOpacity = v, 5, 70, "%"));
            case 2 -> {
                result.add(Option.slider("Сила свечения шаров", "Яркость объёмных одноцветных сфер. Каждому выбранному цвету соответствует отдельный шар.", () -> PvPOverlayConfig.enemyHighlightOrbGlow, v -> PvPOverlayConfig.enemyHighlightOrbGlow = v, 10, 100, "%"));
                result.add(Option.slider("Размер шаров", "Размер цельных шаров, вращающихся вокруг выбранной цели. Число шаров совпадает с числом цветов палитры.", () -> PvPOverlayConfig.enemyHighlightOrbSize, v -> PvPOverlayConfig.enemyHighlightOrbSize = v, 0, 100, "%"));
                result.add(Option.slider("Прозрачность шаров", "Прозрачность заполненных сфер: меньшее значение делает их более прозрачными.", () -> PvPOverlayConfig.enemyHighlightOrbOpacity, v -> PvPOverlayConfig.enemyHighlightOrbOpacity = v, 10, 100, "%"));
                result.add(Option.toggle("Следы шаров (Trails)", "Полупрозрачный кометный хвост за каждой сферой.", () -> PvPOverlayConfig.enemyHighlightOrbTrailsEnabled, v -> { PvPOverlayConfig.enemyHighlightOrbTrailsEnabled = v; scrollIndex = 0; clearAndInit(); }));
                if (PvPOverlayConfig.enemyHighlightOrbTrailsEnabled) {
                    result.add(Option.slider("Длина следа", "Длина полупрозрачного кометного хвоста.", () -> PvPOverlayConfig.enemyHighlightOrbTrailLength, v -> PvPOverlayConfig.enemyHighlightOrbTrailLength = v, 10, 100, "%"));
                    result.add(Option.slider("Прозрачность следа", "Насколько заметен кометный шлейф.", () -> PvPOverlayConfig.enemyHighlightOrbTrailOpacity, v -> PvPOverlayConfig.enemyHighlightOrbTrailOpacity = v, 10, 100, "%"));
                }
            }
        }
        return result;
    }

    private List<Option> languageOptions() {
        return List.of(
                Option.choice("Язык интерфейса", "Переключает язык меню, описаний и встроенных HUD-подписей.",
                        () -> PvPOverlayConfig.language,
                        value -> { PvPOverlayConfig.language = value; scrollIndex = 0; clearAndInit(); },
                        new String[]{"Русский", "Английский"})
        );
    }

    private static boolean validHex(String value) {
        return value != null && value.trim().matches("(?i)#?[0-9a-f]{6}");
    }

    private List<Option> timeOptions() {
        return List.of(
                Option.toggle("Показывать время суток", "Показывает текущую фазу игрового дня на HUD.", () -> PvPOverlayConfig.showTimeOfDay, v -> PvPOverlayConfig.showTimeOfDay = v),
                Option.intField("Отступ справа", "Расстояние виджета от правого края.", () -> PvPOverlayConfig.timeX, v -> PvPOverlayConfig.timeX = v, 0, 2000),
                Option.intField("Положение Y", "Расстояние от верхнего края экрана.", () -> PvPOverlayConfig.timeY, v -> PvPOverlayConfig.timeY = v, 0, 1000),
                Option.slider("Скругление углов", "Радиус скругления фона виджета.", () -> PvPOverlayConfig.timeCornerRadius, v -> PvPOverlayConfig.timeCornerRadius = v, 0, 20, " px"),
                Option.hexInt("Цвет фона", "Hex-цвет фона виджета.", () -> PvPOverlayConfig.timeBg, v -> PvPOverlayConfig.timeBg = v),
                Option.hexInt("Цвет текста", "Hex-цвет текста виджета.", () -> PvPOverlayConfig.timeTextColor, v -> PvPOverlayConfig.timeTextColor = v),
                Option.slider("Прозрачность фона", "0% — прозрачный, 100% — непрозрачный.", () -> PvPOverlayConfig.timeOpacity, v -> PvPOverlayConfig.timeOpacity = v, 0, 100, "%")
        );
    }

    private List<Option> silverVisibleOptions() {
        return List.of(
                Option.toggle("Показывать окно на экране", "После входа SilverVisible автоматически публикует привязанный игровой ник, версию мода и текущий сервер. Адрес сервера виден в публичном каталоге. Этот параметр управляет только HUD-счётчиком.",
                        () -> PvPOverlayConfig.silverVisibleShowHud, v -> PvPOverlayConfig.silverVisibleShowHud = v)
        );
    }

    private List<Option> hitFeedbackOptions() {
        return List.of(
                Option.toggle("Включить HitFeedback", "Подсвечивает только игрока, которого атаковал именно ты. Драки других игроков не затрагиваются. Отдельный HitFeedback.jar не нужен.",
                        () -> PvPOverlayConfig.hitFeedbackEnabled, v -> {
                            PvPOverlayConfig.hitFeedbackEnabled = v;
                            if (!v) ru.pvpoverlay.hitfeedback.HitTintTracker.clear();
                        }),
                Option.stringField("Обычный удар (HEX)", "Цвет цели после обычного удара. Формат #RRGGBB.",
                        () -> PvPOverlayConfig.hitFeedbackNormalColor,
                        v -> { if (validHex(v)) PvPOverlayConfig.hitFeedbackNormalColor = PvPOverlayConfig.normalizeColorHex(v, "#39E65C"); }),
                Option.stringField("Критический удар (HEX)", "Цвет цели после подтверждённого Minecraft критического удара. Формат #RRGGBB.",
                        () -> PvPOverlayConfig.hitFeedbackCriticalColor,
                        v -> { if (validHex(v)) PvPOverlayConfig.hitFeedbackCriticalColor = PvPOverlayConfig.normalizeColorHex(v, "#FF3838"); }),
                Option.slider("Длительность подсветки", "Сколько миллисекунд модель цели будет окрашена после попадания.",
                        () -> PvPOverlayConfig.hitFeedbackDurationMs, v -> PvPOverlayConfig.hitFeedbackDurationMs = v, 150, 2000, " мс"),
                Option.slider("Сила окрашивания", "Насколько заметно цвет смешивается с текстурой модели.",
                        () -> PvPOverlayConfig.hitFeedbackStrength, v -> PvPOverlayConfig.hitFeedbackStrength = v, 10, 100, "%")
        );
    }

    private List<Option> visibilityOptions() {
        return List.of(
                Option.toggle("Яркость", "Постоянное ночное зрение: делает тёмные места хорошо видимыми без эффекта зелья и таймера.", () -> PvPOverlayConfig.fullBrightEnabled, v -> PvPOverlayConfig.fullBrightEnabled = v),
                Option.toggle("GPS", "Показывает текущее измерение и координаты игрока X / Y / Z на экране.", () -> PvPOverlayConfig.gpsEnabled, v -> PvPOverlayConfig.gpsEnabled = v),
                Option.intField("Положение GPS X", "Отступ панели GPS от левого края экрана.", () -> PvPOverlayConfig.gpsX, v -> PvPOverlayConfig.gpsX = v, 0, 2000),
                Option.intField("Положение GPS Y", "Отступ панели GPS от верхнего края экрана.", () -> PvPOverlayConfig.gpsY, v -> PvPOverlayConfig.gpsY = v, 0, 2000),
                Option.slider("Прозрачность GPS", "Прозрачность фона панели координат.", () -> PvPOverlayConfig.gpsOpacity, v -> PvPOverlayConfig.gpsOpacity = v, 0, 100, "%"),
                Option.toggle("Без огня", "Скрывает только пламя, закрывающее ваш экран при горении. Блоки огня и пламя на других игроках остаются видимыми.", () -> PvPOverlayConfig.hideOwnFireOverlay, v -> PvPOverlayConfig.hideOwnFireOverlay = v)
        );
    }

    private List<Option> fogOptions() {
        return List.of(
                Option.toggle("Включить цветной туман", "Включает туман с выбранными цветом и расстоянием начала.", () -> PvPOverlayConfig.fogEnabled, v -> PvPOverlayConfig.fogEnabled = v),
                Option.slider("Начало тумана", "Расстояние в блоках, с которого туман начинает появляться.", () -> PvPOverlayConfig.fogStartDistance, v -> PvPOverlayConfig.fogStartDistance = v, 0, 256, " блоков"),
                Option.stringField("Цвет тумана (Hex)", "Формат #RRGGBB, например #C8D8E8.", () -> PvPOverlayConfig.fogColorHex, v -> {
                    if (v != null && v.trim().matches("(?i)#?[0-9a-f]{6}")) PvPOverlayConfig.fogColorHex = PvPOverlayConfig.normalizeFogHex(v);
                })
        );
    }

    private List<Option> lookOptions() {
        return List.of(
                Option.toggle("Включить свободный обзор", "Разрешает использовать клавишу свободного просмотра V.", () -> PvPOverlayConfig.freeLookEnabled, v -> PvPOverlayConfig.freeLookEnabled = v),
                Option.toggle("Режим удержания", "Включено — обзор активен, пока удерживается клавиша. Выключено — переключение нажатием.", () -> PvPOverlayConfig.freeLookHoldMode, v -> PvPOverlayConfig.freeLookHoldMode = v),
                Option.slider("Дистанция камеры", "Смещение назад от 0,5 до 2,5 блока; значение на шкале указано в десятых долях блока.", () -> PvPOverlayConfig.freeLookDistance, v -> PvPOverlayConfig.freeLookDistance = v, 5, 25, " / 10 блока"),
                Option.intField("Смещение окна X", "Сдвигает информационную панель свободного просмотра по горизонтали. Отрицательное значение — влево, положительное — вправо.", () -> PvPOverlayConfig.freeLookHudOffsetX, v -> PvPOverlayConfig.freeLookHudOffsetX = v, -1000, 1000),
                Option.intField("Смещение окна Y", "Сдвигает информационную панель свободного просмотра по вертикали. Отрицательное значение — вверх, положительное — вниз.", () -> PvPOverlayConfig.freeLookHudOffsetY, v -> PvPOverlayConfig.freeLookHudOffsetY = v, -1000, 1000)
        );
    }

    private List<Option> movementOptions() {
        return List.of(
                Option.toggle("Автоспринт", "Автоматически включает обычный спринт при удержании W. Отпускание W или нажатие Shift останавливает автоспринт; Ctrl по-прежнему работает вручную.", () -> PvPOverlayConfig.autoSprintEnabled, v -> PvPOverlayConfig.autoSprintEnabled = v)
        );
    }

    private List<Option> gameplayOptions() {
        List<Option> result = new java.util.ArrayList<>();
        result.add(Option.toggle("Режим стримера", "Показывает на экране аккуратную панель с твоим игровым ником и адресом сервера. Информация отображается локально в HUD.",
                () -> PvPOverlayConfig.streamerModeEnabled, v -> PvPOverlayConfig.streamerModeEnabled = v));
        if (PvPOverlayConfig.streamerModeEnabled) {
            result.add(Option.toggle("Показывать адрес сервера", "Включает строку с IP/адресом текущего Minecraft-сервера.",
                    () -> PvPOverlayConfig.streamerShowServer, v -> PvPOverlayConfig.streamerShowServer = v));
            result.add(Option.toggle("Показывать игровой ник", "Включает строку с ником текущего игрока.",
                    () -> PvPOverlayConfig.streamerShowNickname, v -> PvPOverlayConfig.streamerShowNickname = v));
        }
        return result;
    }

    private List<Option> optimizationOptions() {
        return List.of(
                Option.toggle("Включить оптимизацию", "Главный переключатель. Выключение возвращает исходные графические настройки.", () -> PvPOverlayConfig.optimizationEnabled, v -> PvPOverlayConfig.optimizationEnabled = v),
                Option.toggle("Уменьшать частицы", "Переключает режим частиц на «Уменьшено». Может снизить нагрузку в насыщенных сценах.", () -> PvPOverlayConfig.optimizationParticlesEnabled, v -> PvPOverlayConfig.optimizationParticlesEnabled = v),
                Option.toggle("Отключать облака", "Скрывает облака и уменьшает объём их отрисовки.", () -> PvPOverlayConfig.optimizationCloudsEnabled, v -> PvPOverlayConfig.optimizationCloudsEnabled = v),
                Option.toggle("Отключать тени сущностей", "Убирает тени игроков и мобов. Влияет только на графическое отображение.", () -> PvPOverlayConfig.optimizationEntityShadowsEnabled, v -> PvPOverlayConfig.optimizationEntityShadowsEnabled = v),
                Option.toggle("Уменьшать дальность сущностей", "Снижает дистанцию отрисовки игроков и мобов. На PvP может ухудшить видимость далёких противников.", () -> PvPOverlayConfig.optimizationEntityDistanceEnabled, v -> PvPOverlayConfig.optimizationEntityDistanceEnabled = v),
                Option.slider("Дальность сущностей", "Процент от исходной дальности. 100% — без уменьшения.", () -> PvPOverlayConfig.optimizationEntityDistancePercent, v -> PvPOverlayConfig.optimizationEntityDistancePercent = v, 50, 100, "%"),
                Option.toggle("Уменьшенный масштаб рендера", "Рисует 3D-сцену в меньшем буфере и растягивает её до размера окна. Снижает число обрабатываемых пикселей; интерфейс остаётся в обычном разрешении.", () -> PvPOverlayConfig.optimizationRenderScaleEnabled, v -> PvPOverlayConfig.optimizationRenderScaleEnabled = v),
                Option.slider("Разрешение 3D-рендера", "50% по ширине и высоте — примерно четверть исходного числа пикселей. 100% — стандартное разрешение.", () -> PvPOverlayConfig.optimizationRenderScalePercent, v -> PvPOverlayConfig.optimizationRenderScalePercent = v, 50, 100, "%"),
                Option.toggle("Сократить рендер вверх", "Сокращает видимую вертикальную дальность выше камеры, сохраняя обычную горизонтальную дальность. Работает через дополнительный оптимизатор Sodium.", () -> PvPOverlayConfig.optimizationVerticalRenderEnabled, v -> PvPOverlayConfig.optimizationVerticalRenderEnabled = v),
                Option.slider("Вертикальная дальность", "Процент от обычной дальности вверх. 50% — половина заданной дальности; ниже камеры дальность не меняется.", () -> PvPOverlayConfig.optimizationVerticalRenderPercent, v -> PvPOverlayConfig.optimizationVerticalRenderPercent = v, 25, 100, "%")
        );
    }

    private List<Option> crosshairOptions() {
        return List.of(
                Option.toggle("Включить пользовательский прицел", "Заменяет ванильный прицел собственным в центре экрана.", () -> PvPOverlayConfig.customCrosshairEnabled, v -> PvPOverlayConfig.customCrosshairEnabled = v),
                Option.choice("Тип прицела", "Крестик, точка, окружность, окружность с точкой или четыре точки.", () -> PvPOverlayConfig.customCrosshairType, v -> PvPOverlayConfig.customCrosshairType = v, new String[]{"Крестик", "Точка", "Круг", "Круг с точкой", "Четыре точки"}),
                Option.slider("Размер", "Общий размер прицела в пикселях.", () -> PvPOverlayConfig.customCrosshairSize, v -> PvPOverlayConfig.customCrosshairSize = v, 3, 16, " px"),
                Option.slider("Толщина линий", "Толщина элементов прицела.", () -> PvPOverlayConfig.customCrosshairThickness, v -> PvPOverlayConfig.customCrosshairThickness = v, 1, 4, " px"),
                Option.slider("Зазор от центра", "Расстояние между центром и четырьмя лучами крестика.", () -> PvPOverlayConfig.customCrosshairGap, v -> PvPOverlayConfig.customCrosshairGap = v, 1, 10, " px"),
                Option.hexInt("Цвет прицела", "Цвет в HEX-формате, например #FFFFFF.", () -> PvPOverlayConfig.customCrosshairColor, v -> PvPOverlayConfig.customCrosshairColor = v),
                Option.toggle("Чёрная обводка", "Добавляет контур для лучшей видимости на светлом фоне.", () -> PvPOverlayConfig.customCrosshairOutline, v -> PvPOverlayConfig.customCrosshairOutline = v)
        );
    }

    private List<Option> aspectOptions() {
        return List.of(
                Option.choice("Соотношение экрана", "Меняет горизонтальный угол обзора и область определения видимых объектов; размер окна и HUD не меняются.", () -> PvPOverlayConfig.aspectRatioMode, v -> PvPOverlayConfig.aspectRatioMode = v, new String[]{"Стандартное", "16 : 10", "16 : 9", "4 : 3", "Custom"}),
                Option.intField("Ширина Custom", "Числитель пользовательского соотношения, от 1 до 64.", () -> PvPOverlayConfig.aspectRatioCustomWidth, v -> PvPOverlayConfig.aspectRatioCustomWidth = v, 1, 64),
                Option.intField("Высота Custom", "Знаменатель пользовательского соотношения, от 1 до 64.", () -> PvPOverlayConfig.aspectRatioCustomHeight, v -> PvPOverlayConfig.aspectRatioCustomHeight = v, 1, 64)
        );
    }

    private List<Option> betterMinecraftOptions() {
        return List.of(
                Option.toggle("Включить Better Minecraft", "Активирует набор анимаций интерфейса и лёгких атмосферных эффектов.", () -> PvPOverlayConfig.betterMinecraftEnabled, v -> PvPOverlayConfig.betterMinecraftEnabled = v),
                Option.toggle("Shader-атмосфера", "Кинематографичная цветокоррекция неба и дальней дымки: тёплый закат, прохладные тени и мягкая глубина. Не требует внешнего шейдерпака.", () -> PvPOverlayConfig.betterShaderAtmosphere, v -> PvPOverlayConfig.betterShaderAtmosphere = v),
                Option.slider("Сила shader-атмосферы", "Интенсивность тёплой цветокоррекции и атмосферной глубины.", () -> PvPOverlayConfig.betterShaderStrength, v -> PvPOverlayConfig.betterShaderStrength = v, 0, 100, "%"),
                Option.toggle("Анимация чата", "Новые сообщения плавно выезжают слева направо.", () -> PvPOverlayConfig.betterChatAnimation, v -> PvPOverlayConfig.betterChatAnimation = v),
                Option.slider("Время выезда сообщения", "Длительность анимации появления. Можно увеличить до 3 секунд.", () -> PvPOverlayConfig.betterChatDurationMs, v -> PvPOverlayConfig.betterChatDurationMs = v, 120, 3000, " мс"),
                Option.toggle("Панель хотбара", "Заменяет стандартную подложку хотбара тёмной полупрозрачной табличкой со скруглением.", () -> PvPOverlayConfig.betterHotbarPanelEnabled, v -> PvPOverlayConfig.betterHotbarPanelEnabled = v),
                Option.toggle("Разделители слотов", "Добавляет тонкие вертикальные линии между ячейками улучшенного хотбара.", () -> PvPOverlayConfig.betterHotbarSeparatorsEnabled, v -> PvPOverlayConfig.betterHotbarSeparatorsEnabled = v),
                Option.slider("Скругление хотбара", "Радиус углов панели и подсветки выбранного слота.", () -> PvPOverlayConfig.betterHotbarCornerRadius, v -> PvPOverlayConfig.betterHotbarCornerRadius = v, 0, 11, " px"),
                Option.slider("Прозрачность хотбара", "Насколько заметна тёмная подложка.", () -> PvPOverlayConfig.betterHotbarOpacity, v -> PvPOverlayConfig.betterHotbarOpacity = v, 0, 100, "%"),
                Option.toggle("Плавный хотбар", "Подсветка выбранного слота плавно перемещается между предметами.", () -> PvPOverlayConfig.betterHotbarAnimation, v -> PvPOverlayConfig.betterHotbarAnimation = v),
                Option.slider("Скорость хотбара", "Время перемещения подсветки между соседними слотами.", () -> PvPOverlayConfig.betterHotbarDurationMs, v -> PvPOverlayConfig.betterHotbarDurationMs = v, 90, 500, " мс"),
                Option.toggle("Мягкие тени по краям", "Добавляет многослойное затемнение краёв, создавая кинематографичную глубину.", () -> PvPOverlayConfig.betterSoftVignette, v -> PvPOverlayConfig.betterSoftVignette = v),
                Option.slider("Сила теней", "Интенсивность мягкого затемнения по краям экрана.", () -> PvPOverlayConfig.betterVignetteStrength, v -> PvPOverlayConfig.betterVignetteStrength = v, 0, 100, "%"),
                Option.toggle("Индикатор солнца и луны", "Показывает иконку солнца или луны внутри панели времени суток.", () -> PvPOverlayConfig.betterCelestialMarker, v -> PvPOverlayConfig.betterCelestialMarker = v),
                Option.toggle("Свечение светила", "Добавляет мягкий ореол вокруг солнца или луны в виджете времени суток.", () -> PvPOverlayConfig.betterCelestialGlow, v -> PvPOverlayConfig.betterCelestialGlow = v),
                Option.toggle("Лёгкая атмосферная дымка", "Добавляет дальнюю цветную дымку, оттенок которой меняется вместе с временем суток; это лёгкий эффект без настоящего шейдерпака.", () -> PvPOverlayConfig.betterSoftMist, v -> PvPOverlayConfig.betterSoftMist = v),
                Option.slider("Сила атмосферной дымки", "Насколько заметнее становится дальний туман.", () -> PvPOverlayConfig.betterMistStrength, v -> PvPOverlayConfig.betterMistStrength = v, 0, 100, "%")
        );
    }

    private List<Option> armorOptions() {
        return List.of(
                Option.slider("Порог предупреждения", "Предупреждать, когда прочность любой надетой части брони опустится до этого процента.", () -> PvPOverlayConfig.warningPercent, v -> PvPOverlayConfig.warningPercent = v, 1, 50, "%"),
                Option.slider("Тип уведомления", "0 — над хотбаром, 1 — сообщение в чате, 2 — подсветка хотбара.", () -> PvPOverlayConfig.warningMode, v -> PvPOverlayConfig.warningMode = v, 0, 2, " (0–2)"),
                Option.slider("Формат прочности", "0 — осталось, 1 — процент, 2 — процент и остаток.", () -> PvPOverlayConfig.durabilityMode, v -> PvPOverlayConfig.durabilityMode = v, 0, 2, " (0–2)")
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        context.fill(10, 10, width - 10, height - 10, PANEL);
        context.fill(10, 10, width - 10, 66, 0xFF211A36);
        long menuTime = System.currentTimeMillis();
        int pulseAlpha = 110 + (int) (65 * (0.5 + 0.5 * Math.sin(menuTime / 520.0)));
        context.fill(10, 64, width - 10, 66, (pulseAlpha << 24) | 0x009A7BDB);
        int sweepWidth = Math.max(44, Math.min(130, width / 7));
        int sweepRange = Math.max(1, width - 20 - sweepWidth);
        int sweepX = 10 + (int) ((menuTime / 18L) % (sweepRange * 2L));
        if (sweepX > 10 + sweepRange) sweepX = 10 + sweepRange - (sweepX - (10 + sweepRange));
        context.fill(sweepX, 64, Math.min(width - 10, sweepX + sweepWidth), 66,
                (Math.min(210, pulseAlpha + 35) << 24) | 0x00C0A8FF);
        context.drawText(textRenderer, Text.literal("SilverVisualistic"), 25, 23, INK, false);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr("Настройки SilverVisualistic  •  RIGHT_SHIFT — открыть  •  Fabric 1.21.4")), 25, 42, MUTED, false);
        context.drawText(textRenderer, Text.literal("2.0.0"), width - 62, 27, ACCENT, false);

        // Left category rail. Draw one balanced frame: the old layout placed an
        // extra divider five pixels beside the sidebar's right border, which made
        // the separator look doubled and slightly misaligned.
        int railLeft = sidebarX;
        int railRight = sidebarX + sidebarWidth;
        int railTop = 78;
        int railBottom = height - 8;
        context.fill(railLeft, railTop, railRight, railBottom, 0xFF171227);
        context.fill(railLeft, railTop, railLeft + 1, railBottom, BORDER);
        context.fill(railRight - 1, railTop, railRight, railBottom, BORDER);
        context.fill(railLeft, railTop, railRight, railTop + 1, BORDER);
        context.fill(railLeft, railBottom - 1, railRight, railBottom, BORDER);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr("Разделы")), sidebarX + 10, 86, MUTED, false);
        int sidebarRows = sidebarVisibleRows();
        int sidebarEnd = Math.min(pages.size(), sidebarScrollIndex + sidebarRows);
        // Clip the category rail to its own viewport. The complete page list
        // remains available even on short GUI heights and can be scrolled.
        context.enableScissor(sidebarX + 2, sidebarListTop() - 5, sidebarX + sidebarWidth - 2, sidebarListBottom());
        for (int i = sidebarScrollIndex; i < sidebarEnd; i++) {
            int y = sidebarPageY(i);
            boolean active = i == selectedPage;
            boolean hovered = mouseX >= sidebarX + 5 && mouseX <= sidebarX + sidebarWidth - 5
                    && mouseY >= y - 4 && mouseY <= y + 18;
            // About Author remains the final entry, immediately after Crosshair.
            if ("AUTHOR".equals(pages.get(i).key)) {
                context.fill(sidebarX + 10, y - 5, sidebarX + sidebarWidth - 10, y - 4, BORDER);
            }
            context.fill(sidebarX + 5, y - 4, sidebarX + sidebarWidth - 5, y + 18,
                    active ? 0xFF33274F : hovered ? 0xFF241D38 : 0x00000000);
            if (active) context.fill(sidebarX + 5, y - 4, sidebarX + 8, y + 18, ACCENT);
            context.drawText(textRenderer, Text.literal(UiLanguage.tr(pages.get(i).title)), sidebarX + 15, y + 2, active ? INK : MUTED, false);
        }
        context.disableScissor();

        // Small scrollbar indicator makes it clear that the sidebar itself is
        // scrollable, without adding another separator line.
        if (maxSidebarScrollIndex() > 0) {
            String sidebarHint = UiLanguage.tr("Колесо — разделы");
            int footerTop = height - 34;
            context.fill(sidebarX + 1, footerTop, sidebarX + sidebarWidth - 1, height - 9, 0xFF211A36);
            context.fill(sidebarX + 1, footerTop, sidebarX + sidebarWidth - 1, footerTop + 1, BORDER);
            int hintX = sidebarX + (sidebarWidth - textRenderer.getWidth(sidebarHint)) / 2;
            context.drawText(textRenderer, Text.literal(sidebarHint), hintX, footerTop + 9, MUTED, false);

            int trackX = sidebarX + sidebarWidth - 5;
            int trackTop = sidebarListTop();
            int trackBottom = sidebarListBottom() - 2;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(18, trackHeight * sidebarRows / pages.size());
            int travel = Math.max(0, trackHeight - thumbHeight);
            int thumbY = trackTop + (int) Math.round((double) travel * sidebarScrollIndex / maxSidebarScrollIndex());
            context.fill(trackX, trackTop, trackX + 2, trackBottom, 0x3381735F);
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, ACCENT);
        }

        // Right settings area; its border uses the same one-pixel geometry as
        // the category rail, keeping both columns visually parallel.
        int panelLeft = contentX - 8;
        int panelRight = contentX + contentWidth + 8;
        int panelTop = 78;
        int panelBottom = height - 8;
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF141020);
        context.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, BORDER);
        context.fill(panelRight - 1, panelTop, panelRight, panelBottom, BORDER);
        Page page = pages.get(selectedPage);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr(page.title)), contentX + 5, 84, INK, false);

        if ("AUTHOR".equals(page.key)) {
            int cardX = contentX;
            int cardY = contentY + 12;
            int cardW = contentWidth;
            int cardH = 214;
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, CARD);
            context.fill(cardX, cardY, cardX + 3, cardY + cardH, ACCENT);
            context.drawText(textRenderer, Text.literal("SilverVisualistic"), cardX + 18, cardY + 16, INK, false);
            context.drawText(textRenderer, Text.literal(UiLanguage.tr("Автор и разработчик — Imperos")), cardX + 18, cardY + 38, MUTED, false);

            int rowX = cardX + 14;
            int rowW = Math.max(120, cardW - 28);
            context.fill(rowX, cardY + 68, rowX + rowW, cardY + 116, 0xFF302747);
            context.drawText(textRenderer, Text.literal("Telegram"), rowX + 12, cardY + 77, ACCENT, false);
            context.drawText(textRenderer, Text.literal("@Mark_Danil"), rowX + 12, cardY + 94, INK, false);

            context.fill(rowX, cardY + 126, rowX + rowW, cardY + 174, 0xFF302747);
            context.drawText(textRenderer, Text.literal("Discord"), rowX + 12, cardY + 135, ACCENT, false);
            context.drawText(textRenderer, Text.literal("@imperos.real"), rowX + 12, cardY + 152, INK, false);
            context.drawText(textRenderer, Text.literal(UiLanguage.tr("Спасибо, что используешь SilverVisualistic!")), cardX + 18, cardY + 190, MUTED, false);
        }

        List<Option> list = options();
        int end = Math.min(list.size(), scrollIndex + visibleRows());
        for (int i = scrollIndex; i < end; i++) {
            Option option = list.get(i);
            int y = rowY(i);
            int h = rowHeight(option);
            int right = contentX + contentWidth;
            boolean hoveredOption = mouseX >= contentX && mouseX <= right && mouseY >= y && mouseY <= y + h;
            context.fill(contentX, y, right, y + h, hoveredOption ? 0xFF2B2342 : CARD);
            context.fill(contentX, y, contentX + 3, y + h, hoveredOption ? ACCENT : BORDER);

            int reserve = option.kind == Kind.TOGGLE ? 92
                    : option.kind == Kind.SLIDER ? 138
                    : option.kind == Kind.CYCLE ? 146 : 150;
            int textWidth = Math.max(110, contentWidth - reserve - 18);
            List<net.minecraft.text.OrderedText> titleLines = textRenderer.wrapLines(Text.literal(UiLanguage.tr(option.title)), textWidth);
            int textY = y + 3;
            for (net.minecraft.text.OrderedText line : titleLines) {
                context.drawText(textRenderer, line, contentX + 12, textY, INK, false);
                textY += textRenderer.fontHeight;
            }
            int descriptionWidth = Math.max(120, (int) (textWidth / DESCRIPTION_SCALE));
            List<net.minecraft.text.OrderedText> descriptionLines = textRenderer.wrapLines(Text.literal(UiLanguage.tr(option.description)), descriptionWidth);
            context.getMatrices().push();
            context.getMatrices().scale(DESCRIPTION_SCALE, DESCRIPTION_SCALE, 1.0f);
            float descriptionY = textY;
            for (net.minecraft.text.OrderedText line : descriptionLines) {
                context.drawText(textRenderer, line,
                        (int) ((contentX + 12) / DESCRIPTION_SCALE),
                        (int) (descriptionY / DESCRIPTION_SCALE), MUTED, false);
                descriptionY += textRenderer.fontHeight * DESCRIPTION_SCALE;
            }
            context.getMatrices().pop();

            if (option.kind == Kind.TOGGLE) {
                drawToggle(context, right - 60, y + (h - 19) / 2, option.booleanGetter.getAsBoolean());
            } else if (option.kind == Kind.SLIDER) {
                drawSlider(context, right - 132, y + h - 11, 110, option);
                context.drawText(textRenderer, Text.literal(option.intGetter.getAsInt() + UiLanguage.tr(option.suffix)), right - 76, y + 4, ACCENT, false);
            } else if (option.kind == Kind.CYCLE) {
                String value = UiLanguage.tr(option.choices[MathHelper.clamp(option.intGetter.getAsInt(), 0, option.choices.length - 1)]);
                int pillW = Math.max(90, textRenderer.getWidth(value) + 22);
                int pillX = right - pillW - 10;
                int pillY = y + (h - 22) / 2;
                context.fill(pillX, pillY, right - 10, pillY + 22, 0xFF342A50);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(UiLanguage.tr(value)), pillX + pillW / 2, pillY + 7, ACCENT);
            }
        }

        if ("SILVER_VISIBLE".equals(page.key)) {
            renderSilverVisibleDirectory(context, list, end);
        }

        if (maxScrollIndex() > 0) {
            context.drawText(textRenderer, Text.literal((scrollIndex + 1) + "–" + end + " / " + list.size()), contentX + contentWidth - 72, 84, MUTED, false);
            int footerTop = height - 37;
            int footerLeft = contentX - 8;
            int footerRight = contentX + contentWidth + 8;
            context.fill(footerLeft, footerTop, footerRight, height - 8, 0xFF211A36);
            context.fill(footerLeft, footerTop, footerRight, footerTop + 1, BORDER);
            String scrollHint = "Колесо мыши — прокрутка";
            int hintX = footerLeft + (footerRight - footerLeft - textRenderer.getWidth(scrollHint)) / 2;
            context.drawText(textRenderer, Text.literal(scrollHint), hintX, footerTop + 10, MUTED, true);
        }

        int saveX = width - 144;
        int saveY = 21;
        context.fill(saveX, saveY, width - 24, saveY + 26, ACCENT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(UiLanguage.tr("Сохранить")), saveX + 60, saveY + 8, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private int silverVisibleSectionTop(List<Option> list, int end) {
        if (end <= 0) return contentY + 8;
        return rowY(end - 1) + rowHeight(list.get(end - 1)) + 10;
    }

    private void renderSilverVisibleDirectory(DrawContext context, List<Option> list, int end) {
        int top = silverVisibleSectionTop(list, end);
        int right = contentX + contentWidth;
        SilverVisibleProfileStore.Profile profile = SilverVisibleClient.activeProfile();

        // Account card: the player's linked nickname and account role come from SilverVisible auth.
        if (profile != null) {
            context.fill(contentX, top, right, top + 36, 0xFF241D3A);
            context.fill(contentX, top, contentX + 3, top + 36, 0xFF9D83E8);
            String accountLine = profile.nickname + "  ·  " + profile.login;
            context.drawText(textRenderer, Text.literal(accountLine), contentX + 12, top + 5, INK, false);
            String role = "author".equalsIgnoreCase(profile.role) ? "✦ Автор" : "Участник SilverVisible";
            context.drawText(textRenderer, Text.literal(role), contentX + 12, top + 20,
                    "author".equalsIgnoreCase(profile.role) ? ACCENT : MUTED, false);
            silverVisibleSwitchX = right - 150;
            silverVisibleSwitchY = top + 7;
            silverVisibleLogoutX = right - 78;
            silverVisibleLogoutY = top + 7;
            context.fill(silverVisibleSwitchX, silverVisibleSwitchY, silverVisibleSwitchX + 66, silverVisibleSwitchY + 22, 0xFF3A2E59);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Сменить"), silverVisibleSwitchX + 33, silverVisibleSwitchY + 7, 0xFFF1EAFF);
            context.fill(silverVisibleLogoutX, silverVisibleLogoutY, silverVisibleLogoutX + 66, silverVisibleLogoutY + 22, 0xFF503047);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Выйти"), silverVisibleLogoutX + 33, silverVisibleLogoutY + 7, 0xFFFFEAF0);
            top += 44;
        }

        int headerHeight = 43;
        silverVisibleRefreshX = right - 104;
        silverVisibleRefreshY = top + 9;
        context.fill(contentX, top, right, top + headerHeight, 0xFF241D3A);
        context.fill(contentX, top, contentX + 3, top + headerHeight, 0xFF9D83E8);
        String serviceText = SilverVisibleClient.status();
        int online = SilverVisibleClient.onlineCount();
        context.drawText(textRenderer, Text.literal(UiLanguage.tr("Сервис:") + " " + UiLanguage.tr(serviceText)), contentX + 12, top + 7, INK, false);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr("Сейчас в сети:") + " " + online), contentX + 12, top + 23, ACCENT, false);
        context.fill(silverVisibleRefreshX, silverVisibleRefreshY, right - 10, silverVisibleRefreshY + 24, 0xFF3A2E59);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(UiLanguage.tr("Обновить")), silverVisibleRefreshX + 47, silverVisibleRefreshY + 8, 0xFFF1EAFF);

        int listTop = top + headerHeight + 8;
        int listBottom = contentBottom - 4;
        silverVisibleRowsTop = Math.min(listBottom, listTop + 20);
        silverVisibleRowsBottom = Math.max(silverVisibleRowsTop, listBottom);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr("Игроки SilverVisualistic 2.0.0+")), contentX + 4, listTop, INK, false);
        List<SilverVisibleClient.PlayerInfo> current = SilverVisibleClient.players();
        int availableHeight = Math.max(0, listBottom - silverVisibleRowsTop);
        int rowHeight = 38;
        int visible = Math.max(0, availableHeight / rowHeight);
        int maxScroll = Math.max(0, current.size() - visible);
        silverVisibleScrollIndex = Math.max(0, Math.min(silverVisibleScrollIndex, maxScroll));
        context.enableScissor(contentX, silverVisibleRowsTop, right, listBottom);
        if (!SilverVisibleClient.isAuthenticated()) {
            context.drawText(textRenderer, Text.literal(UiLanguage.tr("Войди в SilverVisible, чтобы использовать каталог.")), contentX + 10, silverVisibleRowsTop + 8, MUTED, false);
        } else if (current.isEmpty()) {
            String empty = SilverVisibleClient.isServiceReachable()
                    ? UiLanguage.tr("Пока нет игроков в сети с SilverVisualistic 2.0.0+.")
                    : UiLanguage.tr("Ожидаем ответ сервиса. Нажми «Обновить» через несколько секунд.");
            context.drawText(textRenderer, Text.literal(empty), contentX + 10, silverVisibleRowsTop + 8, MUTED, false);
        } else {
            int shown = Math.min(current.size(), silverVisibleScrollIndex + visible);
            for (int i = silverVisibleScrollIndex; i < shown; i++) {
                int y = silverVisibleRowsTop + (i - silverVisibleScrollIndex) * rowHeight;
                SilverVisibleClient.PlayerInfo player = current.get(i);
                context.fill(contentX + 2, y, right - 2, y + 35, (i & 1) == 0 ? 0xFF29213F : 0xFF211A36);
                context.fill(contentX + 8, y + 9, contentX + 12, y + 13, 0xFF51C99A);
                context.drawText(textRenderer, Text.literal(player.nickname()), contentX + 19, y + 4, INK, false);
                String version = "v" + player.modVersion();
                context.drawText(textRenderer, Text.literal(version), right - textRenderer.getWidth(version) - 10, y + 4, ACCENT, false);
                String server = player.serverAddress() == null || player.serverAddress().isBlank() ? "Сервер не указан" : player.serverAddress();
                int serverMax = Math.max(40, contentWidth - 34);
                while (textRenderer.getWidth(server) > serverMax && server.length() > 4) server = server.substring(0, server.length() - 2) + "…";
                context.drawText(textRenderer, Text.literal(server), contentX + 19, y + 19, MUTED, false);
                if ("author".equalsIgnoreCase(player.role())) {
                    String author = "✦ Автор";
                    int authorWidth = textRenderer.getWidth(author) + 10;
                    int x = right - textRenderer.getWidth(version) - authorWidth - 20;
                    context.fill(x, y + 1, x + authorWidth, y + 16, 0xFF49386F);
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(author), x + authorWidth / 2, y + 4, 0xFFE9DDFF);
                }
            }
        }
        context.disableScissor();
        if (current.size() > visible && visible > 0) {
            String range = (silverVisibleScrollIndex + 1) + "–" + Math.min(current.size(), silverVisibleScrollIndex + visible) + " / " + current.size();
            context.drawText(textRenderer, Text.literal(range + "  •  " + UiLanguage.tr("колесо мыши")), right - textRenderer.getWidth(range + "  •  колесо мыши") - 4, listBottom - 10, MUTED, false);
        }
    }

    private void drawToggle(DrawContext context, int x, int y, boolean enabled) {
        context.fill(x, y, x + 46, y + 19, enabled ? GREEN : OFF);
        int knobX = enabled ? x + 25 : x + 3;
        context.fill(knobX, y + 3, knobX + 13, y + 16, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal(UiLanguage.tr(enabled ? "Да" : "Нет")), x - 37, y + 5, enabled ? GREEN : MUTED, false);
    }

    private void drawSlider(DrawContext context, int x, int y, int barWidth, Option option) {
        int value = MathHelper.clamp(option.intGetter.getAsInt(), option.min, option.max);
        double fraction = option.max == option.min ? 0 : (double) (value - option.min) / (option.max - option.min);
        int filled = (int) Math.round(barWidth * fraction);
        context.fill(x, y, x + barWidth, y + 4, 0xFF4B4262);
        context.fill(x, y, x + filled, y + 4, ACCENT);
        context.fill(x + filled - 4, y - 3, x + filled + 5, y + 11, 0xFFE1D6FF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int saveX = width - 144;
            int saveY = 21;
            if (mouseX >= saveX && mouseX <= width - 24 && mouseY >= saveY && mouseY <= saveY + 26) {
                close();
                return true;
            }

            if ("SILVER_VISIBLE".equals(pages.get(selectedPage).key)) {
                SilverVisibleProfileStore.Profile active = SilverVisibleClient.activeProfile();
                if (active != null && mouseX >= silverVisibleSwitchX && mouseX <= silverVisibleSwitchX + 66
                        && mouseY >= silverVisibleSwitchY && mouseY <= silverVisibleSwitchY + 22) {
                    client.setScreen(SilverVisibleAuthScreen.switcher(this));
                    return true;
                }
                if (active != null && mouseX >= silverVisibleLogoutX && mouseX <= silverVisibleLogoutX + 66
                        && mouseY >= silverVisibleLogoutY && mouseY <= silverVisibleLogoutY + 22) {
                    SilverVisibleClient.resetSession();
                    SilverVisibleAuthClient.logout(active, true, () -> {
                        PvPOverlayConfig.silverVisibleEnabled = false;
                        client.setScreen(SilverVisibleAuthScreen.gate(parent));
                    });
                    return true;
                }
                if (mouseX >= silverVisibleRefreshX && mouseX <= contentX + contentWidth - 10
                        && mouseY >= silverVisibleRefreshY && mouseY <= silverVisibleRefreshY + 24) {
                    SilverVisibleClient.refreshNow();
                    return true;
                }
            }

            if (mouseX >= sidebarX + 5 && mouseX <= sidebarX + sidebarWidth - 5
                    && mouseY >= sidebarListTop() - 4 && mouseY <= sidebarListBottom()) {
                int row = (int) ((mouseY - sidebarListTop() + 4) / 23);
                int pageIndex = sidebarScrollIndex + row;
                if (pageIndex >= sidebarScrollIndex && pageIndex < Math.min(pages.size(), sidebarScrollIndex + sidebarVisibleRows())) {
                    int y = sidebarPageY(pageIndex);
                    if (mouseY >= y - 4 && mouseY <= y + 18) {
                        selectedPage = pageIndex;
                        scrollIndex = 0;
                        clearAndInit();
                        return true;
                    }
                }
            }

            List<Option> list = options();
            int end = Math.min(list.size(), scrollIndex + visibleRows());
            for (int i = scrollIndex; i < end; i++) {
                Option option = list.get(i);
                int row = i - scrollIndex;
                int y = rowY(i);
                int h = rowHeight(option);
                int right = contentX + contentWidth;
                if (option.kind == Kind.TOGGLE && mouseX >= right - 64 && mouseX <= right - 8 && mouseY >= y + 4 && mouseY <= y + h - 3) {
                    option.booleanSetter.accept(!option.booleanGetter.getAsBoolean());
                    return true;
                }
                if (option.kind == Kind.SLIDER && mouseX >= right - 160 && mouseX <= right - 15 && mouseY >= y + 3 && mouseY <= y + h - 2) {
                    draggingSlider = option;
                    updateSlider(option, mouseX, right - 132, 110);
                    return true;
                }
                if (option.kind == Kind.CYCLE && mouseX >= right - 180 && mouseX <= right - 8 && mouseY >= y && mouseY <= y + h) {
                    int next = (option.intGetter.getAsInt() + 1) % option.choices.length;
                    option.intSetter.accept(next);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingSlider != null) {
            updateSlider(draggingSlider, mouseX, contentX + contentWidth - 132, 110);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean refreshHighlightPalette = draggingSlider != null
                && "Количество HEX-цветов".equals(draggingSlider.title);
        draggingSlider = null;
        if (refreshHighlightPalette) {
            scrollIndex = 0;
            clearAndInit();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int direction = verticalAmount < 0 ? 1 : -1;
        int steps = Math.max(1, Math.min(4, (int) Math.ceil(Math.abs(verticalAmount))));

        // The whole category rail owns the wheel, not only the text rows.
        // Update the scroll offset directly: rebuilding the screen here can
        // steal the first wheel event from the sidebar on some UI focus states.
        boolean overSidebarRail = mouseX >= sidebarX - 6 && mouseX <= sidebarX + sidebarWidth + 6
                && mouseY >= 78 && mouseY <= height - 8;
        if (overSidebarRail) {
            sidebarScrollIndex = MathHelper.clamp(sidebarScrollIndex + direction * steps, 0, maxSidebarScrollIndex());
            return true;
        }

        if ("SILVER_VISIBLE".equals(pages.get(selectedPage).key)
                && mouseX >= contentX && mouseX <= contentX + contentWidth
                && mouseY >= silverVisibleRowsTop && mouseY <= silverVisibleRowsBottom) {
            int available = Math.max(0, silverVisibleRowsBottom - silverVisibleRowsTop);
            int visible = Math.max(0, available / 38);
            int max = Math.max(0, SilverVisibleClient.players().size() - visible);
            silverVisibleScrollIndex = Math.max(0, Math.min(max, silverVisibleScrollIndex + direction * 2));
            return true;
        }

        boolean overSettingsPanel = mouseX >= contentX - 8 && mouseX <= contentX + contentWidth + 8
                && mouseY >= contentY - 10 && mouseY <= contentBottom;
        if (overSettingsPanel) {
            int next = MathHelper.clamp(scrollIndex + direction, 0, maxScrollIndex());
            if (next != scrollIndex) {
                scrollIndex = next;
                clearAndInit();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateSlider(Option option, double mouseX, int barX, int barWidth) {
        double fraction = MathHelper.clamp((mouseX - barX) / barWidth, 0.0, 1.0);
        option.intSetter.accept((int) Math.round(option.min + fraction * (option.max - option.min)));
    }

    @Override
    public void close() {
        PvPOverlayConfig.save();
        if (client != null) client.setScreen(parent);
    }

    private enum Kind { TOGGLE, SLIDER, CYCLE, INT_FIELD, HEX_FIELD, STRING_FIELD }

    private record Page(String title, String description, String key) { }

    private static final class Option {
        final String title;
        final String description;
        final Kind kind;
        final BooleanSupplier booleanGetter;
        final Consumer<Boolean> booleanSetter;
        final IntSupplier intGetter;
        final IntConsumer intSetter;
        final Supplier<String> stringGetter;
        final Consumer<String> stringSetter;
        final int min;
        final int max;
        final String suffix;
        String[] choices;

        private Option(String title, String description, Kind kind,
                       BooleanSupplier booleanGetter, Consumer<Boolean> booleanSetter,
                       IntSupplier intGetter, IntConsumer intSetter,
                       Supplier<String> stringGetter, Consumer<String> stringSetter,
                       int min, int max, String suffix) {
            this.title = title;
            this.description = description;
            this.kind = kind;
            this.booleanGetter = booleanGetter;
            this.booleanSetter = booleanSetter;
            this.intGetter = intGetter;
            this.intSetter = intSetter;
            this.stringGetter = stringGetter;
            this.stringSetter = stringSetter;
            this.min = min;
            this.max = max;
            this.suffix = suffix == null ? "" : suffix;
            this.choices = null;
        }

        static Option choice(String title, String description, IntSupplier getter, IntConsumer setter, String[] choices) {
            Option option = new Option(title, description, Kind.CYCLE, null, null, getter, setter, null, null, 0, choices.length - 1, "");
            option.choices = choices;
            return option;
        }

        static Option toggle(String title, String description, BooleanSupplier getter, Consumer<Boolean> setter) {
            return new Option(title, description, Kind.TOGGLE, getter, setter, null, null, null, null, 0, 0, "");
        }

        static Option slider(String title, String description, IntSupplier getter, IntConsumer setter, int min, int max, String suffix) {
            return new Option(title, description, Kind.SLIDER, null, null, getter, setter, null, null, min, max, suffix);
        }

        static Option intField(String title, String description, IntSupplier getter, IntConsumer setter, int min, int max) {
            return new Option(title, description, Kind.INT_FIELD, null, null, getter, setter,
                    () -> Integer.toString(getter.getAsInt()), value -> {
                        try {
                            setter.accept(MathHelper.clamp(Integer.parseInt(value.trim()), min, max));
                        } catch (NumberFormatException ignored) { }
                    }, min, max, "");
        }

        static Option hexInt(String title, String description, IntSupplier getter, IntConsumer setter) {
            return new Option(title, description, Kind.HEX_FIELD, null, null, getter, setter,
                    () -> String.format(Locale.ROOT, "#%06X", getter.getAsInt() & 0xFFFFFF), value -> {
                        String hex = value == null ? "" : value.trim().replace("#", "");
                        if (hex.matches("(?i)[0-9a-f]{6}")) setter.accept(Integer.parseInt(hex, 16));
                    }, 0, 0xFFFFFF, "");
        }

        static Option stringField(String title, String description, Supplier<String> getter, Consumer<String> setter) {
            return new Option(title, description, Kind.STRING_FIELD, null, null, null, null, getter, setter, 0, 0, "");
        }

        String fieldValue() {
            return stringGetter == null ? "" : stringGetter.get();
        }

        void acceptFieldValue(String value) {
            if (stringSetter != null) stringSetter.accept(value);
        }
    }
}
