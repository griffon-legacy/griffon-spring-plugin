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

package org.codehaus.griffon.runtime.core;

import griffon.core.*;
import java.util.Map;
import java.util.Collections;
import griffon.spring.ApplicationContextHolder;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andres Almiray
 */
class SpringServiceArtifactHandler extends SpringArtifactHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SpringServiceArtifactHandler.class);
    private final ServiceManager serviceManager;

    private class SpringServiceManager extends AbstractServiceManager {
        public SpringServiceManager(GriffonApplication app) {
            super(app);
        }

        public Map<String, GriffonService> getServices() {
            return Collections.unmodifiableMap(ApplicationContextHolder.getApplicationContext().getBeansOfType(GriffonService.class));
        }

        public GriffonService findService(String name) {
            if(!name.endsWith(GriffonServiceClass.TRAILING)) name += GriffonServiceClass.TRAILING;
            return (GriffonService) ApplicationContextHolder.getApplicationContext().getBean(name);
        }
    }
    
    SpringServiceArtifactHandler(GriffonApplication app) {
        super(app, GriffonServiceClass.TYPE, GriffonServiceClass.TRAILING);
        serviceManager = new SpringServiceManager(app);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registering " + serviceManager + " as ServiceManager.");
        }
        InvokerHelper.setProperty(app, "serviceManager", serviceManager);
    }

    protected GriffonClass newGriffonClassInstance(Class clazz) {
        return new DefaultGriffonServiceClass(getApp(), clazz);
    }
}
