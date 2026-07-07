package pathrewind;

import org.junit.Test;

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
}
