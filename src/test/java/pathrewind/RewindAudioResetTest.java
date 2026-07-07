package pathrewind;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

public class RewindAudioResetTest {
    public static void main(String[] args) throws Exception {
        Class<?> resetClass = Class.forName("pathrewind.RewindAudioReset");

        Method keysMethod = resetClass.getDeclaredMethod("getLoopingSoundKeysToStop");
        keysMethod.setAccessible(true);
        Object keys = keysMethod.invoke(null);
        if (!(keys instanceof Collection)) {
            throw new AssertionError("getLoopingSoundKeysToStop must return a Collection");
        }
        if (!((Collection<?>) keys).contains("REST_FIRE_WET")) {
            throw new AssertionError("Rewind audio reset must stop the rest-room fire loop");
        }

        String source = new String(Files.readAllBytes(
                Paths.get("src/main/java/pathrewind/SnapshotManager.java")), StandardCharsets.UTF_8);
        int resetCall = source.indexOf("RewindAudioReset.prepareForRewindReload()");
        int loadFlag = source.indexOf("CardCrawlGame.loadingSave = true");
        if (resetCall < 0) {
            throw new AssertionError("SnapshotManager must reset audio before triggering save reload");
        }
        if (loadFlag < 0 || resetCall > loadFlag) {
            throw new AssertionError("Audio reset must happen before CardCrawlGame.loadingSave is set");
        }
    }
}
