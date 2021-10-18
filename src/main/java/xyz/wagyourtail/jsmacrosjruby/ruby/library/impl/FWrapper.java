package xyz.wagyourtail.jsmacrosjruby.ruby.library.impl;

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
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyLanguageDefinition;

import java.util.concurrent.Semaphore;

@Library(value = "JavaWrapper", languages = RubyLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<ScriptingContainer> implements IFWrapper<RubyMethod> {
    
    public FWrapper(BaseScriptContext context, Class language) {
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



    private static class RubyMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, BaseScriptContext<ScriptingContainer>> {
        private final RubyMethod fn;
        private final boolean await;

        RubyMethodWrapper(RubyMethod fn, boolean await, BaseScriptContext<ScriptingContainer> ctx) {
            super(ctx);
            this.fn = fn;
            this.await = await;
        }

        private Object inner_accept(boolean await, Object... params) {

            if (await) {
                if (ctx.getBoundThreads().contains(Thread.currentThread())) {
                    ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                    return fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
                }

                ctx.bindThread(Thread.currentThread());
            }

            Object[] retval = {null};
            Throwable[] error = {null};
            Semaphore lock = new Semaphore(0);
            boolean joinedThread = Core.instance.profile.checkJoinedThreadStack();

            Thread t = new Thread(() -> {
                if (ctx.isContextClosed()) throw new RuntimeException("Context Closed");
                ctx.bindThread(Thread.currentThread());

                try {
                    if (await && joinedThread) {
                        Core.instance.profile.joinedThreadStack.add(Thread.currentThread());
                    }
                    ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
                    threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                    IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                    retval[0] = fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
                } catch (Throwable ex) {
                    if (!await) {
                        Core.instance.profile.logError(ex);
                    }
                    error[0] = ex;
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                    Core.instance.profile.joinedThreadStack.remove(Thread.currentThread());

                    ctx.releaseBoundEventIfPresent(Thread.currentThread());

                    lock.release();
                }
            });
            t.start();

            if (await) {
                try {
                    lock.acquire();
                    if (error[0] != null) throw new RuntimeException(error[0]);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                }
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
    }
    
}
