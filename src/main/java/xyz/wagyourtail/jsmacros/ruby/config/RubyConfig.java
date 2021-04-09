package xyz.wagyourtail.jsmacros.ruby.config;

import xyz.wagyourtail.jsmacros.core.config.Option;

public class RubyConfig {
    @Option(translationKey = "jsmacrosruby.globalcontext", group = {"jsmacros.settings.languages", "jsmacrosruby.settings.languages.ruby"})
    public boolean useGlobalContext = true;
}
