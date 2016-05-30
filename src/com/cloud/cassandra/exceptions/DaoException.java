package com.cloud.cassandra.exceptions;
/**
 * 
 * @author maher
 *this is class is used in DaoLayer is there is any problem
 */
public class DaoException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String[] operations = { "Insert", "Update", "Delete",
			"Query", "Convert", "Meta", "AutoInc", "Prepare" };
	public static final int INSERT = 0;
	public static final int UPDATE = 1;
	public static final int DELETE = 2;
	public static final int QUERY = 3;
	public static final int CONVERT = 4;
	public static final int META = 5;
	public static final int AUTOINC = 6;
	public static final int PREPARE = 7;
	private int operation;
	private String message;

	public DaoException(int operation) {
		this.operation = operation;
		this.message = null;
	}

	public DaoException(int operation, String message) {
		this.operation = operation;
		this.message = message;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(
				"An error has occurred from DAO layer, Operation:")
				.append(operations[operation]);
		if (this.message != null) {
			sb.append(", Local Message: ").append(this.message);
		}
		return sb.toString();
	}
}
