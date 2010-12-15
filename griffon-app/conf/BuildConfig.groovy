griffon.project.dependency.resolution = {
    inherits("global")
    log "warn"
    repositories {
        griffonPlugins()
        griffonHome()
        griffonCentral()

        mavenCentral()
        mavenRepo 'http://repository.springsource.com/maven/bundles/release'
        flatDir name: 'springPluginLib', dirs: 'lib'
    }
    dependencies {
        def springVersion = '3.0.5.RELEASE'
        compile("org.springframework:org.springframework.aop:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.asm:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.aspects:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.core:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.beans:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.context:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.context.support:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.expression:$springVersion") { transitive = false }
        compile("org.springframework:org.springframework.transaction:$springVersion") { transitive = false }
        compile("org.grails:grails-spring:1.3.6") { transitive = false }
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
