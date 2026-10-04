/*
 * JBoss, Home of Professional Open Source
 * Copyright 2026, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.weld.bean.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.EmptyStackException;
import java.util.NoSuchElementException;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.junit.Test;

public class InterceptionDecorationContextTest {

    @Test
    public void testStackSemantics() {
        assertTrue(InterceptionDecorationContext.empty());
        assertNull(InterceptionDecorationContext.peekIfNotEmpty());
        assertNull(InterceptionDecorationContext.startIfNotEmpty());

        CombinedInterceptorAndDecoratorStackMethodHandler a = new CombinedInterceptorAndDecoratorStackMethodHandler();
        CombinedInterceptorAndDecoratorStackMethodHandler b = new CombinedInterceptorAndDecoratorStackMethodHandler();

        Stack stack = InterceptionDecorationContext.startIfNotOnTop(a);
        assertNotNull(stack);
        assertFalse(InterceptionDecorationContext.empty());
        assertSame(a, InterceptionDecorationContext.peek());
        // already on top - not pushed again
        assertNull(InterceptionDecorationContext.startIfNotOnTop(a));
        assertFalse(stack.startIfNotOnTop(a));
        assertEquals(1, stack.size());

        // a client proxy invocation starts a new context
        assertSame(stack, InterceptionDecorationContext.startIfNotEmpty());
        assertSame(CombinedInterceptorAndDecoratorStackMethodHandler.NULL_INSTANCE, stack.peek());
        assertTrue(stack.startIfNotOnTop(a));
        assertEquals(3, stack.size());

        // grow beyond the initial capacity, recursive invocations
        for (int i = 0; i < 20; i++) {
            assertTrue(stack.startIfNotOnTop(i % 2 == 0 ? b : a));
        }
        assertEquals(23, stack.size());
        assertSame(a, stack.peek());
        for (int i = 19; i >= 0; i--) {
            assertSame(i % 2 == 0 ? b : a, stack.peek());
            stack.end();
        }
        assertSame(a, stack.peek());
        stack.end();
        assertSame(CombinedInterceptorAndDecoratorStackMethodHandler.NULL_INSTANCE, stack.peek());
        stack.end();
        assertSame(a, InterceptionDecorationContext.peekIfNotEmpty());
        assertTrue(stack.toString().startsWith("Stack [elements=["));

        InterceptionDecorationContext.endInterceptorContext();
        assertTrue(InterceptionDecorationContext.empty());
        assertEquals(0, stack.size());
        assertNull(stack.peek());
        assertEquals("Stack [elements=[]]", stack.toString());
        assertThrows(EmptyStackException.class, InterceptionDecorationContext::peek);
        assertThrows(EmptyStackException.class, InterceptionDecorationContext::endInterceptorContext);
        assertThrows(NoSuchElementException.class, stack::end);
        assertThrows(NullPointerException.class, () -> stack.startIfNotOnTop(null));
        assertTrue(InterceptionDecorationContext.empty());

        // the idle stack is reused
        Stack again = InterceptionDecorationContext.startIfNotOnTop(b);
        assertSame(stack, again);
        assertSame(b, again.peek());
        again.end();
        assertTrue(InterceptionDecorationContext.empty());
    }
}
