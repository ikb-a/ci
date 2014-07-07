package edu.toronto.cs.se.ci;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Fulfils {
	String[] value();
	Class<?> from() default Void.class;
	Class<?> to() default Void.class;
}
