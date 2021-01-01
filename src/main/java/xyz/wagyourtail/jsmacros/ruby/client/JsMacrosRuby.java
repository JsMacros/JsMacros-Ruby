package xyz.wagyourtail.jsmacros.ruby.client;

import net.fabricmc.api.ClientModInitializer;
import org.jruby.embed.ScriptingContainer;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyLanguageDefinition;
import xyz.wagyourtail.jsmacrosjruby.ruby.library.impl.FConsumer;

public class JsMacrosRuby implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        JsMacros.core.addLanguage(new RubyLanguageDefinition(".rb", JsMacros.core));
        JsMacros.core.sortLanguages();
        JsMacros.core.libraryRegistry.addLibrary(FConsumer.class);
        
        
        Thread t = new Thread(() -> {
            ScriptingContainer instance = new ScriptingContainer();
            instance.runScriptlet("p \"Ruby Pre-Loaded\"");
        });
        t.start();
    }
    
    
}
