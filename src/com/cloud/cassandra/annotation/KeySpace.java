package com.cloud.cassandra.annotation;
/****
 * @author maher.safadi@gmail.com
 * this annotation is used to connect the session to the correct key-space
 * Just specify the name of the keyspace
 */
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeySpace {
	/**
	 * this annotation is used to connect the session to the correct key-space
	 * Just specify the name of the keyspace
	 * @return
	 */
	String name();
}
