package org.jetel.component.hadooploader;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.jetel.component.hadooploader.HadoopCloverConvert.Hadoop2Clover;
import org.jetel.data.DataRecord;
import org.jetel.data.DataRecordFactory;
import org.jetel.database.IConnection;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.IParserExceptionHandler;
import org.jetel.exception.JetelException;
import org.jetel.exception.PolicyType;
import org.jetel.graph.ContextProvider;
import org.jetel.graph.TransformationGraph;
import org.jetel.hadoop.component.IHadoopSequenceFileParser;
import org.jetel.hadoop.connection.HadoopConnection;
import org.jetel.hadoop.connection.HadoopURLUtils;
import org.jetel.hadoop.connection.IHadoopConnection;
import org.jetel.metadata.DataFieldType;
import org.jetel.metadata.DataRecordMetadata;

public class HadoopSequenceFileParser implements IHadoopSequenceFileParser {

	private FileSystem dfs;
	private SequenceFile.Reader reader;
	private int keyField;
	private int valueField;
	long counter;
	private String keyFieldName;
	private String valueFieldName;
	private Writable keyValue;
	private Writable dataValue;
	private Hadoop2Clover keyCopy;
	private Hadoop2Clover valCopy;
	private DataRecordMetadata metadata;
	private TransformationGraph graph;
	
	private IParserExceptionHandler exceptionHandler;
	
	
	public HadoopSequenceFileParser(FileSystem dfs) {
		this.dfs=dfs;
	}
	
	public HadoopSequenceFileParser() {
	}
	
	public HadoopSequenceFileParser(FileSystem dfs,DataRecordMetadata metadata, String keyFieldName, String valueFieldName) {
		this.dfs=dfs;
		this.metadata=metadata;
		this.keyFieldName=keyFieldName;
		this.valueFieldName=valueFieldName;
	}
	
	
	@Override
	public DataRecord getNext() throws JetelException {
		 // create a new data record
        DataRecord record = DataRecordFactory.newRecord(metadata);
        record.init();
        return getNext(record);
	}

	@Override
	public int skip(int nRec) throws JetelException {
		for(int i=0;i<nRec;i++){
			try {
				reader.next(keyValue,dataValue);
			} catch (IOException e) {
				throw new JetelException("Error when skipping record.",e);
			}
		}
		return nRec;
	}

	@Override
	public void init() throws ComponentNotReadyException {
		counter=0;
		if (metadata==null)
			throw new ComponentNotReadyException("No metadata defined");
		
		if (keyFieldName==null){
			keyField=0;
			valueField=1;
		}else{
			keyField = metadata.getFieldPosition(keyFieldName);
			valueField = metadata.getFieldPosition(valueFieldName);
			
			if (keyField == -1){
				throw new ComponentNotReadyException("Can't find key field of name \""+keyFieldName+"\" in metadata.");
			}
			if (valueField == -1){
				throw new ComponentNotReadyException("Can't find value field of name \""+keyFieldName+"\" in metadata.");
			}
		}
	}

