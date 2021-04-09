package xyz.wagyourtail.jsmacrosjruby.ruby.language.impl;

import org.jruby.embed.ScriptingContainer;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;

public class RubyScriptContext extends ScriptContext<ScriptingContainer> {
    public boolean closed = false;
    
    @Override
    public boolean isContextClosed() {
        return super.isContextClosed() || closed;
    }
    
    @Override
    public void closeContext() {
        if (context != null) {
            synchronized (this) {
                closed = true;
                Core.instance.threadContext.entrySet().stream().filter(e -> e.getValue() == this).forEach(e -> e.getKey().interrupt());
            }
        }
    }
    
}
