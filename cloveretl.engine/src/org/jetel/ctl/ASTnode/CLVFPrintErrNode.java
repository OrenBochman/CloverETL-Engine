/* Generated By:JJTree: Do not edit this line. CLVFPrintErrNode.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFPrintErrNode extends SimpleNode {

	public boolean printLine = false;

	public CLVFPrintErrNode(int id) {
		super(id);
	}

	public CLVFPrintErrNode(ExpParser p, int id) {
		super(p, id);
	}

	public CLVFPrintErrNode(CLVFPrintErrNode node) {
		super(node);
		this.printLine = node.printLine;
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public void setPrintLine(boolean printLine) {
		this.printLine = printLine;
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFPrintErrNode(this);
	}
}
