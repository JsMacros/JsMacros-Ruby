package xyz.wagyourtail.jsmacrosjruby.ruby.library.impl;

import org.jruby.RubyMethod;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.ContextContainer;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerLanguageLibrary;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyLanguageDefinition;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyScriptContext;

import java.util.concurrent.Semaphore;

@Library(value = "JavaWrapper", languages = RubyLanguageDefinition.class)
public class FWrapper extends PerLanguageLibrary implements IFWrapper<RubyMethod> {
    
    public FWrapper(Class<? extends BaseLanguage<ScriptingContainer>> language) {
        super(language);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJava(RubyMethod c) {
        RubyScriptContext ctx = (RubyScriptContext) Core.instance.threadContext.get(Thread.currentThread());
        ctx.nonGCdMethodWrappers.incrementAndGet();
        return new RubyMethodWrapper<>(c, true, ctx);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJavaAsync(RubyMethod c) {
        RubyScriptContext ctx = (RubyScriptContext) Core.instance.threadContext.get(Thread.currentThread());
        ctx.nonGCdMethodWrappers.incrementAndGet();
        return new RubyMethodWrapper<>(c, false, ctx);
    }
    
    @Override
    public void stop() {
        RubyScriptContext ctx = (RubyScriptContext) Core.instance.threadContext.get(Thread.currentThread());
        ctx.closeContext();
    }



    private static class RubyMethodWrapper<T, U, R> extends MethodWrapper<T, U, R> {
        private final RubyMethod fn;
        private final boolean await;
        private final RubyScriptContext ctx;
        private final ScriptingContainer sc;

        RubyMethodWrapper(RubyMethod fn, boolean await, RubyScriptContext ctx) {
            this.fn = fn;
            this.await = await;
            this.ctx = ctx;
            this.sc = ctx.getContext().get();
        }

        private Object inner_accept(boolean await, Object... params) {

            if (await && Core.instance.threadContext.get(Thread.currentThread()) == ctx) {
                ThreadContext threadContext = sc.getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                return fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
            }

            Object[] retval = {null};
            Throwable[] error = {null};
            Semaphore lock = new Semaphore(0);

            Thread t = new Thread(() -> {
                synchronized (ctx) {
                    if (ctx.closed) throw new RuntimeException("Context Closed");
                    Core.instance.threadContext.put(Thread.currentThread(), ctx);
                }
                ThreadContext threadContext = sc.getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                try {
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                    retval[0] = fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
                } catch (Throwable ex) {
                    if (!await) {
                        Core.instance.profile.logError(ex);
                    }
                    error[0] = ex;
                } finally {
                    ContextContainer<?> cc = Core.instance.eventContexts.get(Thread.currentThread());
                    if (cc != null) cc.releaseLock();

                    lock.release();
                }
            });
            t.start();

            if (await) {
                try {
                    lock.acquire();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (error[0] != null) throw new RuntimeException(error[0]);
            }
            return retval[0];
        }

        @Override
        public void accept(T t) {
            inner_accept(await, t);
        }

        @Override
        public void accept(T t, U u) {
            inner_accept(await, t, u);
        }

        @Override
        public R apply(T t) {
            return (R) inner_accept(true, t);
        }

        @Override
        public R apply(T t, U u) {
            return (R) inner_accept(true, t, u);
        }

        @Override
        public boolean test(T t) {
            return (boolean) inner_accept(true, t);
        }

        @Override
        public boolean test(T t, U u) {
            return (boolean) inner_accept(true, t, u);
        }

        @Override
        public void run() {
            inner_accept(await);
        }

        @Override
        public int compare(T o1, T o2) {
            return (int) inner_accept(true, o1, o2);
        }

        @Override
        public R get() {
            return (R) inner_accept(true);
        }


        @Override
        protected void finalize() throws Throwable {
            int val = ctx.nonGCdMethodWrappers.decrementAndGet();
            if (val == 0) ctx.closeContext();
        }

    }
    
}
