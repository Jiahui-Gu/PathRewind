package pathrewind;

import basemod.BaseMod;
import basemod.interfaces.PostDungeonInitializeSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pathrewind.ui.RewindConfirmPopup;

@SpireInitializer
public class PathRewindMod implements PostUpdateSubscriber, PostDungeonInitializeSubscriber, PostInitializeSubscriber {

    public static final Logger logger = LogManager.getLogger(PathRewindMod.class.getName());
    private static boolean snapshotsClearedOnEnd = false;

    public PathRewindMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        logger.info("=== PathRewind Mod Initializing ===");
        new PathRewindMod();
        logger.info("=== PathRewind Mod Initialized ===");
    }

    @Override
    public void receivePostInitialize() {
        // Register dev console command for testing
        basemod.devcommands.ConsoleCommand.addCommand("rewind", pathrewind.commands.RewindCommand.class);
        logger.info("PathRewind: Console command 'rewind' registered");
    }

    @Override
    public void receivePostDungeonInitialize() {
        snapshotsClearedOnEnd = false;
        SnapshotManager.isRewinding = false;
        RewindConfirmPopup.hide();

        if (AbstractDungeon.floorNum == 0) {
            SnapshotManager.clearAllSnapshots();
            logger.info("PathRewind: New run started (floor 0), snapshots cleared");
        } else {
            SnapshotManager.restoreFromDisk();
            logger.info("PathRewind: Dungeon initialized at floor " + AbstractDungeon.floorNum
                    + ", restored snapshots from disk");
        }
    }

    @Override
    public void receivePostUpdate() {
        // BUG FIX: Reset isRewinding flag once the game is back in gameplay after a rewind.
        // receivePostDungeonInitialize may not fire reliably after a rewind reload,
        // so we detect completion of the rewind here as a fallback.
        if (SnapshotManager.isRewinding) {
            if (CardCrawlGame.mode == CardCrawlGame.GameMode.GAMEPLAY
                    && AbstractDungeon.isPlayerInDungeon()) {
                SnapshotManager.isRewinding = false;
                // Also restore snapshots from disk since PostDungeonInitialize may not have fired
                SnapshotManager.restoreFromDisk();
                logger.info("PathRewind: Rewind complete, isRewinding reset via postUpdate fallback, snapshots restored");
            }
        }

        // FIX 1: Auto-hide popup if game is not in GAMEPLAY mode (prevents softlock on main menu)
        if (CardCrawlGame.mode != CardCrawlGame.GameMode.GAMEPLAY) {
            if (RewindConfirmPopup.isVisible()) {
                RewindConfirmPopup.hide();
            }
            // FIX 3: Reset isRewinding if we're back on the main menu / splash screen.
            // This catches the case where a rewind-triggered save load fails
            // and the game falls back to the menu without entering a dungeon.
            // During normal rewind the mode goes GAMEPLAY -> CHAR_SELECT -> GAMEPLAY,
            // so we only reset when mode is NOT CHAR_SELECT (e.g., SPLASH, MAIN_MENU).
            if (SnapshotManager.isRewinding
                    && CardCrawlGame.mode != CardCrawlGame.GameMode.CHAR_SELECT) {
                SnapshotManager.isRewinding = false;
                logger.info("PathRewind: isRewinding force-reset (not in GAMEPLAY or CHAR_SELECT)");
            }
        } else {
            RewindConfirmPopup.update();
        }

        try {
            if (CardCrawlGame.mode == CardCrawlGame.GameMode.GAMEPLAY
                    && AbstractDungeon.isPlayerInDungeon()
                    && AbstractDungeon.currMapNode != null
                    && AbstractDungeon.getCurrRoom() != null) {
                if (!snapshotsClearedOnEnd && (AbstractDungeon.deathScreen != null || AbstractDungeon.victoryScreen != null)) {
                    SnapshotManager.clearAllSnapshots();
                    RewindConfirmPopup.hide();
                    snapshotsClearedOnEnd = true;
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }
}
