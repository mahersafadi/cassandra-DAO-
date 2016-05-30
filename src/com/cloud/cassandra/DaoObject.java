package com.cloud.cassandra;

import java.util.List;

import com.cloud.cassandra.exceptions.DaoException;

public interface DaoObject<T> {
	public T insert(T t) throws DaoException;
	public T update(T t) throws DaoException;
	public boolean delete(T t) throws DaoException;
	public List<T> queryAll(Class<T> clazz)throws DaoException;
	public List<T> queryByPrimaryKey(T t) throws DaoException;
	public List<T> query(Class<T> clazz, String query, Object[] parameters) throws DaoException;
}
