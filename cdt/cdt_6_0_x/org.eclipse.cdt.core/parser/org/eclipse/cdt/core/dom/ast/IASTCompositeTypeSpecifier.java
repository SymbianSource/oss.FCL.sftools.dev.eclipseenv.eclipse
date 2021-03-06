/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Doug Schaefer (IBM) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.dom.ast;

/**
 * A composite type specifier represents a ocmposite structure (contains
 * declarations).
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IASTCompositeTypeSpecifier extends IASTDeclSpecifier , IASTNameOwner, IASTDeclarationListOwner {

	/**
	 * <code>TYPE_NAME</code> represents the relationship between an
	 * <code>IASTCompositeTypeSpecifier</code> and its <code>IASTName</code>.
	 */
	public static final ASTNodeProperty TYPE_NAME = new ASTNodeProperty(
			"IASTCompositeTypeSpecifier.TYPE_NAME - IASTName for IASTCompositeTypeSpecifier"); //$NON-NLS-1$

	/**
	 * <code>MEMBER_DECLARATION</code> represents the relationship between an
	 * <code>IASTCompositeTypeSpecifier</code> and its nested<code>IASTDeclaration</code>s.
	 */
	public static final ASTNodeProperty MEMBER_DECLARATION = new ASTNodeProperty(
			"IASTCompositeTypeSpecifier.MEMBER_DECLARATION - Nested IASTDeclaration for IASTCompositeTypeSpecifier"); //$NON-NLS-1$

	/**
	 * Get the type (key) of this composite specifier.
	 * 
	 * @return key for this type
	 */
	public int getKey();

	/**
	 * <code>k_struct</code> represents 'struct' in C & C++
	 */
	public static final int k_struct = 1;

	/**
	 * <code>k_union</code> represents 'union' in C & C++
	 */
	public static final int k_union = 2;

	/**
	 * <code>k_last</code> allows for subinterfaces to continue enumerating
	 * keys
	 */
	public static final int k_last = k_union;

	/**
	 * Set the type (key) of this composite specifier.
	 * 
	 * @param key
	 */
	public void setKey(int key);

	/**
	 * Return the name for this composite type. If this is an anonymous type,
	 * this will return an empty name.
	 * 
	 * @return the name of the type
	 */
	public IASTName getName();

	/**
	 * Set the name for this composite type.
	 * 
	 * @param name
	 */
	public void setName(IASTName name);

	/**
	 * Returns a list of member declarations.
	 * 
	 * @return List of IASTDeclaration
	 */
	public IASTDeclaration[] getMembers();

	/**
	 * Add a member declaration.
	 * 
	 * @param declaration
	 */
	public void addMemberDeclaration(IASTDeclaration declaration);

	/**
	 * Get the scope that this interface eludes to in the logical tree.
	 * 
	 */
	public IScope getScope();
	
	/**
	 * @since 5.1
	 */
	public IASTCompositeTypeSpecifier copy();
}
