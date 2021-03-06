/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.core.dom.ast;

/**
 * Enumerates various kinds of scopes
 * @since 5.1
 */
public enum EScopeKind {
	/**
	 * Used for local scope, but also for function-scope (labels) and 
	 * function-prototype scope (parameters in function prototypes).
	 */
	eLocal, 
	eNamespace,
	/**
	 * For classes, structs or unions.
	 */
	eClassType,
	eGlobal,
	/**
	 * For each template declaration a scope is created in which the template
	 * parameters can be looked up.
	 */
	eTemplateDeclaration
}
