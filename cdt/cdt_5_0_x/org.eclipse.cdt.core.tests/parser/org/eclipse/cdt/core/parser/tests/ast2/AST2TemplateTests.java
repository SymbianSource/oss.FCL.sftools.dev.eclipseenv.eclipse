/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     Bryan Wilkinson (QNX)
 *     Andrew Ferguson (Symbian)
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.core.parser.tests.ast2;

import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.SemanticUtil.getUltimateType;
import junit.framework.TestSuite;

import org.eclipse.cdt.core.dom.ast.ASTTypeUtil;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IQualifierType;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBasicType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassTemplate;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassTemplatePartialSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPField;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunctionTemplate;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPReferenceType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateInstance;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateNonTypeParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateTypeParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPVariable;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.core.parser.util.ObjectMap;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPDeferredClassInstance;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPInternalUnknownScope;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPUnknownBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;

/**
 * @author aniefer
 */
public class AST2TemplateTests extends AST2BaseTest {
	
	public AST2TemplateTests() {
	}
	
	public AST2TemplateTests(String name) {
		super(name);
	}
	
	public static TestSuite suite() {
		return suite(AST2TemplateTests.class);
	}
	
	public void testBasicClassTemplate() throws Exception {
		IASTTranslationUnit tu = parse("template <class T> class A{ T t; };", ParserLanguage.CPP); //$NON-NLS-1$
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertEquals(col.size(), 4);
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		
		ICPPTemplateScope scope = (ICPPTemplateScope) T.getScope();
		IScope s2 = A.getScope();
		assertSame(scope, s2);
		
		ICPPField t = (ICPPField) col.getName(3).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		
		assertSame(T, T2);
		IType type = t.getType();
		assertSame(type, T);
		
		assertNotNull(T);
		assertNotNull(A);
	}
	
	// template < class T > class A {             
	//    T t1;                                   
	//    T * t2;                                 
	// };                                         
	// void f(){                                  
	//    A<int> a;                               
	//    a.t1; a.t2;                             
	// }                                          
	public void testBasicTemplateInstance_1() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertEquals(col.size(), 14);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPField t1 = (ICPPField) col.getName(3).resolveBinding();
		ICPPField t2 = (ICPPField) col.getName(5).resolveBinding();
		
		assertSame(t1.getType(), T);
		assertSame(((IPointerType)t2.getType()).getType(), T);
		
		ICPPVariable a = (ICPPVariable) col.getName(9).resolveBinding();
		
		ICPPClassType A_int = (ICPPClassType) col.getName(7).resolveBinding();
		assertSame(A_int, a.getType());
		
