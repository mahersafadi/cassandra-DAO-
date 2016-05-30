package com.cloud.cassandra.annotation;

/**
 * @author maher.safadi@gmail.com
 * This annotation is used to specify what column family you want to link with
 * Just specify the name of it 
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ColumnFamily {
	/**
	 * This annotation is used to specify what column family you want to link
	 * with Just specify the name of it
	 * 
	 * @return
	 */
	String name();
}
