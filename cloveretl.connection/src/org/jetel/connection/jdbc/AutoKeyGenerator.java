package org.jetel.connection.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.jetel.connection.jdbc.specific.DBConnectionInstance;
import org.jetel.connection.jdbc.specific.JdbcSpecific.AutoGeneratedKeysType;
import org.jetel.data.DataField;
import org.jetel.data.DataRecord;
import org.jetel.data.Defaults;
import org.jetel.data.RecordKey;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.string.StringUtils;

/**
 * Class for filling output record by keys got from database.
 * 
 * @author avackova (agata.vackova@javlinconsulting.cz) ; 
 * (c) JavlinConsulting s.r.o.
 *  www.javlinconsulting.cz
 *
 * @since Jun 13, 2007
 *
 */
public class AutoKeyGenerator{
	
	protected DBConnectionInstance connection;
	protected String sqlQuery;
	protected String[] columns;
	protected String[] fillFields;
	protected Log logger;
	protected AutoGeneratedKeysType autoKeyType = AutoGeneratedKeysType.NONE;
	private DataRecord keyRecord;
	private CopySQLData[] keyTransMap;
	private int[][] fieldMap;

	public final static String AUTOGENERATED_FIELD_NAME = "AUTO_GENERATED";

	private final static int KEY_RECORD_INDEX = 0;
	private final static int IN_RECORD_INDEX = 1;

	/**
	 * @param connection connection to database
	 * @param sqlQuery sql query
	 * @param columns list of columns to return
	 */
	AutoKeyGenerator(DBConnectionInstance connection, String sqlQuery, String[] columns) {
		this.connection = connection;
		this.sqlQuery = sqlQuery;
		this.columns = columns;
	} 
	
	/**
	 * Prepares statement from which autogeneratedKeys will be taken.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement prepareStatement() throws SQLException{
		PreparedStatement statement;

		if (columns != null && columns.length > 0) {
			List<String> fromDb = new ArrayList<String>();
			for (int i = 0; i < columns.length; i++) {
				if (!columns[i].startsWith(Defaults.CLOVER_FIELD_INDICATOR)) {
					fromDb.add(columns[i]);
				}
			}
			statement  = connection.getSqlConnection().prepareStatement(sqlQuery, fromDb.toArray(new String[fromDb.size()]));
			//autoKeyType will be changed only if statement was prepared successfully and we want back autogenerated columns
			autoKeyType = connection.getJdbcSpecific().getAutoKeyType();
		} else {
			statement = connection.getSqlConnection().prepareStatement(sqlQuery);
		}

		return statement;
	}
	
	public PreparedStatement reset() throws SQLException{
		return prepareStatement();
	}
	
	/**
	 * Fills keyRecord by values from input record and result set 
	 * 
	 * @param inRecord input record
	 * @param keyRecord key record to fill
	 * @param autogeneratedKeys result set with autogenerated keys (preparedStatement.getGeneratedKeys())
	 * @return
	 * @throws SQLException
	 */
	public DataRecord fillKeyRecord(DataRecord inRecord, DataRecord keyRecord, ResultSet autogeneratedKeys) throws SQLException{
		if (this.keyRecord == null) {
			this.keyRecord = keyRecord;
			init(inRecord.getMetadata());
		}
		switch (autoKeyType) {
		case MULTI:
			return fillMultiKeyRecord(inRecord, keyRecord, autogeneratedKeys);
		case SINGLE:
			return fillSingleKeyRecord(inRecord, keyRecord, autogeneratedKeys);
		default:
			return keyRecord;
		}
	}
	
	private void init(DataRecordMetadata inRecordMetadata){
		switch (autoKeyType) {
		case MULTI:
			if (fillFields != null) {
				List<String> fromDb = new ArrayList<String>();
				for (int i = 0; i < fillFields.length; i++) {
					if (!columns[i].startsWith(Defaults.CLOVER_FIELD_INDICATOR)) {
						fromDb.add(fillFields[i]);
					}
				}
				RecordKey tmp = new RecordKey(fromDb.toArray(new String[fromDb.size()]), keyRecord.getMetadata());
				tmp.init();
				keyTransMap = CopySQLData.sql2JetelTransMap(SQLUtil.getFieldTypes(tmp.generateKeyRecordMetadata(), connection.getJdbcSpecific()), 
						keyRecord.getMetadata(), keyRecord, tmp.getKeyFieldNames());
				fieldMap = fieldMap(inRecordMetadata, keyRecord, columns);
			}else {
				keyTransMap = CopySQLData.sql2JetelTransMap(SQLUtil.getFieldTypes(keyRecord.getMetadata(), connection.getJdbcSpecific()), 
						keyRecord.getMetadata(), keyRecord);
			}
			break;
		case SINGLE:
			fieldMap = fieldMap(inRecordMetadata, keyRecord, columns);
			break;
		default:
			break;
		}
	}
	
