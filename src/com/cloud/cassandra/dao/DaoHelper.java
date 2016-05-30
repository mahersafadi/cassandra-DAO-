package com.cloud.cassandra.dao;
/**
 * @author maher
 * This class has the major commons things used in my DAO layer
 * 
 */
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.cassandra.annotation.*;
import com.cloud.cassandra.exceptions.DaoException;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;


public abstract class DaoHelper <T>{
	protected Logger logger = LoggerFactory
			.getLogger(DaoHelper.class);
	protected static final String clusterPath = "127.0.0.1";

	protected static class ClusterObject {
		public static Cluster cluster = Cluster.builder()
				.addContactPoint(clusterPath).build();
	}

	protected static class SensitiveData {
		public static Map<String, ColumnFamilyMetaData> meta = new ConcurrentHashMap<String, ColumnFamilyMetaData>();
	}

	protected static class ColumnMetaData {
		Column c;
		Object defaultValue;
	}

	protected static class ColumnFamilyMetaData {
		public ColumnFamily cf;
		public KeySpace keySpace;
		public Map<String, ColumnMetaData> columns = new java.util.HashMap<String, ColumnMetaData>();
	}

	protected ColumnFamilyMetaData getMetaFromOrmObject(Class<?> ormclass) throws DaoException {
		ColumnFamilyMetaData cfmd = null;
		try {
			String name = ormclass.getSimpleName();
			if (SensitiveData.meta.containsKey(name))
				cfmd = SensitiveData.meta.get(name);
			else {
				String ormClassName = ormclass.getSimpleName();
				Annotation[] a = ormclass.getAnnotations();
				ColumnFamily currCF = null;
				KeySpace currKeySpace = null;
				for (int i = 0; i < a.length; i++) {
					Annotation currentAnnotation = a[i];
					if (currentAnnotation instanceof KeySpace) {
						currKeySpace = (KeySpace) currentAnnotation;
					} else if (currentAnnotation instanceof ColumnFamily) {
						currCF = (ColumnFamily) currentAnnotation;
					}
				}
				if (currCF == null || currKeySpace == null)
					throw new DaoException(DaoException.META,
							"Keyspace or column family are not set as annotations in ORM class "
									+ ormClassName);
				cfmd = new ColumnFamilyMetaData();
				cfmd.keySpace = currKeySpace;
				cfmd.cf = currCF;
				Field[] fs = ormclass.getDeclaredFields();
				for (int i = 0; i < fs.length; i++) {
					Field currField = fs[i];
					Annotation[] _a = currField.getAnnotations();
					for (int k = 0; k < _a.length; k++) {
						Annotation currAnnot = _a[k];
						if (currAnnot instanceof Column) {
							ColumnMetaData cmd = new ColumnMetaData();
							cmd.c = (Column) currAnnot;
							cfmd.columns.put(currField.getName(), cmd);
						}
					}
				}
				SensitiveData.meta.put(name, cfmd);
			}
			// refresh default data
			for (Map.Entry<String, ColumnMetaData> entry : cfmd.columns
					.entrySet()) {
				if (entry.getValue().c.defaultVal() != null
						&& !"".equals(entry.getValue().c.defaultVal()))
					entry.getValue().defaultValue = this.fillDefaultValue(entry
							.getValue().c.defaultVal());
				else
					entry.getValue().defaultValue = null;
			}
			return cfmd;
		} catch (Exception ex) {
			if (ex instanceof DaoException) {
				throw (DaoException) ex;
			} else {
				throw new DaoException(DaoException.META, ex.getMessage());
			}
		}
	}

	protected ColumnFamilyMetaData getMetaFromOrmObject(Object ormObject)
			throws DaoException {
		if(ormObject == null)
			throw new DaoException(DaoException.META, "ORM object is mandatory can not be null");
		return getMetaFromOrmObject(ormObject.getClass());
	}

	protected void primaryCheck(ColumnFamilyMetaData meta, T t)
			throws DaoException {
		if (meta == null)
			throw new DaoException(DaoException.PREPARE, "Meta is empty");
		if (t == null)
			throw new DaoException(DaoException.PREPARE, "ORM Object is null");
	}

	protected void checkPartionKeyExisting(Session session,
			ColumnFamilyMetaData meta, T t) throws DaoException {
		boolean partionKey = false;
		for (Map.Entry<String, ColumnMetaData> entry : meta.columns.entrySet()) {
			Column col = entry.getValue().c;
			if (col.partionKey()) {
				partionKey = true;
				Field field = null;
				try {
					field = t.getClass().getDeclaredField(entry.getKey());
					field.setAccessible(true);
					// process default value
					Object value = field.get(t);
					if (value == null) {
						throw new DaoException(DaoException.PREPARE,
								"Partion key is exist put empty");
					}
				} catch (Exception ex) {

				} finally {
					if (field != null)
						field.setAccessible(false);
				}
			}
		}
		if (!partionKey) {
			throw new DaoException(DaoException.PREPARE, "No partion key found");
		}
	}