		assertTrue(A_int instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A_int).getTemplateDefinition(), A);
		
		ICPPClassScope A_int_Scope = (ICPPClassScope) A_int.getCompositeScope();
		assertNotSame(A_int_Scope, ((ICompositeType) A).getCompositeScope());
		
		ICPPField t = (ICPPField) col.getName(11).resolveBinding();
		assertTrue(t instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)t).getSpecializedBinding(), t1);
		assertSame(t.getScope(), A_int_Scope);
		IType type = t.getType();
		assertTrue(type instanceof IBasicType);
		assertEquals(((IBasicType)type).getType(), IBasicType.t_int);
		
		t = (ICPPField) col.getName(13).resolveBinding();
		assertTrue(t instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)t).getSpecializedBinding(), t2);
		assertSame(t.getScope(), A_int_Scope);
		type = t.getType();
		assertTrue(type instanceof IPointerType);
		assertTrue(((IPointerType)type).getType() instanceof IBasicType);
		assertEquals(((IBasicType)((IPointerType)type).getType()).getType(), IBasicType.t_int);
	}
	
	// template < class T > class A {             
	//    T f(T *);                              
	// };                                         
	// void g(){                                  
	//    A<int> a;                               
	//    a.f((int*)0);                         
	// }                                          
	public void testBasicTemplateInstance_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassType A = (ICPPClassType) col.getName(1).resolveBinding();
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPMethod f = (ICPPMethod) col.getName(3).resolveBinding();
		IFunctionType ft = f.getType();
		
		assertSame(ft.getReturnType(), T);
		assertSame(((IPointerType)ft.getParameterTypes()[0]).getType(), T);
		
		ICPPClassType A_int = (ICPPClassType) col.getName(7).resolveBinding();
		assertTrue(A_int instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A_int).getTemplateDefinition(), A);
		
		ICPPMethod f_int = (ICPPMethod) col.getName(11).resolveBinding();
		assertTrue(f_int instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f_int).getSpecializedBinding(), f);
		ft = f_int.getType();
		assertTrue(ft.getReturnType() instanceof IBasicType);
		assertTrue(((IPointerType)ft.getParameterTypes()[0]).getType() instanceof IBasicType);
	}
	
	// template <class T > void f(T);          
	// template <class T > void f(T) {         
	//    T * d;                                 
	// }                                         
	// void foo() {                              
	//    f<int>(0);                           
	// }                                         
	public void testBasicTemplateFunction() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		
		IParameter p1 = (IParameter) col.getName(3).resolveBinding();
		
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(4).resolveBinding();
		ICPPFunction f2 = (ICPPFunction) col.getName(5).resolveBinding();
		IParameter p2 = (IParameter) col.getName(7).resolveBinding();
		
		assertSame(T, T2);
		assertSame(f, f2);
		assertSame(p1, p2);
		assertSame(p1.getType(), T);
		
		ICPPFunction f3 = (ICPPFunction) col.getName(11).resolveBinding();
		assertTrue(f3 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f3).getTemplateDefinition(), f);
		
		assertInstances(col, T, 5);
	}
	
	// template < class T > class pair {                
	//    template < class U > pair(const pair<U> &); 
	// };                                               
	public void testStackOverflow() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertTrue(col.getName(0).resolveBinding() instanceof ICPPTemplateParameter);
		ICPPClassTemplate pair = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter U = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		assertTrue(col.getName(3).resolveBinding() instanceof ICPPFunctionTemplate);
		ICPPTemplateInstance pi = (ICPPTemplateInstance) col.getName(4).resolveBinding();
		ICPPClassTemplate p = (ICPPClassTemplate) col.getName(5).resolveBinding();
		ICPPTemplateParameter U2 = (ICPPTemplateParameter) col.getName(6).resolveBinding();
		
		assertSame(U, U2);
		assertSame(pair, p);
		assertSame(pi.getTemplateDefinition(), pair);
	}
	
	// template < class T > class A {};       
	// template < class T > class A< T* > {}; 
	// template < class T > class A< T** > {}; 
	public void testBasicClassPartialSpecialization() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T1 = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		ICPPClassTemplatePartialSpecialization A2 = (ICPPClassTemplatePartialSpecialization) col.getName(3).resolveBinding();
		ICPPTemplateParameter T3 = (ICPPTemplateParameter) col.getName(5).resolveBinding();
		ICPPClassTemplatePartialSpecialization A3 = (ICPPClassTemplatePartialSpecialization) col.getName(7).resolveBinding();
		ICPPTemplateParameter T4 = (ICPPTemplateParameter) col.getName(6).resolveBinding();
		
		assertSame(A2.getPrimaryClassTemplate(), A1);
		assertSame(A3.getPrimaryClassTemplate(), A1);
		assertNotSame(T1, T2);
		assertNotSame(A1, A2);
		assertNotSame(A1, A3);
		assertNotSame(A2, A3);
		assertSame(T2, T3);
		assertNotSame(T2, T4);
	}
	
	// template < class T > class A { typedef int TYPE; };  
	// template < class T > A<T>::TYPE foo(T);            
	// template < class T > A<T>::TYPE foo(T);            
	public void testStackOverflow_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T0 = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateParameter T1 = (ICPPTemplateParameter) col.getName(3).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(12).resolveBinding();
		
		assertNotSame(T0, T1);
		assertSame(T1, T2);
		
		ICPPFunctionTemplate foo1 = (ICPPFunctionTemplate) col.getName(9).resolveBinding();
		ICPPFunctionTemplate foo2 = (ICPPFunctionTemplate) col.getName(18).resolveBinding();
		assertSame(foo1, foo2);
		
		ITypedef TYPE = (ITypedef) col.getName(2).resolveBinding();
		IBinding b0 = col.getName(8).resolveBinding();
		IBinding b1 = col.getName(17).resolveBinding();
		assertSame(b1, b0);
		
		// the instantiation of A<T> has to be deferred.
		assertInstance(b0, ICPPUnknownBinding.class);
		final ICPPBinding parent = ((ICPPInternalUnknownScope)b0.getScope()).getScopeBinding();
		assertInstance(parent, ICPPDeferredClassInstance.class);
		assertSame(((ICPPDeferredClassInstance) parent).getSpecializedBinding(), A);

		assertInstances(col, T1, 6);
	}
	
	// template < class T > class A {                       
	//    void f();                                         
	// };                                                   
	// template < class T > void A<T>::f() { }              
	public void testTemplateMemberDef() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPMethod f1 = (ICPPMethod) col.getName(2).resolveBinding();
		ICPPMethod f2 = (ICPPMethod) col.getName(8).resolveBinding();
		
		assertSame(f2, f1);
	}
	
	// template < class T > void f (T);           
	// void main() {                                
	//    f(1);                                   
	// }                                            
	public void testTemplateFunctionImplicitInstantiation() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		IFunction f2 = (IFunction) col.getName(5).resolveBinding();
		
		assertTrue(f2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f2).getTemplateDefinition(), f1);
	}
	
	// template < class T > void f(T);         // #1 
	// template < class T > void f(T*);        // #2
	// template < class T > void f(const T*);  // #3 
	// void main() {                            
	//    const int *p;                         
	//    f(p); //calls f(const T *) , 3 is more specialized than 1 or 2
	// }                                        
	public void test_14_5_5_2s5_OrderingFunctionTemplates_1() throws Exception{
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate f2 = (ICPPFunctionTemplate) col.getName(5).resolveBinding();
		ICPPFunctionTemplate f3 = (ICPPFunctionTemplate) col.getName(9).resolveBinding();
		
		assertNotSame(f1, f2);
		assertNotSame(f2, f3);
		assertNotSame(f3, f1);
		
		IFunction f = (IFunction) col.getName(14).resolveBinding();
		assertTrue(f instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f).getTemplateDefinition(), f3);
	}
	
	// template < class T > void f(T);    // #1
	// template < class T > void f(T&);   // #2
	// void main() {                            
	//    float x;                              
	//    f(x); //ambiguous 1 or 2
	// }                                        
	public void test_14_5_5_2s5_OrderingFunctionTemplates_2() throws Exception{
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate f2 = (ICPPFunctionTemplate) col.getName(5).resolveBinding();
		
		assertNotSame(f1, f2);
		
		IProblemBinding f = (IProblemBinding) col.getName(10).resolveBinding();
		assertEquals(f.getID(), IProblemBinding.SEMANTIC_AMBIGUOUS_LOOKUP);
	}
	
	// template < class T, template < class X > class U, T *pT > class A {   
	// };                                                                    
	public void testTemplateParameters() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateTypeParameter T = (ICPPTemplateTypeParameter) col.getName(0).resolveBinding();
		ICPPTemplateTemplateParameter U = (ICPPTemplateTemplateParameter) col.getName(2).resolveBinding();
		ICPPTemplateNonTypeParameter pT = (ICPPTemplateNonTypeParameter) col.getName(4).resolveBinding();
		
		ICPPTemplateTypeParameter X = (ICPPTemplateTypeParameter) col.getName(1).resolveBinding();
		
		ICPPTemplateParameter[] ps = U.getTemplateParameters();
		assertEquals(ps.length, 1);
		assertSame(ps[0], X);
		
		IPointerType ptype = (IPointerType) pT.getType();
		assertSame(ptype.getType(), T);
	}
	
	// template <class T> class A {        
	//    A<T>* a;                         
	//    A<T>* a2;                        
	// };                                  
	// void f(){                           
	//    A<int> * b;                      
	//    b->a;                            
	// }                                   
	public void testDeferredInstances() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPTemplateInstance A_T = (ICPPTemplateInstance) col.getName(2).resolveBinding();
		assertSame(A_T.getTemplateDefinition(), A);
		
		ICPPTemplateInstance A_T2 = (ICPPTemplateInstance) col.getName(6).resolveBinding();
		assertSame(A_T, A_T2);
		
		ICPPVariable a = (ICPPVariable) col.getName(5).resolveBinding();
		IPointerType pt = (IPointerType) a.getType();
		assertSame(pt.getType(), A_T);
		
		ICPPVariable b = (ICPPVariable) col.getName(13).resolveBinding();
		IType bt = b.getType();
		assertTrue(bt instanceof IPointerType);
		
		ICPPVariable a2 = (ICPPVariable) col.getName(15).resolveBinding();
		assertTrue(a2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)a2).getSpecializedBinding(), a);
		IType at = a2.getType();
		assertTrue(at instanceof IPointerType);
		
		assertSame(((IPointerType)at).getType(), ((IPointerType)bt).getType());
	}
	
	// template < class T1, class T2, int I > class A                {}; //#1   
	// template < class T, int I >            class A < T, T*, I >   {}; //#2   
	// template < class T1, class T2, int I > class A < T1*, T2, I > {}; //#3   
	// template < class T >                   class A < int, T*, 5 > {}; //#4   
	// template < class T1, class T2, int I > class A < T1, T2*, I > {}; //#5   
	//
	// A <int, int, 1>   a1;		//uses #1                                    
	// A <int, int*, 1>  a2;		//uses #2, T is int, I is 1                  
	// A <int, char*, 5> a3;		//uses #4, T is char                         
	// A <int, char*, 1> a4;		//uses #5, T is int, T2 is char, I is1       
	// A <int*, int*, 2> a5;		//ambiguous, matches #3 & #5.                
	public void test_14_5_4_1s2_MatchingTemplateSpecializations() throws Exception{
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(3).resolveBinding();
		ICPPClassTemplate A2 = (ICPPClassTemplate) col.getName(6).resolveBinding();
		ICPPClassTemplate A3 = (ICPPClassTemplate) col.getName(14).resolveBinding();
		ICPPClassTemplate A4 = (ICPPClassTemplate) col.getName(20).resolveBinding();
		ICPPClassTemplate A5 = (ICPPClassTemplate) col.getName(26).resolveBinding();
		
		assertTrue(A3 instanceof ICPPClassTemplatePartialSpecialization);
		assertSame(((ICPPClassTemplatePartialSpecialization)A3).getPrimaryClassTemplate(), A1);
		
		ICPPTemplateTypeParameter T1 = (ICPPTemplateTypeParameter) col.getName(11).resolveBinding();
		ICPPTemplateTypeParameter T2 = (ICPPTemplateTypeParameter) col.getName(12).resolveBinding();
		ICPPTemplateNonTypeParameter I = (ICPPTemplateNonTypeParameter) col.getName(13).resolveBinding();
		
		ICPPTemplateParameter TR1 = (ICPPTemplateParameter) col.getName(16).resolveBinding();
		ICPPTemplateParameter TR2 = (ICPPTemplateParameter) col.getName(17).resolveBinding();
		ICPPTemplateParameter TR3 = (ICPPTemplateParameter) col.getName(18).resolveBinding();
		
		assertSame(T1, TR1);
		assertSame(T2, TR2);
		assertSame(I, TR3);
		
		ICPPTemplateInstance R1 = (ICPPTemplateInstance) col.getName(31).resolveBinding();
		ICPPTemplateInstance R2 = (ICPPTemplateInstance) col.getName(34).resolveBinding();
		ICPPTemplateInstance R3 = (ICPPTemplateInstance) col.getName(37).resolveBinding();
		ICPPTemplateInstance R4 = (ICPPTemplateInstance) col.getName(40).resolveBinding();
		IProblemBinding R5 = (IProblemBinding) col.getName(43).resolveBinding();
		assertEquals(R5.getID(), IProblemBinding.SEMANTIC_AMBIGUOUS_LOOKUP);
		
		assertSame(R1.getTemplateDefinition(), A1);
		assertSame(R2.getTemplateDefinition(), A2);
		assertSame(R4.getTemplateDefinition(), A5);
		assertSame(R3.getTemplateDefinition(), A4);
	}
	
	// template <class T> void f(T);                  
	// template <class T> void f(T*);                 
	// template <> void f(int);       //ok            
	// template <> void f<int>(int*); //ok            
	public void test14_7_3_FunctionExplicitSpecialization() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate fT1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate fT2 = (ICPPFunctionTemplate) col.getName(5).resolveBinding();
		
		ICPPSpecialization f1 = (ICPPSpecialization) col.getName(8).resolveBinding();
		ICPPSpecialization f2 = (ICPPSpecialization) col.getName(10).resolveBinding();
		
		assertSame(f1.getSpecializedBinding(), fT1);
		assertSame(f2.getSpecializedBinding(), fT2);
	}
	
	// template<class T> void f(T*);        
	// void g(int* p) { f(p); }             
	public void test_14_5_5_1_FunctionTemplates_1() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		
		ICPPFunction ref = (ICPPFunction) col.getName(6).resolveBinding();
		assertTrue(ref instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)ref).getTemplateDefinition(), f);
	}
	
	// template<class T> void f(T);        
	// void g(int* p) { f(p); }            
	public void test_14_5_5_1_FunctionTemplates_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		
		ICPPFunction ref = (ICPPFunction) col.getName(6).resolveBinding();
		assertTrue(ref instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)ref).getTemplateDefinition(), f);
	}
	
	// template<class X, class Y> X f(Y);                      
	// void g(){                                               
	//    int i = f<int>(5); // Y is int                       
	// }                                                       
	public void test_14_8_1s2_FunctionTemplates() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(3).resolveBinding();
		ICPPFunction ref1 = (ICPPFunction) col.getName(8).resolveBinding();
		
		assertTrue(ref1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance) ref1).getTemplateDefinition(), f);
	}
	
	// template<class T> void f(T);  
	// void g(){                     
	//    f("Annemarie");          
	// }                             
	public void test14_8_3s6_FunctionTemplates() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunction ref = (ICPPFunction) col.getName(5).resolveBinding();
		assertTrue(ref instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)ref).getTemplateDefinition(), f);
	}
	
	// template<class T> void f(T);         // #1
	// template<class T> void f(T*, int=1); // #2
	// template<class T> void g(T);         // #3
	// template<class T> void g(T*, ...);   // #4
	// int main() {                              
	//    int* ip;                               
	//    f(ip);                       //calls #2
	//    g(ip);                       //calls #4
	// }                                         
	public void test14_5_5_2s6_FunctionTemplates() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate f2 = (ICPPFunctionTemplate) col.getName(5).resolveBinding();
		assertNotSame(f1, f2);
		
		ICPPFunctionTemplate g1 = (ICPPFunctionTemplate) col.getName(10).resolveBinding();
		ICPPFunctionTemplate g2 = (ICPPFunctionTemplate) col.getName(14).resolveBinding();
		assertNotSame(g1, g2);
		
		ICPPFunction ref1 = (ICPPFunction) col.getName(19).resolveBinding();
		ICPPFunction ref2 = (ICPPFunction) col.getName(21).resolveBinding();
		
		assertTrue(ref1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance) ref1).getTemplateDefinition(), f2);
		
		assertTrue(ref2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance) ref2).getTemplateDefinition(), g2);
	}
	
	// template<class T> class X {           
	//    X* p;               // meaning X<T>
	//    X<T>* p2;                          
	// };                                    
	public void test14_6_1s1_LocalNames() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate X = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType x1 = (ICPPClassType) col.getName(2).resolveBinding();
		ICPPClassType x2 = (ICPPClassType) col.getName(4).resolveBinding();
		
		assertTrue(x1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)x1).getTemplateDefinition(), X);
		
		assertSame(x1, x2);
	}
	
	// template<class T> T f(T* p){                  
	// };                                            
	// void g(int a, char* b){                       
	//    f(&a);              //call f<int>(int*)    
	//    f(&b);              //call f<char*>(char**)
	// }                                             
	public void test14_8s2_() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(2).resolveBinding();
		
		ICPPFunction f1 = (ICPPFunction) col.getName(8).resolveBinding();
		ICPPFunction f2 = (ICPPFunction) col.getName(10).resolveBinding();
		
		assertNotSame(f1, f2);
		assertTrue(f1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f1).getTemplateDefinition(), f);
		assertTrue(f2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f2).getTemplateDefinition(), f);
		
		IType fr1 = f1.getType().getReturnType();
		IType fr2 = f2.getType().getReturnType();
		
		assertTrue(fr1 instanceof IBasicType);
		assertEquals(((IBasicType)fr1).getType(), IBasicType.t_int);
		
		assertTrue(fr2 instanceof IPointerType);
		assertTrue(((IPointerType)fr2).getType() instanceof IBasicType);
		assertEquals(((IBasicType) ((IPointerType)fr2).getType()).getType(), IBasicType.t_char);
	}
	
	// template<class T> void f(T) {  }                  
	// template<class T> inline T g(T) {  }              
	// template<> inline void f<>(int) {  } //OK: inline 
	// template<> int g<>(int) {  }     // OK: not inline
	public void test14_7_3s14() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate g1 = (ICPPFunctionTemplate) col.getName(6).resolveBinding();
		
		ICPPSpecialization f2 = (ICPPSpecialization) col.getName(9).resolveBinding();
		ICPPSpecialization g2 = (ICPPSpecialization) col.getName(12).resolveBinding();
		
		assertSame(f2.getSpecializedBinding(), f1);
		assertSame(g2.getSpecializedBinding(), g1);
		
		assertFalse(((ICPPFunction)f1).isInline());
		assertTrue(((ICPPFunction)g1).isInline());
		assertTrue(((ICPPFunction)f2).isInline());
		assertFalse(((ICPPFunction)g2).isInline());
	}
	
	// template<class T> class X {                                       
	//    X<T*> a; // implicit generation of X<T> requires               
	//             // the implicit instantiation of X<T*> which requires 
	//             // the implicit instantiation of X<T**> which ...     
	// };                                                                
	// void f() {                                                        
	//    X<int> x;                                                      
	//    x.a.a.a.a;                                                     
	// }                                                                 
	public void test14_7_1s14_InfiniteInstantiation() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate X = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPVariable x = (ICPPVariable) col.getName(9).resolveBinding();
		IType t = x.getType();
		assertTrue(t instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance) t).getTemplateDefinition(), X);
		
		ICPPField a = (ICPPField) col.getName(5).resolveBinding();
		ICPPField a1 = (ICPPField) col.getName(11).resolveBinding();
		ICPPField a2 = (ICPPField) col.getName(12).resolveBinding();
		ICPPField a3 = (ICPPField) col.getName(13).resolveBinding();
		ICPPField a4 = (ICPPField) col.getName(14).resolveBinding();
		
		assertTrue(a1 instanceof ICPPSpecialization);
		assertTrue(a2 instanceof ICPPSpecialization);
		assertTrue(a3 instanceof ICPPSpecialization);
		assertTrue(a4 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)a1).getSpecializedBinding(), a);
		assertSame(((ICPPSpecialization)a2).getSpecializedBinding(), a);
		assertSame(((ICPPSpecialization)a3).getSpecializedBinding(), a);
		assertSame(((ICPPSpecialization)a4).getSpecializedBinding(), a);
	}
	
	// template<class T> class Y;         
	// template<> class Y<int> {          
	//    Y* p; // meaning Y<int>         
	//    Y<char>* q; // meaning Y<char>  
	// };                                 
	public void test14_6_1s2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate Y = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPSpecialization Yspec = (ICPPSpecialization) col.getName(2).resolveBinding();
		
		assertTrue(Yspec instanceof ICPPClassType);
		assertSame(Yspec.getSpecializedBinding(), Y);
		
		ICPPClassType y1 = (ICPPClassType) col.getName(4).resolveBinding();
		assertSame(y1, Yspec);
		
		ICPPClassType y2 = (ICPPClassType) col.getName(6).resolveBinding();
		assertTrue(y2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)y2).getTemplateDefinition(), Y);
	}
	
	// template < class T, class U > void f (T (*) (T, U));   
	// int g (int, char);                                       
	// void foo () {                                              
	//    f(g);                                                 
	// }                                                          
	public void testBug45129() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunction f1 = (ICPPFunction) col.getName(2).resolveBinding();
		ICPPFunction g1 = (ICPPFunction) col.getName(9).resolveBinding();
		
		IBinding f2 = col.getName(13).resolveBinding();
		IBinding g2 = col.getName(14).resolveBinding();
		
		assertTrue(f2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f2).getTemplateDefinition(), f1);
		assertSame(g1, g2);
	}
	
	// template <class T, class U = T > U f(T);   
	// void g() {                                   
	//    f(1);                                   
	// }                                            
	public void testBug76951_1() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		assertSame(T, T2);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(4).resolveBinding();
		ICPPFunction f2 = (ICPPFunction) col.getName(8).resolveBinding();
		
		assertTrue(f2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f2).getTemplateDefinition(), f1);
		
		IFunctionType ft = f2.getType();
		assertTrue(ft.getReturnType() instanceof IBasicType);
		assertEquals(((IBasicType)ft.getReturnType()).getType(), IBasicType.t_int);
	}
	
	// template <class T, class U = T > class A {   
	//    U u;                                      
	// };                                           
	// void f() {                                   
	//    A<int> a;                                 
	//    a.u;                                      
	// }                                            
	public void testBug76951_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPTemplateTypeParameter U = (ICPPTemplateTypeParameter) col.getName(1).resolveBinding();
		assertSame(U.getDefault(), T);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(3).resolveBinding();
		ICPPField u1 = (ICPPField) col.getName(5).resolveBinding();
		assertSame(u1.getType(), U);
		
		ICPPClassType A1 = (ICPPClassType) col.getName(7).resolveBinding();
		assertTrue(A1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A1).getTemplateDefinition(), A);
		
		ICPPField u2 = (ICPPField) col.getName(11).resolveBinding();
		assertTrue(u2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)u2).getSpecializedBinding(), u1);
		
		IType type = u2.getType();
		assertTrue(type instanceof IBasicType);
		assertEquals(((IBasicType)type).getType(), IBasicType.t_int);
	}
	
	// template < class T > class A {               
	//    A< int > a;                               
	// };                                           
	// void f(A<int> p) { }                       
	public void testInstances() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType A1 = (ICPPClassType) col.getName(2).resolveBinding();
		ICPPClassType A2 = (ICPPClassType) col.getName(6).resolveBinding();
		
		assertSame(A1, A2);
		assertTrue(A1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A1).getTemplateDefinition(), A);
	}
	
	// template <class T> void f(T);      
	// template <class T> void f(T) {}    
	public void testTemplateParameterDeclarations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T1 = (ICPPTemplateParameter) col.getName(4).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		
		assertSame(T1, T2);
		
		assertInstances(col, T1, 4);
	}
	
	// template < class T > class A {                                
	//    int f(A *);                                              
	//    A < T > *pA;                                               
	// };                                                            
	// void f () {                                                   
	//    A< int > *a;                                               
	//    a->f(a);                                                 
	//    a->pA;                                                     
	// };                                                            
	public void testDeferredInstantiation() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPMethod f = (ICPPMethod) col.getName(2).resolveBinding();
		ICPPClassType A1 = (ICPPClassType) col.getName(3).resolveBinding();
		ICPPClassType A2 = (ICPPClassType) col.getName(5).resolveBinding();
		ICPPField pA = (ICPPField) col.getName(8).resolveBinding();
		
		assertSame(A1, A2);
		assertTrue(A1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A1).getTemplateDefinition(), A);
		
		ICPPClassType AI = (ICPPClassType) col.getName(10).resolveBinding();
		ICPPMethod f2 = (ICPPMethod) col.getName(14).resolveBinding();
		ICPPField pA2 = (ICPPField) col.getName(17).resolveBinding();
		
		assertTrue(f2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f2).getSpecializedBinding(), f);
		assertTrue(pA2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)pA2).getSpecializedBinding(), pA);
		
		IType paT = pA2.getType();
		assertTrue(paT instanceof IPointerType);
		assertSame(((IPointerType)paT).getType(), AI);
		
		IParameter p = f2.getParameters()[0];
		IType pT = p.getType();
		assertTrue(pT instanceof IPointerType);
		assertSame(((IPointerType)pT).getType(), AI);
	}
	
	// template <class T> struct A {                                 
	//    void f(int);                                               
	//    template <class T2> void f(T2);                            
	// };                                                            
	// template <> void A<int>::f(int) { } //nontemplate             
	// template <> template <> void A<int>::f<>(int) { } //template  
	// int main() {                                                  
	//    A<int> ac;                                                 
	//    ac.f(1);   //nontemplate                                   
	//    ac.f('c'); //template                                      
	//    ac.f<>(1); //template                                      
	// }                                                             
	public void test14_5_2s2_MemberSpecializations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPMethod f1 = (ICPPMethod) col.getName(2).resolveBinding();
		ICPPMethod f2 = (ICPPMethod) col.getName(5).resolveBinding();
		
		ICPPMethod f1_2 = (ICPPMethod) col.getName(11).resolveBinding();
		assertNotSame(f1, f1_2);
		assertTrue(f1_2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f1_2).getSpecializedBinding(), f1);
		
		ICPPClassType A2 = (ICPPClassType) col.getName(9).resolveBinding();
		assertTrue(A2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A2).getTemplateDefinition(), A);
		
		ICPPMethod f2_2 = (ICPPMethod) col.getName(16).resolveBinding();
		assertTrue(f2_2 instanceof ICPPSpecialization);
		IBinding speced = ((ICPPSpecialization)f2_2).getSpecializedBinding();
		assertTrue(speced instanceof ICPPFunctionTemplate && speced instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)speced).getSpecializedBinding(), f2);
		
		ICPPClassType A3 = (ICPPClassType) col.getName(14).resolveBinding();
		assertSame(A2, A3);
		
		ICPPClassType A4 = (ICPPClassType) col.getName(20).resolveBinding();
		assertSame(A2, A4);
		
		IFunction r1 = (IFunction) col.getName(24).resolveBinding();
		IFunction r2 = (IFunction) col.getName(26).resolveBinding();
		IFunction r3 = (IFunction) col.getName(28).resolveBinding();
		
		assertSame(r1, f1_2);
		assertTrue(r2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)r2).getTemplateDefinition(), speced);
		assertSame(r3, f2_2);
	}
	
	// template <class T> class A { };       
	// template <> class A<int> {};          
	// A<char> ac;                           
	// A<int> ai;                            
	public void testClassSpecializations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType A2 = (ICPPClassType) col.getName(2).resolveBinding();
		
		assertTrue(A2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)A2).getSpecializedBinding(), A1);
		
		ICPPClassType r1 = (ICPPClassType) col.getName(4).resolveBinding();
		ICPPClassType r2 = (ICPPClassType) col.getName(7).resolveBinding();
		
		assertTrue(r1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)r1).getTemplateDefinition(), A1);
		assertSame(r2, A2);
	}
	
	// template<class T> struct A {                                           
	//    void f(T) {  }                                                      
	// };                                                                     
	// template<> struct A<int> {                                             
	//    void f(int);                                                        
	// };                                                                     
	// void h(){                                                              
	//    A<int> a;                                                           
	//    a.f(16);   // A<int>::f must be defined somewhere                   
	// }                                                                      
	// // explicit specialization syntax not used for a member of             
	// // explicitly specialized class template specialization                
	// void A<int>::f(int) {  }                                               
	public void test14_7_3s5_SpecializationMemberDefinition() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPMethod f1 = (ICPPMethod) col.getName(2).resolveBinding();
		
		ICPPClassType A2 = (ICPPClassType) col.getName(5).resolveBinding();
		assertTrue(A2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)A2).getSpecializedBinding(), A1);
		
		ICPPMethod f2 = (ICPPMethod) col.getName(7).resolveBinding();
		assertNotSame(f1, f2);
		
		ICPPClassType A3 = (ICPPClassType) col.getName(10).resolveBinding();
		assertSame(A3, A2);
		ICPPMethod f3 = (ICPPMethod) col.getName(14).resolveBinding();
		assertSame(f3, f2);
		
		ICPPClassType A4 = (ICPPClassType) col.getName(16).resolveBinding();
		assertSame(A4, A2);
		ICPPMethod f4 = (ICPPMethod) col.getName(18).resolveBinding();
		assertSame(f4, f3);
	}
	
	// class C{};                                                     
	// template <class T> class A {                                   
	//    template <class T2> class B {                               
	//       T f(T2);                                               
	//    };                                                          
	// };                                                             
	// void g(){                                                      
	//    A<int>::B<C> b;                                             
	//    C c;                                                        
	//    b.f(c);                                                   
	// }                                                              
	public void testNestedSpecializations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassType C = (ICPPClassType) col.getName(0).resolveBinding();
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(2).resolveBinding();
		ICPPClassTemplate B = (ICPPClassTemplate) col.getName(4).resolveBinding();
		ICPPMethod f = (ICPPMethod) col.getName(6).resolveBinding();
		
		ICPPClassType A1 = (ICPPClassType) col.getName(11).resolveBinding();
		assertTrue(A1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A1).getTemplateDefinition(), A);
		
		ICPPClassType B1 = (ICPPClassType) col.getName(13).resolveBinding();
		assertTrue(B1 instanceof ICPPTemplateInstance);
		ICPPClassType B2 = (ICPPClassType) ((ICPPTemplateInstance)B1).getTemplateDefinition();
		assertTrue(B2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)B2).getSpecializedBinding(), B);
		
		ICPPMethod f1 = (ICPPMethod) col.getName(20).resolveBinding();
		assertTrue(f1 instanceof ICPPSpecialization);
		assertTrue(((ICPPSpecialization)f1).getSpecializedBinding() instanceof ICPPMethod);
		ICPPMethod f2 = (ICPPMethod) ((ICPPSpecialization)f1).getSpecializedBinding();
		assertTrue(f2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f2).getSpecializedBinding(), f);
		
		IFunctionType ft = f1.getType();
		assertTrue(ft.getReturnType() instanceof IBasicType);
		assertEquals(((IBasicType)ft.getReturnType()).getType(), IBasicType.t_int);
		
		assertSame(ft.getParameterTypes()[0], C);
	}
	
	// namespace N {                                                
	//    template<class T1, class T2> class A { };                 
	// }                                                            
	// using N::A;                                                  
	// namespace N {                                                
	//    template<class T> class A<T, T*> { };                     
	// }                                                            
	// A<int,int*> a;                                               
	public void test14_5_4s7_UsingClassTemplate() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(3).resolveBinding();
		ICPPClassTemplatePartialSpecialization A2 = (ICPPClassTemplatePartialSpecialization) col.getName(9).resolveBinding();
		
		ICPPClassType A3 = (ICPPClassType) col.getName(13).resolveBinding();
		assertTrue(A3 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A3).getTemplateDefinition(), A2);
		
		ICPPClassTemplate A4 = (ICPPClassTemplate) col.getName(14).resolveBinding();
		assertSame(A4, A1);
	}
	
	// template<class T> class A {                      
	//    int x;                                        
	// };                                               
	// template<class T> class A<T*> {                  
	//    char x;                                       
	// };                                               
	// template<template<class U> class V> class C {    
	//    V<int> y;                                     
	//    V<int*> z;                                    
	// };                                               
	// void f() {                                       
	//    C<A> c;                                       
	//    c.y.x;   c.z.x;                               
	// }                                                
	public void testTemplateTemplateParameter() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPField x1 = (ICPPField) col.getName(2).resolveBinding();
		ICPPClassTemplatePartialSpecialization A2 = (ICPPClassTemplatePartialSpecialization) col.getName(4).resolveBinding();
		ICPPField x2 = (ICPPField) col.getName(7).resolveBinding();
		
		ICPPClassTemplate C = (ICPPClassTemplate) col.getName(10).resolveBinding();
		ICPPField y = (ICPPField) col.getName(13).resolveBinding();
		ICPPField z = (ICPPField) col.getName(16).resolveBinding();
		
		ICPPClassType C1 = (ICPPClassType) col.getName(18).resolveBinding();
		assertTrue(C1 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)C1).getTemplateDefinition(), C);
		
		ICPPField y2 = (ICPPField) col.getName(23).resolveBinding();
		assertTrue(y2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)y2).getSpecializedBinding(), y);
		IType t = y2.getType();
		assertTrue(t instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)t).getTemplateDefinition(), A1);
		ICPPField x3 = (ICPPField) col.getName(24).resolveBinding();
		assertTrue(x3 instanceof ICPPSpecialization);
		assertEquals(((ICPPSpecialization)x3).getSpecializedBinding(), x1);
		
		ICPPField z2 = (ICPPField) col.getName(26).resolveBinding();
		assertTrue(z2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)z2).getSpecializedBinding(), z);
		t = z2.getType();
		assertTrue(t instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)t).getTemplateDefinition(), A2);
		ICPPField x4 = (ICPPField) col.getName(27).resolveBinding();
		assertTrue(x4 instanceof ICPPSpecialization);
		assertEquals(((ICPPSpecialization)x4).getSpecializedBinding(), x2);
	}
	
	// template <class T> class A {               
	//    typedef T _T;                           
	//   _T t;                                    
	// };                                         
	// void f() {                                 
	//    A<int> a;                               
	//    a.t;                                    
	// }                                          
	public void testNestedTypeSpecializations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ITypedef _T = (ITypedef) col.getName(3).resolveBinding();
		assertSame(_T.getType(), T);
		
		ICPPField t = (ICPPField) col.getName(5).resolveBinding();
		assertSame(t.getType(), _T);
		
		ICPPField t2 = (ICPPField) col.getName(11).resolveBinding();
		assertTrue(t2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)t2).getSpecializedBinding(), t);
		
		IType type = t2.getType();
		assertTrue(type instanceof ITypedef);
		assertTrue(type instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)type).getSpecializedBinding(), _T);
		
		type = ((ITypedef)type).getType();
		assertTrue(type instanceof IBasicType);
		assertEquals(((IBasicType)type).getType(), IBasicType.t_int);
	}
	
	// template <class T> class A {                          
	//    class B { T t; };                                  
	//    B b;                                               
	// };                                                    
	// void f() {                                            
	//    A<int> a;                                          
	//    a.b.t;                                             
	// }                                                     
	public void testNestedClassTypeSpecializations() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassType B = (ICPPClassType) col.getName(2).resolveBinding();
		ICPPField t = (ICPPField) col.getName(4).resolveBinding();
		assertSame(t.getType(), T);
		ICPPField b = (ICPPField) col.getName(6).resolveBinding();
		assertSame(b.getType(), B);
		
		ICPPField b2 = (ICPPField) col.getName(12).resolveBinding();
		ICPPField t2 = (ICPPField) col.getName(13).resolveBinding();
		
		assertTrue(b2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)b2).getSpecializedBinding(), b);
		
		IType type = b2.getType();
		assertTrue(type instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)type).getSpecializedBinding(), B);
		
		assertTrue(t2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)t2).getSpecializedBinding(), t);
		assertTrue(t2.getType() instanceof IBasicType);
		assertEquals(((IBasicType)t2.getType()).getType(), IBasicType.t_int);
	}
	
	// template <class T> class A {                     
	//    typedef typename T::X _xx;                    
	//    _xx s;                                        
	// };                                               
	// class B {};                                      
	// template < class T > class C {                   
	//    typedef T X;                                  
	// };                                               
	// void f() {                                       
	//    A< C<B> > a; a.s;                             
	// };                                               
	public void testTemplateParameterQualifiedType_1() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateTypeParameter T = (ICPPTemplateTypeParameter) col.getName(0).resolveBinding();
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		
		IBinding T1 = col.getName(3).resolveBinding();
		assertSame(T1, T);
		
		ICPPClassType X = (ICPPClassType) col.getName(4).resolveBinding();
		
		ITypedef _xx = (ITypedef) col.getName(5).resolveBinding();
		
		IBinding _xx2 = col.getName(6).resolveBinding();
		assertSame(_xx, _xx2);
		assertSame(_xx.getType(), X);
		
		ICPPField s = (ICPPField) col.getName(7).resolveBinding();
		
		ICPPClassType B = (ICPPClassType) col.getName(8).resolveBinding();
		ITypedef X2 = (ITypedef) col.getName(12).resolveBinding();
		
		ICPPClassType Acb = (ICPPClassType) col.getName(14).resolveBinding();
		assertTrue(Acb instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)Acb).getTemplateDefinition(), A);
		
		ICPPField  s2 = (ICPPField) col.getName(21).resolveBinding();
		assertTrue(s2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)s2).getSpecializedBinding(), s);
		
		IType t = s2.getType();
		//		assertTrue(t instanceof ITypedef);
		//		assertTrue(t instanceof ICPPSpecialization);
		//		assertSame(((ICPPSpecialization)t).getSpecializedBinding(), _xx);
		
		t = ((ITypedef)t).getType();
		assertTrue(t instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)t).getSpecializedBinding(), X2);
		
		t = ((ITypedef)t).getType();
		assertSame(t, B);
	}
	
	// template <class T> class A {               
	//    A<T> a;                                 
	//    void f();                               
	// };                                         
	// template <class U> void A<U>::f(){         
	//    U u;                                    
	// }                                          
	public void testTemplateScopes() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType A2 = (ICPPClassType) col.getName(2).resolveBinding();
		
		ICPPTemplateParameter U = (ICPPTemplateParameter) col.getName(7).resolveBinding();
		assertSame(U, T);
		ICPPClassType A3 = (ICPPClassType) col.getName(9).resolveBinding();
		assertSame(A, A3);
		
		
		ICPPTemplateParameter U2 = (ICPPTemplateParameter) col.getName(13).resolveBinding();
		assertSame(U, U2);
		assertSame(T, U);
	}
	
	// class A {                              
	//    template < class T > void f(T);     
	// };                                     
	// template <class U> void A::f<>(U){}    
	public void testTemplateScopes_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassType A = (ICPPClassType) col.getName(0).resolveBinding();
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(1).resolveBinding();
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(2).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(3).resolveBinding();
		assertSame(T, T2);
		
		ICPPTemplateParameter U = (ICPPTemplateParameter) col.getName(5).resolveBinding();
		assertSame(T, U);
		ICPPClassType A2 = (ICPPClassType) col.getName(7).resolveBinding();
		assertSame(A, A2);
		ICPPMethod f2 = (ICPPMethod) col.getName(8).resolveBinding();
		IBinding U2 = col.getName(10).resolveBinding();
		assertSame(U, U2);
		
		assertTrue(f2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f2).getSpecializedBinding(), f1);
	}
	
	// template<typename T>
    // class A {};
    //
    // class B {};
    // 
    // template<>
    // class A<B> {};
    //
    // class C {};
    //
    // A<B> ab;
    // A<C> ac;
    public void testEnclosingScopes_a() throws Exception {
    	BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
    	
    	ICPPSpecialization   b0= ba.assertNonProblem("A<B>", 4, ICPPSpecialization.class, ICPPClassType.class);
    	ICPPTemplateInstance b1= ba.assertNonProblem("A<C>", 4, ICPPTemplateInstance.class, ICPPClassType.class);
    	
    	ICPPClassType sc0= assertInstance(b0.getSpecializedBinding(), ICPPClassType.class);
    	ICPPClassType sc1= assertInstance(b1.getSpecializedBinding(), ICPPClassType.class);
    	assertTrue(sc0.isSameType(sc1));
    	    	
    	assertInstance(b0, ICPPSpecialization.class);
    	assertInstance(b1, ICPPTemplateInstance.class);
    	
    	assertInstance(b0.getScope(), ICPPTemplateScope.class);
    	
    	IScope ts0= ((ICPPClassType) b0.getSpecializedBinding()).getScope();
    	IScope ts1= ((ICPPClassType) b1.getSpecializedBinding()).getScope();
    	
    	assertInstance(ts0, ICPPTemplateScope.class);
    	
    	assertSame(ts0, ts1);
    	assertNotSame(ts0, b0.getScope());
    	assertSame(ts1, b1.getScope()); // a class instance exists in the same scope as the template its defined from
    }
    
    // template<typename T>
    // class A {
    //    public:
    //    class B {};
    // };
    //
    // class C {}; class D {};
    //
    // template<>
    // class A<C> {
    //   public:
    //   class B {};
    // };
    //
    // void refs() {
    //    A<C>::B acb;
    //    A<D>::B adb;
    // }
    public void testEnclosingScopes_b() throws Exception {
    	BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
    	
    	ICPPClassType b0= ba.assertNonProblem("B acb", 1, ICPPClassType.class);
    	ICPPClassType b1= ba.assertNonProblem("B adb", 1, ICPPClassType.class, ICPPSpecialization.class);
    	ICPPClassType b2= ba.assertNonProblem("A<C>", 4, ICPPClassType.class, ICPPSpecialization.class);
    	ICPPClassType b3= ba.assertNonProblem("A {", 1, ICPPClassType.class, ICPPTemplateDefinition.class);
    	ICPPClassType b4= ba.assertNonProblem("B {}", 1, ICPPClassType.class);
    	
    	assertFalse(b0 instanceof ICPPSpecialization);
    	
    	assertSame(b0.getScope(), b2.getCompositeScope());
    	ICPPClassScope cs1= assertInstance(b1.getScope(), ICPPClassScope.class);
    	assertInstance(cs1.getClassType(), ICPPTemplateInstance.class);
    	assertSame(b4.getScope(), b3.getCompositeScope());
    }
	
	// class A {};
	// 
	// template<typename T>
	// class X {
	// public:
	//    class Y {
	//    public:
	//       class Z {};
	//    };
	// };
	//
	// X<A>::Y::Z xayz;
    public void testEnclosingScopes_c() throws Exception {
    	BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
    	
    	ICPPClassType b0= ba.assertNonProblem("Y::Z x", 1, ICPPClassType.class);
    	ICPPClassType b1= ba.assertNonProblem("Z xayz", 1, ICPPClassType.class);
    	
    	ICPPClassScope cs0= assertInstance(b0.getScope(), ICPPClassScope.class);
    	assertInstance(cs0.getClassType(), ICPPSpecialization.class);
    	
    	ICPPClassScope cs1= assertInstance(b1.getScope(), ICPPClassScope.class);
    	assertInstance(cs1.getClassType(), ICPPSpecialization.class);    	
    }
    
    // class A {}; class B {};
    //
    // template<typename T1, typename T2>
    // class X {};
    //
    // template<typename T3>
    // class X<T3, A> {
    // public:
    //     class N {};
    // };
    //
    // X<B,A>::N n;
    public void testEnclosingScopes_d() throws Exception {
    	BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
    	
    	ICPPClassType b0= ba.assertNonProblem("N n", 1, ICPPClassType.class);
    	ICPPClassType b1= ba.assertNonProblem("N {", 1, ICPPClassType.class);
    	
    	ICPPClassScope s0= assertInstance(b0.getScope(), ICPPClassScope.class);
    	assertInstance(s0.getClassType(), ICPPTemplateInstance.class);
    	
    	ICPPClassScope s1= assertInstance(b1.getScope(), ICPPClassScope.class);
    	assertInstance(s1.getClassType(), ICPPTemplateDefinition.class);
    	
    	ICPPTemplateScope s2= assertInstance(s1.getClassType().getScope(), ICPPTemplateScope.class);
    }
	
	// template<class T> struct A {                              
	//    void f(T);                                             
	//    template<class X> void g(T,X);                         
	//    void h(T) { }                                          
	// };                                                        
	// template<> void A<int>::f(int);                           
	// template<class T> template<class X> void A<T>::g(T,X) { } 
	// template<> template<class X> void A<int>::g(int,X);       
	// template<> template<> void A<int>::g(int,char);           
	// template<> template<> void A<int>::g<char>(int,char);     
	// template<> void A<int>::h(int) { }                        
	public void test14_7_3s16() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassType A = (ICPPClassType) col.getName(1).resolveBinding();
		ICPPMethod f = (ICPPMethod) col.getName(2).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(3).resolveBinding();
		assertSame(T, T2);
		
		ICPPTemplateParameter X = (ICPPTemplateParameter) col.getName(5).resolveBinding();
		ICPPFunctionTemplate g = (ICPPFunctionTemplate) col.getName(6).resolveBinding();
		ICPPTemplateParameter T3 = (ICPPTemplateParameter) col.getName(7).resolveBinding();
		assertSame(T, T3);
		ICPPTemplateParameter X2 = (ICPPTemplateParameter) col.getName(9).resolveBinding();
		assertSame(X, X2);
		
		ICPPMethod h = (ICPPMethod) col.getName(11).resolveBinding();
		ICPPTemplateParameter T4 = (ICPPTemplateParameter) col.getName(12).resolveBinding();
		assertSame(T, T4);
		
		ICPPClassType A2 = (ICPPClassType) col.getName(15).resolveBinding();
		assertTrue(A2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A2).getTemplateDefinition(), A);
		ICPPMethod f2 = (ICPPMethod) col.getName(17).resolveBinding();
		assertTrue(f2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)f2).getSpecializedBinding(), f);
		
		ICPPTemplateParameter TR = (ICPPTemplateParameter) col.getName(19).resolveBinding();
		assertSame(T, TR);
		ICPPTemplateParameter XR = (ICPPTemplateParameter) col.getName(20).resolveBinding();
		assertSame(X, XR);
		ICPPClassType A3 = (ICPPClassType) col.getName(22).resolveBinding();
		assertSame(A3, A);
		
		ICPPMethod g2 = (ICPPMethod) col.getName(25).resolveBinding();
		assertSame(g2, g);
		TR = (ICPPTemplateParameter) col.getName(26).resolveBinding();
		assertSame(T, TR);
		XR = (ICPPTemplateParameter) col.getName(28).resolveBinding();
		assertSame(X, XR);
		
		assertSame(col.getName(32).resolveBinding(), A2);
		assertSame(col.getName(39).resolveBinding(), A2);
		assertSame(col.getName(45).resolveBinding(), A2);
		assertSame(col.getName(52).resolveBinding(), A2);
		
		ICPPMethod h2 = (ICPPMethod) col.getName(54).resolveBinding();
		assertTrue(h2 instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)h2).getSpecializedBinding(), h);
	}
	
	// namespace N {                                                 
	//    int C;                                                     
	//    template<class T> class B {                                
	//       void f(T);                                              
	//    };                                                         
	// }                                                             
	// template<class C> void N::B<C>::f(C) {                        
	//    C b; // C is the template parameter, not N::C              
	// }                                                             
	public void test14_6_1s6() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(2).resolveBinding();
		ICPPClassTemplate B = (ICPPClassTemplate) col.getName(3).resolveBinding();
		ICPPMethod f = (ICPPMethod) col.getName(4).resolveBinding();
		ICPPTemplateParameter TR = (ICPPTemplateParameter) col.getName(5).resolveBinding();
		assertSame(T, TR);
		
		ICPPTemplateParameter C = (ICPPTemplateParameter) col.getName(7).resolveBinding();
		assertSame(C, T);
		
		ICPPClassType B2 = (ICPPClassType) col.getName(10).resolveBinding();
		assertSame(B2, B);
		
		ICPPTemplateParameter CR = (ICPPTemplateParameter) col.getName(12).resolveBinding();
		assertSame(CR, T);
		
		ICPPMethod f2 = (ICPPMethod) col.getName(13).resolveBinding();
		assertSame(f2, f);
		
		CR = (ICPPTemplateParameter) col.getName(14).resolveBinding();
		assertSame(CR, T);

		CR = (ICPPTemplateParameter) col.getName(16).resolveBinding();
		assertSame(CR, T);
	}
	
	// template <class T> class Array {};                    
	// template <class T> void sort(Array<T> &);           
	// template void sort<>(Array<int> &);                 
	public void testBug90689_ExplicitInstantiation() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPFunctionTemplate s = (ICPPFunctionTemplate) col.getName(3).resolveBinding();
		
		ICPPClassType A2 = (ICPPClassType) col.getName(4).resolveBinding();
		assertTrue(A2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A2).getTemplateDefinition(), A);
		
		ICPPFunction s2 = (ICPPFunction) col.getName(8).resolveBinding();
		assertTrue(s2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)s2).getTemplateDefinition(), s);
		
		ICPPClassType A3 = (ICPPClassType) col.getName(10).resolveBinding();
		assertTrue(A3 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A3).getTemplateDefinition(), A);
		assertNotSame(A2, A3);
	}
	
	// template<class T> class Array { };                              
	// template class Array<char>;                                     
	// template<class T> void sort(Array<T>& v) {  }                   
	// template void sort(Array<char>&); // argument is deduced here   
	public void test14_7_2s2_ExplicitInstantiation() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A1 = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType A2 = (ICPPClassType) col.getName(2).resolveBinding();
		assertTrue(A2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A2).getTemplateDefinition(), A1);
		
		ICPPFunctionTemplate s1 = (ICPPFunctionTemplate) col.getName(5).resolveBinding();
		ICPPFunction s2 = (ICPPFunction) col.getName(10).resolveBinding();
		assertTrue(s2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)s2).getTemplateDefinition(), s1);
		
		ICPPClassType A3 = (ICPPClassType) col.getName(11).resolveBinding();
		assertSame(A2, A3);
	}
	
	// template <class T> class A {       
	//    A<T>* p;                        
	//    void f() { this; }              
	// };                                 
	public void testBug74204() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		IField p = (IField) col.getName(5).resolveBinding();
		
		IASTName f = col.getName(6);
		IASTFunctionDefinition fdef = (IASTFunctionDefinition) f.getParent().getParent();
		IASTExpressionStatement statement = (IASTExpressionStatement) ((IASTCompoundStatement)fdef.getBody()).getStatements()[0];
		IType type = CPPVisitor.getExpressionType(statement.getExpression());
		
		assertTrue(type.isSameType(p.getType()));
	}
	
	// template <class T > void f(T);               
	// template <class T > void g(T t){             
	//    f(t);                                     
	// }                                              
	public void testDeferredFunctionTemplates() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunction f2 = (ICPPFunction) col.getName(8).resolveBinding();
		assertTrue(f2 instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)f2).getTemplateDefinition(), f);
	}
	
	// template < class T > class A {};                          
	// template < class T > class B {                            
	//    void init(A<T> *);                                   
	// };                                                        
	// template < class T > class C : public B<T> {              
	//    C(A<T> * a) {                                        
	//       init(a);                                          
	//    }                                                      
	// };                                                        
	public void testRelaxationForTemplateInheritance() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPMethod init = (ICPPMethod) col.getName(4).resolveBinding();
		// the instantiation of B<T> has to be deferred, therefore 'init' is an unknown binding.
		assertInstance(col.getName(19).resolveBinding(), ICPPUnknownBinding.class);
	}
	
	// template <class Tp, class Tr > class iter {                         
	//    Tp operator -> () const;                                         
	//    Tr operator [] (int) const;                                      
	// };                                                                  
	// template <class T> class list {                                     
	//    typedef iter< T*, T& > iterator;                                 
	//    iterator begin();                                                
	//    iterator end();                                                  
	// };                                                                  
	// class Bar { public: int foo; };                                     
	// void f() {                                                          
	//    list<Bar> bar;                                                   
	//    for(list<Bar>::iterator i = bar.begin(); i != bar.end(); ++i){ 
	//       i->foo;  i[0].foo;                                            
	//    }                                                                
	// }                                                                   
	public void testBug91707() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPMethod begin = (ICPPMethod) col.getName(16).resolveBinding();
		ICPPMethod end = (ICPPMethod) col.getName(18).resolveBinding();
		
		ICPPField foo = (ICPPField) col.getName(20).resolveBinding();
		
		IBinding r = col.getName(33).resolveBinding();
		assertTrue(r instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)r).getSpecializedBinding(), begin);
		
		r = col.getName(36).resolveBinding();
		assertTrue(r instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)r).getSpecializedBinding(), end);
		
		assertSame(foo, col.getName(39).resolveBinding());
		assertSame(foo, col.getName(41).resolveBinding());
	}
	
	// class B { int i; };                       
	// template <class T > class A {             
	//    typedef T* _T;                         
	// };                                        
	// void f(){                                 
	//    A<B>::_T t;                            
	//    (*t).i;                                
	// }                                         
	public void testBug98961() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassType B = (ICPPClassType) col.getName(0).resolveBinding();
		ICPPField i = (ICPPField) col.getName(1).resolveBinding();
		ITypedef _T = (ITypedef) col.getName(5).resolveBinding();
		ICPPVariable t = (ICPPVariable) col.getName(12).resolveBinding();
		
		IType type = t.getType();
		assertTrue(type instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)type).getSpecializedBinding(), _T);
		assertSame(((IPointerType)((ITypedef)type).getType()).getType(), B);
		assertSame(i, col.getName(14).resolveBinding());
	}
	
	// class A {                                 
	//    template <class T > void f(T) {      
	//       begin();                            
	//    }                                      
	//    void begin();                          
	// };                                        
	public void testBug98784() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertSame(col.getName(5).resolveBinding(), col.getName(6).resolveBinding());
	}
	
	// template <class T> class A {                  
	//    A(T t);                                  
	// };                                            
	// void f(A<int> a);                           
	// void m(){                                     
	//    f(A<int>(1));                            
	// }                                             
	public void testBug99254() throws Exception{
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPConstructor ctor = (ICPPConstructor) col.getName(2).resolveBinding();
		ICPPFunction f = (ICPPFunction) col.getName(5).resolveBinding();
		
		ICPPSpecialization spec = (ICPPSpecialization) col.getName(11).resolveBinding();
		assertSame(spec.getSpecializedBinding(), ctor);
		
		assertSame(f, col.getName(10).resolveBinding());
	}
	
	// namespace core {                                
	//    template<class T> class A {                  
	//       A(T x, T y);                            
	//    };                                           
	// }                                               
	// class B {                                       
	//    int add(const core::A<int> &rect);          
	// };                                              
	// void f(B* b){                                 
	//    b->add(core::A<int>(10, 2));               
	// }                                               
	public void testBug99254_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPConstructor ctor = (ICPPConstructor) col.getName(3).resolveBinding();
		ICPPMethod add = (ICPPMethod) col.getName(9).resolveBinding();
		
		ICPPSpecialization spec = (ICPPSpecialization) col.getName(20).resolveBinding();
		assertSame(spec.getSpecializedBinding(), ctor);
		
		assertSame(add, col.getName(19).resolveBinding());
	}
	
	// template <class T> class A { A(T); };         
	// typedef signed int s32;                         
	// class B {                                       
	//    int add(const A<s32> &rect);                
	// };                                              
	// void f(B* b){                                 
	//    b->add(A<int>(10));                        
	// }                                               
	public void testBug99254_3() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPConstructor ctor = (ICPPConstructor) col.getName(2).resolveBinding();
		ICPPMethod add = (ICPPMethod) col.getName(7).resolveBinding();
		
		ICPPSpecialization spec = (ICPPSpecialization) col.getName(17).resolveBinding();
		assertSame(spec.getSpecializedBinding(), ctor);
		
		assertSame(add, col.getName(16).resolveBinding());
	}
	
	public void testBug98666() throws Exception {
		IASTTranslationUnit tu = parse("A::template B<T> b;", ParserLanguage.CPP); //$NON-NLS-1$
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPASTQualifiedName qn = (ICPPASTQualifiedName) col.getName(0);
		IASTName[] ns = qn.getNames();
		assertTrue(ns[1] instanceof ICPPASTTemplateId);
		assertEquals(((ICPPASTTemplateId)ns[1]).toString(), "B"); //$NON-NLS-1$
	}
	
	// template <class T> struct A{                          
	//    class C {                                          
	//       template <class T2> struct B {};                
	//    };                                                 
	// };                                                    
	// template <class T> template <class T2>                
	// struct A<T>::C::B<T2*>{};                             
	// A<short>::C::B<int*> ab;                              
	public void testBug90678() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPTemplateParameter T2 = (ICPPTemplateParameter) col.getName(3).resolveBinding();
		
		ICPPClassTemplate B = (ICPPClassTemplate) col.getName(4).resolveBinding();
		
		assertSame(T, col.getName(5).resolveBinding());
		assertSame(T2, col.getName(6).resolveBinding());
		assertSame(T, col.getName(10).resolveBinding());
		assertSame(T2, col.getName(14).resolveBinding());
		
		ICPPClassTemplatePartialSpecialization spec = (ICPPClassTemplatePartialSpecialization) col.getName(12).resolveBinding();
		assertSame(spec.getPrimaryClassTemplate(), B);
		
		ICPPClassType BI = (ICPPClassType) col.getName(19).resolveBinding();
		assertTrue(BI instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)BI).getSpecializedBinding(), spec);
	}
	
	// template <class T> int f(T); // #1
	// int f(int);                  // #2
	// int k = f(1);           // uses #2
	// int l = f<>(1);         // uses #1
	public void testBug95208() throws Exception {
	    String content= getAboveComment();
		IASTTranslationUnit tu = parse(content, ParserLanguage.CPP);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPFunctionTemplate f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		ICPPFunction f2 = (ICPPFunction) col.getName(4).resolveBinding();
		
		assertSame(f2, col.getName(7).resolveBinding());
		
		IBinding b = col.getName(9).resolveBinding(); // resolve the binding of the ICPPASTTemplateId first
		assertTrue(b instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)b).getSpecializedBinding(), f1);
		assertSame(f1, col.getName(10).resolveBinding());
		
		
		tu = parse(content,ParserLanguage.CPP);
		col = new CPPNameCollector();
		tu.accept(col);
		
		f1 = (ICPPFunctionTemplate) col.getName(1).resolveBinding();
		assertSame(f1, col.getName(10).resolveBinding());
	}
	
	// template <class T, int someConst = 0 >  class A {};      
	// int f() {                                                
	//    const int local = 10;                                 
	//    A<int, local> broken;                                 
	// };                                                       
	public void testBug103578() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate A = (ICPPClassTemplate) col.getName(2).resolveBinding();
		IVariable local = (IVariable) col.getName(4).resolveBinding();
		
		ICPPClassType a = (ICPPClassType) col.getName(5).resolveBinding();
		assertTrue(a instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)a).getTemplateDefinition(), A);
		assertSame(local, col.getName(7).resolveBinding());
	}
	
	// template <class T> class A : public T {};   
	// class B { int base; };                      
	// void f() {                                  
	//    A< B > a;                                
	//    a.base;                                  
	// }                                           
	public void testBug103715() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPField base = (ICPPField) col.getName(4).resolveBinding();
		assertSame(base, col.getName(11).resolveBinding());
		
		ICPPClassType B = (ICPPClassType) col.getName(3).resolveBinding();
		ICPPClassType A = (ICPPClassType) col.getName(6).resolveBinding();
		
		ICPPBase[] bases = A.getBases();
		assertEquals(bases.length, 1);
		assertSame(bases[0].getBaseClass(), B);
	}
	
	// template < class T > class complex;         
	// template <> class complex <float>;          
	// template < class T > class complex{         
	// };                                          
	// template <> class complex< float > {        
	//    void f(float);                         
	// };                                          
	// void complex<float>::f(float){              
	// }                                           
	public void testBug74276() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPClassTemplate complex = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPClassType cspec = (ICPPClassType) col.getName(2).resolveBinding();
		assertTrue(cspec instanceof ICPPSpecialization);
		assertSame(((ICPPSpecialization)cspec).getSpecializedBinding(), complex);
		
		assertSame(complex, col.getName(5).resolveBinding());
		assertSame(cspec, col.getName(6).resolveBinding());
		
		ICPPMethod f = (ICPPMethod) col.getName(8).resolveBinding();
		assertSame(f, col.getName(10).resolveBinding());
	}
	
	// template< class T1, int q > class C {};      
	// template< class T1, class T2> class A {};    
	// template< class T1, class T2, int q1, int q2>
	// class A< C<T1, q1>, C<T2, q2> > {};          
	// class N {};                                  
	// typedef A<C<N,1>, C<N,1> > myType;            
	// void m(){                                    
	//    myType t;                                 
	// }                                            
	public void testBug105852() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ITypedef myType = (ITypedef) col.getName(31).resolveBinding();
		ICPPClassType A = (ICPPClassType) myType.getType();
		
		ICPPSpecialization Aspec = (ICPPSpecialization) col.getName(10).resolveBinding();
		
		assertTrue(A instanceof ICPPTemplateInstance);
		assertSame(((ICPPTemplateInstance)A).getTemplateDefinition(), Aspec);
	}
	
	// template< class T > class A : public T {};      
	// class C { public: int c; };                     
	// class B : public A<C> { };                      
	// void main(){                                    
	//    B k;                                         
	//    k.c;                                         
	// }                                               
	public void testBug105769() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPVariable c = (ICPPVariable) col.getName(13).resolveBinding();
		assertSame(c, col.getName(4).resolveBinding());
	}
	
	// template< class T > class C {                   
	//    public: void * blah;                         
	//    template<typename G> C(G* g) : blah(g) {}    
	//    template <> C(char * c) : blah(c) {}         
	//    template <> C(wchar_t * c) : blah(c) {}      
	// };                                              
	public void testBug162230() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPTemplateParameter T = (ICPPTemplateParameter) col.getName(0).resolveBinding();
		ICPPClassTemplate C = (ICPPClassTemplate) col.getName(1).resolveBinding();
		ICPPField blah = (ICPPField) col.getName(2).resolveBinding();
		ICPPTemplateTypeParameter G = (ICPPTemplateTypeParameter) col.getName(3).resolveBinding();
		ICPPFunctionTemplate ctor = (ICPPFunctionTemplate) col.getName(4).resolveBinding();
		
		assertSame(G, col.getName(5).resolveBinding());
		ICPPParameter g = (ICPPParameter) col.getName(6).resolveBinding();
		assertSame(blah, col.getName(7).resolveBinding());
		assertSame(g, col.getName(8).resolveBinding());
		
		ICPPSpecialization spec = (ICPPSpecialization) col.getName(9).resolveBinding();
		assertSame(spec.getSpecializedBinding(), ctor);
		
		ICPPSpecialization c = (ICPPSpecialization) col.getName(10).resolveBinding();
		assertSame(c.getSpecializedBinding(), g);
		
		assertSame(blah, col.getName(11).resolveBinding());
		assertSame(c, col.getName(12).resolveBinding());
		
		ICPPSpecialization spec2 = (ICPPSpecialization) col.getName(13).resolveBinding();
		assertSame(spec.getSpecializedBinding(), ctor);
		
		ICPPSpecialization c2 = (ICPPSpecialization) col.getName(14).resolveBinding();
		assertSame(c2.getSpecializedBinding(), g);
		
		assertSame(blah, col.getName(15).resolveBinding());
		assertSame(c2, col.getName(16).resolveBinding());
	}
	
	// template< class T > class C {};                 
	// typedef struct C<int> CInt;                     
	public void testBug169628() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertTrue(col.getName(2).resolveBinding() instanceof ICPPSpecialization);
	}
	
    // template<class T1>
    // struct Closure {
    //   Closure(T1* obj1, void (T1::*method1)()) {}
    // };
    //
    // template<class T2>
    // Closure<T2>* makeClosure(T2* obj2, void (T2::*method2)()) {
    //   return new Closure<T2>(obj2, method2);
    // }
    //
    // struct A {
    //   void m1() {}
    //   void m2() {
    //     makeClosure(this, &A::m1);
    //   }
    // };
    public void testBug201204() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("makeClosure(this", 11, ICPPFunction.class);
    }

	// template <class R, class T, class P1, class P2, class P3, class P4>
	// class A {};
	//
	// template <class R, class T, class P1, class P2, class P3, class P4>
	// A<R, T, P1, P2, P3, P4>* func(const T* obj, R (T::*m)(P1, P2, P3, P4) const);
	//
	// template <class R, class T, class P1, class P2, class P3, class P4>
	// class B {};
	//
	// template <class R, class T, class P1, class P2, class P3, class P4>
	// B<R, T, P1, P2, P3, P4>* func(T* obj, R (T::*m)(P1, P2, P3, P4));
	//
	// struct C {
	//	 int m1(int a1, int a2, int a3, int a4);
	//	 int m2(int a1, int a2, int a3, int a4) const;
	// };
	//
	// void f(C* c, const C* d) {
	//	 func(c, &C::m1);
	//	 func(d, &C::m2);
	// }
    public void testBug233889() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn1= bh.assertNonProblem("func(c", 4, ICPPFunction.class);
		ICPPFunction fn2= bh.assertNonProblem("func(d", 4, ICPPFunction.class);
		assertNotSame(fn1, fn2);
    }

    // template<class _T1, class _T2>
    // struct pair {
    //   typedef _T1 first_type;
    // };
    //
    // template <typename _Key, typename _Tp>
    // struct map {
    //   typedef pair<_Key, _Tp> value_type;
    // };
    //
    // template <class _C>
    // typename _C::value_type GetPair(_C& collection, typename _C::value_type::first_type key);
    // 
    // int main(map<int, int> x) {
    //   GetPair(x, 1);
    // }
    public void testBug229917_1() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn = bh.assertNonProblem("GetPair(x", 7, ICPPFunction.class);
    }

    // template<class _T1, class _T2>
    // struct pair {
    //   typedef _T1 first_type;
    // };
    //
    // template <typename _Key, typename _Tp>
    // struct map {
    //   typedef pair<_Key, _Tp> value_type;
    // };
    //
    // template <class _C>
    // typename _C::value_type GetPair(_C& collection, typename _C::value_type::first_type key);
    public void testBug229917_2() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		IBinding b0 = bh.assertNonProblem("value_type GetPair", 10, IBinding.class);
    }

    // template<typename _T1>
    // class A {};
    //
    // template<typename _T2, template<typename> class _Base = A>
    // struct B {
    //   const _T2* m() const { return 0; }
    // };
    //
    // template<typename _T3>
    // class C : public B<_T3> {};
    //
    // void f(C<char>& str) {
    //   str.m();
    // }
    public void testBug232086() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction b0 = bh.assertNonProblem("m();", 1, ICPPFunction.class);
    }

    //    class A {};
	//
	//    template <class T> class C {
	//    public:
	//    	inline C(T& aRef) {}
	//    	inline operator T&() {}
	//    };
	//
	//    void foo(A a) {}
	//    void bar(C<const A> ca) {}
	//
	//    void main2() {
	//    	const A a= *new A();
	//    	const C<const A> ca= *new C<const A>(*new A());
	//
	//    	foo(a); 
	//    	bar(ca);
	//    }
	public void testBug214646() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		
		IBinding b0= bh.assertNonProblem("foo(a)", 3);
		IBinding b1= bh.assertNonProblem("bar(ca)", 3);
		
		assertInstance(b0, ICPPFunction.class);
		assertInstance(b1, ICPPFunction.class);
		
		ICPPFunction f0= (ICPPFunction) b0, f1= (ICPPFunction) b1;
		assertEquals(1, f0.getParameters().length);
		assertEquals(1, f1.getParameters().length);
		
		assertInstance(f0.getParameters()[0].getType(), ICPPClassType.class);
		assertFalse(f0 instanceof ICPPTemplateInstance);
		assertFalse(f0 instanceof ICPPTemplateDefinition);
		assertInstance(f1.getParameters()[0].getType(), ICPPClassType.class);
		assertInstance(f1.getParameters()[0].getType(), ICPPTemplateInstance.class);
	}

	// namespace ns {
	//
	// template<class _M1, class _M2>
	// struct pair {
	//   pair(const _M1& _a, const _M2& _b) {}
	// };
    //
	// template<class _T1, class _T2>
	// pair<_T1, _T2> make_pair(_T1 _x, _T2 _y) { return pair<_T1, _T2>(_x, _y); }
	//
	// }
	//
	// using ns::pair;
	// using ns::make_pair;
	// pair<int, int> p = make_pair(1, 2);
    public void testFunctionTemplateWithUsing() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("make_pair(1", 9, ICPPFunction.class);
    }

	// // Brian W.'s example from bugzilla#167098
	//    template<class K>
	//    class D { //CPPClassTemplate
	//    public:
	//            template<class T, class X>
	//            D(T t, X x) {} // CPPConstructorTemplate
	//
	//            template<class T, class X>
	//            void foo(T t, X x) {} // CPPMethodTemplate
	//    };
	//
	//    void bar() {
	//            D<int> *var = new D<int>(5, 6);
	//            // First D<int>: CPPClassInstance
	//            // Second D<int>: CPPConstructorInstance
	//            // Now, getting the instance's specialized binding should
	//            // result in a CPPConstructorTemplateSpecialization
	//            var->foo<int,int>(7, 8);
	//            // foo -> CPPMethodTemplateSpecialization
	//            // foo<int,int> -> CPPMethodInstance
	//    }
	public void testCPPConstructorTemplateSpecialization() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		ICPPASTTemplateId tid= (ICPPASTTemplateId) col.getName(20);
		IASTName cn= col.getName(21);
		assertInstance(cn.resolveBinding(), ICPPClassTemplate.class); // *D*<int>(5, 6)
		assertInstance(cn.resolveBinding(), ICPPClassType.class); // *D*<int>(5, 6)
		assertInstance(tid.resolveBinding(), ICPPTemplateInstance.class); // *D<int>*(5, 6)
		assertInstance(tid.resolveBinding(), ICPPConstructor.class); // *D<int>*(5, 6)
		
		IBinding tidSpc= ((ICPPTemplateInstance)tid.resolveBinding()).getSpecializedBinding();
		assertInstance(tidSpc, ICPPConstructor.class);
		assertInstance(tidSpc, ICPPSpecialization.class);
		assertInstance(tidSpc, ICPPFunctionTemplate.class);
	}
	
	// template<class T> const T& (max)(const T& lhs, const T& rhs) {
	//    return (lhs < rhs ? rhs : lhs);
	// }
	public void testNestedFuncTemplatedDeclarator_bug190241() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		IASTName name;
		for (Object element : col.nameList) {
			name = (IASTName) element;
			assertFalse(name.resolveBinding() instanceof IProblemBinding);
		}
		
		name= col.nameList.get(0);
		assertTrue(name.resolveBinding() instanceof ICPPTemplateParameter);
		name= col.nameList.get(1);
		assertTrue(name.resolveBinding() instanceof ICPPTemplateParameter);
		name= col.nameList.get(2);
		assertTrue(name.resolveBinding() instanceof ICPPFunction);
		name= col.nameList.get(3);
		assertTrue(name.resolveBinding() instanceof ICPPTemplateParameter);
		name= col.nameList.get(4);
		assertTrue(name.resolveBinding() instanceof IParameter);
		name= col.nameList.get(5);
		assertTrue(name.resolveBinding() instanceof ICPPTemplateParameter);
		name= col.nameList.get(6);
		assertTrue(name.resolveBinding() instanceof IParameter);
		name= col.nameList.get(7);
		assertTrue(name.resolveBinding() instanceof IParameter);
		name= col.nameList.get(8);
		assertTrue(name.resolveBinding() instanceof IParameter);
		name= col.nameList.get(9);
		assertTrue(name.resolveBinding() instanceof IParameter);
		name= col.nameList.get(10);
		assertTrue(name.resolveBinding() instanceof IParameter);
	}
	
	// template<typename TpA>
	// class A {
	// public:
	//   typedef TpA ta;
	// };
	//
	// template<typename TpB>
	// class B {
	// public:
	//   typedef typename A<TpB>::ta tb;
	// };
	//
	// void f(B<int>::tb r) {}
	public void testTemplateTypedef_214447() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("r".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
	}
	
	// template<typename _TpAllocator>
	// class Allocator {
	// public:
	//   typedef _TpAllocator& alloc_reference;
	//   template<typename _TpRebind>
	//   struct rebind {
	//     typedef Allocator<_TpRebind> other;
	//   };
	// };
	//
	// template<typename _Tp, typename _Alloc = Allocator<_Tp> >
	// class Vec {
	// public:
	//   typedef typename _Alloc::template rebind<_Tp>::other::alloc_reference reference;
	// };
	//
	// void f(Vec<int>::reference r) {}
	public void testRebindPattern_214447_1() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("r".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
	}
	
	// template<typename _TpAllocator>
	// class Allocator {
	// public:
	//   typedef _TpAllocator& alloc_reference;
	//   template<typename _TpRebind>
	//   struct rebind {
	//     typedef Allocator<_TpRebind> other;
	//   };
	// };
	//
	// template<typename _TpBase, typename _AllocBase>
	// class VecBase {
	// public:
	//   typedef typename _AllocBase::template rebind<_TpBase>::other _Tp_alloc_type;
	// };
	//
	// template<typename _Tp, typename _Alloc = Allocator<_Tp> >
	// class Vec : protected VecBase<_Tp, _Alloc> {
	// public:
	//   typedef typename VecBase<_Tp, _Alloc>::_Tp_alloc_type::alloc_reference reference;
	// };
	//
	// void f(Vec<int>::reference r) {}
	public void testRebindPattern_214447_2() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("r".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
	}

	// template<typename _Tp>
	// struct allocator {
	//   template<typename _Tp1>
	//   struct rebind {
	//     typedef allocator<_Tp1> other;
	//   };
	// };
	//
	// template<typename _Val1, typename _Alloc1 = allocator<_Val1> >
	// struct _Rb_tree {
	//   typedef _Val1 value_type1;
	// };
	//
	// template <typename _Val2, typename _Alloc2 = allocator<_Val2> >
	// struct map {
	//   typedef _Val2 value_type2;
	//   typedef typename _Alloc2::template rebind<value_type2>::other _Val_alloc_type;
	//   typedef _Rb_tree<_Val2, _Val_alloc_type> _Rep_type;
	//   typedef typename _Rep_type::value_type1 value_type;
	// };
	//
	// void f(map<int>::value_type r) {}
	public void _testRebindPattern_236197() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("r".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
	}

	// template<typename _Iterator>
	// struct IterTraits {
	//   typedef typename _Iterator::iter_reference traits_reference;
	// };
	//
	// template<typename _Tp>
	// struct IterTraits<_Tp*> {
	//   typedef _Tp& traits_reference;
	// };
	//
	// template<typename _Pointer>
	// struct Iter {
	//   typedef typename IterTraits<_Pointer>::traits_reference iter_reference;
	// };
    //
    // void main(Iter<int*>::iter_reference r);
    public void testSpecializationSelection_229218() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("r".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
    }

    // template<typename _Tp>
	// class A {
	// public:
	//   typedef _Tp a;
	// };
	//
	// template<typename _Tp1, typename _Tp2 = A<_Tp1> >
	// class B {
	// public:
	//   typedef _Tp2 b;
	// };
	//
	// B<int>::b::a x;
	public void testDefaultTemplateParameter() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		for (IASTName name : col.nameList) {
			if ("x".equals(String.valueOf(name))) {
				IBinding b0 = name.resolveBinding();
				IType type = ((ICPPVariable) b0).getType();
				type = getUltimateType(type, false);
				assertInstance(type, IBasicType.class);
				assertEquals("int", ASTTypeUtil.getType(type));
			}
		}
	}
	
	//    class A {};
	//    class B {};
	//    template<typename T>
	//    class C {
	//    public:
	//    	T t;
	//    	operator B() {B b; return b;}
	//    };
	//    template<typename T>
	//    class D : public C<T> {};
	//    void foo(B b) {}
	//
	//    void refs() {
	//    	D<A> d;
	//    	foo(d);
	//    }
	public void testUserDefinedConversions_224364() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("foo(d)", 3, ICPPFunction.class);
	}
	
	//    class B {};
	//    template<typename T>
	//    class C {
	//    public:
	//    	T t;
	//    	operator T() {return t;}
	//    };
	//    template<typename T>
	//    class D : public C<T> {};
	//    void foo(B b) {}
	//
	//    void refs() {
	//    	D<B> d;
	//    	foo(d);
	//    }
	public void testUserDefinedConversions_224364_2() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("foo(d)", 3, ICPPFunction.class);
	}
	
	//    class Z {};
	//    template<typename TA>
	//    class A {
	//    	public:
	//    		TA ta;
	//          operator TA() {return ta;}
	//    };
	//    template<typename TB>
	//    class B : public A<TB> {};
	//    template<typename TC>
	//    class C : public B<TC> {}; 
	//    template<typename TD>
	//    class D : public C<TD> {};
	//    template<typename TE>
	//    class E : public D<TE> {};
	//    Z foo(Z z) {return z;}
	//
	//    Z z= foo(*new E<Z>());
	public void testUserDefinedConversions_224364_3() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("foo(*new", 3, ICPPFunction.class);
	}
	
	//    class X {}; class B {};
	//    template<typename T>
	//    class C {
	//    	public:
	//    		T t;
	//          operator T() {return t;}
	//    };
	//    template<>
	//    class C<X> {
	//    	public:
	//    		X t;
	//          operator B() {B b; return b;}
	//    };
	//    void foo(B b) {}
	//
	//    void refs() {
	//    	C<X> cx;
	//    	foo(cx);
	//    }
	public void testUserDefinedConversions_226231() throws Exception {
		BindingAssertionHelper bh= new BindingAssertionHelper(getAboveComment(), true);
		ICPPFunction fn= bh.assertNonProblem("foo(cx", 3, ICPPFunction.class);
	}
	
	//	class A;
	//
	//	int foo(A a);
	//
	//	template <class T>
	//	class C {
	//	public:
	//		inline operator A();
	//	};
	//
	//	template<typename T>
	//	void ref(C<T> c) {
	//	 return foo(c);
	//	}
	public void testUserDefinedConversions_239023() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ba.assertNonProblem("foo(c);", 3);
	}
	
	//	template<int x>
	//	class A {};
	//
	//	const int i= 1;
	//	A<i> a1;
	public void testNonTypeArgumentIsIDExpression_229942_a() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertInstance(col.getName(4).getParent(), ICPPASTTemplateId.class);
		assertInstance(col.getName(5).getParent(), IASTIdExpression.class);
	}
	
	//  class X {
	//	   template<int x>
	//	   class A {};
	//
	//	   void foo() {
   	//	      A<i> a1;
	//     }
	//
	//     const int i= 1;
	//  };
	public void testNonTypeArgumentIsIDExpression_229942_b() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertInstance(col.getName(5).getParent(), ICPPASTTemplateId.class);
		assertInstance(col.getName(6).getParent(), IASTIdExpression.class);
	}
	
	//	template<int x>
	//	class A {};
	//
	//	const int i= 1;
	//	A<i+1> a1;
	public void testExpressionArgumentIsExpression_229942_c() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertInstance(col.getName(4).getParent(), ICPPASTTemplateId.class);
		assertInstance(col.getName(5).getParent(), IASTIdExpression.class);
		assertInstance(col.getName(5).getParent().getParent(), IASTBinaryExpression.class);
	}
	
	//	template<int x>
	//	class A {};
	//
	//	const int i= 1;
	//	A<typeid(1)> a1;
	public void testTypeIdOperatorArgumentIsUnaryExpression_229942_d() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertInstance(col.getName(3), ICPPASTTemplateId.class);
		assertInstance(((ICPPASTTemplateId)col.getName(3)).getTemplateArguments()[0], ICPPASTUnaryExpression.class);
	}
	
	// template<class T1, int q> class C {};    
	// template<class T1, class T2> class A {};
	// template< class T1, class T2, int q1, int q2>
	// class A< C<T1, q1>, C<T2, q2> > {};      
	public void testTemplateIdAsTemplateArgumentIsTypeId_229942_e() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		// 12 is template-id: C<T1, q1>
		assertInstance(col.getName(12), ICPPASTTemplateId.class);
		assertInstance(col.getName(12).getParent(), ICPPASTNamedTypeSpecifier.class);
		assertInstance(col.getName(12).getParent().getParent(), IASTTypeId.class);

		// 16 is template-id: C<T2, q2>
		assertInstance(col.getName(16), ICPPASTTemplateId.class);
		assertInstance(col.getName(16).getParent(), ICPPASTNamedTypeSpecifier.class);
		assertInstance(col.getName(16).getParent().getParent(), IASTTypeId.class);
	}
	
	//	template <class T>
	//	struct A {
	//		A(T* t) {}
	//	};
	//
	//	template <class T>
	//	inline const A<T> foo(T* t) {
	//		return A<T>(t);
	//	}
	//
	//	template <class T>
	//	inline const A<T> foo(const A<T> at) {
	//		return at;
	//	}
	public void testTypeIdAsTemplateArgumentIsTypeId_229942_f() throws Exception {
		BindingAssertionHelper ba=new BindingAssertionHelper(getAboveComment(), true);
		ba.assertNonProblem("T> at) {", 1);
		
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		assertInstance(col.getName(23).getParent().getParent(), IASTTypeId.class);
		assertInstance(col.getName(23).resolveBinding(), ICPPTemplateTypeParameter.class);
	}
	
	//	template <class T>
	//	struct A {};
	//
	//	template <class T>
	//	inline const void foo(void (*f)(A<T>), T t) {
	//	}
	//
	//	const int i= 5;
	//	template <class T>
	//	inline const void foo(void (*f)(A<i>), T* t) { // disallowed, but we're testing the AST
	//	}
	public void testTypeIdAsTemplateArgumentIsTypeId_229942_g() throws Exception {
		IASTTranslationUnit tu = parse(getAboveComment(), ParserLanguage.CPP, true, true);
		CPPNameCollector col = new CPPNameCollector();
		tu.accept(col);
		
		// 7 is T in A<T>
		assertInstance(col.getName(7).getParent(), ICPPASTNamedTypeSpecifier.class);
		assertInstance(col.getName(7).getParent().getParent(), IASTTypeId.class);
		
		// 17 is i in A<i>
		assertInstance(col.getName(17).getParent(), IASTIdExpression.class);
	}
	
	//	typedef int td;
	//	template<> class Alias<td const *> {
	//	};
	public void testNonAmbiguityCase_229942_h() throws Exception {
		IASTTranslationUnit tu= parse(getAboveComment(), ParserLanguage.CPP);
		CPPNameCollector col= new CPPNameCollector();
		tu.accept(col);

		// 2 is Alias
		ICPPASTTemplateId tid= assertInstance(col.getName(2).getParent(), ICPPASTTemplateId.class);
		IASTNode[] args= tid.getTemplateArguments();
		assertEquals(1, args.length);
		assertInstance(args[0], IASTTypeId.class);
	}
	
	//  // From discussion in 207840. See 14.3.4.
	//	class A {};
	//
	//	template<typename T>
	//	class B {};
	//
	//	template<typename T = A>
	//	class C {};
	//
	//	B b1;
	//	B<> b2; // error - no default args
	//
	//	C c1;   
	//	C<> c2; // ok - default args
	public void testMissingTemplateArgumentLists() throws Exception {
		BindingAssertionHelper ba=new BindingAssertionHelper(getAboveComment(), true);
		ba.assertProblem("B b1", 1);
		ba.assertNonProblem("B<> b2", 1, ICPPTemplateDefinition.class, ICPPClassType.class);
		ba.assertProblem("B<> b2", 3);
		ba.assertProblem("C c1", 1);
		ba.assertNonProblem("C<> c2", 1, ICPPTemplateDefinition.class, ICPPClassType.class);
		ba.assertNonProblem("C<> c2", 3, ICPPTemplateInstance.class, ICPPClassType.class);
	}
	
	//	template<class T1, int N> class TestClass {
	//		int member1;
	//		void fun1(void);
	//	};
	//	template<class T1,int N> inline void TestClass<T1,N>::fun1(void) {
	//		member1 = 0; 
	//	}
	public void testDefinitionOfClassTemplateWithNonTypeParameter() throws Exception {
		BindingAssertionHelper ba=new BindingAssertionHelper(getAboveComment(), true);
		ICPPMethod f1= ba.assertNonProblem("fun1(void);", 4, ICPPMethod.class);
		ICPPField m1= ba.assertNonProblem("member1;", 7, ICPPField.class);
		ICPPMethod f2= ba.assertNonProblem("fun1(void) {", 4, ICPPMethod.class);
		ICPPField m2= ba.assertNonProblem("member1 =", 7, ICPPField.class);
		assertSame(m1, m2);
		assertSame(f1, f2);
	}

	//	class Z {};
	//	
	//	template<typename T1>
	//	class A {
	//		public:
	//			template<typename T2 = Z> class B;
	//	};
	//	
	//	template<> template<typename T3> class A<short>::B {
	//		public:
	//			T3 foo() { return (T3) 0; }
	//	};
	//	
	//	void ref() {
	//		A<short>::B<> b;
	//	}
	public void testNestedTemplateDefinitionParameter() throws Exception  {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ICPPTemplateTypeParameter T3a= ba.assertNonProblem("T3 f", 2, ICPPTemplateTypeParameter.class);
		ICPPTemplateTypeParameter T3b= ba.assertNonProblem("T3)", 2, ICPPTemplateTypeParameter.class);
		ICPPClassType b= ba.assertNonProblem("B<>", 3, ICPPClassType.class, ICPPTemplateInstance.class);
	}

	//	template<class T, int x> class A {public: class X {};};
	//	template<class T1> class A<T1,1> {public: class Y {};};
	//	template<class T2> class A<T2,2> {public: class Z {};};
	//
	//	class B {};
	//
	//	A<B, 0>::X x;
	//	A<B, 1>::Y y;
	//	A<B, 2>::Z z;
	public void testNonTypeArgumentDisambiguation_233460() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ICPPClassType b2= ba.assertNonProblem("A<B, 0>", 7, ICPPClassType.class, ICPPTemplateInstance.class);
		ICPPClassType b3= ba.assertNonProblem("A<B, 1>", 7, ICPPClassType.class, ICPPTemplateInstance.class);
		ICPPClassType b4= ba.assertNonProblem("A<B, 2>", 7, ICPPClassType.class, ICPPTemplateInstance.class);
		
		assertTrue(!b2.isSameType(b3));
		assertTrue(!b3.isSameType(b4));
		assertTrue(!b4.isSameType(b2));
		
		ICPPClassType X= ba.assertNonProblem("X x", 1, ICPPClassType.class);
		ICPPClassType Y= ba.assertNonProblem("Y y", 1, ICPPClassType.class);
		ICPPClassType Z= ba.assertNonProblem("Z z", 1, ICPPClassType.class);
		
		assertTrue(!X.isSameType(Y));
		assertTrue(!Y.isSameType(Z));
		assertTrue(!Z.isSameType(X));
	}
	
	//	template<class T, bool b> class A {public: class X {};};
	//	template<class T1> class A<T1,true> {public: class Y {};};
	//
	//	class B {};
	//
	//	A<B, false>::X x; //1
	//	A<B, true>::Y y; //2
	//
	//	A<B, true>::X x; //3 should be an error
	//	A<B, false>::Y y; //4 should be an error
	public void testNonTypeBooleanArgumentDisambiguation() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		
		ICPPClassType X= ba.assertNonProblem("X x; //1", 1, ICPPClassType.class);
		ICPPClassType Y= ba.assertNonProblem("Y y; //2", 1, ICPPClassType.class);
		ba.assertProblem("X x; //3", 1);
		ba.assertProblem("Y y; //4", 1);
		
		assertTrue(!X.isSameType(Y));
	}
	
	//	template <int x>
	//	class C {
	//	public:
	//		inline C() {};
	//	};
	//
	//	const int _256=0x100;
	//
	//	typedef C<_256> aRef;
	//
	//	void foo(aRef& aRefence) {}
	//	void bar(C<_256>& aRefence) {}
	//	void baz(void) {}
	//
	//	int main (void) {
	//		C<256> t;
	//		foo(t);
	//		bar(t);
	//		baz();
	//	}
	public void testBug207871() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		
		ICPPVariable _256= ba.assertNonProblem("_256=0x100", 4, ICPPVariable.class);
		IQualifierType qt1= assertInstance(_256.getType(), IQualifierType.class);
		ICPPBasicType bt1= assertInstance(qt1.getType(), ICPPBasicType.class);
		assertEquals(256, CPPVisitor.parseIntegral(bt1.getValue().toString()).intValue());
		
		ICPPVariable t= ba.assertNonProblem("t;", 1, ICPPVariable.class);
		ICPPTemplateInstance ci1= assertInstance(t.getType(), ICPPTemplateInstance.class, ICPPClassType.class);
		ObjectMap args1= ci1.getArgumentMap();
		assertEquals(1, args1.size());
		assertInstance(args1.keyAt(0), ICPPTemplateNonTypeParameter.class);
		
		// non-type arguments are currently modelled as a type with attached expression
		ICPPBasicType bt0= assertInstance(args1.getAt(0), ICPPBasicType.class);
		assertEquals(bt0.getType(), IBasicType.t_int);
		assertEquals(256, CPPVisitor.parseIntegral(bt0.getValue().toString()).intValue());
		
		ICPPTemplateInstance ct= ba.assertNonProblem("C<_256> ", 7, ICPPTemplateInstance.class, ICPPClassType.class);
		ObjectMap args= ct.getArgumentMap();
		assertEquals(1, args.size());
		assertInstance(args.keyAt(0), ICPPTemplateNonTypeParameter.class);
		
		// non-type arguments are currently modelled as a type with attached expression
		ICPPBasicType bt= assertInstance(args.getAt(0), ICPPBasicType.class);
		assertEquals(bt.getType(), IBasicType.t_int);
		assertEquals(256, CPPVisitor.parseIntegral(bt.getValue().toString()).intValue());
		
		ba.assertNonProblem("foo(t)", 3);
		ba.assertNonProblem("bar(t)", 3);
	}
	
	//	template<int x>
	//	class C {};
	//
	//	template<int y>
	//	class D {
	//	public:
	//		C<y> go();
	//	};
	public void _testDeferredNonTypeArgument() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ICPPDeferredClassInstance ci= ba.assertNonProblem("C<y>", 4, ICPPDeferredClassInstance.class);
		ObjectMap args= ci.getArgumentMap();
		assertEquals(1, args.size());
		assertInstance(args.keyAt(0), ICPPTemplateNonTypeParameter.class);
		assertInstance(args.getAt(0), ICPPTemplateNonTypeParameter.class);
	}
	
	//	template<int x>
	//	class A {};
	//
	//	A<int> aint; // should be an error
	public void _testTypeArgumentToNonTypeParameter() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ba.assertProblem("A<int>", 6);
	}
	
	//	template<int I>
	//	class That {
	//	public:
	//		That(int x) {}
	//	};
	//
	//	template<int T>
	//	class This : public That<T> {
	//	public:
	//		inline This();
	//	};
	//
	//	template <int I>
	//	inline This<I>::This() : That<I>(I) {
	//	}
	public void testParameterReferenceInChainInitializer_a() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		
		// These intermediate assertions will not hold until deferred non-type arguments are
		// correctly modelled
		/*
		ICPPClassType tid= ba.assertNonProblem("This<I>::T", 7, ICPPClassType.class);
		assertFalse(tid instanceof ICPPSpecialization);
		ICPPConstructor th1sCtor= ba.assertNonProblem("This() :", 4, ICPPConstructor.class);
		assertFalse(th1sCtor instanceof ICPPSpecialization);ICPPTemplateNonTypeParameter np= ba.assertNonProblem("I)", 1, ICPPTemplateNonTypeParameter.class);
		*/
		
		ICPPTemplateNonTypeParameter np= ba.assertNonProblem("I>(I)", 1, ICPPTemplateNonTypeParameter.class);
		ICPPClassType clazz= ba.assertNonProblem("That<I>(I)", 4, ICPPClassType.class);
		ICPPConstructor ctor= ba.assertNonProblem("That<I>(I)", 7, ICPPConstructor.class);
	}
	
	//	template<typename I>
	//	class That {
	//		public:
	//			That() {}
	//	};
	//
	//	template<typename T>
	//	class This : public That<T> {
	//		public:
	//			inline This();
	//	};
	//
	//	template <typename I>
	//	inline This<I>::This() : That<I>() {
	//	}
	public void testParameterReferenceInChainInitializer_b() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
	
		ICPPClassType tid= ba.assertNonProblem("This<I>::T", 7, ICPPClassType.class);
		assertFalse(tid instanceof ICPPSpecialization);
		ICPPConstructor th1sCtor= ba.assertNonProblem("This() :", 4, ICPPConstructor.class);
		assertFalse(th1sCtor instanceof ICPPSpecialization);
		
		ICPPTemplateTypeParameter np= ba.assertNonProblem("I>()", 1, ICPPTemplateTypeParameter.class);
		ICPPClassType clazz= ba.assertNonProblem("That<I>()", 4, ICPPClassType.class);
		ICPPConstructor ctor= ba.assertNonProblem("That<I>()", 7, ICPPConstructor.class);
	}
	
	// template<typename T, int I>
	// class C {};
	//
	// template<typename T>
	// class C<T, 5> {};
	// 
	// class A {}; 
	//
	// C<A,5L> ca5L;
	public void testIntegralConversionInPartialSpecializationMatching_237914() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ICPPTemplateInstance ctps= ba.assertNonProblem("C<A,5L>", 7, ICPPTemplateInstance.class, ICPPClassType.class);
		assertInstance(ctps.getTemplateDefinition(), ICPPClassTemplatePartialSpecialization.class);
	}
	
	// template<typename T, int I>
	// class C {};
	//
	// class A {};
	//
	// template<>
	// class C<A, 5> {};
	//
	// C<A,5L> ca5L;
	public void testIntegralConversionInSpecializationMatching_237914() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ICPPSpecialization ctps= ba.assertNonProblem("C<A,5L>", 7, ICPPSpecialization.class, ICPPClassType.class);
		assertFalse(ctps instanceof ICPPTemplateInstance);
	}
	
	//	class A {
	//		public:
	//			A(const A& a) {}
	//	};
	//
	//	template<typename T>
	//	class B : A {
	//		public:
	//			B(const B<T>& other) : A(other) {}
	//	};
	public void testChainInitializerLookupThroughDeferredClassBase() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ba.assertNonProblem("A(other", 1);
	}
	
	//	class A {};
	//
	//	class B {
	//	public:
	//		void foo(const A& b);
	//	};
	//
	//	template<typename T>
	//	class C : public B {
	//	public:
	//		void foo(T *t) {
	//			B::foo(static_cast<A*>(t));
	//		}
	//	};
	public void testMemberLookupThroughDeferredClassBase() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ba.assertNonProblem("foo(s", 3);
	}
	
	//	template <class T>
	//	class A {
	//	public:
	//		inline int foo() const;
	//		inline int bar() const;
	//	};
	//
	//	template <class T>
	//	inline int A<T>::bar() const {
	//		return foo();
	//	}
	public void testMemberReferenceFromTemplatedMethodDefinition_238232() throws Exception {
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		ba.assertNonProblem("foo();", 3);
	}
	
	//	namespace result_of {
	//		template <typename Sequence, typename T, bool is_associative_sequence = false>
	//		struct find;
	//
	//		template <typename Sequence, typename T>
	//		struct find<Sequence, T, false> {
	//			typedef
	//			detail::static_seq_find_if<
	//			typename result_of::begin<Sequence>::type
	//			, typename result_of::end<Sequence>::type
	//			, is_same<mpl::_, T>
	//			>
	//			filter;
	//		};
	//
	//		template <typename Sequence, typename T>
	//		struct find<Sequence, T, true> {
	//			typedef detail::assoc_find<Sequence, T> filter;
	//		}; 
	//	}
	public void testBug238180_ArrayOutOfBounds() throws Exception {
		// the code above used to trigger an ArrayOutOfBoundsException
		parse(getAboveComment(), ParserLanguage.CPP);
	}
	
	//	namespace detail {
	//		template<bool AtoB, bool BtoA, bool SameType, class A, class B>
	//		struct str;
	//		template<class A, class B>
	//		struct str<true, true, false, A, B> {
	//			typedef
	//			detail::return_type_deduction_failure<str> type;
	//			// ambiguous type in conditional expression
	//		};
	//		template<class A, class B>
	//		struct str<true, true, true, A, B> {
	//			typedef A type;
	//		};
	//	} // detail
	public void testBug238180_ClassCast() throws Exception {
		// the code above used to trigger a ClassCastException
		BindingAssertionHelper ba= new BindingAssertionHelper(getAboveComment(), true);
		String tmplId= "str<true, true, false, A, B>";
		ICPPClassType p= ba.assertNonProblem(tmplId, tmplId.length(), ICPPClassType.class);
		ICPPConstructor con= p.getConstructors()[1];
		ICPPReferenceType reftype= (ICPPReferenceType) con.getType().getParameterTypes()[0];
		IQualifierType qt= (IQualifierType) reftype.getType();
		ICPPDeferredClassInstance dcl= (ICPPDeferredClassInstance) qt.getType();
		ICPPClassTemplatePartialSpecialization spec= (ICPPClassTemplatePartialSpecialization) dcl.getSpecializedBinding();
		ICPPTemplateTypeParameter tp= (ICPPTemplateTypeParameter) spec.getTemplateParameters()[0];
		assertNull(tp.getDefault());
	}
	
	//	class X {
	//		template <typename S> X(S s);
	//	};
	//
	//	void test(X a);
	//	void bla(int g) {
	//		test(new X(g));
	//	} 
	public void testBug239586_ClassCast() throws Exception {
		parseAndCheckBindings(getAboveComment(), ParserLanguage.CPP);
	}

}
