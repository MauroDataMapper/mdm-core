
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '1.0.0-SNAPSHOT')
        classNode.putNodeMetaData('projectName', 'mdm-plugin-profile')
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
