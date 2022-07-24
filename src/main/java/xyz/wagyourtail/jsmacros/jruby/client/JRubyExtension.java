package xyz.wagyourtail.jsmacros.jruby.client;

import com.google.common.collect.Sets;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.jruby.language.impl.JRubyLanguageDefinition;
import xyz.wagyourtail.jsmacros.jruby.library.impl.FWrapper;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class JRubyExtension implements Extension {

    JRubyLanguageDefinition languageDescription;

    @Override
    public void init() {
    
//        try {
//            JsMacros.core.config.addOptions("ruby", RubyConfig.class);
//        } catch (IllegalAccessException | InstantiationException e) {
//            throw new RuntimeException(e);
//        }

        Thread t = new Thread(() -> {
            ScriptingContainer instance = new ScriptingContainer();
            instance.runScriptlet("p \"Ruby Pre-Loaded\"");
        });
        t.start();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getLanguageImplName() {
        return "jruby";
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (file.getName().endsWith(".rb")) {
            if (file.getName().contains(getLanguageImplName())) {
                return ExtMatch.MATCH_WITH_NAME;
            } else {
                return ExtMatch.MATCH;
            }
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return "rb";
    }

    @Override
    public BaseLanguage<?, ?> getLanguage(Core<?, ?> core) {
        if (languageDescription == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(JRubyExtension.class.getClassLoader());
            languageDescription = new JRubyLanguageDefinition(this, core);
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return languageDescription;
    }

    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(FWrapper.class);
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

    @Override
    public boolean isGuestObject(Object o) {
        return o instanceof IRubyObject;
    }


}
