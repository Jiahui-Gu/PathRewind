package pathrewind.commands;

import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import pathrewind.SnapshotManager;

import java.util.ArrayList;

public class RewindCommand extends ConsoleCommand {

    public RewindCommand() {
        requiresPlayer = true;
        minExtraTokens = 1;
        maxExtraTokens = 2;
    }

    @Override
    protected void execute(String[] tokens, int depth) {
        if (tokens.length < depth + 1) {
            DevConsole.log("Usage: rewind [list|to|save|clear|status]");
            return;
        }

        String sub = tokens[depth];

        if (sub.equals("list")) {
            SnapshotManager.listSnapshots();

        } else if (sub.equals("to")) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: rewind to <floor_number>");
                return;
            }
            try {
                int floor = Integer.parseInt(tokens[depth + 1]);
                if (!SnapshotManager.hasSnapshotForNode(floor)) {
                    DevConsole.log("No snapshot for floor " + floor);
                    return;
                }
                DevConsole.log("Rewinding to floor " + floor + "...");
                boolean success = SnapshotManager.loadSnapshotForNode(floor);
                if (!success) {
                    DevConsole.log("Rewind failed!");
                }
            } catch (NumberFormatException e) {
                DevConsole.log("Invalid floor number: " + tokens[depth + 1]);
            }

        } else if (sub.equals("save")) {
            SnapshotManager.saveSnapshot();
            DevConsole.log("Snapshot saved for floor " + AbstractDungeon.floorNum);

        } else if (sub.equals("clear")) {
            SnapshotManager.clearAllSnapshots();
            DevConsole.log("All snapshots cleared");

        } else if (sub.equals("status")) {
            SnapshotManager.printStatus();

        } else {
            DevConsole.log("Unknown subcommand: " + sub);
            DevConsole.log("Usage: rewind [list|to|save|clear|status]");
        }
    }

    @Override
    protected ArrayList<String> extraOptions(String[] tokens, int depth) {
        ArrayList<String> options = new ArrayList<String>();
        if (tokens.length == depth + 1) {
            options.add("list");
            options.add("to");
            options.add("save");
            options.add("clear");
            options.add("status");
        } else if (tokens.length == depth + 2 && tokens[depth].equals("to")) {
            options.addAll(SnapshotManager.getAvailableFloors());
        }
        return options;
    }
}
