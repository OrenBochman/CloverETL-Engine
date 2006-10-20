
/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2005-06  Javlin Consulting <info@javlinconsulting.cz>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/

package org.jetel.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.data.DataRecord;
import org.jetel.data.formatter.CloverDataFormatter;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.graph.InputPort;
import org.jetel.graph.Node;
import org.jetel.graph.TransformationGraph;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.metadata.DataRecordMetadataXMLReaderWriter;
import org.jetel.util.ComponentXMLAttributes;
import org.jetel.util.SynchronizeUtils;
import org.w3c.dom.Element;

/**
 *  <h3>Clover Data Writer Component</h3>
 *
 * <!-- Writes data in Clover internal format to binary file. -->
 *
 * <table border="1">
 *  <th>Component:</th>
 * <tr><td><h4><i>Name:</i></h4></td>
 * <td>CloverDataWriter</td></tr>
 * <tr><td><h4><i>Category:</i></h4></td>
 * <td>Writers</td></tr>
 * <tr><td><h4><i>Description:</i></h4></td>
 * <td>Reads data from input port and writes them to binary file in Clover internal
 *  format. With records can be saved indexes of records in binary file (for 
 *  reading not all records afterward) and metadata definition. Data are saved
 *   in zip file with the structure:<br>DATA/fileName<br>INDEX/fileName.idx<br>
 *   METADATA/fileName.fmt<br>Because POI currently uses a lot of memory for large sheets it impossible to save large data (over ~1.8MB) to xls file</td></tr>
 * <tr><td><h4><i>Inputs:</i></h4></td>
 * <td>one input port defined/connected.</td></tr>
 * <tr><td><h4><i>Outputs:</i></h4></td>
 * <td></tr>
 * <tr><td><h4><i>Comment:</i></h4></td></tr>
 * </table>
 *  <br>
 *  <table border="1">
 *  <th>XML attributes:</th>
 *  <tr><td><b>type</b></td><td>"CLOVER_WRITER"</td></tr>
 *  <tr><td><b>id</b></td><td>component identification</td>
 *  <tr><td><b>fileURL</b></td><td>path and name for output file (filePath/fileName).
 *   Part after last file.separator will be use as name for data file (fileName). 
 *   If compressLevel is not zero data will be save in file fileName.zip with 
 *   structure described above. If compress level equals zero data will be saved
 *   in following files:<br>filePath/DATA/fileName<br>filePath/INDEX/fileName.idx<br>
 *   filePath/METADATA/fileName.fmt </td>
 *  <tr><td><b>saveIndex</b></td><td>indicates if indexes to records in binary 
 *  file are saved or not (true/false - default false)</td>
 *  <tr><td><b>saveMetadata</b></td><td>indicates if metadata definition is saved
 *   or not (true/false - default false)</td>
 *  <tr><td><b>compressLevel</b><br><i>optional</i></td><td>Sets the compression level. The default
 *   setting is to compress using default ZIP compression level.If compressLevel is not zero data will be save in file fileName.zip with 
 *   structure described above. If compress level equals zero data will be saved
 *   in following files:<br>filePath/DATA/fileName<br>filePath/INDEX/fileName.idx<br>
 *   filePath/METADATA/fileName.fmt</td>
 *  </tr>
 *  </table>
 *
 *  <h4>Example:</h4>
 *  <pre>&lt;Node compressLevel="0" fileURL="customers.clv" id="CLOVER_WRITER0"
 *   saveIndex="true" saveMetadata="true" type="CLOVER_WRITER"/&gt;
 *  
 *  <pre>&lt;Node fileURL="customers.clv" id="CLOVER_WRITER0"
 *   saveIndex="true" type="CLOVER_WRITER"/&gt;
 *
/**
 * @author avackova (agata.vackova@javlinconsulting.cz) ; 
 * (c) JavlinConsulting s.r.o.
 *  www.javlinconsulting.cz
 *
 * @since Oct 12, 2006
 * @see CloverDataFormater
 *
 */
public class CloverDataWriter extends Node {

	private static final String XML_FILEURL_ATTRIBUTE = "fileURL";
	private static final String XML_APPEND_ATTRIBUTE = "append";
	private static final String XML_SAVEINDEX_ATRRIBUTE = "saveIndex";
	private static final String XML_SAVEMETADATA_ATTRIBUTE = "saveMetadata";
	private static final String XML_COMPRESSLEVEL_ATTRIBUTE = "compressLevel";

	public final static String COMPONENT_TYPE = "CLOVER_WRITER";
	private final static int READ_FROM_PORT = 0;
	
	private String fileURL;
	private boolean append;
	private CloverDataFormatter formatter;
	private boolean saveMetadata;
	private DataRecordMetadata metadata;
	private OutputStream out;
	private InputPort inPort;
	private int compressLevel;
	String fileName;
	
    static Log logger = LogFactory.getLog(CloverDataWriter.class);

