/*
* Copyright 2009-2010 the original author or authors.
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

package org.codehaus.griffon.runtime.core

import griffon.core.GriffonApplication
import griffon.core.GriffonClass
import griffon.core.ArtifactInfo
import grails.spring.BeanBuilder
import griffon.spring.factory.support.ObjectFactoryBean
import griffon.spring.factory.support.GriffonClassInstanceFactoryBean

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
abstract class SpringArtifactHandlerAdapter extends ArtifactHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SpringArtifactHandlerAdapter)
    private final boolean autoRegisterArtifacts

    SpringArtifactHandlerAdapter(GriffonApplication app, String type, String trailing) {
        this(app, type, trailing, true)
    }
    
    SpringArtifactHandlerAdapter(GriffonApplication app, String type, String trailing, boolean autoRegisterArtifacts) {
        super(app, type, trailing)
        this.autoRegisterArtifacts = autoRegisterArtifacts
    }

    void initialize(ArtifactInfo[] artifacts) {
        super.initialize(artifacts)
        if(!artifacts) return
        if(autoRegisterArtifacts) registerArtifacts()
    }

    void registerArtifacts() {
        registerGriffonClasses()
        registerInstances()
    }
    
    protected void registerGriffonClasses() {
        LOG.trace("Registering beans of type GriffonClass for ${this.class.name}")
        doWithBeanBuilder(app) { 
            classes.each { GriffonClass targetGriffonClass ->
                SpringArtifactHandlerAdapter.LOG.trace("Registering ${targetGriffonClass.class.name} as ${targetGriffonClass.propertyName}Class")
                "${targetGriffonClass.propertyName}Class"(ObjectFactoryBean) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = 'byName'
                    object = targetGriffonClass
                }
            }
        }
    }

    protected void registerInstances() {
        LOG.trace("Registering bean instances of type GriffonClass for ${this.class.name}")
        doWithBeanBuilder(app) { 
            classes.each { GriffonClass targetGriffonClass ->
                SpringArtifactHandlerAdapter.LOG.trace("Registering bean of ${targetGriffonClass.class.name} as ${targetGriffonClass.propertyName}")
                "${targetGriffonClass.propertyName}"(GriffonClassInstanceFactoryBean) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = 'byName'
                    griffonClass = targetGriffonClass
                }
            }
        }
    }

    static void doWithBeanBuilder(GriffonApplication app, Closure closure) {
        BeanBuilder beanBuilder = new BeanBuilder(app.applicationContext, app.class.classLoader)
        beanBuilder.beans(closure)
        beanBuilder.registerBeans(app.applicationContext)
    }
}
