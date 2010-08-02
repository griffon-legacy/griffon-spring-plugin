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

import grails.spring.BeanBuilder

import griffon.core.GriffonClass
import griffon.core.GriffonApplication
import griffon.core.ArtifactManager
import griffon.util.UIThreadHelper
import griffon.spring.factory.support.GriffonApplicationFactoryBean
import griffon.spring.factory.support.ObjectFactoryBean
import org.codehaus.griffon.runtime.spring.GriffonApplicationContext
import org.codehaus.griffon.runtime.spring.GriffonRuntimeConfigurator
import org.codehaus.griffon.runtime.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.griffon.runtime.core.SpringServiceArtifactHandler
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

/**
 * @author Andres Almiray
 */
class SpringGriffonAddon {
    GriffonApplication app

    def addonInit(appInstance) {
        app = appInstance

        GriffonApplicationContext rootAppCtx = new GriffonApplicationContext()        
        rootAppCtx.refresh()
        def configurator = new GriffonRuntimeConfigurator(app, rootAppCtx)
        def springConfig = new DefaultRuntimeSpringConfiguration(rootAppCtx, app.class.classLoader)
        def bb = new BeanBuilder(rootAppCtx, app.class.classLoader)
        bb.beans {
            'app'(GriffonApplicationFactoryBean) {
                application = appInstance
            }
            'appConfig'(ObjectFactoryBean) {
                object = appInstance.config
                objectClass = ConfigObject
            }
            'artifactManager'(ObjectFactoryBean) {
                object = ArtifactManager.instance
            }
            'uiThreadHelper'(ObjectFactoryBean) {
                object = UIThreadHelper.instance
            }

            def registerClass = { GriffonClass griffonClass ->
                "${griffonClass.propertyName}Class"(ObjectFactoryBean) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = 'byName'
                    object = griffonClass
                }
            }
            ArtifactManager.instance.modelClasses.each(registerClass)
            ArtifactManager.instance.controllerClasses.each(registerClass)
            ArtifactManager.instance.viewClasses.each(registerClass)
        }
        bb.registerBeans(springConfig)
        GriffonRuntimeConfigurator.loadSpringGroovyResourcesIntoContext(springConfig, app.class.classLoader, rootAppCtx)
        def applicationContext = configurator.configure(springConfig)
        app.metaClass.applicationContext = applicationContext

        app.artifactManager.registerArtifactHandler(new SpringServiceArtifactHandler(app))
    }

    // ================== EVENTS =================

    def events = [
        NewInstance: { klass, type, instance ->
            app.applicationContext.getAutowireCapableBeanFactory()
                .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        },
        LoadAddonsEnd: { app, addons ->
            addons.each { withSpring(it.value) }
            addons.each { springReady(it.value) }
        }
    ]

    // =================== IMPL ==================

    private void withSpring(addon) {
        def addonMetaClass = addon.metaClass
        def doWithSpring = addonMetaClass.getMetaProperty('doWithSpring')
        if(doWithSpring) {
            def beans = addon.getProperty('doWithSpring')
            if(beans instanceof Closure) {
                registerBeans(app.applicationContext, beans)
            }
        } 
    }

    private registerBeans(Closure beans) {
        def appCtx = new GriffonApplicationContext()
        appCtx.refresh()
        registerBeans(appCtx, beans)
    }

    private registerBeans(appCtx, Closure beans) {
        def beanBuilder = new BeanBuilder(appCtx, app.class.classLoader)
        beanBuilder.beans(beans)
        beanBuilder.registerBeans(appCtx)
        beanBuilder.createApplicationContext()
    }

    private void springReady(addon) {
        try {
            addon.whenSpringReady(app)
        } catch(MissingMethodException mme) {
            if(mme.method != 'whenSpringReady') throw mme
        }
    }
}
