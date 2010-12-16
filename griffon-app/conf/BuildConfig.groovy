griffon.project.dependency.resolution = {
    inherits("global")
    log "warn"
    repositories {
        griffonPlugins()
        griffonHome()
        griffonCentral()

        mavenCentral()
        mavenRepo 'http://repository.springsource.com/maven/bundles/release'
    }
    dependencies {
        def springVersion = '3.0.5.RELEASE'
        compile("org.springframework:org.springframework.aop:$springVersion",
                "org.springframework:org.springframework.asm:$springVersion",
                "org.springframework:org.springframework.aspects:$springVersion",
                "org.springframework:org.springframework.core:$springVersion",
                "org.springframework:org.springframework.beans:$springVersion",
                "org.springframework:org.springframework.context:$springVersion",
                "org.springframework:org.springframework.context.support:$springVersion",
                "org.springframework:org.springframework.expression:$springVersion",
                "org.springframework:org.springframework.transaction:$springVersion",
                "org.springframework:org.springframework.instrument:$springVersion",
                'cglib:cglib-nodep:2.2',
                'aopalliance:aopalliance:1.0',
                'org.aspectj:aspectjweaver:1.6.10',
                'org.aspectj:aspectjrt:1.6.10',
                'org.grails:grails-spring:1.3.6') {
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

griffon.jars.destDir='target/addon'
