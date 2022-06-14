package xyz.wagyourtail.jsmacrosjruby.ruby.language.impl;

import org.jruby.embed.ScriptingContainer;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;

public class RubyScriptContext extends BaseScriptContext<ScriptingContainer> {
    public RubyScriptContext(BaseEvent event, File file) {
        super(event, file);
    }

}