 	public CloverDataWriter(String id, String fileURL, boolean saveIndex) {
		super(id);
		this.fileURL = fileURL;
		if (fileURL.toLowerCase().endsWith(".zip")){
			fileName = fileURL.substring(fileURL.lastIndexOf(File.separatorChar)+1,fileURL.lastIndexOf('.'));
		}else{
			fileName = fileURL.substring(fileURL.lastIndexOf(File.separatorChar)+1);
		}
		formatter = new CloverDataFormatter(fileURL,saveIndex);
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.Node#getType()
	 */
	@Override
	public String getType() {
		return COMPONENT_TYPE;
	}

	/**
	 * This method saves metadata definition to METADATA/fileName.fmt or
	 * fileName.zip#METADATA/fileName.fmt
	 * 
	 * @throws IOException
	 */
	private void saveMetadata() throws IOException{
		if (out instanceof FileOutputStream) {
			File metadataDir = new File(fileURL.substring(0,fileURL.lastIndexOf(File.separatorChar)+1) + "METADATA");
			metadataDir.mkdir();
			FileOutputStream metaFile = new FileOutputStream(
					metadataDir.getPath() + File.separator + fileName +".fmt");
			DataRecordMetadataXMLReaderWriter.write(metadata,metaFile);			
		}else{//out is ZipOutputStream
			((ZipOutputStream)out).putNextEntry(new ZipEntry(
					"METADATA" + File.separator + fileName+".fmt"));
			DataRecordMetadataXMLReaderWriter.write(metadata, out);
			((ZipOutputStream)out).closeEntry();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jetel.graph.Node#run()
	 */
	@Override
	public void run() {
		DataRecord record = new DataRecord(metadata);
		record.init();
		while (record != null && runIt) {
			try {
				record = inPort.readRecord(record);
				if (record != null) {
					formatter.write(record);
				}
			}
			catch (IOException ex) {
				resultMsg=ex.getMessage();
				resultCode=Node.RESULT_ERROR;
				closeAllOutputPorts();
				return;
			}
			catch (Exception ex) {
				resultMsg=ex.getClass().getName()+" : "+ ex.getMessage();
				resultCode=Node.RESULT_FATAL_ERROR;
				return;
			}
			SynchronizeUtils.cloverYield();
		}
		formatter.close();
		try{
			if (saveMetadata){
				saveMetadata();
			}
			out.close();
		}catch (IOException ex) {
			resultMsg=ex.getMessage();
			resultCode=Node.RESULT_ERROR;
			closeAllOutputPorts();
			return;
		}
		if (runIt) resultMsg="OK"; else resultMsg="STOPPED";
		resultCode=Node.RESULT_OK;
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.GraphElement#checkConfig()
	 */
	@Override
	public boolean checkConfig() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.GraphElement#init()
	 */
	@Override
	public void init() throws ComponentNotReadyException {
		// test that we have at least one input port
		if (inPorts.size() != 1) {
			throw new ComponentNotReadyException("One input port has to be defined!");
		}
		inPort = getInputPort(READ_FROM_PORT);
		metadata = inPort.getMetadata();
		try{//create output stream
			if (compressLevel != 0) {
				if (fileURL.toLowerCase().endsWith(".zip")){
					out = new ZipOutputStream(new FileOutputStream(fileURL+".zip"));
				}else{
					out = new ZipOutputStream(new FileOutputStream(fileURL+".zip"));
				}
				if (compressLevel != -1){
					((ZipOutputStream)out).setLevel(compressLevel);
				}
			}else{
				File dataDir = new File(fileURL.substring(0,fileURL.lastIndexOf(File.separatorChar)+1) + "DATA");
				dataDir.mkdir();
				out = new FileOutputStream(dataDir.getPath() + File.separator + fileName, append);
			}
		}catch(IOException ex){
			throw new ComponentNotReadyException(ex);
		}
		formatter.open(out, metadata);
	}
	/**
	 *  Description of the Method
	 *
	 * @param  nodeXML  Description of Parameter
	 * @return          Description of the Returned Value
	 * @since           May 21, 2002
	 */
	public static Node fromXML(TransformationGraph graph, Element nodeXML) {
		ComponentXMLAttributes xattribs=new ComponentXMLAttributes(nodeXML, graph);
		CloverDataWriter aDataWriter = null;
		
		try{
			aDataWriter = new CloverDataWriter(xattribs.getString(Node.XML_ID_ATTRIBUTE),
					xattribs.getString(XML_FILEURL_ATTRIBUTE),
					xattribs.getBoolean(XML_SAVEINDEX_ATRRIBUTE,false));
			aDataWriter.setAppend(xattribs.getBoolean(XML_APPEND_ATTRIBUTE,false));
			aDataWriter.setSaveMetadata(xattribs.getBoolean(XML_SAVEMETADATA_ATTRIBUTE,false));
			aDataWriter.setCompressLevel(xattribs.getInteger(XML_COMPRESSLEVEL_ATTRIBUTE,-1));
		}catch(Exception ex){
			System.err.println(COMPONENT_TYPE + ":" + xattribs.getString(Node.XML_ID_ATTRIBUTE,"unknown ID") + ":" + ex.getMessage());
			return null;
		}
		
		return aDataWriter;
	}

	public void toXML(org.w3c.dom.Element xmlElement) {
		super.toXML(xmlElement);
		xmlElement.setAttribute(XML_FILEURL_ATTRIBUTE,this.fileURL);
		xmlElement.setAttribute(XML_APPEND_ATTRIBUTE,String.valueOf(append));
		xmlElement.setAttribute(XML_SAVEMETADATA_ATTRIBUTE,String.valueOf(saveMetadata));
		xmlElement.setAttribute(XML_SAVEINDEX_ATRRIBUTE,String.valueOf(formatter.isSaveIndex()));
		if (compressLevel > -1){
			xmlElement.setAttribute(XML_COMPRESSLEVEL_ATTRIBUTE,String.valueOf(compressLevel));
		}
	}

	public void setSaveMetadata(boolean saveMetadata) {
		this.saveMetadata = saveMetadata;
	}

	public void setCompressLevel(int compressLevel) {
		this.compressLevel = compressLevel;
	}

	public void setAppend(boolean append) {
		this.append = append;
		formatter.setAppend(append);
	}


}
