package xyz.wagyourtail.jsmacrosjruby.ruby.library.impl;

import org.jruby.RubyMethod;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.library.IFConsumer;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacrosjruby.ruby.language.impl.RubyLanguageDefinition;

@Library(value = "consumer", languages = RubyLanguageDefinition.class)
public class FConsumer extends PerExecLanguageLibrary<IFConsumer> implements IFConsumer<RubyMethod, RubyMethod, RubyMethod> {
    
    
    public FConsumer(Class<? extends BaseLanguage> language, Object context, Thread thread) {
        super(language, context, thread);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toConsumer(RubyMethod c) {
        return autoWrap(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toBiConsumer(RubyMethod c) {
        return autoWrap(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toAsyncConsumer(RubyMethod c) {
        return autoWrapAsync(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toAsyncBiConsumer(RubyMethod c) {
        return autoWrapAsync(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> autoWrap(RubyMethod c) {
        return new MethodWrapper<A, B, R>() {
            private Object internalAccept(Object ...objects) {
                ThreadContext threadContext = ((ScriptingContainer) context).getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, objects);
                IRubyObject rubyReturn = c.call(threadContext, rubyObjects, threadContext.getFrameBlock());
                if (rubyReturn == null) {
                    return null;
                } else {
                    return rubyReturn.toJava(Object.class);
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
    public <A, B, R> MethodWrapper<A, B, R> autoWrapAsync(RubyMethod c) {
        return new MethodWrapper<A, B, R>() {
            private Object internalAccept(Object ...objects) {
                ThreadContext threadContext = ((ScriptingContainer) context).getProvider().getRuntime().getCurrentContext();
                threadContext.pushNewScope(threadContext.getCurrentStaticScope());
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, objects);
                IRubyObject rubyReturn = c.call(threadContext, rubyObjects, threadContext.getFrameBlock());
                if (rubyReturn == null) {
                    return null;
                } else {
                    return rubyReturn.toJava(Object.class);
                }
            }
            
            private void internalAsyncAccept(Object ...objects) {
                Thread t = new Thread(() -> {
                    ThreadContext threadContext = ((ScriptingContainer) context).getProvider().getRuntime().getCurrentContext();
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
    
}
