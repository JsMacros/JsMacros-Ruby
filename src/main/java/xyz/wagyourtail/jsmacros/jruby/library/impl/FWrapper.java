package xyz.wagyourtail.jsmacros.jruby.library.impl;

import org.jruby.RubyMethod;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.jruby.language.impl.JRubyLanguageDefinition;
import xyz.wagyourtail.jsmacros.jruby.language.impl.JRubyScriptContext;

import java.util.concurrent.Semaphore;

@Library(value = "JavaWrapper", languages = JRubyLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<ScriptingContainer, JRubyScriptContext> implements IFWrapper<RubyMethod> {
    
    public FWrapper(JRubyScriptContext context, Class language) {
        super(context, language);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(RubyMethod c) {
        return new RubyMethodWrapper<>(c, true, ctx);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(RubyMethod c) {
        return new RubyMethodWrapper<>(c, false, ctx);
    }
    
    @Override
    public void stop() {
        ctx.closeContext();
    }



    private static class RubyMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, JRubyScriptContext> {
        private final RubyMethod fn;
        private final boolean await;

        RubyMethodWrapper(RubyMethod fn, boolean await, JRubyScriptContext ctx) {
            super(ctx);
            this.fn = fn;
            this.await = await;
        }

        private void inner_accept(boolean await, Object... params) {

            if (await) {
                inner_apply(params);
                return;
            }

            Semaphore lock = new Semaphore(0);
            boolean joinedThread = Core.getInstance().profile.checkJoinedThreadStack();

            Thread t = new Thread(() -> {
                ctx.bindThread(Thread.currentThread());

                try {
                    ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                    fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
                } catch (Throwable ex) {
                    Core.getInstance().profile.logError(ex);
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                    Core.getInstance().profile.joinedThreadStack.remove(Thread.currentThread());

                    ctx.releaseBoundEventIfPresent(Thread.currentThread());

                    lock.release();
                }
            });
            t.start();
        }

        private Object inner_apply(Object... params) {
            if (ctx.getBoundThreads().contains(Thread.currentThread())) {
                ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                return fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
            }

            try {
                ctx.bindThread(Thread.currentThread());
                if (Core.getInstance().profile.checkJoinedThreadStack()) {
                    Core.getInstance().profile.joinedThreadStack.add(Thread.currentThread());
                }
                ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                return fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            } finally {
                ctx.releaseBoundEventIfPresent(Thread.currentThread());
                ctx.unbindThread(Thread.currentThread());
                Core.getInstance().profile.joinedThreadStack.remove(Thread.currentThread());
            }
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
            return (R) inner_apply(true, t);
        }

        @Override
        public R apply(T t, U u) {
            return (R) inner_apply(true, t, u);
        }

        @Override
        public boolean test(T t) {
            return (boolean) inner_apply(true, t);
        }

        @Override
        public boolean test(T t, U u) {
            return (boolean) inner_apply(true, t, u);
        }

        @Override
        public void run() {
            inner_accept(await);
        }

        @Override
        public int compare(T o1, T o2) {
            return (int) inner_apply(true, o1, o2);
        }

        @Override
        public R get() {
            return (R) inner_apply(true);
        }
    }
    
}