	protected T prepareForInsertOrUpdate(Session session,
			ColumnFamilyMetaData meta, T t, boolean isInsert)
			throws DaoException {
		if (meta == null)
			throw new DaoException(DaoException.PREPARE, "Meta is empty");
		if (t == null)
			throw new DaoException(DaoException.PREPARE, "ORM Object is null");
		for (Map.Entry<String, ColumnMetaData> entry : meta.columns.entrySet()) {
			Column col = entry.getValue().c;
			Field field = null;
			try {
				field = t.getClass().getDeclaredField(entry.getKey());
				field.setAccessible(true);
				// process default value
				Object value = field.get(t);
				if (value == null) {
					if (isInsert) {
						// Process auto-generate
						if (col.generate() == AutoGenerateStartegy.auto_inc) {
							Long l = this.getNext(session, col.name());
							if (col.type() == Types.BigInt)
								field.set(t, l);
							else if (col.type() == Types.Int)
								field.set(t, l.intValue());
						}
						Object _defVal = entry.getValue().defaultValue;
						if (_defVal != null && !"".equals(col.defaultVal())
								&& entry.getValue().defaultValue != null) {
							switch (col.type()) {
							case BigInt:
								field.set(
										t,
										new Long(entry.getValue().defaultValue
												.toString()));
								break;
							case Int:
								field.set(
										t,
										new Integer(
												entry.getValue().defaultValue
														.toString()));
								break;
							case Float:
								field.set(
										t,
										new Float(entry.getValue().defaultValue
												.toString()));
								break;
							default:
								field.set(t, entry.getValue().defaultValue);
							}
						}
						// it is null without default value and it is also
						// required
						if (col.required()) {
							throw new DaoException(
									DaoException.PREPARE,
									new StringBuilder("During prepare ")
											.append(t.getClass().getName())
											.append(".")
											.append(field.getName())
											.append(" is required but the value is not set")
											.toString());
						}
					}
					value = field.get(t);
					if ((value == null) && (col.compositKey() || col.partionKey() || col.sliceKey())) {
						throw new DaoException(
								DaoException.PREPARE,
								new StringBuilder(
										"Row key items must not be empty, ")
										.append(field.getName())
										.append("Is a part of row key but has no value")
										.toString());
					}
				}

			} catch (NoSuchFieldException e) {
				e.printStackTrace();
				throw new DaoException(DaoException.PREPARE, new StringBuilder(
						"Field ").append(entry.getKey())
						.append(" is not exist in ")
						.append(t.getClass().getName()).toString()

				);
			} catch (SecurityException e) {
				e.printStackTrace();
				throw new DaoException(DaoException.PREPARE, e.getMessage());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw new DaoException(DaoException.PREPARE, new StringBuilder(
						"Field ").append(entry.getKey())
						.append(" passed invalid data ")
						.append(t.getClass().getName()).append(", field type:")
						.append(entry.getValue().c.type()).toString());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new DaoException(DaoException.PREPARE, new StringBuilder(
						"Field ").append(entry.getKey())
						.append(" illegal access data ")
						.append(t.getClass().getName()).toString());
			}
			if (field != null)
				field.setAccessible(false);
		}
		return t;
	}

	protected Long getNext(Session session, String columnFamily)
			throws DaoException {
		try {
			Long l = null;
			StringBuilder sb = new StringBuilder();
			try {
				ResultSet r = session.execute(new StringBuilder(
						"select id from tbls where column_family='")
						.append(columnFamily).append("'").toString());
				java.util.List<Row> row = r.all();
				Row firstRow = row.get(0);
				l = firstRow.getLong("id");
				// sb.append("update _tbls_ set id = ").append(++l)
				// .append(" where column_family='")
				// .append(columnFamily).append("'");
			} catch (Exception ex) {
				l = 0L;
			}
			sb.append("insert into tbls (column_family,id) values('")
					.append(columnFamily).append("', ").append(++l).append(")");
			session.execute(sb.toString());
			return l;
		} catch (Exception ex) {
			throw new DaoException(DaoException.AUTOINC,
					"Maybe _tbl_(columns_family varchar primary key, id long)is not created"
							+ ex.getMessage());
		}
	}

	protected Object fillDefaultValue(String frmt) {
		// systim
		if ("#{systim}".equals(frmt))
			return System.currentTimeMillis();

		// gmt
		else if ("#{gmt}".equals(frmt)) {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			return cal.getTimeInMillis();
		}
		return frmt;
	}
	
	protected List<T> convert(Class<T> clazz, ColumnFamilyMetaData meta, List<Row> data) throws DaoException{
		if(clazz == null)
			throw new DaoException(DaoException.QUERY, "class is mandatory, it is null now");
		List<T> l = new ArrayList<T>();
		if(data == null)
			return l;
		for(int i=0; i<data.size(); i++){
			Row row = data.get(i);
			Field f = null;
			try{
				T t = clazz.newInstance();
				for(Map.Entry<String, ColumnMetaData> entry: meta.columns.entrySet()){
					f = clazz.getDeclaredField(entry.getKey());
					f.setAccessible(true);
					switch(entry.getValue().c.type()){
					case BigInt:
						f.set(t, row.getLong(entry.getValue().c.name()));
						break;
					case Int:
						f.set(t, row.getInt(entry.getValue().c.name()));
						break;
					case Float:
						f.set(t, row.getFloat(entry.getValue().c.name()));
						break;
					default:
						f.set(t, row.getString(entry.getValue().c.name()));
						break;
					}
					f.setAccessible(false);
				}
				l.add(t);
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			finally{
				if(f != null)
					f.setAccessible(false);
			}
		}
		return l;
	}
}
