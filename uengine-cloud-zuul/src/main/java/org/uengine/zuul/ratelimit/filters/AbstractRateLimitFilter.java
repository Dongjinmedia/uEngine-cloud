/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uengine.zuul.ratelimit.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.web.util.UrlPathHelper;
import org.uengine.zuul.ratelimit.config.RateLimitUtils;
import org.uengine.zuul.ratelimit.config.properties.RateLimitProperties;
import org.uengine.zuul.ratelimit.config.properties.validators.Policies;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractRateLimitFilter extends ZuulFilter {

    private final RateLimitProperties properties;
    private final RouteLocator routeLocator;
    private final UrlPathHelper urlPathHelper;
    private final RateLimitUtils rateLimitUtils;

    @Override
    public boolean shouldFilter() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        return properties.isEnabled() && !policy(route(request), request).isEmpty();
    }

    Route route(HttpServletRequest request) {
        String requestURI = urlPathHelper.getPathWithinApplication(request);
        return routeLocator.getMatchingRoute(requestURI);
    }

    protected List<RateLimitProperties.Policy> policy(Route route, HttpServletRequest request) {
        String routeId = Optional.ofNullable(route).map(Route::getId).orElse(null);
        return properties.getPolicies(routeId).stream()
            .filter(policy -> applyPolicy(request, route, policy))
            .collect(Collectors.toList());
    }

    private boolean applyPolicy(HttpServletRequest request, Route route, RateLimitProperties.Policy policy) {
        List<RateLimitProperties.Policy.MatchType> types = policy.getType();
        return types.isEmpty() || types.stream().allMatch(type -> type.apply(request, route, rateLimitUtils));
    }
}
