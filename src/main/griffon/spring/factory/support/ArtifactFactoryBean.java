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

package griffon.spring.factory.support;

import griffon.core.ArtifactInfo;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Andres Almiray (aalmiray)
 */
public class ArtifactFactoryBean implements FactoryBean {
    private ArtifactInfo artifact;
    private boolean singleton = true;

    public void setArtifact(ArtifactInfo artifact) {
        this.artifact = artifact;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public Object getObject() throws Exception {
        return artifact.newInstance();
    }

    public Class getObjectType() {
        return artifact.getKlass();
    }

    public boolean isSingleton() {
        return singleton;
    }
}
