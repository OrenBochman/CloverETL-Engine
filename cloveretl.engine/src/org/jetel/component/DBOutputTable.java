/*
 *  jETeL/Clover - Java based ETL application framework.
 *  Copyright (C) 2002  David Pavlis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jetel.component;

import java.io.*;
import java.sql.*;
import org.w3c.dom.NamedNodeMap;
import org.jetel.graph.*;
import org.jetel.database.*;
import org.jetel.data.DataRecord;
import org.jetel.exception.ComponentNotReadyException;


/**
 *  <h3>DatabaseOutputTable Component</h3>
 *
 * <!-- his component performs append (so far) operation on specified database table.
 *  The metadata describing data comming in through input port[0] must be in the same
 *  structure as the target table. -->
 *
 * <table border="1">
 * <th>Component:</th>
 * <tr><td><h4><i>Name:</i></h4></td>
 * <td>DBOutputTable</td></tr>
 * <tr><td><h4><i>Category:</i></h4></td>
 * <td></td></tr>
 * <tr><td><h4><i>Description:</i></h4></td>
 * <td>This component performs append (so far) operation on specified database table.<br>
 *  The metadata describing data comming in through input port[0] must be in the same
 *  structure as the target table.</td></tr>
 * <tr><td><h4><i>Inputs:</i></h4></td>
 * <td>[0]- input records</td></tr>
 * <tr><td><h4><i>Outputs:</i></h4></td>
 * <td></td></tr>
 * <tr><td><h4><i>Comment:</i></h4></td>
 * <td></td></tr>
 * </table>
 *  <br>  
 *  <table border="1">
 *  <th>XML attributes:</th>
 *  <tr><td><b>type</b></td><td>"DB_OUTPUT_TABLE"</td></tr>
 *  <tr><td><b>id</b></td><td>component identification</td>
 *  <tr><td><b>dbTable</b></td><td>name of the DB table to populate data with</td>
 *  <tr><td><b>dbConnection</b></td><td>id of the Database Connection object to be used to access the database</td>
 *  </tr>
 *  </table>  
 *
 *  <h4>Example:</h4>  
 *  <pre>&lt;Node id="OUTPUT" type="DB_OUTPUT_TABLE" dbConnection="NorthwindDB" dbTable="employee_z"/&gt;</pre>
 * @author     dpavlis
 * @since    September 27, 2002
 * @see		org.jetel.database.AnalyzeDB
 * @revision   $Revision$ 
 */
public class DBOutputTable extends Node {

	private DBConnection dbConnection;
	private String dbConnectionName;
	private String dbTableName;
	private PreparedStatement preparedStatement;

	public final static String COMPONENT_TYPE = "DB_OUTPUT_TABLE";
	private final static int SQL_FETCH_SIZE_ROWS = 100;
	private final static int READ_FROM_PORT = 0;
	private final static int RECORDS_IN_COMMIT = 100;

	/**
	 *Constructor for the DBInputTable object
	 *
	 * @param  id                Description of Parameter
	 * @param  dbConnectionName  Description of Parameter
	 * @param  sqlQuery          Description of Parameter
	 * @since                    September 27, 2002
	 */
	public DBOutputTable(String id, String dbConnectionName, String dbTableName) {
		super(id);
		this.dbConnectionName = dbConnectionName;
		this.dbTableName = dbTableName;

	}


	/**
	 *  Gets the Type attribute of the DBInputTable object
	 *
	 * @return    The Type value
	 * @since     September 27, 2002
	 */
	public String getType() {
		return COMPONENT_TYPE;
	}


	/**
	 *  Description of the Method
	 *
	 * @exception  ComponentNotReadyException  Description of Exception
	 * @since                                  September 27, 2002
	 */
	public void init() throws ComponentNotReadyException {
		if (inPorts.size()<1){
			throw new ComponentNotReadyException("At least one input port has to be defined!");
		}
		// get dbConnection from graph
		dbConnection=TransformationGraph.getReference().getDBConnection(dbConnectionName);
		if (dbConnection==null){
			throw new ComponentNotReadyException("Can't find DBConnection ID: "+dbConnectionName);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Returned Value
	 * @since     September 27, 2002
	 */
	public org.w3c.dom.Node toXML() {
		// TODO
		return null;
	}


	/**
	 *  Main processing method for the DBInputTable object
	 *
	 * @since    September 27, 2002
	 */
	public void run() {
		InputPort inPort = getInputPort(READ_FROM_PORT);
		DataRecord inRecord = new DataRecord(inPort.getMetadata());
		CopySQLData[] transMap;
		int i;
		int result;
		int recCount=0;

		inRecord.init();
		try {
			String sql=SQLUtil.assembleInsertSQLStatement(inPort.getMetadata(),dbTableName);
			preparedStatement = dbConnection.prepareStatement(sql);
			dbConnection.getConnection().setAutoCommit(false);
			/* this somehow doesn't work (crashes system) at least when tested with Interbase
			ParameterMetaData metaData=preparedStatement.getParameterMetaData();
			if (metaData==null){
				System.err.println("metada data is null!");
			}
			*/
			transMap=CopySQLData.jetel2sqlTransMap(SQLUtil.getFieldTypes(dbConnection.getConnection().getMetaData(), dbTableName), 
							inRecord);

			while (inRecord!=null && runIt) {
				inRecord=readRecord(READ_FROM_PORT, inRecord);
				if (inRecord!=null){
					for (i = 0; i < transMap.length; i++) {
						transMap[i].jetel2sql(preparedStatement);
					}
					result = preparedStatement.executeUpdate();
					if (result!=1){
						throw new SQLException("Error when inserting record");
					}
					preparedStatement.clearParameters(); 
				}
				if (recCount++ % RECORDS_IN_COMMIT ==0 ) dbConnection.getConnection().commit();
			}
			dbConnection.getConnection().commit();
		}
		catch (IOException ex) {
			resultMsg = ex.getMessage();
			resultCode = Node.RESULT_ERROR;
			closeAllOutputPorts();
			return;
		}
		catch (SQLException ex) {
			ex.printStackTrace();
			resultMsg = ex.getMessage();
			resultCode = Node.RESULT_ERROR;
			closeAllOutputPorts();
			return;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			resultMsg = ex.getMessage();
			resultCode = Node.RESULT_FATAL_ERROR;
			//closeAllOutputPorts();
			return;
		}
		finally {
			try {
				broadcastEOF();
				if (preparedStatement!=null) preparedStatement.close();
				if (resultMsg==null){
					if (runIt) {
						resultMsg = "OK";
					} else {
						resultMsg = "STOPPED";
					}
					resultCode = Node.RESULT_OK;
				}
			}
			catch (SQLException ex) {
				resultMsg = ex.getMessage();
				resultCode = Node.RESULT_ERROR;
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  nodeXML  Description of Parameter
	 * @return          Description of the Returned Value
	 * @since           September 27, 2002
	 */
	public static Node fromXML(org.w3c.dom.Node nodeXML) {
		NamedNodeMap attribs = nodeXML.getAttributes();

		if (attribs != null) {
			String id = attribs.getNamedItem("id").getNodeValue();
			String dbTable = attribs.getNamedItem("dbTable").getNodeValue();
			String dbConnectionName = attribs.getNamedItem("dbConnection").getNodeValue();
			if (id != null && dbTable != null && dbConnectionName != null) {
				return new DBOutputTable(id, dbConnectionName, dbTable);
			}
		}
		return null;
	}

}

