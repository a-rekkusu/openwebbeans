/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.util;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.creation.BeanCreator.MetaDataProvider;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.ejb.component.EjbBean;
import org.apache.webbeans.ejb.component.creation.EjbBeanCreatorImpl;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.ProcessSessionBeanImpl;
import org.apache.webbeans.util.WebBeansUtil;

@SuppressWarnings("unchecked")
public final class EjbUtility
{
    private EjbUtility()
    {
        
    }
        
    public static <T> void fireEvents(Class<T> clazz, EjbBean<T> ejbBean)
    {
        AnnotatedType<T> annotatedType = AnnotatedElementFactory.newAnnotatedType(clazz);
        
        //Fires ProcessAnnotatedType
        ProcessAnnotatedTypeImpl<T> processAnnotatedEvent = WebBeansUtil.fireProcessAnnotatedTypeEvent(annotatedType);             
        EjbBeanCreatorImpl<T> ejbBeanCreator = new EjbBeanCreatorImpl<T>(ejbBean);
        
        if(processAnnotatedEvent.isVeto())
        {
            return;
        }
        
        if(processAnnotatedEvent.isSet())
        {
            ejbBeanCreator.setMetaDataProvider(MetaDataProvider.THIRDPARTY);
        }
        
        //Define meta-data
        ejbBeanCreator.defineSerializable();
        ejbBeanCreator.defineStereoTypes();
        Class<? extends Annotation> deploymentType = ejbBeanCreator.defineDeploymentType("There are more than one @DeploymentType annotation in Session Bean implementation class : " + ejbBean.getReturnType().getName());
        ejbBeanCreator.defineApiType();
        ejbBeanCreator.defineScopeType("Session Bean implementation class : " + clazz.getName() + " stereotypes must declare same @ScopeType annotations");
        ejbBeanCreator.defineBindingType();
        ejbBeanCreator.defineName(WebBeansUtil.getSimpleWebBeanDefaultName(clazz.getSimpleName()));            
        Set<ProducerMethodBean<?>> producerMethodBeans = ejbBeanCreator.defineProducerMethods();        
        Set<ProducerFieldBean<?>> producerFieldBeans = ejbBeanCreator.defineProducerFields();           
        ejbBeanCreator.defineDisposalMethods();
        ejbBeanCreator.defineInjectedFields();
        ejbBeanCreator.defineInjectedMethods();
        ejbBeanCreator.defineObserverMethods();        
        
        //Fires ProcessInjectionTarget
        ProcessInjectionTargetImpl<T> processInjectionTargetEvent = WebBeansUtil.fireProcessInjectionTargetEvent(ejbBean);            
        if(processInjectionTargetEvent.isSet())
        {
            ejbBeanCreator.setInjectedTarget(processInjectionTargetEvent.getInjectionTarget());
        }
        
        Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods = new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>(); 
        for(ProducerMethodBean<?> producerMethod : producerMethodBeans)
        {
            AnnotatedMethod<?> method = AnnotatedElementFactory.newAnnotatedMethod(producerMethod.getCreatorMethod(), producerMethod.getParent().getReturnType());
            ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForMethod(producerMethod,method);

            annotatedMethods.put(producerMethod, method);
            
            if(producerEvent.isProducerSet())
            {
                producerMethod.setProducer((Producer) ejbBeanCreator.getProducer());
            }
            
            producerEvent.setProducerSet(false);
        }
        
        Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields = new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();
        for(ProducerFieldBean<?> producerField : producerFieldBeans)
        {
            AnnotatedField<?> field = AnnotatedElementFactory.newAnnotatedField(producerField.getCreatorField(), producerField.getParent().getReturnType());
            ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForField(producerField, field);
            
            annotatedFields.put(producerField, field);
            
            if(producerEvent.isProducerSet())
            {
                producerField.setProducer((Producer) ejbBeanCreator.getProducer());
            }
            
            producerEvent.setProducerSet(false);
        }

        //Fires ProcessManagedBean
        ProcessSessionBeanImpl<T> processBeanEvent = new ProcessSessionBeanImpl<T>((Bean<Object>)ejbBean,annotatedType,ejbBean.getEjbName(),ejbBean.getEjbType());            
        BeanManagerImpl.getManager().fireEvent(processBeanEvent, new Annotation[0]);
        
        //Fires ProcessProducerMethod
        WebBeansUtil.fireProcessProducerMethodBeanEvent(annotatedMethods);
        
        //Fires ProcessProducerField
        WebBeansUtil.fireProcessProducerFieldBeanEvent(annotatedFields);
                
        //Set InjectionTarget that is used by the container to inject dependencies!
        if(ejbBeanCreator.isInjectionTargetSet())
        {
            ejbBean.setInjectionTarget(ejbBeanCreator.getInjectedTarget());   
        }
        
        // Check if the deployment type is enabled.
        if (WebBeansUtil.isDeploymentTypeEnabled(deploymentType))
        {                                
            BeanManagerImpl.getManager().addBean(ejbBean);
            BeanManagerImpl.getManager().getBeans().addAll(producerMethodBeans);
            BeanManagerImpl.getManager().getBeans().addAll(producerFieldBeans);
        }        
    }
}