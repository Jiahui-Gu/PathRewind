package pathrewind;

import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.SoundMaster;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.rooms.RestRoom;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class RewindAudioReset {
    private static final Collection<String> LOOPING_SOUND_KEYS = Collections.unmodifiableList(Arrays.asList(
            "REST_FIRE_WET"
    ));

    static Collection<String> getLoopingSoundKeysToStop() {
        return LOOPING_SOUND_KEYS;
    }

    public static void prepareForRewindReload() {
        stopLoopingSounds(CardCrawlGame.sound);
        resetRestRoomFireSoundId();
        resetMusic(CardCrawlGame.music);
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
