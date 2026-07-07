package pathrewind;

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
        RestRoom.lastFireSoundId = 0L;

        if (CardCrawlGame.music != null) {
            CardCrawlGame.music.silenceTempBgmInstantly();
            CardCrawlGame.music.fadeAll();
        }
    }

    private static void stopLoopingSounds(SoundMaster sound) {
        if (sound == null) {
            return;
        }

        for (String key : LOOPING_SOUND_KEYS) {
            sound.stop(key);
        }
    }
}
