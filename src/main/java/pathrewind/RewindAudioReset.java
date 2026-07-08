package pathrewind;

import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.SoundMaster;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.scenes.AbstractScene;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class RewindAudioReset {
    private static final Collection<String> LOOPING_SOUND_KEYS = Collections.unmodifiableList(Arrays.asList(
            "REST_FIRE_WET",
            "AMBIANCE_BOTTOM",
            "AMBIANCE_CITY",
            "AMBIANCE_BEYOND",
            "WIND"
    ));

    static Collection<String> getLoopingSoundKeysToStop() {
        return LOOPING_SOUND_KEYS;
    }

    public static void prepareForRewindReload() {
        prepareForRewindReload(getCurrentScene(), CardCrawlGame.sound, CardCrawlGame.music);
    }

    static void prepareForRewindReload(AbstractScene scene, SoundMaster sound, MusicMaster music) {
        fadeOutCurrentSceneAmbiance(scene);
        stopLoopingSounds(sound);
        resetRestRoomFireSoundId();
        resetMusic(music);
    }

    private static AbstractScene getCurrentScene() {
        try {
            return AbstractDungeon.scene;
        } catch (RuntimeException e) {
            PathRewindMod.logger.warn("PathRewind: Failed to read current scene before rewind reload; continuing", e);
        } catch (LinkageError e) {
            PathRewindMod.logger.warn("PathRewind: Failed to read current scene before rewind reload; continuing", e);
        }
        return null;
    }

    static void fadeOutCurrentSceneAmbiance(AbstractScene scene) {
        if (scene != null) {
            runBestEffort("fade out current scene ambience", new AudioResetStep() {
                @Override
                public void run() {
                    scene.fadeOutAmbiance();
                }
            });
        }
    }

    private static void stopLoopingSounds(SoundMaster sound) {
        if (sound == null) {
            return;
        }

        for (String key : LOOPING_SOUND_KEYS) {
            runBestEffort("stop looping sound " + key, new AudioResetStep() {
                @Override
                public void run() {
                    sound.stop(key);
                }
            });
        }
    }

    private static void resetRestRoomFireSoundId() {
        runBestEffort("reset rest-room fire sound state", new AudioResetStep() {
            @Override
            public void run() {
                RestRoom.lastFireSoundId = 0L;
            }
        });
    }

    private static void resetMusic(final MusicMaster music) {
        if (music == null) {
            return;
        }

        runBestEffort("silence temp BGM", new AudioResetStep() {
            @Override
            public void run() {
                music.silenceTempBgmInstantly();
            }
        });
        runBestEffort("fade music", new AudioResetStep() {
            @Override
            public void run() {
                music.fadeAll();
            }
        });
    }

    private static void runBestEffort(String description, AudioResetStep step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            PathRewindMod.logger.warn("PathRewind: Failed to " + description
                    + " before rewind reload; continuing", e);
        } catch (LinkageError e) {
            PathRewindMod.logger.warn("PathRewind: Failed to " + description
                    + " before rewind reload; continuing", e);
        }
    }

    private interface AudioResetStep {
        void run();
    }
}
