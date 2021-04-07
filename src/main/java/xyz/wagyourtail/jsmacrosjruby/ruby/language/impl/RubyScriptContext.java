package xyz.wagyourtail.jsmacrosjruby.ruby.language.impl;

import org.jruby.embed.ScriptingContainer;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;
import xyz.wagyourtail.jsmacros.ruby.config.RubyConfig;

public class RubyScriptContext extends ScriptContext<ScriptingContainer> {
    public boolean closed = false;
    
    @Override
    public boolean isContextClosed() {
        return super.isContextClosed() || closed;
    }
    
    @Override
    public void closeContext() {
        if (context != null) {
            ScriptingContainer ctx = context.get();
            if (ctx != RubyLanguageDefinition.globalInstance) {
                if (ctx != null) {
                    ctx.terminate();
                }
                closed = true;
            } else {
                synchronized (this) {
                    Core.instance.threadContext.entrySet().stream().filter(e -> e.getValue() == this).forEach(e -> e.getKey().interrupt());
                }
            }
        }
    }
    
}
