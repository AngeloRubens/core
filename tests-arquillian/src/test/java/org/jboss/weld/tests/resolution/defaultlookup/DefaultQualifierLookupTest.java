/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.resolution.defaultlookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.BeanDiscoveryMode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.weld.test.util.Utils;
import org.jboss.weld.tests.category.Integration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * {@code BeanManager.getBeans(Class)} with no qualifiers is answered from a cache keyed by the class. It must give the same
 * beans as a lookup with an explicit {@code @Default}, and it must not keep a result from before the container finished
 * booting.
 */
@Category(Integration.class)
@RunWith(Arquillian.class)
public class DefaultQualifierLookupTest {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(BeanArchive.class, Utils.getDeploymentNameAsHash(DefaultQualifierLookupTest.class))
                .beanDiscoveryMode(BeanDiscoveryMode.ALL)
                .addPackage(DefaultQualifierLookupTest.class.getPackage())
                .addAsServiceProvider(Extension.class, LateBeanExtension.class);
    }

    @Inject
    BeanManager manager;

    @Inject
    LateBeanExtension extension;

    @Test
    public void sameBeansAsExplicitDefault() {
        assertEquals(1, manager.getBeans(Greeter.class).size());
        assertEquals(manager.getBeans(Greeter.class, Default.Literal.INSTANCE), manager.getBeans(Greeter.class));
        // the second lookup is a cache hit and returns the same immutable set
        assertSame(manager.getBeans(Greeter.class), manager.getBeans(Greeter.class));
    }

    @Test
    public void noBeanForUnknownType() {
        assertTrue(manager.getBeans(Runnable.class).isEmpty());
        assertTrue(manager.getBeans(Runnable.class).isEmpty());
    }

    @Test
    public void lookupDuringBootDoesNotHideBeanAddedLater() {
        assertTrue("nothing is registered for Late when the extension looks it up", extension.getLookupDuringBoot().isEmpty());
        assertEquals(1, manager.getBeans(Late.class).size());
    }
}
