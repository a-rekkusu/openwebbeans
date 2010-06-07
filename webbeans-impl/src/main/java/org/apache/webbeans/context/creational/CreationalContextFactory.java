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
package org.apache.webbeans.context.creational;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.config.WebBeansFinder;

/**
 * Factory for {@link CreationalContext} instances.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> contextual type info
 */
public final class CreationalContextFactory<T>
{
    /**
     * Creates a new <code>CreationalContextFactory</code> instance.
     */
    public CreationalContextFactory()
    {
    }
    
    /**
     * Gets singleton instance.
     * 
     * @return singleton factory per class loader per VM
     */
    @SuppressWarnings("unchecked")
    public static CreationalContextFactory getInstance()
    {
        return (CreationalContextFactory)WebBeansFinder.getSingletonInstance(CreationalContextFactory.class.getName());
    }
    
    /**
     * Returns a new creational context for given contextual.
     * 
     * @param contextual contextual instance
     * @return new creational context for given contextual
     */
    public CreationalContext<T> getCreationalContext(Contextual<T> contextual)
    {        
        return new CreationalContextImpl<T>(contextual);   
    }        
    
    public CreationalContext<T> wrappedCreationalContext(CreationalContext<T> creationalContext, Contextual<T> contextual)
    {
        return new WrappedCreationalContext<T>(contextual, creationalContext);
    }
    
}