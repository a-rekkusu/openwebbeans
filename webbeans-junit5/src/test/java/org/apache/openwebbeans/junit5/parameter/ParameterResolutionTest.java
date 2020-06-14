/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openwebbeans.junit5.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Inject;
import javax.inject.Qualifier;
import org.apache.openwebbeans.junit5.Cdi;
import org.apache.openwebbeans.junit5.bean.MyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@Cdi(classes = MyService.class)
class ParameterResolutionTest
{
    @Inject MyService service;

    @Test
    void testThatParameterGetsInjected(MyService service)
    {
        assertEquals("ok", service.ok());
    }

    @Test
    void testThatParameterGetsInjectedSameScopedBeanInstance(MyService service)
    {
        assertSame(service, this.service);
    }

    static class OnlyFirstParameterResolver implements ParameterResolver
    {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return parameterContext.getIndex() == 0;
        }

        @Override
        public MyService resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return null; // a different value than a MyService instance CDI resolves that can be distinguished in the tests
        }
    }

    @Qualifier
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface NotResolvedAsCdiBeanIntoJunitParameter {
    }

    @Test
    @ExtendWith(OnlyFirstParameterResolver.class)
    void testThatParameterDoesNotGetInjectedDueToQualifier(@NotResolvedAsCdiBeanIntoJunitParameter MyService service)
    {
        assertNull(service); // OnlyFirstParameterResolver.resolveParameter resolves service to null
    }

    @Test
    @ExtendWith(OnlyFirstParameterResolver.class)
    void testThatParameterDoesNotGetInjectedDueToDontInject(@Cdi.DontInject MyService service)
    {
        assertNull(service); // OnlyFirstParameterResolver.resolveParameter resolves service to null
    }

    @Test
    @ExtendWith(OnlyFirstParameterResolver.class)
    void testMixedCdiAndOtherParameterResolver(@NotResolvedAsCdiBeanIntoJunitParameter MyService service1, MyService service2)
    {
        // OnlyFirstParameterResolver.resolveParameter resolves service1 to null
        assertNull(service1);

        // Cdi/CdiExtension resolves service2 to a MyService instance and OnlyFirstParameterResolver knows
        // not to resolve it because it is not the first method parameter. This way the test here can assert that
        // both parameters are resolved by the right parameter resolver.
        assertEquals("ok", service2.ok());
    }
}
