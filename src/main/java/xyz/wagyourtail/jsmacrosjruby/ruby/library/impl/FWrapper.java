package xyz.wagyourtail.jsmacrosjruby.ruby.library.impl;

import org.jruby.RubyMethod;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.ContextContainer;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyLanguageDefinition;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyScriptContext;

@Library(value = "JavaWrapper", languages = RubyLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<ScriptingContainer> implements IFWrapper<RubyMethod> {
    public RubyScriptContext ctx;
    
    public FWrapper(ContextContainer<ScriptingContainer> context, Class<? extends BaseLanguage<ScriptingContainer>> language) {
        super(context, language);
        ctx = (RubyScriptContext) context.getCtx();
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJava(RubyMethod c) {
        ScriptingContainer sc = ctx.getContext().get();
        return new MethodWrapper<A, B, R>() {
            private Object internalAccept(Object ...objects) {
                synchronized (ctx) {
                    if (ctx.closed) throw new RuntimeException("Context Closed");
                    Core.instance.threadContext.put(Thread.currentThread(), ctx);
                }
                try {
                    ThreadContext threadContext = ((ScriptingContainer) sc).getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, objects);
                    IRubyObject rubyReturn = c.call(threadContext, rubyObjects, threadContext.getFrameBlock());
                    if (rubyReturn == null) {
                        return null;
                    } else {
                        return rubyReturn.toJava(Object.class);
                    }
                } finally {
                    synchronized (ctx) {
                        Core.instance.threadContext.remove(Thread.currentThread());
                    }
                }
            }
            
            @Override
            public void accept(A a) {
                internalAccept(a);
            }
    
            @Override
            public void accept(A a, B b) {
                internalAccept(a, b);
            }
    
            @Override
            public R apply(A a) {
                return (R) internalAccept(a);
            }
    
            @Override
            public R apply(A a, B b) {
                return (R) internalAccept(a, b);
            }
    
            @Override
            public boolean test(A a) {
                return (boolean) internalAccept(a);
            }
    
            @Override
            public boolean test(A a, B b) {
                return (boolean) internalAccept(a, b);
            }
    
            @Override
            public void run() {
                internalAccept();
            }
    
            @Override
            public int compare(A o1, A o2) {
                return (int) internalAccept(o1, o2);
            }
    
            @Override
            public R get() {
                return (R) internalAccept();
            }
        };
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJavaAsync(RubyMethod c) {
        ScriptingContainer sc = ctx.getContext().get();
        return new MethodWrapper<A, B, R>() {
            private Object internalAccept(Object ...objects) {
                synchronized (ctx) {
                    if (ctx.closed) throw new RuntimeException("Context Closed");
                    Core.instance.threadContext.put(Thread.currentThread(), ctx);
                }
                try {
                    ThreadContext threadContext = ((ScriptingContainer) sc).getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, objects);
                    IRubyObject rubyReturn = c.call(threadContext, rubyObjects, threadContext.getFrameBlock());
                    if (rubyReturn == null) {
                        return null;
                    } else {
                        return rubyReturn.toJava(Object.class);
                    }
                } finally {
                    synchronized (ctx) {
                        Core.instance.threadContext.remove(Thread.currentThread());
                    }
                }
            }
            
            private void internalAsyncAccept(Object ...objects) {
                Thread t = new Thread(() -> {
                    synchronized (ctx) {
                        if (ctx.closed) throw new RuntimeException("Context Closed");
                        Core.instance.threadContext.put(Thread.currentThread(), ctx);
                    }
                    ThreadContext threadContext = ((ScriptingContainer) sc).getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, objects);
                    c.call(threadContext, rubyObjects, threadContext.getFrameBlock());
                });
                t.start();
            }
            
            @Override
            public void accept(A a) {
                internalAsyncAccept(a);
            }
    
            @Override
            public void accept(A a, B b) {
                internalAsyncAccept(a, b);
            }
    
            @Override
            public R apply(A a) {
                return (R) internalAccept(a);
            }
    
            @Override
            public R apply(A a, B b) {
                return (R) internalAccept(a, b);
            }
    
            @Override
            public boolean test(A a) {
                return (boolean) internalAccept(a);
            }
    
            @Override
            public boolean test(A a, B b) {
                return (boolean) internalAccept(a, b);
            }
    
            @Override
            public void run() {
                internalAsyncAccept();
            }
    
            @Override
            public int compare(A o1, A o2) {
                return (int) internalAccept(o1, o2);
            }
    
            @Override
            public R get() {
                return (R) internalAccept();
            }
        };
    }
    
    @Override
    public void stop() {
    
    }
    
}
