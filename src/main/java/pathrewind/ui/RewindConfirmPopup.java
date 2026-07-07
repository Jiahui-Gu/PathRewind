package pathrewind.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import pathrewind.PathRewindMod;
import pathrewind.SnapshotManager;

import java.util.HashMap;
import java.util.Map;

public class RewindConfirmPopup {

    private static boolean visible = false;
    private static int pendingFloor = -1;
    private static int pendingNodeY = -1;
    private static int pendingAct = -1;
    private static String pendingRoomDesc = "";
    private static String errorMessage = null;
    private static float errorTimer = 0f;

    // Dynamic layout
    private static float popupW() { return 500f * Settings.scale; }
    private static float popupH() { return 280f * Settings.scale; }
    private static float popupX() { return (Settings.WIDTH - popupW()) / 2f; }
    private static float popupY() { return (Settings.HEIGHT - popupH()) / 3f; }
    private static float btnW() { return 160f * Settings.scale; }
    private static float btnH() { return 50f * Settings.scale; }
    private static float btnGap() { return 40f * Settings.scale; }
    private static float yesX() { return popupX() + popupW() / 2f - btnW() - btnGap() / 2f; }
    private static float noX() { return popupX() + popupW() / 2f + btnGap() / 2f; }
    private static float btnY() { return popupY() + 25f * Settings.scale; }

    private static Hitbox yesHb = new Hitbox(160f, 50f);
    private static Hitbox noHb = new Hitbox(160f, 50f);

    // FIX 5: Static color constants to avoid GC pressure
    private static final Color OVERLAY_COLOR = new Color(0f, 0f, 0f, 0.7f);
    private static final Color BG_COLOR = new Color(0.12f, 0.12f, 0.18f, 0.95f);
    private static final Color BORDER_COLOR = new Color(0.5f, 0.5f, 1.0f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.9f, 0.9f, 1.0f, 1.0f);
    private static final Color ROOM_COLOR = new Color(1.0f, 0.85f, 0.4f, 1.0f);
    private static final Color WARN_COLOR = new Color(1.0f, 0.3f, 0.3f, 1.0f);
    private static final Color WARN2_COLOR = new Color(1.0f, 0.5f, 0.5f, 0.9f);
    private static final Color YES_HOVER = new Color(0.2f, 0.6f, 0.2f, 0.9f);
    private static final Color YES_NORMAL = new Color(0.15f, 0.4f, 0.15f, 0.85f);
    private static final Color NO_HOVER = new Color(0.6f, 0.2f, 0.2f, 0.9f);
    private static final Color NO_NORMAL = new Color(0.4f, 0.15f, 0.15f, 0.85f);

    // Bilingual helpers
    private static boolean isChinese() {
        return Settings.language == Settings.GameLanguage.ZHS
            || Settings.language == Settings.GameLanguage.ZHT;
    }

    private static String t(String en, String zh) {
        return isChinese() ? zh : en;
    }

