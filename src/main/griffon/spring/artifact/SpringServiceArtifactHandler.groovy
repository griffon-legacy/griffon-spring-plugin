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

package griffon.spring.artifact

import griffon.core.ArtifactInfo
import griffon.core.ArtifactHandlerAdapter
import grails.spring.BeanBuilder

/**
 * @author Andres Almiray (aalmiray)
 */
class SpringServiceArtifactHandler extends ArtifactHandlerAdapter {
    SpringServiceArtifactHandler() {
        super("service")
    }

    void initialize(ArtifactInfo[] artifacts) {
        super.initialize(artifacts)
        if(!artifacts) return
        BeanBuilder beanBuilder = new BeanBuilder(app.applicationContext, app.class.classLoader)
        beanBuilder.beans { 
            artifacts.each { artifact ->
                "${artifact.simpleName}"(artifact.klass) { bean ->
                    bean.scope = "singleton"
                    bean.autowire = "byName"
                }
            }
        }
        beanBuilder.registerBeans(app.applicationContext)
    }
}
