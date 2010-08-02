/*
* Copyright 2004-2010 the original author or authors.
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

package org.codehaus.griffon.runtime.spring;

import grails.spring.BeanBuilder;
// import griffon.util.GriffonUtil;
import groovy.lang.Closure;
import groovy.lang.Script;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import griffon.core.GriffonApplication;
// import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A class that handles the runtime configuration of the Griffon ApplicationContext.<p>
 * Tweaked from its Grails counterpart.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class GriffonRuntimeConfigurator implements ApplicationContextAware {
    public static final String BEAN_ID = "griffonConfigurator";
    public static final String SPRING_RESOURCES_XML = "/spring/resources.xml";
    public static final String SPRING_RESOURCES_GROOVY = "/spring/resources.groovy";
    public static final String SPRING_RESOURCES_CLASS = "resources";
    public static final String MESSAGE_SOURCE_BEAN = "messageSource";
    public static final String EXCEPTION_HANDLER_BEAN = "exceptionHandler";
    public static final String CUSTOM_EDITORS_BEAN = "customEditors";
    public static final String CLASS_EDITOR_BEAN = "classEditor";
    public static final String CLASS_LOADER_BEAN = "classLoader";

    private static final Log LOG = LogFactory.getLog(GriffonRuntimeConfigurator.class);

    private GriffonApplication application;
    private ApplicationContext parent;
    private boolean loadExternalPersistenceConfig;
    private static final String DEVELOPMENT_SPRING_RESOURCES_XML = "file:./griffon-app/conf/spring/resources.xml";

    public GriffonRuntimeConfigurator(GriffonApplication application) {
        this(application, null);
    }

    public GriffonRuntimeConfigurator(GriffonApplication application, ApplicationContext parent) {
        super();
        this.application = application;
        this.parent = parent;
    }

    /**
     * Configures the Griffon application context at runtime
     *
     * @return A ApplicationContext instance
     */
    public ApplicationContext configure() {
        return configure(true);
    }

    public ApplicationContext configure(boolean loadExternalBeans) {
        DefaultRuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(parent, application.getClass().getClassLoader());
        return configure(springConfig, loadExternalBeans);
    }

    public ApplicationContext configure(DefaultRuntimeSpringConfiguration springConfig) {
        return configure(springConfig, true);
    }

    public ApplicationContext configure(DefaultRuntimeSpringConfiguration springConfig, boolean loadExternalBeans) {
        Assert.notNull(application);

        springConfig = springConfig != null ? springConfig : new DefaultRuntimeSpringConfiguration(parent, application.getClass().getClassLoader());
        registerParentBeanFactoryPostProcessors(springConfig);
        
        LOG.debug("[RuntimeConfiguration] Processing additional external configurations");

        if (loadExternalBeans) {
            doPostResourceConfiguration(application,springConfig);
        }

        reset();

        // TODO GRAILS-720 this causes plugin beans to be re-created - should get getApplicationContext always call refresh?
        ApplicationContext ctx = (ApplicationContext) springConfig.getApplicationContext();

        return ctx;
    }

    private void registerParentBeanFactoryPostProcessors(DefaultRuntimeSpringConfiguration springConfig) {
        if(parent != null) {
            Map parentPostProcessors = parent.getBeansOfType(BeanFactoryPostProcessor.class);
            for (Object o : parentPostProcessors.values()) {
                BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) o;
                ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                        .addBeanFactoryPostProcessor(postProcessor);

            }
        }
    }

    public void reconfigure(GriffonApplicationContext current, boolean loadExternalBeans) {
        RuntimeSpringConfiguration springConfig = parent != null ? new DefaultRuntimeSpringConfiguration(parent) : new DefaultRuntimeSpringConfiguration();

        List beanNames = springConfig.getBeanNames();
        for (Object beanName : beanNames) {
            String name = (String) beanName;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Re-creating bean definition [" + name + "]");
            }
            current.registerBeanDefinition(name, springConfig.createBeanDefinition(name));
            // force initialisation
            current.getBean(name);
        }

        if (loadExternalBeans)
            doPostResourceConfiguration(application, springConfig);
                
        reset();
    }

    public ApplicationContext configureDomainOnly() {
        DefaultRuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(parent, application.getClass().getClassLoader());


        ApplicationContext ctx = (ApplicationContext) springConfig.getApplicationContext();

        return ctx;
    }

    private void doPostResourceConfiguration(GriffonApplication application, RuntimeSpringConfiguration springConfig) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Resource springResources;
            ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
            springResources = patternResolver.getResource(SPRING_RESOURCES_XML);

            if (springResources != null && springResources.exists()) {
                LOG.debug("[RuntimeConfiguration] Configuring additional beans from " + springResources.getURL());
                XmlBeanFactory xmlBf = new XmlBeanFactory(springResources);
                xmlBf.setBeanClassLoader(classLoader);
                String[] beanNames = xmlBf.getBeanDefinitionNames();
                LOG.debug("[RuntimeConfiguration] Found [" + beanNames.length + "] beans to configure");
                for (String beanName : beanNames) {
                    BeanDefinition bd = xmlBf.getBeanDefinition(beanName);
                    final String beanClassName = bd.getBeanClassName();
                    Class beanClass = beanClassName == null ? null : ClassUtils.forName(beanClassName, classLoader);

                    springConfig.addBeanDefinition(beanName, bd);
                    String[] aliases = xmlBf.getAliases(beanName);
                    for (String alias : aliases) {
                        springConfig.addAlias(alias, beanName);
                    }
                    if (beanClass != null) {
                        if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass)) {
                            ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                                    .addBeanFactoryPostProcessor((BeanFactoryPostProcessor) xmlBf.getBean(beanName));
                        }
                    }
                }


            } else if (LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] " + GriffonRuntimeConfigurator.SPRING_RESOURCES_XML + " not found. Skipping configuration.");
            }

            GriffonRuntimeConfigurator.loadSpringGroovyResources(springConfig, classLoader);

        } catch (Exception ex) {
            LOG.warn("[RuntimeConfiguration] Unable to perform post initialization config: " + SPRING_RESOURCES_XML, ex);
        }
    }

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "resources.groovy"
     *
     * @param config
     * @param classLoader
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader,

                                                    GenericApplicationContext context) {

        loadExternalSpringConfig(config, classLoader);
        if (springGroovyResourcesBeanBuilder != null && context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }
    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object
     * @param config The config instance
     * @param classLoader The class loader
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, ClassLoader classLoader) {
        if(springGroovyResourcesBeanBuilder == null) {
            loadPluginGroovyResources(config, classLoader);
            try {
                Class groovySpringResourcesClass = null;
                try {
                    groovySpringResourcesClass = ClassUtils.forName(SPRING_RESOURCES_CLASS, classLoader);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                loadBeansFromScript(config, groovySpringResourcesClass);
            } catch (Exception ex) {
                // GriffonUtil.deepSanitize(ex);
                LOG.error("[RuntimeConfiguration] Unable to load beans from resources.groovy", ex);
            }
        } else {
            if(!springGroovyResourcesBeanBuilder.getSpringConfig().equals(config)) {
                springGroovyResourcesBeanBuilder.registerBeans(config);
            }
        }
    }

    private static void loadBeansFromScript(RuntimeSpringConfiguration config, Class scriptClass) throws Exception {
        if(scriptClass == null) return;
        if(springGroovyResourcesBeanBuilder == null) {
            springGroovyResourcesBeanBuilder = new BeanBuilder(null, config, Thread.currentThread().getContextClassLoader());
        }

        Script script = (Script) scriptClass.newInstance();
        script.run();
        Object beans = script.getProperty("beans");
        springGroovyResourcesBeanBuilder.beans((Closure) beans);
    }

    private static void loadPluginGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        GroovyClassLoader gcl = new GroovyClassLoader(classLoader);
        try {
            Resource[] resources = resolver.getResources("classpath*:/META-INF/spring/springbeans.groovy");
            for(Resource resource: resources) {
                try {
                    Class scriptClass = gcl.parseClass(new GroovyCodeSource(resource.getURL()));
                    loadBeansFromScript(config, scriptClass);
                } catch(Exception ex) {
                    // GriffonUtil.deepSanitize(ex);
                    LOG.error("[RuntimeConfiguration] Unable to load beans from "+ resource, ex);
                }
            }
        } catch(IOException ioe) {
            // GriffonUtil.deepSanitize(ioe);
            LOG.error("[RuntimeConfiguration] Unable to load beans from plugin resources", ioe);
        }
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader) {
        loadExternalSpringConfig(config, classLoader);
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, ClassLoader classLoader,
                                                            GenericApplicationContext context) {
        loadExternalSpringConfig(config, classLoader);  
        doLoadSpringGroovyResources(config, classLoader, context);
    }

    public void setLoadExternalPersistenceConfig(boolean b) {
        this.loadExternalPersistenceConfig = b;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parent = applicationContext;
    }

    /**
     * Resets the GriffonRumtimeConfigurator
     */
    public void reset() {
        springGroovyResourcesBeanBuilder = null;
    }
}
