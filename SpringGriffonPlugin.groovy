/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class SpringGriffonPlugin {
    // the plugin version
    String version = '1.1.0'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.1.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-spring-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Spring framework libraries and helpers'
    // accepts Markdown syntax. See http://daringfireball.net/projects/markdown/ for details
    String description = '''
Enables the usage of the [Spring framework][1] as Dependency Injection provider, and much more!

Usage
-----
The Spring plugin will manage all relationships between artifacts by autowiring them by name.
It relies on [Grail's BeanBuilder][2] to get the job done. You may place additional beans to
be wired in a script named `springbeans.groovy` and place it under `src/spring`. This script
should contain bean definitions as expected by BeanBuilder.

You can also create addons that are Spring aware. Follow these steps to create such an addon

1. Create a new plugin project

        griffon create-plugin foo

2. Change into the newly created plugin's directory and create an addon

        griffon create-addon foo

3. Edit your plugin's descriptor by declaring a dependency on the spring plugin

        class FooGriffonPlugin {
            Map dependsOn = [spring: '1.0']
            ...
        }

4. Your addon must contain at least one of the following properties

        class FooGriffonAddon {
            ...
            def doWithSpring = {
                // place bean definitions here
                // this closure will be called in the
                // context of a BeanBuilder instance
            }

            def whenSpringReady = { app ->
                // called after all addons have been initialized
                // and all bean contributions have been processed
            }
            ...
        }

Addons that do not define a hard dependency on the Spring plugin can still contribute beans,
either by placing bean definitions inside the `doWithSpring` property (might require access
to Spring classes like factory beans) or inside `griffon-app/conf/metainf/spring/springbeans.groovy`
(using the same format as `src/spring/springbeans.groovy`). The latter form is preferred for bean
contributions that require spring classes which are not available in the plugin's classpath at buildtime.

### Exposed Beans

The following beans are automatically configured in the ApplicationContext

| Name            | Type                            | Notes                           |
| ----------------| ------------------------------- | ------------------------------- |
| application     | griffon.core.GriffonApplication | current application instance    |
| appConfig       | groovy.util.ConfigObject        | the application's configuration |
| artifactManager | griffon.core.ArtifactManager    |                                 |
| addonManager    | griffon.core.AddonManager       |                                 |
| mvcGroupManager | griffon.core.MVCGroupManager    |                                 |
| uiThreadManager | griffon.core.UIThreadManager    |                                 |

Additionally, all Model, View, Controller classes will see their respective GriffonClass exposed
following a naming convention. If the model class is `sample.SampleModel` then it's griffonClass
will be exposed as `sampleModelClass`.

### Events

The following events will be triggered by this addon

 * WithSpringStart[app, applicationContext] - triggered before bean contributions from addons are processed
 * WithSpringEnd[app, applicationContext] - triggered after bean contributions from addons have been processed
 * WhenSpringReadyStart[app, applicationContext] - triggered before addons tweak the applicationContext
 * WhenSpringReadyEnd[app, applicationContext] - triggered after addons have had a chance to tweak the applicationContext


[1]: http://www.springframework.org
[2]: http://www.grails.org/Spring+Bean+Builder
'''
}
