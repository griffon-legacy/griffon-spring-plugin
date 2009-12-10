/*
 * Copyright 2009 the original author or authors.
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

import griffon.util.IGriffonApplication
import griffon.spring.artifact.SpringServiceArtifactHandler
import griffon.spring.factory.support.GriffonApplicationFactoryBean
import org.codehaus.griffon.commons.spring.GriffonApplicationContext
import org.codehaus.griffon.commons.spring.GriffonRuntimeConfigurator
import org.codehaus.griffon.commons.spring.DefaultRuntimeSpringConfiguration
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

/**
 * @author Andres Almiray
 */
class SpringGriffonAddon {
    IGriffonApplication app
    private final List addons = []

    def addonInit(appInstance) {
        app = appInstance
        
        def rootAppCtx = registerBeans {
            'app'(GriffonApplicationFactoryBean) {
                application = appInstance
            }
        }

        def configurator = new GriffonRuntimeConfigurator(app, rootAppCtx)
        def springConfig = new DefaultRuntimeSpringConfiguration(rootAppCtx, app.class.classLoader)
        GriffonRuntimeConfigurator.loadExternalSpringConfig(springConfig, app.class.classLoader)
        def applicationContext = configurator.configure(springConfig)
        app.metaClass.applicationContext = applicationContext
        app.addApplicationEventListener(this)

        app.artifactManager.registerArtifactHandler(new SpringServiceArtifactHandler())
    }

    def addonPostInit(app) {
        addons.each { withSpring(it) }
        addons.each { springReady(it) }
    }

    // ================== EVENTS =================

    def onNewInstance = { klass, type, instance ->
        app.applicationContext.getAutowireCapableBeanFactory()
                .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
    }

    def onSpringAddon = { addon ->
        if(addon) addons << addon
    }

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