    // Room name maps (English and Chinese)
    private static final Map<String, String> ROOM_NAMES_EN = new HashMap<String, String>();
    private static final Map<String, String> ROOM_NAMES_ZH = new HashMap<String, String>();
    static {
        ROOM_NAMES_EN.put("MonsterRoom", "Monster");
        ROOM_NAMES_EN.put("MonsterRoomElite", "Elite");
        ROOM_NAMES_EN.put("MonsterRoomBoss", "Boss");
        ROOM_NAMES_EN.put("EventRoom", "Event");
        ROOM_NAMES_EN.put("ShopRoom", "Shop");
        ROOM_NAMES_EN.put("RestRoom", "Rest Site");
        ROOM_NAMES_EN.put("TreasureRoom", "Treasure");
        ROOM_NAMES_EN.put("TreasureRoomBoss", "Boss Treasure");
        ROOM_NAMES_EN.put("EmptyRoom", "Empty");
        ROOM_NAMES_EN.put("VictoryRoom", "Victory");
        ROOM_NAMES_EN.put("TrueVictoryRoom", "True Victory");

        ROOM_NAMES_ZH.put("MonsterRoom", "\u602a\u7269");
        ROOM_NAMES_ZH.put("MonsterRoomElite", "\u7cbe\u82f1");
        ROOM_NAMES_ZH.put("MonsterRoomBoss", "\u9996\u9886");
        ROOM_NAMES_ZH.put("EventRoom", "\u4e8b\u4ef6");
        ROOM_NAMES_ZH.put("ShopRoom", "\u5546\u5e97");
        ROOM_NAMES_ZH.put("RestRoom", "\u7bc0\u706b");
        ROOM_NAMES_ZH.put("TreasureRoom", "\u5b9d\u7bb1");
        ROOM_NAMES_ZH.put("TreasureRoomBoss", "\u9996\u9886\u5b9d\u7bb1");
        ROOM_NAMES_ZH.put("EmptyRoom", "\u7a7a\u623f\u95f4");
        ROOM_NAMES_ZH.put("VictoryRoom", "\u80dc\u5229");
        ROOM_NAMES_ZH.put("TrueVictoryRoom", "\u771f\u6b63\u80dc\u5229");
    }

