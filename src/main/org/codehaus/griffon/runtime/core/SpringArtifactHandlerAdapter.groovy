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

/**
 * @author Andres Almiray
 */
abstract class SpringArtifactHandlerAdapter extends ArtifactHandlerAdapter {
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
        doWithBeanBuilder(app) { 
            classes.each { GriffonClass targetGriffonClass ->
                "${targetGriffonClass.propertyName}Class"(ObjectFactoryBean) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = 'byName'
                    object = targetGriffonClass
                }
            }
        }
    }

    protected void registerInstances() {
	    doWithBeanBuilder(app) { 
            classes.each { GriffonClass targetGriffonClass ->
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