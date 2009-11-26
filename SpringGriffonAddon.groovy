import grails.spring.BeanBuilder

import griffon.util.IGriffonApplication
import griffon.spring.artifact.SpringServiceArtifactHandler
import griffon.spring.factory.support.GriffonApplicationFactoryBean
import org.codehaus.griffon.commons.spring.GriffonApplicationContext
import org.codehaus.griffon.commons.spring.GriffonRuntimeConfigurator
import org.codehaus.griffon.commons.spring.DefaultRuntimeSpringConfiguration
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

class SpringGriffonAddon {
    IGriffonApplication app
    private final List addons = []
    private BeanBuilder beanBuilder

    def addonInit(appInstance) {
        app = appInstance
        
        def rootAppCtx = new GriffonApplicationContext()
        beanBuilder = new BeanBuilder(rootAppCtx, app.class.classLoader)
        registerBeans(rootAppCtx) {
            'app'(GriffonApplicationFactoryBean) {
                application = appInstance
            }
        }
        rootAppCtx.refresh()

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

    private void registerBeans(appCtx, Closure beans) {
        beanBuilder.beans(beans)
        beanBuilder.registerBeans(appCtx)
    }

    private void springReady(addon) {
        try {
            addon.whenSpringReady(app)
        } catch(MissingMethodException mme) {
            if(mme.method != 'whenSpringReady') throw mme
        }
    }
}