    public static void show(int floor, int nodeY, int act, String roomDesc) {
        pendingFloor = floor;
        pendingNodeY = nodeY;
        pendingAct = act;
        Map<String, String> roomNames = isChinese() ? ROOM_NAMES_ZH : ROOM_NAMES_EN;
        String displayName = roomNames.get(roomDesc);
        pendingRoomDesc = displayName != null ? displayName : roomDesc;
        errorMessage = null;
        errorTimer = 0f;
        visible = true;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void hide() {
        visible = false;
        pendingFloor = -1;
        errorMessage = null;
        errorTimer = 0f;
    }

    public static void update() {
        if (!visible) return;

        // FIX 7: If showing error message, count down and auto-dismiss
        if (errorMessage != null) {
            errorTimer -= com.badlogic.gdx.Gdx.graphics.getDeltaTime();
            if (errorTimer <= 0f || InputHelper.justClickedLeft) {
                InputHelper.justClickedLeft = false;
                hide();
            }
            return;
        }

        // FIX 1: Allow Escape key to dismiss the popup
        if (com.megacrit.cardcrawl.helpers.input.InputHelper.pressedEscape) {
            com.megacrit.cardcrawl.helpers.input.InputHelper.pressedEscape = false;
            hide();
            return;
        }

        yesHb.resize(btnW(), btnH());
        noHb.resize(btnW(), btnH());
        yesHb.move(yesX() + btnW() / 2f, btnY() + btnH() / 2f);
        noHb.move(noX() + btnW() / 2f, btnY() + btnH() / 2f);
        yesHb.update();
        noHb.update();

        if (InputHelper.justClickedLeft) {
            if (yesHb.hovered) {
                InputHelper.justClickedLeft = false;
                PathRewindMod.logger.info("PathRewind: Confirmed rewind to floor " + pendingFloor);
                boolean success = SnapshotManager.loadSnapshotForNode(pendingFloor);
                if (success) {
                    hide();
                } else {
                    // FIX 7: Show error feedback instead of silently doing nothing
                    errorMessage = t("Rewind failed! Snapshot may be corrupted.",
                                     "\u56de\u6eaf\u5931\u8d25! \u5feb\u7167\u53ef\u80fd\u5df2\u635f\u574f\u3002");
                    errorTimer = 2.0f;
                    PathRewindMod.logger.error("PathRewind: loadSnapshotForNode returned false");
                }
            } else if (noHb.hovered) {
                InputHelper.justClickedLeft = false;
                hide();
                PathRewindMod.logger.info("PathRewind: Cancelled rewind");
            } else {
                InputHelper.justClickedLeft = false;
            }
        }
    }

    public static void render(SpriteBatch sb) {
        if (!visible) return;

        float pW = popupW();
        float pH = popupH();
        float pX = popupX();
        float pY = popupY();
        float bW = btnW();
        float bH = btnH();
        float yX = yesX();
        float nX = noX();
        float bY = btnY();

        Texture tex = ImageMaster.WHITE_SQUARE_IMG;

        // Dark overlay
        sb.setColor(OVERLAY_COLOR);
        sb.draw(tex, 0, 0, Settings.WIDTH, Settings.HEIGHT);

        // Popup background
        sb.setColor(BG_COLOR);
        sb.draw(tex, pX, pY, pW, pH);

        // Border
        float bw = 2f * Settings.scale;
        sb.setColor(BORDER_COLOR);
        sb.draw(tex, pX, pY + pH - bw, pW, bw);
        sb.draw(tex, pX, pY, pW, bw);
        sb.draw(tex, pX, pY, bw, pH);
        sb.draw(tex, pX + pW - bw, pY, bw, pH);

        // FIX 7: If showing error, render error message instead of normal content
        if (errorMessage != null) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                    t("Error", "\u9519\u8bef"),
                    pX + pW / 2f, pY + pH - 35f * Settings.scale,
                    WARN_COLOR);
            FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                    errorMessage,
                    pX + pW / 2f, pY + pH / 2f,
                    Color.WHITE);
            FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                    t("Click anywhere to dismiss", "\u70b9\u51fb\u4efb\u610f\u4f4d\u7f6e\u5173\u95ed"),
                    pX + pW / 2f, pY + pH / 2f - 30f * Settings.scale,
                    Color.LIGHT_GRAY);
            sb.setColor(Color.WHITE);
            return;
        }

        // Title
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                t("Path Rewind", "\u8def\u5f84\u56de\u6eaf"),
                pX + pW / 2f, pY + pH - 35f * Settings.scale,
                TITLE_COLOR);

        // Act and floor info
        String actLine;
        if (isChinese()) {
            actLine = "\u7b2c " + pendingAct + " \u7ae0 - \u7b2c " + pendingNodeY + " \u5c42 (\u603b\u5c42\u6570 " + pendingFloor + ")";
        } else {
            actLine = "Act " + pendingAct + " - Row " + pendingNodeY + " (Floor " + pendingFloor + ")";
        }
        FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                actLine,
                pX + pW / 2f, pY + pH - 80f * Settings.scale,
                Color.LIGHT_GRAY);

        // Room type
        String roomLine = t("Room: ", "\u623f\u95f4: ") + pendingRoomDesc;
        FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                roomLine,
                pX + pW / 2f, pY + pH - 110f * Settings.scale,
                ROOM_COLOR);

        // Warning
        FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                t("WARNING: This cannot be undone!", "\u8b66\u544a: \u6b64\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500!"),
                pX + pW / 2f, pY + pH - 155f * Settings.scale,
                WARN_COLOR);
        FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N,
                t("Current progress after this point will be lost.",
                  "\u6b64\u8282\u70b9\u4e4b\u540e\u7684\u6240\u6709\u8fdb\u5ea6\u5c06\u4f1a\u4e22\u5931\u3002"),
                pX + pW / 2f, pY + pH - 180f * Settings.scale,
                WARN2_COLOR);

        // Yes button
        sb.setColor(yesHb.hovered ? YES_HOVER : YES_NORMAL);
        sb.draw(tex, yX, bY, bW, bH);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                t("Confirm", "\u786e\u8ba4"), yX + bW / 2f, bY + bH / 2f, Color.WHITE);

        // No button
        sb.setColor(noHb.hovered ? NO_HOVER : NO_NORMAL);
        sb.draw(tex, nX, bY, bW, bH);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                t("Cancel", "\u53d6\u6d88"), nX + bW / 2f, bY + bH / 2f, Color.WHITE);

        // Reset color for other renderers
        sb.setColor(Color.WHITE);

        yesHb.render(sb);
        noHb.render(sb);
    }
}
