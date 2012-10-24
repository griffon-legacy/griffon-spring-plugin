/*
 * Copyright 2009-2012 the original author or authors.
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
import griffon.spring.ApplicationContextHolder
import griffon.spring.factory.support.ObjectFactoryBean
import org.codehaus.griffon.runtime.core.SpringServiceArtifactHandler
import org.codehaus.griffon.runtime.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.griffon.runtime.spring.GriffonApplicationContext
import org.codehaus.griffon.runtime.spring.GriffonRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext

import static org.codehaus.griffon.runtime.spring.GriffonRuntimeConfigurator.loadSpringGroovyResourcesIntoContext

/**
 * @author Andres Almiray
 */
class SpringGriffonAddon {
    private BeanBuilder beanBuilder

    void addonInit(GriffonApplication app) {
        GriffonApplicationContext rootAppCtx = new GriffonApplicationContext()
        rootAppCtx.refresh()
        GriffonRuntimeConfigurator configurator = new GriffonRuntimeConfigurator(app, rootAppCtx)
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(rootAppCtx, app.class.classLoader)
        beanBuilder = new BeanBuilder(rootAppCtx, app.class.classLoader)
        beanBuilder.beans {
            'application'(ObjectFactoryBean) {
                object = app
                objectClass = GriffonApplication
            }
            'appConfig'(ObjectFactoryBean) {
                object = app.config
                objectClass = ConfigObject
            }
            'artifactManager'(ObjectFactoryBean) {
                object = app.artifactManager
                objectClass = griffon.core.ArtifactManager
            }
            'addonManager'(ObjectFactoryBean) {
                object = app.addonManager
                objectClass = griffon.core.AddonManager
            }
            'mvcGroupManager'(ObjectFactoryBean) {
                object = app.mvcGroupManager
                objectClass = griffon.core.MVCGroupManager
            }
            'uiThreadManager'(ObjectFactoryBean) {
                object = UIThreadManager.instance
            }

            def registerClass = { GriffonClass griffonClass ->
                "${griffonClass.propertyName}Class"(ObjectFactoryBean) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = 'byName'
                    object = griffonClass
                }
            }
            app.artifactManager.modelClasses.each(registerClass)
            app.artifactManager.controllerClasses.each(registerClass)
            app.artifactManager.viewClasses.each(registerClass)
        }
        beanBuilder.registerBeans(springConfig)
        loadSpringGroovyResourcesIntoContext(springConfig, app, rootAppCtx)
        ApplicationContext applicationContext = configurator.configure(springConfig)
        ApplicationContextHolder.applicationContext = applicationContext
        app.metaClass.applicationContext = applicationContext

        app.artifactManager.registerArtifactHandler(new SpringServiceArtifactHandler(app))
    }

    // ================== EVENTS =================

    Map events = [
        NewInstance: { klass, type, instance ->
            app.applicationContext.getAutowireCapableBeanFactory()
                .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
            if (type == 'service') app.addApplicationEventListener(instance)
        },
        LoadAddonsEnd: { app, addons ->
            app.event('WithSpringStart', [app, app.applicationContext])
            addons.each { withSpring(it.value) }
            app.event('WithSpringEnd', [app, app.applicationContext])
            app.event('WhenSpringReadyStart', [app, app.applicationContext])
            addons.each { springReady(it.value) }
            app.event('WhenSpringReadyEnd', [app, app.applicationContext])
        }
    ]

    // =================== IMPL ==================

    private void withSpring(addon) {
        def target = addon instanceof GriffonAddon ? addon : addon.addonDelegate
        def addonMetaClass = target.metaClass
        def doWithSpring = addonMetaClass.getMetaProperty('doWithSpring')
        if (doWithSpring) {
            def beans = target.getProperty('doWithSpring')
            if (beans instanceof Closure) {
                registerBeans(app.applicationContext, beans)
            }
        }
    }

    private registerBeans(ApplicationContext appCtx, Closure beans) {
        beanBuilder.beans(beans)
        beanBuilder.registerBeans(appCtx)
    }

    private void springReady(addon) {
        try {
            def target = addon instanceof GriffonAddon ? addon : addon.addonDelegate
            target.whenSpringReady(app)
        } catch (MissingMethodException mme) {
            if (mme.method != 'whenSpringReady') throw mme
        }
    }
}