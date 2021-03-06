/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John Camelon (IBM Rational Software) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.parser;

/**
 * @noextend This interface is not intended to be extended by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("nls")
public class Directives {

	public static final String 
		POUND_DEFINE  = "#define",
		POUND_UNDEF   = "#undef",
		POUND_IF      = "#if",
		POUND_IFDEF   = "#ifdef",
		POUND_IFNDEF  = "#ifndef",
		POUND_ELSE    = "#else",
		POUND_ENDIF   = "#endif",
		POUND_INCLUDE = "#include",
		POUND_LINE    = "#line",
		POUND_ERROR   = "#error",
		POUND_PRAGMA  = "#pragma",
		POUND_ELIF    = "#elif",
		POUND_BLANK   = "#",
		_PRAGMA       = "_Pragma",
	    DEFINED       = "defined";
	
}
