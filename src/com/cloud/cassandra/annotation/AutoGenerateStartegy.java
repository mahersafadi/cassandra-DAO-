package com.cloud.cassandra.annotation;
/**
 * 
 * @author maher.safadi@gmail.com
 * Just support auto increment strategy to apply that you must create a table called tbls
   create table tbls(column_family text PRIMARY KEY,id bigint);
   it will save the current increment for each table
 */
public enum AutoGenerateStartegy {
	none,
	auto_inc
}
