/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bryan Wilkinson (QNX)
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.core.parser.tests.prefix;

import org.eclipse.cdt.core.dom.ast.IASTCompletionNode;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IVariable;

public class BasicCompletionTest extends CompletionTestBase {

	private void testVar(IASTCompletionNode node) throws Exception {
		IASTName[] names = node.getNames();
		assertEquals(1, names.length);
		IBinding[] bindings = names[0].getCompletionContext().findBindings(names[0], true);
		assertEquals(1, bindings.length);
		IVariable var = (IVariable)bindings[0];
		assertEquals("blah", var.getName());
	}
	
	public void testVar() throws Exception {
		String code = 
			"int blah = 4;" +
			"int two = bl";
		
		testVar(getGPPCompletionNode(code));
		testVar(getGCCCompletionNode(code));
	}

	public void testFunction() throws Exception {
		String code =
			"void func(int x) { }" +
			"void func2() { fu";
		
		// C++
		IASTCompletionNode node = getGPPCompletionNode(code);
		IASTName[] names = node.getNames();
		// There are two names, one as an expression, one that isn't connected, one as a declaration
		assertTrue(names.length > 1);
		// The expression points to our functions
		IBinding[] bindings = names[0].getCompletionContext().findBindings(names[0], true);
		// There should be two since they both start with fu
		assertEquals(2, bindings.length);
		assertEquals("func", ((IFunction)bindings[0]).getName());
		assertEquals("func2", ((IFunction)bindings[1]).getName());
		// The other names shouldn't be hooked up
		for (int i = 1; i < names.length; i++) {
			assertNull(names[i].getTranslationUnit());
		}

		// C
		node = getGCCCompletionNode(code);
		names = node.getNames();
		// There are two names, one as an expression, one as a declaration
		assertTrue(names.length > 1);
		// The expression points to our functions
		bindings = sortBindings(names[0].getCompletionContext().findBindings(names[0], true));
		// There should be two since they both start with fu
		assertEquals(2, bindings.length);
		assertEquals("func", ((IFunction)bindings[0]).getName());
		assertEquals("func2", ((IFunction)bindings[1]).getName());
		// The other names shouldn't be hooked up
		for (int i = 1; i < names.length; i++) {
			assertNull(names[i].getTranslationUnit());
		}
	}

	public void testTypedef() throws Exception {
		String code = 
			"void test() {typedef int blah;" +
			"bl";
		
		// C++
		IASTCompletionNode node = getGPPCompletionNode(code);
		IASTName[] names = node.getNames();
		assertEquals(2, names.length);
		assertNull(names[1].getTranslationUnit());
		IBinding[] bindings = names[0].getCompletionContext().findBindings(names[0], true);
		assertEquals(1, bindings.length);
		assertEquals("blah", ((ITypedef)bindings[0]).getName());
		
		// C
		node = getGCCCompletionNode(code);
		names = node.getNames();
		assert(names.length > 0);
		bindings = names[0].getCompletionContext().findBindings(names[0], true);
		assertEquals(1, bindings.length);
		assertEquals("blah", ((ITypedef)bindings[0]).getName());
	}
	
	public void testBug181624() throws Exception {
		String code = 
			"void foo() {" +
			"  switch (";
		
		// C++
		IASTCompletionNode node = getGPPCompletionNode(code);
		assertNotNull(node);
		
		// C
		node = getGCCCompletionNode(code);
		assertNotNull(node);
		
		code = 
			"void foo() {" +
			"  while (";
		
		// C++
		node = getGPPCompletionNode(code);
		assertNotNull(node);
		
		// C
		node = getGCCCompletionNode(code);
		assertNotNull(node);
	}
	
	//	template <typename T> class CT {};
	//	template <typename T> class B: public A<T> {
	//	public: 
	//       void doit(){}
	//	};
	//	int main() {
	//	   B<int> b;
	//	   b.
	public void testBug267911() throws Exception {
		String code = getAboveComment();
		String[] expected= {"B", "doit"};
		checkCompletion(code, true, expected);
	}
	
	//	typedef struct MyType {
	//		int aField;
	//	} MyType;
	//  M
	public void testBug279931() throws Exception {
		String code = getAboveComment();
		String[] expected= {"MyType", "MyType"};
		checkCompletion(code, true, expected);
		expected= new String[] {"MyType"};
		checkCompletion(code, false, expected);
	}

	//	typedef struct MyType {
	//		int aField;
	//	} MyType;
	//  struct M
	public void testBug279931a() throws Exception {
		String code = getAboveComment();
		String[] expected= {"MyType"};
		checkCompletion(code, true, expected);
		checkCompletion(code, false, expected);
	}
	
	// template <t
	public void testBug280934() throws Exception {
		String code = getAboveComment();
		String[] expected= {};
		checkCompletion(code, true, expected);
	}
	
	//	struct s1 {
	//		struct {
	//			int a1;
	//			int a2;
	//		};
	//		union {
	//			int u1;
	//			char u2;
	//		};
	//		int b;
	//	};
	//	int test() {
	//		struct s1 s;
	//		s.
	public void testBug284245() throws Exception {
		String code = getAboveComment();
		String[] expectedCpp= {"a1", "a2", "b", "s1", "u1", "u2"};
		String[] expectedC= {"a1", "a2", "b", "u1", "u2"};
		checkCompletion(code, true, expectedCpp);
		checkCompletion(code, false, expectedC);
	}
}
