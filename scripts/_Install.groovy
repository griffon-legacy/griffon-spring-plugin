//
// This script is executed by Griffon after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
//
//    ant.mkdir(dir:"${basedir}/griffon-app/jobs")
//

// check to see if we already have a SpringGriffonAddon
ConfigSlurper configSlurper1 = new ConfigSlurper()
def slurpedBuilder1 = configSlurper1.parse(new File("$basedir/griffon-app/conf/Builder.groovy").toURL())
boolean addonIsSet1
slurpedBuilder1.each() { prefix, v ->
    v.each { builder, views ->
        addonIsSet1 = addonIsSet1 || 'SpringGriffonAddon' == builder
    }
}

if (!addonIsSet1) {
    println 'Adding SpringGriffonAddon to Builders.groovy'
    new File("$basedir/griffon-app/conf/Builder.groovy").append('''
root.'SpringGriffonAddon'.addon=true
''')
}

ant.mkdir(dir: "${basedir}/src/spring")

def checkConfigOptionIsSet = { where, option ->
   boolean optionIsSet = false
   where.each { key, value ->
       optionIsSet = optionIsSet || option == key
   }
   optionIsSet
}

def appConfig = configSlurper.parse(new File("${basedir}/griffon-app/conf/Application.groovy").toURL())
if(!checkConfigOptionIsSet(appConfig, "griffon.basic_injection.disable")) {
    new File("${basedir}/griffon-app/conf/Application.groovy").append("""
griffon.basic_injection.disable = true
""")
}
