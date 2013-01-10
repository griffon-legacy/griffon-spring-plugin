/*
* Copyright 2004-2013 the original author or authors.
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
import griffon.core.GriffonApplication;
import griffon.util.ApplicationClassLoader;
import griffon.util.CollectionUtils;
import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Map;

import static griffon.util.GriffonExceptionHandler.sanitize;

/**
 * A class that handles the runtime configuration of the Griffon ApplicationContext.<p>
 * Tweaked from its Grails counterpart.
 *
 * @author Graeme Rocher (Grails 0.3)
 */
public class GriffonRuntimeConfigurator implements ApplicationContextAware {
    public static final String SPRING_RESOURCES_XML = "spring/springbeans.xml";
    public static final String SPRING_RESOURCES_CLASS = "springbeans";
    public static final String CLASS_LOADER_BEAN = "classLoader";

    private static final Log LOG = LogFactory.getLog(GriffonRuntimeConfigurator.class);

    private GriffonApplication application;
    private ApplicationContext parent;

    public GriffonRuntimeConfigurator(GriffonApplication application) {
        this(application, null);
    }

    public GriffonRuntimeConfigurator(GriffonApplication application, ApplicationContext parent) {
        super();
        this.application = application;
        this.parent = parent;
    }

    public ApplicationContext configure(boolean loadExternalBeans) {
        DefaultRuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(parent, ApplicationClassLoader.get());
        return configure(springConfig, loadExternalBeans);
    }

    public ApplicationContext configure(DefaultRuntimeSpringConfiguration springConfig) {
        return configure(springConfig, true);
    }

    public ApplicationContext configure(DefaultRuntimeSpringConfiguration springConfig, boolean loadExternalBeans) {
        Assert.notNull(application);

        springConfig = springConfig != null ? springConfig : new DefaultRuntimeSpringConfiguration(parent, ApplicationClassLoader.get());
        registerParentBeanFactoryPostProcessors(springConfig);

        LOG.debug("[RuntimeConfiguration] Processing additional external configurations");

        if (loadExternalBeans) {
            doPostResourceConfiguration(application, springConfig);
        }

        reset();

        // TODO GRAILS-720 this causes plugin beans to be re-created - should get getApplicationContext always call refresh?
        ApplicationContext ctx = springConfig.getApplicationContext();

        return ctx;
    }

    private void registerParentBeanFactoryPostProcessors(DefaultRuntimeSpringConfiguration springConfig) {
        if (parent != null) {
            Map parentPostProcessors = parent.getBeansOfType(BeanFactoryPostProcessor.class);
            for (Object o : parentPostProcessors.values()) {
                BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) o;
                ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                    .addBeanFactoryPostProcessor(postProcessor);
            }
        }
    }

    private void doPostResourceConfiguration(GriffonApplication application, RuntimeSpringConfiguration springConfig) {
        ClassLoader classLoader = ApplicationClassLoader.get();
        String resourceName = SPRING_RESOURCES_XML;
        try {
            Resource springResources = new ClassPathResource(resourceName);

            if (springResources != null && springResources.exists()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[RuntimeConfiguration] Configuring additional beans from " + springResources.getURL());
                DefaultListableBeanFactory xmlBf = new DefaultListableBeanFactory();
                new XmlBeanDefinitionReader(xmlBf).loadBeanDefinitions(springResources);
                xmlBf.setBeanClassLoader(classLoader);
                String[] beanNames = xmlBf.getBeanDefinitionNames();
                if (LOG.isDebugEnabled())
                    LOG.debug("[RuntimeConfiguration] Found [" + beanNames.length + "] beans to configure");
                for (String beanName : beanNames) {
                    BeanDefinition bd = xmlBf.getBeanDefinition(beanName);
                    final String beanClassName = bd.getBeanClassName();
                    Class<?> beanClass = beanClassName == null ? null : ClassUtils.forName(beanClassName, classLoader);

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
                LOG.debug("[RuntimeConfiguration] " + resourceName + " not found. Skipping configuration.");
            }
        } catch (Exception ex) {
            LOG.error("[RuntimeConfiguration] Unable to perform post initialization config: " + resourceName, sanitize(ex));
        }

        loadSpringGroovyResources(springConfig, application);
    }

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "springbeans.groovy"
     *
     * @param config
     * @param application
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config, GriffonApplication application,
                                                    GenericApplicationContext context) {
        loadExternalSpringConfig(config, application);
        if (context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }
    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, GriffonApplication application) {
        if (springGroovyResourcesBeanBuilder == null) {
            try {
                Class<?> groovySpringResourcesClass = null;
                try {
                    groovySpringResourcesClass = ClassUtils.forName(SPRING_RESOURCES_CLASS,
                        ApplicationClassLoader.get());
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (groovySpringResourcesClass != null) {
                    loadPluginGroovyResources(config, application);
                    springGroovyResourcesBeanBuilder = new BeanBuilder(null, config, ApplicationClassLoader.get());
                    reloadSpringResourcesConfig(application, springGroovyResourcesBeanBuilder, groovySpringResourcesClass);
                }
            } catch (Exception ex) {
                LOG.error("[RuntimeConfiguration] Unable to load beans from resources.groovy", sanitize(ex));
            }
        } else {
            if (!springGroovyResourcesBeanBuilder.getSpringConfig().equals(config)) {
                springGroovyResourcesBeanBuilder.registerBeans(config);
            }
        }
    }

    public static BeanBuilder reloadSpringResourcesConfig(GriffonApplication application, BeanBuilder beanBuilder, Class<?> groovySpringResourcesClass) throws InstantiationException, IllegalAccessException {
        beanBuilder.setBinding(new Binding(CollectionUtils.newMap("application", application)));
        Script script = (Script) groovySpringResourcesClass.newInstance();
        script.run();
        Object beans = script.getProperty("beans");
        beanBuilder.beans((Closure<?>) beans);
        return beanBuilder;
    }

    private static BeanBuilder loadPluginGroovyResources(RuntimeSpringConfiguration config, GriffonApplication application) {
        ClassLoader classLoader = ApplicationClassLoader.get();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        GroovyClassLoader gcl = new GroovyClassLoader(classLoader);
        BeanBuilder bb = new BeanBuilder(null, config, classLoader);
        try {
            Resource[] resources = resolver.getResources("classpath*:/META-INF/spring/springbeans.groovy");
            for (Resource resource : resources) {
                try {
                    Class scriptClass = gcl.parseClass(new GroovyCodeSource(resource.getURL()));
                    reloadSpringResourcesConfig(application, bb, scriptClass);
                } catch (Exception ex) {
                    LOG.error("[RuntimeConfiguration] Unable to load beans from " + resource, sanitize(ex));
                }
            }
        } catch (IOException ioe) {
            LOG.error("[RuntimeConfiguration] Unable to load beans from plugin resources", sanitize(ioe));
        }
        return bb;
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, GriffonApplication application) {
        loadExternalSpringConfig(config, application);
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, GriffonApplication application,
                                                            GenericApplicationContext context) {
        loadExternalSpringConfig(config, application);
        doLoadSpringGroovyResources(config, application, context);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        parent = applicationContext;
    }

    /**
     * Resets the GriffonRumtimeConfigurator
     */
    public void reset() {
        springGroovyResourcesBeanBuilder = null;
    }
}
