includeTargets << griffonScript("Compile")

eventCompileEnd = { type ->
    if(type != "source") return
    def spring = "${basedir}/src/spring"
    def springdir = new File(spring)
    if(!springdir.exists()) return

    ant.mkdir(dir: classesDirPath)

    if(sourcesUpToDate("${basedir}/src/spring", classesDirPath, ".groovy")) return

    ant.echo(message: "Compiling Spring resources to $classesDirPath")
    try {
        String classpathId = "griffon.compile.classpath"
        ant.groovyc(destdir: classesDirPath,
                    classpathref: classpathId) {
            src(path: spring)
            javac(classpathref: classpathId)
        }
    }
    catch (Exception e) {
        event("StatusFinal", ["Compilation error: ${e.message}"])
        ant.fail(message: "Could not compile Spring resources: " + e.class.simpleName + ": " + e.message)
    }
}

sourcesUpToDate = { src, dest, srcsfx = ".java", destsfx = ".class" ->
    def srcdir = new File(src)
    def destdir = new File(dest)
    def skipIt = new RuntimeException()
    try {
        srcdir.eachFileRecurse { sf ->
            if(sf.isDirectory()) return
            if(!sf.toString().endsWith(srcsfx)) return
            def target = new File(destdir.toString() + sf.toString().substring(srcdir.toString().length()) - srcsfx + destsfx)
            if(!target.exists()) throw skipIt
            if(sf.lastModified() > target.lastModified()) throw skipIt
        }
    } catch(x) {
       if(x == skipIt) return false
       throw x
    }
    return true
}

def eventClosure1 = binding.variables.containsKey('eventCopyLibsEnd') ? eventCopyLibsEnd : {jardir->}
eventCopyLibsEnd = { jardir ->
    eventClosure1(jardir)
    if (!isPluginProject) {
        ant.fileset(dir:"${getPluginDirForName('spring').file}/lib/", includes:"*.jar").each {
            griffonCopyDist(it.toString(), jardir)
        }
    }
}
