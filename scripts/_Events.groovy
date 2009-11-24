

def eventClosure1 = binding.variables.containsKey('eventCopyLibsEnd') ? eventCopyLibsEnd : {jardir->}
eventCopyLibsEnd = { jardir ->
    eventClosure1(jardir)
    if (!isPluginProject) {
        ant.fileset(dir:"${getPluginDirForName('spring').file}/lib/", includes:"*.jar").each {
            griffonCopyDist(it.toString(), jardir)
        }
    }
}

