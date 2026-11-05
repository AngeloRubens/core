/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.resolution;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.inject.spi.Bean;

import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @author pmuir
 * @author alesj
 * @author Jozef Hartinger
 */
public class TypeSafeBeanResolver extends AbstractTypeSafeBeanResolver<Bean<?>, Set<Bean<?>>> {

    // about this many entries at most: concurrent misses may overshoot it slightly
    private static final int MAX_DEFAULT_QUALIFIER_ENTRIES = 1024;

    // Results of lookups by class with no qualifiers, in front of the resolution cache: the key is the class itself, so a
    // hit needs neither a ResolvableBuilder nor a Resolvable. clear() replaces the map rather than emptying it, see below.
    private volatile ConcurrentMap<Class<?>, Set<Bean<?>>> defaultQualifierResults = new ConcurrentHashMap<>();

    public TypeSafeBeanResolver(BeanManagerImpl beanManager, Iterable<Bean<?>> beans) {
        super(beanManager, beans);
    }

    /**
     * Resolves the beans of a class with no qualifiers (that is, with {@code @Default}). Gives the same result as resolving
     * a {@link Resolvable} built for it.
     */
    public Set<Bean<?>> resolveWithDefaultQualifier(Class<?> type) {
        if (!ResolvableBuilder.isPlainBeanType(type)) {
            return resolve(new ResolvableBuilder(type, getBeanManager()).create(), true);
        }
        // Read the map before resolving. If clear() runs in between, this thread only fills a map that is no longer in use,
        // so a result computed from the state before the clear can never be seen by a later lookup.
        ConcurrentMap<Class<?>, Set<Bean<?>>> results = defaultQualifierResults;
        Set<Bean<?>> result = results.get(type);
        if (result == null) {
            result = resolve(new ResolvableBuilder(type, getBeanManager()).create(), true);
            if (results.size() < MAX_DEFAULT_QUALIFIER_ENTRIES) {
                results.putIfAbsent(type, result);
            }
        }
        return result;
    }

    @Override
    public void clear() {
        // Order matters: a thread that reads the new map has to find the resolution cache already cleared.
        super.clear();
        defaultQualifierResults = new ConcurrentHashMap<>();
    }

    @Override
    protected Set<Bean<?>> sortResult(Set<Bean<?>> matched) {
        return matched;
    }
}
