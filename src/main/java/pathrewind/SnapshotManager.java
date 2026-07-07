package pathrewind;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotManager {

    private static final Logger logger = LogManager.getLogger(SnapshotManager.class.getName());
    private static final String REWIND_PREFIX = ".rewind_floor_";
    private static final String INDEX_SUFFIX = ".rewind_index";

    private static Map<Integer, String> snapshotPaths = new HashMap<Integer, String>();
    private static Map<Integer, Integer> floorToAct = new HashMap<Integer, Integer>();
    private static Map<Integer, Integer> floorToNodeY = new HashMap<Integer, Integer>();
    private static Map<Integer, String> floorToRoomDesc = new HashMap<Integer, String>();

    // FIX 3: Flag to prevent SaveSnapshotPatch from firing during a rewind reload
    public static boolean isRewinding = false;

    private static String getSavePath() {
        AbstractPlayer.PlayerClass pc = null;
        if (AbstractDungeon.player != null) {
            pc = AbstractDungeon.player.chosenClass;
        }
        if (pc == null) {
            pc = CardCrawlGame.chosenCharacter;
        }
        if (pc == null) {
            return null;
        }
        return SaveAndContinue.getPlayerSavePath(pc);
    }

    private static String getIndexPath() {
        String savePath = getSavePath();
        if (savePath == null) return null;
        return Paths.get(savePath + INDEX_SUFFIX).toAbsolutePath().toString();
    }

    private static void saveIndex() {
        try {
            String indexPath = getIndexPath();
            if (indexPath == null) return;

            ArrayList<String> lines = new ArrayList<String>();
            for (Map.Entry<Integer, String> entry : snapshotPaths.entrySet()) {
                int floor = entry.getKey();
                String backupPath = entry.getValue();
                int act = floorToAct.containsKey(floor) ? floorToAct.get(floor) : 0;
                int nodeY = floorToNodeY.containsKey(floor) ? floorToNodeY.get(floor) : -1;
                String roomDesc = floorToRoomDesc.containsKey(floor) ? floorToRoomDesc.get(floor) : "Unknown";
                lines.add(floor + "|" + act + "|" + nodeY + "|" + roomDesc + "|" + backupPath);
            }
            Files.write(Paths.get(indexPath), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("PathRewind: Index saved with " + lines.size() + " entries");
        } catch (Exception e) {
            logger.error("PathRewind: Failed to save index", e);
        }
    }

    public static void restoreFromDisk() {
        snapshotPaths.clear();
        floorToAct.clear();
        floorToNodeY.clear();
        floorToRoomDesc.clear();

        try {
            String indexPath = getIndexPath();
            if (indexPath == null) return;

            Path idxFile = Paths.get(indexPath);
            if (!Files.exists(idxFile)) {
                logger.info("PathRewind: No index file found, starting fresh");
                return;
            }

            List<String> lines = Files.readAllLines(idxFile, StandardCharsets.UTF_8);
            int restored = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", 5);
                if (parts.length < 5) continue;

                try {
                    int floor = Integer.parseInt(parts[0]);
                    int act = Integer.parseInt(parts[1]);
                    int nodeY = Integer.parseInt(parts[2]);
                    String roomDesc = parts[3];
                    String backupPath = parts[4];

                    if (!Files.exists(Paths.get(backupPath))) {
                        logger.info("PathRewind: Backup missing for floor " + floor + ", skipping");
                        continue;
                    }

                    snapshotPaths.put(floor, backupPath);
                    floorToAct.put(floor, act);
                    floorToNodeY.put(floor, nodeY);
                    floorToRoomDesc.put(floor, roomDesc);
                    restored++;
                } catch (NumberFormatException e) {
                    logger.warn("PathRewind: Skipping malformed index line: " + line);
                }
            }
            logger.info("PathRewind: Restored " + restored + " snapshots from index");
        } catch (Exception e) {
            logger.error("PathRewind: Failed to restore index", e);
        }
    }

    public static void saveSnapshot() {
        try {
            int floor = AbstractDungeon.floorNum;
            int act = AbstractDungeon.actNum;
            int nodeY = -1;
            String roomDesc = "Unknown";
            if (AbstractDungeon.currMapNode != null) {
                nodeY = AbstractDungeon.currMapNode.y;
                if (AbstractDungeon.currMapNode.getRoom() != null) {
                    roomDesc = AbstractDungeon.currMapNode.getRoom().getClass().getSimpleName();
                }
            }

            String savePath = getSavePath();
            if (savePath == null) {
                logger.warn("PathRewind: Could not determine save path");
                return;
            }

            Path source = Paths.get(savePath).toAbsolutePath();
            Path backup = Paths.get(savePath + REWIND_PREFIX + floor).toAbsolutePath();

            if (Files.exists(source)) {
                Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
                snapshotPaths.put(floor, backup.toString());
                floorToAct.put(floor, act);
                floorToNodeY.put(floor, nodeY);
                floorToRoomDesc.put(floor, roomDesc);
                logger.info("PathRewind: Snapshot saved for floor " + floor
                        + " (act=" + act + ", nodeY=" + nodeY + ", room=" + roomDesc + ")"
                        + " -> " + backup.toString());
                saveIndex();
            } else {
                logger.warn("PathRewind: Save file does not exist at " + source);
            }
        } catch (Exception e) {
            logger.error("PathRewind: Failed to save snapshot", e);
        }
    }

    public static int getFloorForNode(int act, int nodeY) {
        for (Map.Entry<Integer, Integer> entry : floorToAct.entrySet()) {
            int floor = entry.getKey();
            int snapAct = entry.getValue();
            Integer snapNodeY = floorToNodeY.get(floor);
            if (snapAct == act && snapNodeY != null && snapNodeY.intValue() == nodeY) {
                return floor;
            }
        }
        return -1;
    }

    public static String getRoomDescForFloor(int floor) {
        String desc = floorToRoomDesc.get(floor);
        return desc != null ? desc : "Unknown";
    }

    public static boolean hasSnapshotForNode(int floor) {
        if (floor < 0) return false;
        String path = snapshotPaths.get(floor);
        if (path == null) return false;
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if loadSnapshotForNode completed successfully and a game
     * reload was triggered. Returns false if it failed for any reason.
     */
    public static boolean loadSnapshotForNode(int floor) {
        try {
            String backupPath = snapshotPaths.get(floor);
            if (backupPath == null) {
                logger.warn("PathRewind: No snapshot for floor=" + floor);
                return false;
            }

            String savePath = getSavePath();
            if (savePath == null) {
                logger.warn("PathRewind: Could not determine save path for loading");
                return false;
            }

            Path source = Paths.get(backupPath);
            Path target = Paths.get(savePath).toAbsolutePath();

            if (!Files.exists(source)) {
                logger.warn("PathRewind: Backup file missing at " + backupPath);
                snapshotPaths.remove(floor);
                return false;
            }

            // Set rewinding flag BEFORE modifying anything
            isRewinding = true;

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("PathRewind: Backup restored from floor " + floor);

            // Prune snapshots for floors AFTER the target (keep the target itself)
            ArrayList<Integer> toRemove = new ArrayList<Integer>();
            for (Map.Entry<Integer, String> entry : snapshotPaths.entrySet()) {
                if (entry.getKey() > floor) {
                    try {
                        Files.deleteIfExists(Paths.get(entry.getValue()));
                    } catch (IOException e) {
                        logger.error("PathRewind: Failed to delete snapshot floor "
                                + entry.getKey(), e);
                    }
                    toRemove.add(entry.getKey());
                }
            }
            for (Integer key : toRemove) {
                snapshotPaths.remove(key);
                floorToAct.remove(key);
                floorToNodeY.remove(key);
                floorToRoomDesc.remove(key);
            }
            logger.info("PathRewind: Cleared snapshots for floor > " + floor
                    + ", keeping " + snapshotPaths.size() + " snapshots");

            saveIndex();

            // Null-safe access to player class
            if (AbstractDungeon.player == null) {
                logger.error("PathRewind: AbstractDungeon.player is null, cannot reload");
                isRewinding = false;
                return false;
            }
            AbstractPlayer.PlayerClass pc = AbstractDungeon.player.chosenClass;
            CardCrawlGame.chosenCharacter = pc;
            RewindAudioReset.prepareForRewindReload();
            CardCrawlGame.loadingSave = true;
            CardCrawlGame.mode = CardCrawlGame.GameMode.CHAR_SELECT;

            logger.info("PathRewind: Reload triggered for character " + pc);
            return true;
        } catch (Exception e) {
            logger.error("PathRewind: Failed to load snapshot", e);
            isRewinding = false;
            return false;
        }
    }

    public static void clearAllSnapshots() {
        for (Map.Entry<Integer, String> entry : snapshotPaths.entrySet()) {
            try {
                Files.deleteIfExists(Paths.get(entry.getValue()));
            } catch (IOException e) {
                logger.error("PathRewind: Failed to delete snapshot for floor "
                        + entry.getKey(), e);
            }
        }
        snapshotPaths.clear();
        floorToAct.clear();
        floorToNodeY.clear();
        floorToRoomDesc.clear();

        try {
            String indexPath = getIndexPath();
            if (indexPath != null) {
                Files.deleteIfExists(Paths.get(indexPath));
            }
        } catch (Exception e) {
            logger.error("PathRewind: Failed to delete index file", e);
        }

        cleanOrphanedFiles();
        logger.info("PathRewind: All snapshots cleared");
    }

    private static void cleanOrphanedFiles() {
        try {
            String savePath = getSavePath();
            if (savePath == null) return;

            Path saveFile = Paths.get(savePath).toAbsolutePath();
            Path saveDir = saveFile.getParent();
            String baseName = saveFile.getFileName().toString();

            if (saveDir == null || !Files.isDirectory(saveDir)) return;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir,
                    baseName + REWIND_PREFIX + "*")) {
                for (Path file : stream) {
                    Files.deleteIfExists(file);
                    logger.info("PathRewind: Cleaned orphaned file " + file.getFileName());
                }
            }
        } catch (Exception e) {
            logger.error("PathRewind: Error cleaning orphaned files", e);
        }
    }

    /**
     * Print all snapshots to dev console (for debugging).
     */
    public static void listSnapshots() {
        if (snapshotPaths.isEmpty()) {
            basemod.DevConsole.log("No snapshots saved.");
            return;
        }
        basemod.DevConsole.log("=== Saved Snapshots ===");
        for (java.util.Map.Entry<Integer, String> entry : snapshotPaths.entrySet()) {
            int floor = entry.getKey();
            int act = floorToAct.containsKey(floor) ? floorToAct.get(floor) : -1;
            int nodeY = floorToNodeY.containsKey(floor) ? floorToNodeY.get(floor) : -1;
            String room = floorToRoomDesc.containsKey(floor) ? floorToRoomDesc.get(floor) : "?";
            basemod.DevConsole.log("  Floor " + floor + " | Act " + act + " | Row " + nodeY + " | " + room);
        }
        basemod.DevConsole.log("Total: " + snapshotPaths.size() + " snapshots");
    }

    /**
     * Print mod status to dev console.
     */
    public static void printStatus() {
        basemod.DevConsole.log("=== PathRewind Status ===");
        basemod.DevConsole.log("  isRewinding: " + isRewinding);
        basemod.DevConsole.log("  Snapshots in memory: " + snapshotPaths.size());
        basemod.DevConsole.log("  Current floor: " + com.megacrit.cardcrawl.dungeons.AbstractDungeon.floorNum);
        basemod.DevConsole.log("  Current act: " + com.megacrit.cardcrawl.dungeons.AbstractDungeon.actNum);
        String indexPath = getIndexPath();
        if (indexPath != null) {
            basemod.DevConsole.log("  Index file: " + indexPath);
            basemod.DevConsole.log("  Index exists: " + java.nio.file.Files.exists(java.nio.file.Paths.get(indexPath)));
        } else {
            basemod.DevConsole.log("  Index file: N/A (no save path)");
        }
    }

    /**
     * Get list of available floor numbers as strings (for console autocomplete).
     */
    public static java.util.ArrayList<String> getAvailableFloors() {
        java.util.ArrayList<String> floors = new java.util.ArrayList<String>();
        for (Integer floor : snapshotPaths.keySet()) {
            floors.add(String.valueOf(floor));
        }
        java.util.Collections.sort(floors);
        return floors;
    }
}