	private void initCopyObjects() throws IOException{
		if (reader==null)
			throw new IOException("No source data reader defined.");
		
		DataFieldType hKeyType;
		DataFieldType hValueType;
		try{
			hKeyType=HadoopCloverConvert.hadoopType2Clover(reader.getKeyClass());
			hValueType=HadoopCloverConvert.hadoopType2Clover(reader.getValueClass());
			keyCopy=HadoopCloverConvert.getH2CCopier(reader.getKeyClass());
			valCopy=HadoopCloverConvert.getH2CCopier(reader.getValueClass());
			keyValue = (Writable) reader.getKeyClass().newInstance();
			dataValue = (Writable) reader.getValueClass().newInstance();
		}catch(IOException ex){
			throw ex;
		}catch(Exception ex){
			throw new IOException("Error when initializing HadoopSequenceFile parser.",ex);
		}
		
		if (metadata.getField(keyField).getDataType() !=hKeyType)
			throw new IOException(String.format("Incompatible Clover & Hadoop data types for Key \"%s\" (%s <> %s/%s).",
					metadata.getField(keyField).getName(),metadata.getField(keyField).getDataType(),reader.getKeyClassName(),hKeyType));
		
		if (metadata.getField(valueField).getDataType() !=hValueType)
			throw new IOException(String.format("Incompatible Clover & Hadoop data types for Value \"%s\" (%s <> %s/%s).",
					metadata.getField(valueField).getName(),metadata.getField(keyField).getDataType(),reader.getValueClassName(),hValueType));
	}
	
	
	@Override
	public void setDataSource(Object inputDataSource) throws IOException,
	ComponentNotReadyException {
		FileSystem tmpDFS;
		if (inputDataSource instanceof SequenceFile.Reader) {
			reader = (SequenceFile.Reader) inputDataSource;
			return;
		}

		if (inputDataSource instanceof URI) {
			if (!HadoopURLUtils.isHDFSUri((URI)inputDataSource))
				throw new IOException("Not a valid HDFS/Hadoop URL - "+inputDataSource);
			final String connectionName = ((URI) inputDataSource).getHost();

			if (graph==null) graph = ContextProvider.getGraph();
			if (graph == null) {
				throw new IOException(
						String.format(
								"Internal error: Cannot find HDFS connection [%s] referenced in fileURL \"%s\". Missing TransformationGraph instance.",
								connectionName, inputDataSource));
			}

			ClassLoader formerContextClassloader = Thread.currentThread()
					.getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(
						this.getClass().getClassLoader());

				if (dfs==null){

					IConnection conn = graph.getConnection(connectionName);
					if (conn == null)
						throw new IOException(
								String.format(
										"Cannot find HDFS connection [%s] referenced in fileURL \"%s\".",
										connectionName, inputDataSource));
					if (!(conn instanceof HadoopConnection)) {
						throw new IOException(String.format(
								"Connection [%s:%s] is not of HDFS type.",
								conn.getId(), conn.getName()));
					}
					conn.init(); // try to init - in case it was not already initialized
					tmpDFS=(FileSystem) ((HadoopConnection) conn).getConnection().getDFS();
				}else{
					tmpDFS=dfs;
				}
				
				try{
					reader = new SequenceFile.Reader(tmpDFS, 
							new Path(((URI) inputDataSource).getPath()),
							new Configuration());
				}catch(IOException ex){
					//TODO:may try to improve error message
					throw ex;
				}

			} finally {
				Thread.currentThread().setContextClassLoader(
						formerContextClassloader);
			}

		} else {
			throw new IOException("Unsupported data source type: "
					+ inputDataSource.getClass().getName());
		}
		initCopyObjects();
	}

	@Override
	public void setReleaseDataSource(boolean releaseInputSource) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws IOException {
		if(reader!=null){
			reader.close();
		}

	}

	@Override
	public DataRecord getNext(DataRecord record) throws JetelException {
			try {
				if (!reader.next(keyValue,dataValue))
					return null;
			} catch (IOException e) {
				throw new JetelException("Error when reading data record.", e);
			}
			this.keyCopy.copyValue(keyValue, record.getField(keyField));
			this.valCopy.copyValue(dataValue, record.getField(valueField));
		
			return record;
	}

	@Override
	public void setExceptionHandler(IParserExceptionHandler handler) {
		this.exceptionHandler=handler;

	}

	@Override
	public IParserExceptionHandler getExceptionHandler() {
		return this.exceptionHandler;
	}

	@Override
	public PolicyType getPolicyType() {
		return PolicyType.STRICT;   // we don't support different policy
	}

	@Override
	public void reset() throws ComponentNotReadyException {
	}

	@Override
	public Object getPosition() {
		try{
			return reader.getPosition();
		}catch(Exception ex){
			return -1;
		}
	}

	@Override
	public void movePosition(Object position) throws IOException {
		int pos = 0;
		if (position instanceof Integer) {
			pos = ((Integer) position).intValue();
		} else if (position != null) {
			pos = Integer.parseInt(position.toString());
		}
		if (pos > 0) {
			reader.seek(pos);
		}

	}

	@Override
	public void preExecute() throws ComponentNotReadyException {
		// TODO Auto-generated method stub

	}

	@Override
	public void postExecute() throws ComponentNotReadyException {
		// TODO Auto-generated method stub

	}

	@Override
	public void free() throws ComponentNotReadyException, IOException {
		close();
		reader=null;

	}

	@Override
	public boolean nextL3Source() {
		return false;
	}

	@Override
	public boolean isURISourcePreferred() {
		return true;
	}

	@Override
	public void setKeyValueFields(String keyFieldName, String valueFieldName) {
		this.keyFieldName=keyFieldName;
		this.valueFieldName=valueFieldName;
	}

	@Override
	public void setMetadata(DataRecordMetadata metadata) {
		this.metadata=metadata;
		
	}
	
	public void setDFS(FileSystem dfs){
		this.dfs=dfs;
	}

	@Override
	public void setHadoopConnection(IHadoopConnection conn) {
		this.dfs = (FileSystem)conn.getDFS();
	}

	@Override
	public void setGraph(TransformationGraph graph) {
		this.graph=graph;
		
	}

}
