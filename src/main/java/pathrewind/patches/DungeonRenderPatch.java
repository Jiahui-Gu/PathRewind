package pathrewind.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import pathrewind.ui.RewindConfirmPopup;

/**
 * Renders the rewind confirmation popup AFTER the dungeon renders
 * but BEFORE the cursor renders. This ensures the popup appears
 * below the cursor in Z-order.
 */
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "render"
)
public class DungeonRenderPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, SpriteBatch sb) {
        RewindConfirmPopup.render(sb);
    }
}
