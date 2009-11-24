import griffon.util.IGriffonApplication
import org.codehaus.griffon.commons.spring.GriffonRuntimeConfigurator
import org.codehaus.griffon.commons.spring.DefaultRuntimeSpringConfiguration
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

class SpringGriffonAddon {
    IGriffonApplication app

    def addonInit(app) {
        this.app = app
        def configurator = new GriffonRuntimeConfigurator(app, null)
        def springConfig = new DefaultRuntimeSpringConfiguration(null, app.class.classLoader)
        GriffonRuntimeConfigurator.loadExternalSpringConfig(springConfig, app.class.classLoader)
        app.metaClass.applicationContext = configurator.configure()
        app.addApplicationEventListener(this)

        // TODO
        // 1. register SpringServiceArtefactHandler
    }

    def onNewInstance = { klass, type, instance ->
        app.applicationContext.getAutowireCapableBeanFactory()
                .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
    }

    def addonPostInit(app) {
        // TODO
        // 1. fetch all addons interested in Spring features
        //  - app.applicationContext.springAddon.addons << this (??)
        // 2. for each addon:
        //  - call doWithSpring
    }
}
