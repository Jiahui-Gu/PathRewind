package pathrewind;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.SoundMaster;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RewindAudioResetTest {
    @Test
    public void stopsRestRoomFireLoopBeforeRewindReload() throws Exception {
        Class<?> resetClass = Class.forName("pathrewind.RewindAudioReset");

        Method keysMethod = resetClass.getDeclaredMethod("getLoopingSoundKeysToStop");
        keysMethod.setAccessible(true);
        Object keys = keysMethod.invoke(null);
        assertTrue("getLoopingSoundKeysToStop must return a Collection", keys instanceof Collection);
        assertTrue("Rewind audio reset must stop the rest-room fire loop",
                ((Collection<?>) keys).contains("REST_FIRE_WET"));
    }

    @Test
    public void stopsDungeonAmbienceLoopsBeforeRewindReload() throws Exception {
        Class<?> resetClass = Class.forName("pathrewind.RewindAudioReset");

        Method keysMethod = resetClass.getDeclaredMethod("getLoopingSoundKeysToStop");
        keysMethod.setAccessible(true);
        Object keys = keysMethod.invoke(null);

        assertTrue("Rewind audio reset must stop Exordium ambience loops",
                ((Collection<?>) keys).contains("AMBIANCE_BOTTOM"));
        assertTrue("Rewind audio reset must stop City ambience loops",
                ((Collection<?>) keys).contains("AMBIANCE_CITY"));
        assertTrue("Rewind audio reset must stop Beyond ambience loops",
                ((Collection<?>) keys).contains("AMBIANCE_BEYOND"));
        assertTrue("Rewind audio reset must stop the map wind loop",
                ((Collection<?>) keys).contains("WIND"));
    }

    @Test
    public void fadesOutProvidedSceneAmbienceBeforeRewindReload() throws Exception {
        CountingScene scene = uninitialized(CountingScene.class);

        RewindAudioReset.fadeOutCurrentSceneAmbiance(scene);

        assertEquals("Rewind audio reset must fade out current scene ambience",
                1, scene.fadeOutAmbianceCalls);
    }

    @Test
    public void injectedRewindReloadAudioResetMatchesGameCleanupPath() throws Exception {
        List<String> cleanupEvents = new ArrayList<String>();
        CountingScene scene = uninitialized(CountingScene.class);
        RecordingSoundMaster sound = uninitialized(RecordingSoundMaster.class);
        RecordingMusicMaster music = uninitialized(RecordingMusicMaster.class);
        scene.cleanupEvents = cleanupEvents;
        sound.cleanupEvents = cleanupEvents;
        music.cleanupEvents = cleanupEvents;

        RewindAudioReset.prepareForRewindReload(scene, sound, music);

        assertEquals("Rewind audio reset should clean scene ambience, looping sounds, and music",
                Arrays.asList(
                        "scene:fade-out-ambiance",
                        "sound:REST_FIRE_WET",
                        "sound:AMBIANCE_BOTTOM",
                        "sound:AMBIANCE_CITY",
                        "sound:AMBIANCE_BEYOND",
                        "sound:WIND",
                        "music:silence-temp-bgm",
                        "music:fade-all"),
                cleanupEvents);
    }

    @Test
    public void resetsAudioBeforeTriggeringSaveReload() throws Exception {
        String source = new String(Files.readAllBytes(
                Paths.get("src/main/java/pathrewind/SnapshotManager.java")), StandardCharsets.UTF_8);
        int resetCall = source.indexOf("RewindAudioReset.prepareForRewindReload()");
        int loadFlag = source.indexOf("CardCrawlGame.loadingSave = true");
        assertTrue("SnapshotManager must reset audio before triggering save reload", resetCall >= 0);
        assertTrue("Audio reset must happen before CardCrawlGame.loadingSave is set",
                loadFlag >= 0 && resetCall < loadFlag);
    }

    @Test
    public void audioResetEntryPointDoesNotRequireFullyInitializedGameRuntime() {
        RewindAudioReset.prepareForRewindReload();
    }

    @Test
    public void loopingSoundFailuresDoNotEscapeAudioReset() throws Exception {
        Method stopLoopingSounds = RewindAudioReset.class.getDeclaredMethod("stopLoopingSounds", SoundMaster.class);
        stopLoopingSounds.setAccessible(true);

        stopLoopingSounds.invoke(null, uninitialized(ThrowingSoundMaster.class));
    }

    @Test
    public void tempMusicFailuresDoNotEscapeAudioReset() throws Exception {
        Method resetMusic = RewindAudioReset.class.getDeclaredMethod("resetMusic", MusicMaster.class);
        resetMusic.setAccessible(true);

        resetMusic.invoke(null, uninitialized(ThrowingMusicMaster.class));
    }

    private static <T> T uninitialized(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static class ThrowingSoundMaster extends SoundMaster {
        @Override
        public void stop(String key) {
            throw new RuntimeException("simulated sound failure");
        }
    }

    private static class ThrowingMusicMaster extends MusicMaster {
        @Override
        public void silenceTempBgmInstantly() {
            throw new RuntimeException("simulated temp music failure");
        }

        @Override
        public void fadeAll() {
            throw new RuntimeException("simulated music failure");
        }
    }

    private static class RecordingSoundMaster extends SoundMaster {
        private List<String> cleanupEvents;

        @Override
        public void stop(String key) {
            cleanupEvents.add("sound:" + key);
        }
    }

    private static class RecordingMusicMaster extends MusicMaster {
        private List<String> cleanupEvents;

        @Override
        public void silenceTempBgmInstantly() {
            cleanupEvents.add("music:silence-temp-bgm");
        }

        @Override
        public void fadeAll() {
            cleanupEvents.add("music:fade-all");
        }
    }

    private static class CountingScene extends AbstractScene {
        private int fadeOutAmbianceCalls;
        private List<String> cleanupEvents;

        private CountingScene() {
            super("unused");
        }

        @Override
        public void fadeOutAmbiance() {
            fadeOutAmbianceCalls++;
            if (cleanupEvents != null) {
                cleanupEvents.add("scene:fade-out-ambiance");
            }
        }

        @Override
        public void renderCombatRoomBg(SpriteBatch sb) {
        }

        @Override
        public void renderCombatRoomFg(SpriteBatch sb) {
        }

        @Override
        public void renderCampfireRoom(SpriteBatch sb) {
        }

        @Override
        public void randomizeScene() {
        }
    }
}
