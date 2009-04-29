/* Generated By:JJTree: Do not edit this line. CLVFLogLevel.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.TransformLangParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFLogLevel extends SimpleNode {
	
	private int logLevel;
	
	public CLVFLogLevel(int id) {
		super(id);
	}

	public CLVFLogLevel(TransformLangParser p, int id) {
		super(p, id);
	}

	public CLVFLogLevel(CLVFLogLevel node) {
		super(node);
		this.logLevel  = node.logLevel;
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public int getLogLevel() {
		return logLevel;
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFLogLevel(this);
	}
}
