package dev.bannmann.mandor.annotations;

import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a variable that only exists to turn a switch with otherwise-void {@code case} code blocks into a switch
 * expression checked for exhaustiveness by the compiler.
 * <pre>{@code @ExhaustiveSwitch Runnable handler = switch (someEnumValue)
 * {
 *     case FOO -> () -> {
 *         // do something
 *     };
 *     case BAR -> () -> {
 *         // do something else
 *     };
 * };
 * handler.run();}</pre>
 */
@Target(LOCAL_VARIABLE)
@Retention(CLASS)
public @interface ExhaustiveSwitch
{
}
