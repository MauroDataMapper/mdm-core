ruleset {

    description '''
       Grails-CodeNarc Project RuleSet
        '''

    /*
     rulesets/basic.xml
     */
    AssertWithinFinallyBlock
    AssignmentInConditional
    BigDecimalInstantiation
    BitwiseOperatorInConditional
    BooleanGetBoolean
    BrokenNullCheck
    BrokenOddnessCheck
    ClassForName
    ComparisonOfTwoConstants
    ComparisonWithSelf
    ConstantAssertExpression
    ConstantIfExpression
    ConstantTernaryExpression
    DeadCode
    DoubleNegative
    DuplicateCaseStatement
    DuplicateMapKey
    DuplicateSetValue
    EmptyCatchBlock
    EmptyClass
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement
    EmptyInstanceInitializer
    // Grails controller methods are often empty.
    EmptyMethod {
        doNotApplyToClassNames = '*Controller'
    }
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement
    EqualsAndHashCode
    EqualsOverloaded
    ExplicitGarbageCollection
    ForLoopShouldBeWhileLoop
    HardCodedWindowsFileSeparator
    HardCodedWindowsRootDirectory
    IntegerGetInteger
    MultipleUnaryOperators
    RandomDoubleCoercedToZero
    RemoveAllOnSelf
    ReturnFromFinallyBlock
    ThrowExceptionFromFinallyBlock

    /*
     rulesets/braces.xml
     */
    //    ElseBlockBraces
    ForStatementBraces
    //    IfStatementBraces
    WhileStatementBraces

    // rulesets/comments.xml
    //    ClassJavadoc
    JavadocConsecutiveEmptyLines
    JavadocEmptyAuthorTag
    JavadocEmptyExceptionTag
    JavadocEmptyFirstLine
    JavadocEmptyLastLine
    JavadocEmptyParamTag
    JavadocEmptyReturnTag
    JavadocEmptySeeTag
    JavadocEmptySinceTag
    JavadocEmptyThrowsTag
    JavadocEmptyVersionTag
    JavadocMissingExceptionDescription
    JavadocMissingParamDescription
    JavadocMissingThrowsDescription

    /*
     rulesets/concurrency.xml
     /
    BusyWait
    DoubleCheckedLocking
    InconsistentPropertyLocking
    InconsistentPropertySynchronization
    NestedSynchronization
    StaticCalendarField
    StaticConnection
    StaticDateFormatField
    StaticMatcherField
    StaticSimpleDateFormatField
    SynchronizedMethod
    SynchronizedOnBoxedPrimitive
    SynchronizedOnGetClass
    SynchronizedOnReentrantLock
    SynchronizedOnString
    SynchronizedOnThis
    SynchronizedReadObjectMethod
    SystemRunFinalizersOnExit
    ThisReferenceEscapesConstructor
    ThreadGroup
    ThreadLocalNotStaticFinal
    ThreadYield
    UseOfNotifyMethod
    VolatileArrayField
    VolatileLongOrDoubleField
    WaitOutsideOfWhileLoop
    */

    /*
    rulesets/convention.xml
     */
    //    CompileStatic
    ConfusingTernary
    CouldBeElvis
    CouldBeSwitchStatement
    FieldTypeRequired
    HashtableIsObsolete
    IfStatementCouldBeTernary
    ImplicitClosureParameter
    //    ImplicitReturnStatement
    InvertedCondition
    InvertedIfElse
    LongLiteralWithLowerCaseL
    MethodParameterTypeRequired
    MethodReturnTypeRequired
    // Grails uses def methods a lot.
    //    NoDef
    NoDouble
    NoFloat
    NoJavaUtilDate
    NoTabCharacter
    ParameterReassignment
    PublicMethodsBeforeNonPublicMethods
    // Don't require static fields above instance fields for Grails domain classes.
    StaticFieldsBeforeInstanceFields {
        doNotApplyToFilesMatching = '.+/grails\\-app/domain/.+'
    }
//    StaticMethodsBeforeInstanceMethods // Static methods are better at the bottom
    TernaryCouldBeElvis
    TrailingComma
    VariableTypeRequired
    VectorIsObsolete

    /*
     rulesets/design.xml
     */
    //    AbstractClassWithPublicConstructor
    //    AbstractClassWithoutAbstractMethod
    AssignmentToStaticFieldFromInstanceMethod
    BooleanMethodReturnsNull
    //    BuilderMethodWithSideEffects
    CloneableWithoutClone
    CloseWithoutCloseable
    CompareToWithoutComparable
    ConstantsOnlyInterface
    //    EmptyMethodInAbstractClass
    FinalClassWithProtectedMember
    ImplementationAsType
    //    Instanceof
    LocaleSetDefault
    //    NestedForLoop
    PrivateFieldCouldBeFinal
    PublicInstanceField
    ReturnsNullInsteadOfEmptyArray
    ReturnsNullInsteadOfEmptyCollection
    SimpleDateFormatMissingLocale
    StatelessSingleton
    ToStringReturnsNull

    /*
     rulesets/dry.xml
     * /
    DuplicateListLiteral
    DuplicateMapLiteral
    DuplicateNumberLiteral
    DuplicateStringLiteral
    */

    /*
     rulesets/enhanced.xml
     */
    CloneWithoutCloneable
    JUnitAssertEqualsConstantActualValue
    MissingOverrideAnnotation
    UnsafeImplementationAsMap

    /*
    rulesets/exceptions.xml
     */
    CatchArrayIndexOutOfBoundsException
    CatchError
    CatchException
    CatchIllegalMonitorStateException
    CatchIndexOutOfBoundsException
    CatchNullPointerException
    CatchRuntimeException
    CatchThrowable
    ConfusingClassNamedException
    ExceptionExtendsError
    ExceptionExtendsThrowable
    ExceptionNotThrown
    MissingNewInThrowStatement
    ReturnNullFromCatchBlock
    SwallowThreadDeath
    ThrowError
    ThrowException
    ThrowNullPointerException
    ThrowRuntimeException
    ThrowThrowable

    /*
    rulesets/formatting.xml
     */
    BlankLineBeforePackage
    BlockEndsWithBlankLine
    BlockStartsWithBlankLine
    BracesForClass
    BracesForForLoop
    BracesForIfElse
    BracesForMethod
    BracesForTryCatchFinally
    //    ClassEndsWithBlankLine
    //    ClassStartsWithBlankLine
    ClosureStatementOnOpeningLineOfMultipleLineClosure
    ConsecutiveBlankLines
    //    FileEndsWithoutNewline
    //    Indentation
    LineLength {
        length = 150
    }
    MissingBlankLineAfterImports
    MissingBlankLineAfterPackage
    SpaceAfterCatch
    //    SpaceAfterClosingBrace
    SpaceAfterComma
    SpaceAfterFor
    SpaceAfterIf
    //    SpaceAfterOpeningBrace
    SpaceAfterSemicolon
    SpaceAfterSwitch
    SpaceAfterWhile
    //    SpaceAroundClosureArrow
    //    SpaceAroundMapEntryColon
    //    SpaceAroundOperator
    //    SpaceBeforeClosingBrace
    //    SpaceBeforeOpeningBrace
    TrailingWhitespace

    /*
    rulesets/generic.xml
     */
    IllegalClassMember
    IllegalClassReference
    IllegalPackageReference
    IllegalRegex
    IllegalString
    IllegalSubclass
    RequiredRegex
    RequiredString
    StatelessClass

    /*
    rulesets/grails.xml
     */
    GrailsDomainGormMethods
    //    GrailsDomainHasEquals
    //    GrailsDomainHasToString
    GrailsDomainReservedSqlKeywordName
    GrailsDomainStringPropertyMaxSize
    GrailsDomainWithServiceReference
    GrailsDuplicateConstraint
    GrailsDuplicateMapping
    GrailsMassAssignment
    GrailsPublicControllerMethod
    GrailsServletContextReference
    // Grails service classes have several more common fields that do not violate reentrancy.
    GrailsStatelessService {
        addToIgnoreFieldNames = 'grailsApplication,applicationContext,sessionFactory'
    }

    /*
    rulesets/groovyism.xml
     */
    AssignCollectionSort
    AssignCollectionUnique
    ClosureAsLastMethodParameter
    CollectAllIsDeprecated
    ConfusingMultipleReturns
    ExplicitArrayListInstantiation
    ExplicitCallToAndMethod
    ExplicitCallToCompareToMethod
    ExplicitCallToDivMethod
    ExplicitCallToEqualsMethod
    ExplicitCallToGetAtMethod
    ExplicitCallToLeftShiftMethod
    ExplicitCallToMinusMethod
    ExplicitCallToModMethod
    ExplicitCallToMultiplyMethod
    ExplicitCallToOrMethod
    ExplicitCallToPlusMethod
    ExplicitCallToPowerMethod
    ExplicitCallToPutAtMethod
    ExplicitCallToRightShiftMethod
    ExplicitCallToXorMethod
    ExplicitHashMapInstantiation
    ExplicitHashSetInstantiation
    ExplicitLinkedHashMapInstantiation
    ExplicitLinkedListInstantiation
    ExplicitStackInstantiation
    ExplicitTreeSetInstantiation
    GStringAsMapKey
//    GStringExpressionWithinString // This is used a lot in the json comparison strings
    //    GetterMethodCouldBeProperty
    GroovyLangImmutable
    UseCollectMany
    UseCollectNested

    /*
    rulesets/imports.xml
     */
    DuplicateImport
    ImportFromSamePackage
    ImportFromSunPackages
    //    MisorderedStaticImports
    NoWildcardImports
    UnnecessaryGroovyImport
    UnusedImport

    /*
    rulesets/jdbc.xml
     * /
    DirectConnectionManagement
    JdbcConnectionReference
    JdbcResultSetReference
    JdbcStatementReference
    */

    /*
    rulesets/junit.xml
     */
    ChainedTest
    CoupledTestCase
    JUnitAssertAlwaysFails
    JUnitAssertAlwaysSucceeds
    JUnitFailWithoutMessage
    JUnitLostTest
    JUnitPublicField
    JUnitPublicNonTestMethod
    JUnitPublicProperty
    JUnitSetUpCallsSuper
    //    JUnitStyleAssertions
    JUnitTearDownCallsSuper
    JUnitTestMethodWithoutAssert
    JUnitUnnecessarySetUp
    JUnitUnnecessaryTearDown
    JUnitUnnecessaryThrowsException
    SpockIgnoreRestUsed
    UnnecessaryFail
    UseAssertEqualsInsteadOfAssertTrue
    UseAssertFalseInsteadOfNegation
    UseAssertNullInsteadOfAssertEquals
    UseAssertSameInsteadOfAssertTrue
    UseAssertTrueInsteadOfAssertEquals
    UseAssertTrueInsteadOfNegation

    /*
    rulesets/logging.xml
     */
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    LoggingSwallowsStacktrace
    MultipleLoggers
    PrintStackTrace
    Println
    SystemErrPrint
    SystemOutPrint

    /*
    rulesets/naming.xml
     */
    AbstractClassName
    ClassName
    ClassNameSameAsFilename
    ClassNameSameAsSuperclass
    ConfusingMethodName
    FactoryMethodName
    FieldName
    InterfaceName
    InterfaceNameSameAsSuperInterface
    // Do not not complain about non-standard method names in Spec classes.
    // Admittedly this will also skip checking “regular” helper methods in those classes.
    MethodName {
        doNotApplyToClassNames = '*Spec,*Specification'
    }
    ObjectOverrideMisspelledMethodName
    PackageName
    PackageNameMatchesFilePath
    ParameterName
    PropertyName
    // This will be the default in CodeNarc 2.0.
    VariableName {
        finalRegex = null
    }

    /*
    rulesets/security.xml
     */
    FileCreateTempFile
    InsecureRandom
    JavaIoPackageAccess
    NonFinalPublicField
    NonFinalSubclassOfSensitiveInterface
    ObjectFinalize
    PublicFinalizeMethod
    SystemExit
    UnsafeArrayDeclaration

    /*
    rulesets/serialization.xml
     */
    EnumCustomSerializationIgnored
    SerialPersistentFields
    SerialVersionUID
    SerializableClassMustDefineSerialVersionUID

    /*
     rulesets/size.xml
     */
    AbcMetric // Requires the GMetrics jar
    ClassSize
    // CrapMetric // Requires the GMetrics jar and a Cobertura coverage file
    CyclomaticComplexity // Requires the GMetrics jar
    MethodCount
    MethodSize
    NestedBlockDepth
    //    ParameterCount

    /*
    rulesets/unnecessary.xml
     */
    AddEmptyString
    ConsecutiveLiteralAppends
    ConsecutiveStringConcatenation
    UnnecessaryBigDecimalInstantiation
    UnnecessaryBigIntegerInstantiation
    // Spec methods may use boolean expressions to specify validation criteria, e.g.
    // where:
    //        code    || result
    //        'EN'    || 'en'
    UnnecessaryBooleanExpression {
        doNotApplyToClassNames = '*Spec,*Specification'
    }
    UnnecessaryBooleanInstantiation
    UnnecessaryCallForLastElement
    UnnecessaryCallToSubstring
    UnnecessaryCast
    UnnecessaryCatchBlock
    UnnecessaryCollectCall
    UnnecessaryCollectionCall
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass
    UnnecessaryDoubleInstantiation
    UnnecessaryElseStatement
    UnnecessaryFinalOnPrivateMethod
    UnnecessaryFloatInstantiation
    UnnecessaryGString
    UnnecessaryGetter
    UnnecessaryIfStatement
    UnnecessaryInstanceOfCheck
    UnnecessaryInstantiationToGetClass
    UnnecessaryIntegerInstantiation
    UnnecessaryLongInstantiation
    UnnecessaryModOne
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf
    UnnecessaryObjectReferences
    UnnecessaryOverridingMethod
    UnnecessaryPackageReference
    UnnecessaryParenthesesForMethodCallWithClosure
    UnnecessaryPublicModifier
    UnnecessaryReturnKeyword
    UnnecessarySafeNavigationOperator
    UnnecessarySelfAssignment
    UnnecessarySemicolon
    UnnecessarySetter
    UnnecessaryStringInstantiation
    UnnecessarySubstring
    UnnecessaryTernaryExpression
    UnnecessaryToString
    UnnecessaryTransientModifier

    /*
    rulesets/unused.xml
     */
    UnusedArray
    //UnusedMethodParameter // Our code has a lot of these due to the way plugins are handled and used
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
    UnusedVariable


}