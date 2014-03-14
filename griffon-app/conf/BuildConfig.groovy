griffon.project.dependency.resolution = {
    inherits("global")
    log "warn"
    repositories {
        griffonHome()
        jcenter()
        mavenRepo 'http://repository.springsource.com/maven/bundles/release'
    }
    dependencies {
        compile("org.springframework:spring-aop:$springVersion",
                /*"org.springframework:org.springframework.asm:$springVersion",*/
                "org.springframework:spring-aspects:$springVersion",
                "org.springframework:spring-core:$springVersion",
                "org.springframework:spring-beans:$springVersion",
                "org.springframework:spring-context:$springVersion",
                "org.springframework:spring-context-support:$springVersion",
                "org.springframework:spring-expression:$springVersion",
                "org.springframework:spring-tx:$springVersion",
                "org.springframework:spring-instrument:$springVersion",
                'cglib:cglib-nodep:2.2',
                'aopalliance:aopalliance:1.0',
                'org.aspectj:aspectjweaver:1.6.10',
                'org.aspectj:aspectjrt:1.6.10',
                'org.grails:grails-spring:2.3.7') {
            transitive = false 
        }
    }
}

griffon {
    doc {
        logo = '<a href="http://griffon.codehaus.org" target="_blank"><img alt="The Griffon Framework" src="../img/griffon.png" border="0"/></a>'
        sponsorLogo = "<br/>"
        footer = "<br/><br/>Made with Griffon (@griffon.version@)"
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }

    error 'org.codehaus.griffon',
          'org.springframework',
          'org.apache.karaf',
          'groovyx.net'
    warn  'griffon'
}
