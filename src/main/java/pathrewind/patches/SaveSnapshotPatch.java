package pathrewind.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import pathrewind.PathRewindMod;
import pathrewind.SnapshotManager;

@SpirePatch(
    clz = AbstractDungeon.class,
    method = "nextRoomTransitionStart"
)
public class SaveSnapshotPatch {

    @SpirePrefixPatch
    public static void Prefix() {
        try {
            // FIX 3: Don't save snapshot during a rewind-triggered transition
            if (SnapshotManager.isRewinding) {
                PathRewindMod.logger.info("PathRewind: Skipping snapshot save during rewind");
                return;
            }
            PathRewindMod.logger.info("PathRewind: Player moving to next room, saving snapshot...");
            SnapshotManager.saveSnapshot();
        } catch (Exception e) {
            PathRewindMod.logger.error("PathRewind: Error saving snapshot", e);
        }
    }
}
