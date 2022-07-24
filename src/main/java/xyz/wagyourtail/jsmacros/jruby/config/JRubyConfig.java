package xyz.wagyourtail.jsmacros.jruby.config;

import xyz.wagyourtail.jsmacros.core.config.Option;

public class JRubyConfig {

    @Option(translationKey = "jsmacrosruby.globalcontext", group = {"jsmacros.settings.languages", "jsmacrosruby.settings.languages.ruby"})
    public boolean useGlobalContext = false;
}
