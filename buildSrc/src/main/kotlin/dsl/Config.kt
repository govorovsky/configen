package dsl

import ru.mail.condef.dsl.*
import java.util.regex.Pattern


val rootDefinition = definition(

    "field_root" of StringType()
            withDescription "Some root field"
            withDefault null,

    "feature" of FreeObjectType()
            withDefault empty()
            restrictedBy definition(

        "isSomeFeatureEnabled" of BoolType()
                withDescription "Some feature flag"
                withDefault false,

        "field1" of IntegerType()
                withDescription "Some odd int value"
                withAllowedValues listOf(1, 3, 5, 7)
                withDefault 1,

        "field2" of StringType()
                withDescription "Some string value with regexp validation"
                withAbsenceHandler requiredHandler()
                withAllowedPattern Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d"),

        "field3" of ArrayType(StringType())
                withDescription "Some field with allowed values validation"
                withAllowedValuesInArray listOf("val1", "val2", "val3")
                withDefault emptyList(),

        "nestedConfig" of FreeObjectType()
                withDefault empty()
                restrictedBy definition(

            "nestedField1" of IntegerType()
                    withDescription "Some nestedField"
                    withAllowedRange 10..20
                    withDefault 15,

            "nestedField2" of StringType()
                    withDescription "some desc"
                    withDefault "some default"
        )
    )
)

