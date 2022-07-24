package xyz.wagyourtail.jsmacros.jruby.language.impl;

import org.jruby.RubyException;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.jruby.client.JRubyExtension;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

public class JRubyLanguageDefinition extends BaseLanguage<ScriptingContainer, JRubyScriptContext> {
    public JRubyLanguageDefinition(JRubyExtension extension, Core runner) {
        super(extension, runner);
    }

    protected void runInstance(EventContainer<JRubyScriptContext> ctx, Executor e, @Nullable Path cwd) throws Exception {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        ctx.getCtx().setContext(instance);

        if (cwd != null)
            instance.setCurrentDirectory(cwd.toString());

        retrieveLibs(ctx.getCtx()).forEach((k, v) -> {
            if (k.equals("Time")) k = "FTime";
            instance.put(k,v);

        });

        e.accept(instance);
    }

    @Override
    protected void exec(EventContainer<JRubyScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        runInstance(ctx, instance -> {
            instance.put("event", event);
            instance.put("file", ctx.getCtx().getFile());
            instance.put("context", ctx);

            instance.runScriptlet(new FileReader(ctx.getCtx().getFile()), ctx.getCtx().getFile().getAbsolutePath());
        }, ctx.getCtx().getFile().getParentFile().toPath());

    }

    @Override
    protected void exec(EventContainer<JRubyScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        runInstance(ctx, instance -> {
            instance.put("event", event);
            instance.put("file", ctx.getCtx().getFile());
            instance.put("context", ctx);

            if (ctx.getCtx().getFile() != null) {
                instance.runScriptlet(new StringReader(script), ctx.getCtx().getFile().getAbsolutePath());
            } else {
                instance.runScriptlet(script);
            }
        }, ctx.getCtx().getFile() != null ? ctx.getCtx().getFile().getParentFile().toPath() : null);
    }

    @Override
    public JRubyScriptContext createContext(BaseEvent event, File path) {
        return new JRubyScriptContext(event, path);
    }

    private interface Executor {
        void accept(ScriptingContainer instance) throws Exception;
    }
}
