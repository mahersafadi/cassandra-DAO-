package com.cloud.cassandra.dao;

/**
 * This is DAO implementer for cassandra and based on it, insert,update, delete and query are supported against cassandra
 */
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.cassandra.DaoObject;
import com.cloud.cassandra.annotation.Types;
import com.cloud.cassandra.exceptions.DaoException;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class DaoObjectImpl<T> extends DaoHelper<T> implements DaoObject<T> {
	private Logger logger = LoggerFactory.getLogger(DaoObjectImpl.class);
	private Session session = null;
	private Session getSession(ColumnFamilyMetaData meta){
		if(session == null){
			session = ClusterObject.cluster.connect(meta.keySpace.name());
		}
		return session;
	}
	
	@Override
	public T insert(T t) throws DaoException {
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(t);
		Session session = getSession(meta);
		primaryCheck(meta, t);
		t = prepareForInsertOrUpdate(session, meta, t, true);
		// Create insert statement
		StringBuilder names = new StringBuilder("(");
		StringBuilder values = new StringBuilder("(");
		Iterator<String> itr = meta.columns.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			ColumnMetaData col = meta.columns.get(key);
			names.append(col.c.name());
			Field f = null;
			try {
				f = t.getClass().getDeclaredField(key);
				f.setAccessible(true);
				if (col.c.type() == Types.Int || col.c.type() == Types.Float
						|| col.c.type() == Types.BigInt)
					values.append(f.get(t));
				else
					values.append("'").append(f.get(t)).append("'");
				if (itr.hasNext()) {
					names.append(",");
					values.append(",");
				}
			} catch (Exception e) {
				throw new DaoException(DaoException.PREPARE,
						"During create insert statement, msg:" + e.getMessage());
			} finally {
				if (f != null)
					f.setAccessible(false);
			}
		}
		names.append(")");
		values.append(")");
		String stmt = new StringBuilder("insert into ").append(meta.cf.name())
				.append(names).append(" values ").append(values).toString();
		logger.info(stmt);
		session.execute(stmt);
		return t;
	}

	@Override
	public T update(T t) throws DaoException {
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(t);
		Session session = getSession(meta);
		primaryCheck(meta, t);
		t = prepareForInsertOrUpdate(session, meta, t, false);
		// Create update statement
		StringBuilder names = new StringBuilder("(");
		StringBuilder values = new StringBuilder("(");
		Iterator<String> itr = meta.columns.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			ColumnMetaData col = meta.columns.get(key);
			Field f = null;
			try {
				f = t.getClass().getDeclaredField(key);
				f.setAccessible(true);
				if (f.get(t) != null) {
					if (!"".equals("" + f.get(t))) {
						names.append(col.c.name()).append(",");
						if (col.c.type() == Types.Int
								|| col.c.type() == Types.Float
								|| col.c.type() == Types.BigInt)
							values.append(f.get(t)).append(",");
						else
							values.append("'").append(f.get(t)).append("',");
					}
				}
			} catch (Exception e) {
				throw new DaoException(DaoException.PREPARE,
						"During create update statement, msg:" + e.getMessage());
			} finally {
				if (f != null)
					f.setAccessible(false);
			}
		}
		String stmt = new StringBuilder("insert into ").append(meta.cf.name())
				.append(names.substring(0, names.length() - 1))
				.append(") values ")
				.append(values.substring(0, values.length() - 1)).append(")")
				.toString();
		logger.info(stmt);
		session.execute(stmt);
		return t;
	}

	@Override
	public boolean delete(T t) throws DaoException {
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(t);
		Session session = getSession(meta);
		// prepare for delete
		primaryCheck(meta, t);
		checkPartionKeyExisting(session, meta, t);
		// here is delete
		Iterator<String> itr = meta.columns.keySet().iterator();
		StringBuilder sb = new StringBuilder("delete from ").append(
				meta.cf.name()).append(" where ");
		while (itr.hasNext()) {
			String key = itr.next();
			ColumnMetaData col = meta.columns.get(key);
			Field f = null;
			try {
				if (col.c.compositKey() || col.c.partionKey()
						|| col.c.sliceKey()) {
					f = t.getClass().getDeclaredField(key);
					f.setAccessible(true);
					if (f.get(t) != null) {
						sb.append(" ").append(col.c.name()).append(" = ");
						if (col.c.type() == Types.Int
								|| col.c.type() == Types.Float
								|| col.c.type() == Types.BigInt)
							sb.append(f.get(t)).append(" and ");
						else
							sb.append("'").append(f.get(t)).append("' and ");
					}
				}
			} catch (Exception e) {
				throw new DaoException(DaoException.PREPARE,
						"During create update statement, msg:" + e.getMessage());
			} finally {
				if (f != null)
					f.setAccessible(false);
			}
		}
		String stmt = sb.substring(0, sb.length() - 5);
		logger.info(stmt);
		session.execute(stmt);
		return true;
	}

	@Override
	public List<T> queryAll(Class<T> clazz) throws DaoException {
		if (clazz == null)
			throw new DaoException(DaoException.PREPARE,
					"Query for null type, pass class to the mentod is mandatory");
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(clazz);
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(meta.cf.name());
		Session session = getSession(meta);
		ResultSet rs = session.execute(sb.toString());
		List<Row> rows = rs.all();
		return this.convert(clazz, meta, rows);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> queryByPrimaryKey(T t) throws DaoException {
		if (t == null)
			throw new DaoException(DaoException.PREPARE,
					"Query by partion for null instanceis not allowed");
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(t);
		StringBuilder where = new StringBuilder();
		String className = t.getClass().getSimpleName();
		int trim = 5;
		for (Entry<String, ColumnMetaData> entry : meta.columns.entrySet()) {
			if (entry.getValue().c.partionKey()
					|| entry.getValue().c.compositKey()
					|| entry.getValue().c.sliceKey()) {
				Field f = null;
				try {
					f = t.getClass().getDeclaredField(entry.getKey());
					f.setAccessible(true);
					where.append(entry.getValue().c.name()).append("=");
					switch (entry.getValue().c.type()) {
					case BigInt:
						where.append((Long) f.get(t));
						break;
					case Int:
						where.append((Integer) f.get(t));
						break;
					case Float:
						where.append((Float) f.get(t));
						break;
					default:
						where.append("'").append(String.valueOf(f.get(t)))
								.append("'");
					}
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
					throw new DaoException(DaoException.PREPARE,
							new StringBuilder("Field ").append(entry.getKey())
									.append("No exist in class")
									.append(className).toString());
				} catch (SecurityException e) {
					throw new DaoException(
							DaoException.PREPARE,
							new StringBuilder(
									"Security exception during accessing to Field ")
									.append(entry.getKey()).append("in class")
									.append(className).toString());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new DaoException(
							DaoException.PREPARE,
							new StringBuilder(
									"Type mismatch during bringing value from field ")
									.append(entry.getKey()).append("in class")
									.append(className).toString());
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw new DaoException(DaoException.PREPARE,
							new StringBuilder("Illegal access to field ")
									.append(entry.getKey()).append("in class")
									.append(className).toString());
				} finally {
					if (f != null)
						f.setAccessible(true);
				}
				where.append(" and ");
			}
		}
		String stmt = new StringBuilder("select * from ")
		.append(meta.cf.name()).append(" where ")
		.append(where.substring(0, where.length() - trim)).toString();
		logger.info(stmt);
		Session session = getSession(meta);
		ResultSet rs = session.execute(stmt);
		List<Row> rows = rs.all();
		return this.convert((Class<T>) t.getClass(), meta, rows);
	}

	@Override
	public List<T> query(Class<T> clazz, String query, Object[] parameters) throws DaoException {
		ColumnFamilyMetaData meta = this.getMetaFromOrmObject(clazz);
		String stmt = String.format(query, parameters);
		logger.info(stmt);
		Session session = getSession(meta);
		ResultSet rs = session.execute(stmt);
		List<Row> rows = rs.all();
		return this.convert(clazz, meta, rows);
	}
}
