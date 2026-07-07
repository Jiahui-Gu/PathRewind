package pathrewind.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import pathrewind.PathRewindMod;
import pathrewind.SnapshotManager;
import pathrewind.ui.RewindConfirmPopup;

@SpirePatch(
    clz = MapRoomNode.class,
    method = "update"
)
public class MapNodeClickPatch {

    @SpirePostfixPatch
    public static void Postfix(MapRoomNode __instance) {
        try {
            if (RewindConfirmPopup.isVisible()) return;
            if (SnapshotManager.isRewinding) return;

            if (!__instance.taken) return;

            MapRoomNode currNode = AbstractDungeon.getCurrMapNode();
            if (currNode != null && __instance.equals(currNode)) return;

            if (currNode != null && currNode.isConnectedTo(__instance)) return;

            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) return;

            if (__instance.hb == null || !__instance.hb.hovered) return;

            if (!AbstractDungeon.dungeonMapScreen.clicked) return;

            int act = AbstractDungeon.actNum;
            int snapshotFloor = SnapshotManager.getFloorForNode(act, __instance.y);

            if (snapshotFloor < 0 || !SnapshotManager.hasSnapshotForNode(snapshotFloor)) return;

            AbstractDungeon.dungeonMapScreen.clicked = false;
            AbstractDungeon.dungeonMapScreen.clickTimer = 0f;

            String roomDesc = SnapshotManager.getRoomDescForFloor(snapshotFloor);
            PathRewindMod.logger.info("PathRewind: Clicked taken node at y=" + __instance.y
                    + " act=" + act + ", showing confirm for floor " + snapshotFloor
                    + " (" + roomDesc + ")");

            RewindConfirmPopup.show(snapshotFloor, __instance.y, act, roomDesc);
        } catch (Exception e) {
            PathRewindMod.logger.error("PathRewind: Error in MapNodeClickPatch", e);
        }
    }
}
