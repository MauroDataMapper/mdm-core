ruleset {
    description 'Grails-CodeNarc Project RuleSet'

    ruleset('rulesets/basic.xml')
    ruleset('rulesets/braces.xml') {
        'ElseBlockBraces' {enabled = false}
        'IfStatementBraces' {enabled = false}
    }
    // ruleset('rulesets/concurrency.xml')
    ruleset('rulesets/convention.xml') {
        'NoDef' {enabled = false}
    }
    ruleset('rulesets/design.xml') {
        'AbstractClassWithoutAbstractMethod' {enabled = false}
        'AbstractClassWithPublicConstructor' {enabled = false}
        'EmptyMethodInAbstractClass' {enabled = false}
        'BuilderMethodWithSideEffects' {enabled = false}
        'Instanceof' {enabled = false}
        'NestedForLoop' {enabled = false}
    }
    // ruleset('rulesets/dry.xml')
    ruleset('rulesets/exceptions.xml')
    ruleset('rulesets/formatting.xml') {
        'FileEndsWithoutNewline' {enabled = false}
        'LineLength' {enabled = false}
        'SpaceBeforeOpeningBrace' {enabled = false}
        'SpaceAfterOpeningBrace' {enabled = false}
        'SpaceBeforeClosingBrace' {enabled = false}
        'SpaceAfterClosingBrace' {enabled = false}
        'SpaceAroundClosureArrow' {enabled = false}
        'SpaceAroundMapEntryColon' {enabled = false}
        'SpaceAroundOperator' {enabled = false}
        'ClassJavadoc' {enabled = false}
        'Indentation' {enabled = false}
    }
    ruleset('rulesets/generic.xml')
    ruleset('rulesets/grails.xml') {
        'GrailsDomainHasEquals' {enabled = false}
        'GrailsDomainHasToString' {enabled = false}
    }
    ruleset('rulesets/groovyism.xml') {
        'GetterMethodCouldBeProperty' {enabled = false}
    }
    ruleset('rulesets/imports.xml') {
        'MisorderedStaticImports' {enabled = false}
    }
    // ruleset('rulesets/jdbc.xml')
    ruleset('rulesets/junit.xml') {
        'JUnitStyleAssertions' {enabled = false}
    }
    ruleset('rulesets/logging.xml')
    // ruleset('rulesets/naming.xml')
    // ruleset('rulesets/security.xml')
    // ruleset('rulesets/serialization.xml')
    // ruleset('rulesets/size.xml')
    ruleset('rulesets/unnecessary.xml')
    ruleset('rulesets/unused.xml')
}