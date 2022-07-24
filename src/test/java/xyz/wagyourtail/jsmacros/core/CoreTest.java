package xyz.wagyourtail.jsmacros.core;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.stubs.CoreInstanceCreator;
import xyz.wagyourtail.jsmacros.stubs.EventRegistryStub;
import xyz.wagyourtail.jsmacros.stubs.ProfileStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoreTest {
    @Language("rb")
    private final String TEST_SCRIPT = """
        $event = event
        def log(rp)
            $event.putString(rp, "Hello World!")
        end
        
        log("rp1")
        JavaWrapper.methodToJava(method(:log)).accept("rp2")
        JavaWrapper.methodToJavaAsync(method(:log)).accept("rp3")
        """;
    
    @Test
    public void test() throws InterruptedException {
        Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();
        EventCustom event = new EventCustom("test");
        EventContainer<?> ev = core.exec("rb", TEST_SCRIPT, null, event, null, null);
        ev.awaitLock(() -> {});
        Thread.sleep(100);
        assertEquals("{rp1=Hello World!, rp3=Hello World!, rp2=Hello World!}", event.getUnderlyingMap().toString());
    }

}
