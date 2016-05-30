package com.cloud.cassandra.annotation;

/**
 * maher.safadi@gmail.com
 * This annotation interface is for ORM Object's field
 * name and type are mandatory and they are specified in Types enum
 * if the field is partition key, then use partition key
 * all other keys in primary key between the left one and right one are named as composite keys
 * the final key in left is named slice key
 * example
 * primary key(f1,f2,......, fn, fm)
 * f1 is partition key
 * f2.... fn are composite keys
 * fm is the slice key
 * insert enforces you to set all key parts
 * query by partition needs to partition key
 * query by composite needs to partition key and any composite key
 * query by slice needs to partition key and any all composite keys and slice key from, to
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
	String name() default "";
	/**
	 * Specify the type of field and the got from types
	 * Supported are: boolean, int, bigint, float, varchar and text
	 * @return
	 */
	Types type();
	
	/**
	 * Can be expression
	 * Supported expressions are 
	 * 		#{systime} to get server time
	 * 		#{gmt} to set GMT time
	 * @return
	 */
	String defaultVal() default "";
	
	/**
	 * This annotation is set if you want to query the table using = and it must
	 * be used just for composite keys and one key tables.
	 * it must not be the last field in the primary key
	 * 
	 * @return true means it is slice key
	 */
	boolean partionKey() default false;

	/**
	 * This annotation is set if you want to query the table using = and it must
	 * be used just for composite keys not one key tables.
	 * it must not be the last field in the primary key
	 * 
	 * @return true means it is slice key
	 */
	boolean compositKey() default false;

	/**
	 * This annotation is set if you want to query the table using >= or <=
	 * operations it must be the last field in the primary key
	 * 
	 * @return true means it is slice key
	 */
	boolean sliceKey() default false;

	/**
	 * 
	 * @return This annotation is used if you want to deny the pass the object
	 *         to cassandra data base if it is empty
	 */
	boolean required() default false;

	/**
	 * 
	 * @return If you want the ORM to link the objects add this annotation with
	 *         true example student a school are tables, school has student_id
	 *         field In ORM: School has student field from type Student to link
	 *         objects you must do the following in Student,in id field, add
	 *         annotation relationKey=true in school, in student field, add
	 *         annotation lazy = false
	 */
	boolean lazy() default true;

	/**
	 * 
	 * @return If you want the ORM to link the objects add this annotation with
	 *         true example student a school are tables, school has student_id
	 *         field In ORM: School has student field from type Student to link
	 *         objects you must do the following in Student,in id field, add
	 *         annotation relationKey=true in school, in student field, add
	 *         annotation lazy = false
	 */
	boolean relationKey() default false;

	/**
	 * 
	 * @return If you want the field to auto generate add this annotation with
	 *         auto_inc
	 */
	AutoGenerateStartegy generate() default AutoGenerateStartegy.none;
}
