/* Generated By:JJTree: Do not edit this line. CLVFForeachStatement.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.TransformLangParser;
import org.jetel.ctl.TransformLangParserVisitor;
import org.jetel.ctl.data.Scope;

public class CLVFForeachStatement extends SimpleNode {
	private Scope scope;
	private int[] typeSafeFields;
	
	
	public CLVFForeachStatement(int id) {
		super(id);
	}

	public CLVFForeachStatement(TransformLangParser p, int id) {
		super(p, id);
	}

	public CLVFForeachStatement(CLVFForeachStatement node) {
		super(node);
		this.scope = node.scope;
		if (node.typeSafeFields != null) {
			this.typeSafeFields = new int[node.typeSafeFields.length];
			System.arraycopy(node.typeSafeFields, 0, this.typeSafeFields, 0, node.typeSafeFields.length);
		}
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

	public void setTypeSafeFields(int[] typeSafeFields) {
		this.typeSafeFields = typeSafeFields;
	}

	public int[] getTypeSafeFields() {
		return typeSafeFields;
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFForeachStatement(this);
	}

}
