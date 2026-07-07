# Path Rewind

Path Rewind is a Slay the Spire mod that lets players rewind a run to a previously visited map node.

## Features

- Click a visited map node to rewind to that point.
- Automatically saves snapshots when choosing a path.
- Works with all characters.
- Uses the existing map UI.

## Requirements

- Slay the Spire
- ModTheSpire
- BaseMod

## Build

Update the local dependency paths in `pom.xml` if your Steam library is installed somewhere else, then run:

```powershell
mvn package
```

The built mod jar is written to `target\PathRewind.jar`.
