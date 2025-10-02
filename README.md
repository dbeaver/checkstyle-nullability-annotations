# Checkstyle Nullability Annotations

A custom check for [Checkstyle](https://checkstyle.sourceforge.io/) that ensures
that field, method parameter and return types are annotated with
annotations specified in the check's configuration.

At the moment, check doesn't recognize annotations specified at the class level.

## Supported tokens

- `CTOR_DEF` for checking constructor parameter types
- `METHOD_DEF` for checking method parameter and return types
- `VARIABLE_DEF` for checking non-local field types

## Supported properties

| Name                  | Type       | Default                                  | Description                                                                   |
|-----------------------|------------|------------------------------------------|-------------------------------------------------------------------------------|
| `tokens`              | `String[]` | `CTOR_DEF`, `METHOD_DEF`, `VARIABLE_DEF` | Any combination of tokens above                                               |
| `nonnullClassNames`   | `String[]` | _Not specified, mandatory_               | Fully qualified class names that should be treated as "Nonnull" annotations.  |
| `nullableClassNames`  | `String[]` | _Not specified, mandatory_               | Fully qualified class names that should be treated as "Nullable" annotations. |
| `checkLocalVariables` | `boolean`  | `true`                                   | Whether local variables should be checked as well                             |

## Sample module configuration

```xml
<module name="NullabilityAnnotationsCheck">
    <property name="tokens" value="CTOR_DEF, METHOD_DEF, VARIABLE_DEF"/>
    <property name="nonnullClassNames" value="org.jspecify.annotations.NonNull"/>
    <property name="nullableClassNames" value="org.jspecify.annotations.Nullable"/>
    <property name="checkLocalVariables" value="false"/>
</module>
```