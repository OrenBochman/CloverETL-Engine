/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.ctl.debug;

import java.io.Serializable;

import org.jetel.ctl.debug.Breakpoint;
import org.jetel.util.string.StringUtils;

public class Breakpoint implements Comparable<Breakpoint>, Serializable {

	private static final long serialVersionUID = 1L;
	
		protected int line;
		protected String source;
		//protected boolean disabled;
		
		public Breakpoint(String source, int line){
			this.source=source;
			this.line=line;
		//	this.disabled=false;
		}
		
//		public boolean isDisabled(){
//			return this.disabled;
//		}
//		
//		public void setDisabled(boolean disabled){
//			this.disabled=disabled;
//		}

		public int getLine() {
			return line;
		}

		public String getSource() {
			return source;
		}

		
		public void setLine(int line) {
			this.line = line;
		}

		public void setSource(String source) {
			this.source = source;
		}

		@Override
		public int hashCode(){
			return this.line ^ (this.source==null ? 37 : this.source.hashCode());
		}
		
		@Override
		public boolean equals(Object other){
			 if (other instanceof Breakpoint){
				 final Breakpoint o = (Breakpoint)other;
				 if (this.line == o.line && StringUtils.equalsWithNulls(this.source,o.source)){
					 return true;
				 }
			 }
			 return false;
		}
		
		@Override
		public String toString(){
			return this.source + ":" + this.line;
		}

		@Override
		public int compareTo(Breakpoint o) {
			if (this.source!=null && o.source!=null){
				return this.source.compareTo(o.source) + ( this.line - o.line); 
			}else{
				return (this.line - o.line);
			}
		}
		
	}