	private DataRecord fillMultiKeyRecord(DataRecord inRecord, DataRecord keyRecord, 
			ResultSet autogeneratedKeys) throws SQLException{
		for (int i = 0; i < keyTransMap.length; i++) {
			keyTransMap[i].sql2jetel(autogeneratedKeys);
		}
		if (fieldMap != null) {
			DataField field;
			for (int i=0; i<fieldMap.length; i++) {
				field = keyRecord.getField(fieldMap[i][KEY_RECORD_INDEX]);
				if (fieldMap[i][IN_RECORD_INDEX] > -1){
					field.setValue(inRecord.getField(fieldMap[i][IN_RECORD_INDEX]));
				}
			}
		}
		return this.keyRecord;
	}
	
	private DataRecord fillSingleKeyRecord(DataRecord inRecord, DataRecord keyRecord, 
			ResultSet autogeneratedKeys) throws SQLException{
		DataField field;
		for (int i=0; i<fieldMap.length; i++) {
			field = keyRecord.getField(fieldMap[i][KEY_RECORD_INDEX]);
			if (fieldMap[i][IN_RECORD_INDEX] == -1){
				field.setValue(autogeneratedKeys.getLong(1));
			}else if (fieldMap[i][IN_RECORD_INDEX] == Integer.MIN_VALUE){
				field.fromString(columns[i]);
			}else{
				field.setValue(inRecord.getField(fieldMap[i][IN_RECORD_INDEX]));
			}
		}
		return keyRecord;
		
	}

	/**
	 * Prepares array of integers for mapping input record to key record.
	 * Number greater from -1 is index of input field to get value. 
	 * -1 means that, value will be got from result set.
	 * 
	 * @param inRecord
	 * @param keyRecord
	 * @param autoGeneratedColumns
	 * @return
	 */
	private int[][] fieldMap(DataRecordMetadata inRecordMetadata, DataRecord keyRecord, String[] autoGeneratedColumns) {
		int[][] result = fillFields != null ? new int[fillFields.length][2] : new int[keyRecord.getNumFields()][2];
		
		Map<String, Integer> inFieldsMap = inRecordMetadata.getFieldNamesMap();
		String fieldName;
		for (int i = 0; i < result.length; i++) {
			result[i][KEY_RECORD_INDEX] = fillFields != null ? 
					keyRecord.getMetadata().getFieldPosition(fillFields[i]) : i;
			if (autoGeneratedColumns[i].equalsIgnoreCase(AUTOGENERATED_FIELD_NAME)) {
				result[i][IN_RECORD_INDEX] = -1;
			}else if (autoGeneratedColumns[i].startsWith(Defaults.CLOVER_FIELD_INDICATOR)){
				fieldName = autoGeneratedColumns[i].substring(Defaults.CLOVER_FIELD_INDICATOR.length());
				if (inFieldsMap.containsKey(fieldName)){
				result[i][IN_RECORD_INDEX] = inFieldsMap.get(fieldName);
				}else{
					throw new IllegalArgumentException("Field " + StringUtils.quote(fieldName) + " doesn't exist in input metadata");
				}
			}else{
				result[i][IN_RECORD_INDEX] = Integer.MIN_VALUE;
			}
		}
		return result;
	}

	public Log getLogger() {
		return logger;
	}

	public void setLogger(Log logger) {
		this.logger = logger;
	}

	String[] getFillFields() {
		return fillFields;
	}

	void setFillFields(String[] fillFields) {
		this.fillFields = fillFields;
	}

	public AutoGeneratedKeysType getAutoKeyType() {
		return autoKeyType;
	}

}
