package xyz.wagyourtail.jsmacrosjruby.ruby.language.impl;

import org.jruby.RubyException;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.ruby.config.RubyConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class RubyLanguageDefinition extends BaseLanguage<ScriptingContainer> {
    public RubyLanguageDefinition(String extension, Core runner) {
        super(extension, runner);
    }

    protected void runInstance(EventContainer<ScriptingContainer> ctx, Executor e, @Nullable Path cwd) throws Exception {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        ctx.getCtx().setContext(instance);

        if (cwd != null)
            instance.setCurrentDirectory(cwd.toString());

        retrieveLibs(ctx.getCtx()).forEach(instance::put);

        e.accept(instance);
    }

    @Override
    protected void exec(EventContainer<ScriptingContainer> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        runInstance(ctx, instance -> {
            instance.put("event", event);
            instance.put("file", ctx.getCtx().getFile());
            instance.put("context", ctx);

            instance.runScriptlet(new FileReader(ctx.getCtx().getFile()), ctx.getCtx().getFile().getAbsolutePath());
        }, ctx.getCtx().getFile().getParentFile().toPath());

    }

    @Override
    protected void exec(EventContainer<ScriptingContainer> ctx, String script, BaseEvent event) throws Exception {
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
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (ex instanceof EvalFailedException) {
            Throwable cause = ex.getCause();
            if (cause instanceof RaiseException) {
                RubyException e = ((RaiseException) ex.getCause()).getException();
                Iterator<StackTraceElement> elements = Arrays.stream(e.getBacktraceElements()).map(RubyStackTraceElement::asStackTraceElement).iterator();
                return new BaseWrappedException<>(e, e.getMessageAsJavaString(), null, elements.hasNext() ? traceStack(elements.next(), elements) : null);
            } else {
                Iterator<StackTraceElement> elements = Arrays.stream(cause.getStackTrace()).iterator();
                return new BaseWrappedException<>(cause, cause.getClass().getName() + ": " + cause.getMessage(), null, elements.hasNext() ? traceStack(elements.next(), elements) : null);
            }
        }
        return null;
    }

    @Override
    public BaseScriptContext<ScriptingContainer> createContext(BaseEvent event, File path) {
        return new RubyScriptContext(event, path);
    }

    private BaseWrappedException<StackTraceElement> traceStack(StackTraceElement current, Iterator<StackTraceElement> elements) {
        if (current.getClassName().equals("org.jruby.embed.internal.EmbedEvalUnitImpl")) return null;
        if (current.getClassName().startsWith("org.jruby")) return elements.hasNext() ? traceStack(elements.next(), elements) : null;
        BaseWrappedException.SourceLocation loc;
        if (current.getClassName().equals("RUBY")) {
            loc = new BaseWrappedException.GuestLocation(new File(current.getFileName()), -1, -1, current.getLineNumber(), -1);
        } else {
            loc = new BaseWrappedException.HostLocation(current.getClassName() + " " + current.getLineNumber());
        }
        return new BaseWrappedException<>(current, current.getMethodName(), loc, elements.hasNext() ? traceStack(elements.next(), elements) : null);
    }

    private interface Executor {
        void accept(ScriptingContainer instance) throws Exception;
    }
}
