package pathrewind;

import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.SoundMaster;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

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
}
