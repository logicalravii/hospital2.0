package org.springframework.cloud.gateway.handler.predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.support.ShortcutConfigurable;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PathRoutePredicateFactory extends AbstractRoutePredicateFactory<PathRoutePredicateFactory.Config> {

    private static final Log log = LogFactory.getLog(PathRoutePredicateFactory.class);

    private PathPatternParser pathPatternParser = new PathPatternParser();

    public PathRoutePredicateFactory() {
        super(Config.class);
    }

    public void setPathPatternParser(PathPatternParser pathPatternParser) {
        this.pathPatternParser = pathPatternParser;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("patterns", "matchTrailingSlash");
    }

    @Override
    public ShortcutConfigurable.ShortcutType shortcutType() {
        return ShortcutConfigurable.ShortcutType.GATHER_LIST_TAIL_FLAG;
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        final ArrayList<PathPattern> pathPatterns = new ArrayList<>();
        synchronized (this.pathPatternParser) {
            config.getPatterns().forEach(pattern -> {
                PathPattern pathPattern = this.pathPatternParser.parse(pattern);
                pathPatterns.add(pathPattern);
            });
        }

        return new Predicate<>() {
            @Override
            public boolean test(ServerWebExchange exchange) {
                PathContainer path = PathContainer.parsePath(exchange.getRequest().getURI().getRawPath());

                for (PathPattern pathPattern : pathPatterns) {
                    if (pathPattern.matches(path)) {
                        PathPattern.PathMatchInfo uriTemplateVariables = pathPattern.matchAndExtract(path);
                        exchange.getAttributes().put(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables.getUriVariables());
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return String.format("Paths: %s, matchTrailingSlash: %b", config.getPatterns(), config.isMatchTrailingSlash());
            }
        };
    }

    public static class Config {
        private List<String> patterns = new ArrayList<>();
        private boolean matchTrailingSlash = true;

        public List<String> getPatterns() {
            return patterns;
        }

        public Config setPatterns(List<String> patterns) {
            this.patterns = patterns;
            return this;
        }

        public boolean isMatchTrailingSlash() {
            return matchTrailingSlash;
        }

        public Config setMatchTrailingSlash(boolean matchTrailingSlash) {
            this.matchTrailingSlash = matchTrailingSlash;
            return this;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this)
                .append("patterns", patterns)
                .append("matchTrailingSlash", matchTrailingSlash)
                .toString();
        }
    }
}
