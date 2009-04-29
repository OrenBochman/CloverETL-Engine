/* Generated By:JJTree: Do not edit this line. CLVFDoStatement.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;
import org.jetel.ctl.data.Scope;

public class CLVFDoStatement extends SimpleNode {
	private Scope scope;

	public CLVFDoStatement(int id) {
		super(id);
	}

	public CLVFDoStatement(ExpParser p, int id) {
		super(p, id);
	}
	

	public CLVFDoStatement(CLVFDoStatement node) {
		super(node);
		this.scope = node.scope;
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}
	
	public Scope getScope() {
		return scope;
	}

	@Override
	public SimpleNode duplicate() {
		return new CLVFDoStatement(this);
	}

}
