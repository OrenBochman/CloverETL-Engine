/* Generated By:JJTree: Do not edit this line. CLVFFunctionCallParams.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.TransformLangParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFArguments extends SimpleNode {
	public CLVFArguments(int id) {
		super(id);
	}

	public CLVFArguments(TransformLangParser p, int id) {
		super(p, id);
	}

	
	public CLVFArguments(CLVFArguments arguments) {
		super(arguments);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFArguments(this);
	}
}
