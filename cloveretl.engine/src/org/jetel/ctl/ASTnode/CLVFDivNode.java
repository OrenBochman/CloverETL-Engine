/* Generated By:JJTree: Do not edit this line. CLVFDivNode.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFDivNode extends SimpleNode {

	public CLVFDivNode(int id) {
		super(id);
	}

	public CLVFDivNode(ExpParser p, int id) {
		super(p, id);
	}
	
	

	public CLVFDivNode(CLVFDivNode node) {
		super(node);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFDivNode(this);
	}
	
}
