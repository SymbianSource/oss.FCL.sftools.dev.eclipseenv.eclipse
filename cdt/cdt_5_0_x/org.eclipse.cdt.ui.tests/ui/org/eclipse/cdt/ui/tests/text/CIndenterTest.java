/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anton Leherbauer (Wind River Systems) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.ui.tests.text;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.LineRange;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.ui.tests.BaseUITestCase;

import org.eclipse.cdt.internal.formatter.DefaultCodeFormatterOptions;

import org.eclipse.cdt.internal.ui.editor.CDocumentSetupParticipant;
import org.eclipse.cdt.internal.ui.editor.IndentUtil;
import org.eclipse.cdt.internal.ui.text.CHeuristicScanner;
import org.eclipse.cdt.internal.ui.text.CIndenter;

/**
 * Tests for the CIndenter.
 *
 * @since 4.0
 */
public class CIndenterTest extends BaseUITestCase {

	private HashMap fOptions;
	private Map fDefaultOptions;

	public static TestSuite suite() {
		return suite(CIndenterTest.class, "_");
	}

	protected void setUp() throws Exception {
		super.setUp();
		fDefaultOptions= DefaultCodeFormatterOptions.getDefaultSettings().getMap();
		fOptions= new HashMap();
	}

	protected void tearDown() throws Exception {
		CCorePlugin.setOptions(new HashMap(fDefaultOptions));
		super.tearDown();
	}

	protected void assertIndenterResult() throws Exception {
		CCorePlugin.setOptions(fOptions);
		StringBuffer[] contents= getContentsForTest(2);
		String before= contents[0].toString();
		IDocument document= new Document(before);
		String expected= contents[1].toString();
		new CDocumentSetupParticipant().setup(document);
		CIndenter indenter= new CIndenter(document, new CHeuristicScanner(document));
		IndentUtil.indentLines(document, new LineRange(0, document.getNumberOfLines()), null, null);
		assertEquals(expected, document.get());
	}
	
	//foo(arg,
	//"string");
	
	//foo(arg,
	//		"string");
	public void testIndentationOfStringLiteralAsLastArgument1_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//a::foo(arg,
	//"string");
	
	//a::foo(arg,
	//		"string");
	public void testIndentationOfStringLiteralAsLastArgument2_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//a::foo(arg,
	//		"string");
	
	//a::foo(arg,
	//		"string");
	public void testIndentationOfStringLiteralAsLastArgument3_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//if (1)
	//foo->bar();
	//dontIndent();
	
	//if (1)
	//	foo->bar();
	//dontIndent();
	public void testIndentationAfterArrowOperator_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//if (1)
	//foo>>bar;
	//  dontIndent();
	
	//if (1)
	//	foo>>bar;
	//dontIndent();
	public void testIndentationAfterShiftRight_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//if (1)
	//foo >= bar();
	//  dontIndent();
	
	//if (1)
	//	foo >= bar();
	//dontIndent();
	public void testIndentationAfterGreaterOrEquals_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//std::ostream& operator<<(std::ostream& stream,
	//const BinFileParser::Exception& exp)
	//{
	//}
	
	//std::ostream& operator<<(std::ostream& stream,
	//		const BinFileParser::Exception& exp)
	//{
	//}
	public void testIndentationOfOperatorMethodBody_Bug192412() throws Exception {
		assertIndenterResult();
	}

	//struct x {
	// int f1 : 1;
	// int f2 : 1;
	// int f3 : 1;
	//}
	
	//struct x {
	//	int f1 : 1;
	//	int f2 : 1;
	//	int f3 : 1;
	//}
	public void testIndentationOfBitFields_Bug193298() throws Exception {
		assertIndenterResult();
	}

	//class A {
	//A(int a,
	//int b)
	//{
	//}
	//};

	//class A {
	//	A(int a,
	//			int b)
	//	{
	//	}
	//};
	public void testIndentationOfConstructorBody_Bug194586() throws Exception {
		assertIndenterResult();
	}

	//class A {
	//A(int a,
	//int b)
	//throw()
	//{
	//}
	//};
	
	//class A {
	//	A(int a,
	//			int b)
	//	throw()
	//	{
	//	}
	//};
	public void testIndentationOfConstructorBodyWithThrow_Bug194586() throws Exception {
		assertIndenterResult();
	}

	//class A {
	//A(int a,
	//int b)
	//:f(0)
	//{
	//}
	//};
	
	//class A {
	//	A(int a,
	//			int b)
	//	:f(0)
	//	{
	//	}
	//};
	public void testIndentationOfConstructorBodyWithInitializer_Bug194586() throws Exception {
		assertIndenterResult();
	}

	//void f() {
	//switch(c) {
	//case 'a':
	//{
	//}
	//case 1:
	//{
	//}
	//}
	//}

	//void f() {
	//	switch(c) {
	//	case 'a':
	//	{
	//	}
	//	case 1:
	//	{
	//	}
	//	}
	//}
	public void testIndentationOfCaseBlockAfterCharLiteral_Bug194710() throws Exception {
		assertIndenterResult();
	}

	//int a[]=
	//{
	//1,
	//2
	//};

	//int a[]=
	//{
	//		1,
	//		2
	//};
	public void testIndentationOfInitializerLists_Bug194585() throws Exception {
		assertIndenterResult();
	}

	//struct_t a[]=
	//{
	//{
	//1,
	//2,
	//{ 1,2,3 }
	//},
	//{
	//1,
	//2,
	//{ 1,2,3 }
	//}
	//};
	
	//struct_t a[]=
	//{
	//		{
	//				1,
	//				2,
	//				{ 1,2,3 }
	//		},
	//		{
	//				1,
	//				2,
	//				{ 1,2,3 }
	//		}
	//};
	public void testIndentationOfNestedInitializerLists_Bug194585() throws Exception {
		assertIndenterResult();
	}
	
	//// a comment
	//class MyClass
	//{
	//};
	//  union DisUnion 
	//		{ 
	//};
	
	//// a comment
	//class MyClass
	//	{
	//	};
	//union DisUnion 
	//	{ 
	//	};
	public void testIndentedClassIndentation_Bug210417() throws Exception {
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, 
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED);
		assertIndenterResult();
	}
	
	//// a comment
	//class MyClass : public Base
	//{
	//};
	
	//// a comment
	//class MyClass : public Base
	//	{
	//	};
	public void testIndentedClassIndentation_Bug210417_2() throws Exception {
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, 
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED);
		assertIndenterResult();
	}	
	
	//// a comment
	//class MyClass : public Base, public OtherBase
	//{
	//};
	
	//// a comment
	//class MyClass : public Base, public OtherBase
	//	{
	//	};
	public void testIndentedClassIndentation_Bug210417_3() throws Exception {
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, 
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED);
		assertIndenterResult();
	}	
	
	//// a comment
	//class MyClass : public Base, public OtherBase
	//{
	//};
	
	//// a comment
	//class MyClass : public Base, public OtherBase
	//	{
	//	};
	public void testIndentedClassIndentation_Bug210417_4() throws Exception {
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, 
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED);
		assertIndenterResult();
	}	

	//class A
	//{
	//public:
	//A();
	//};
	
	//class A
	//    {
	//public:
	//    A();
	//    };
	public void testWhiteSmithsAccessSpecifierIndentation1_Bug204575() throws Exception {
		fOptions.putAll(DefaultCodeFormatterOptions.getWhitesmithsSettings().getMap());
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_ACCESS_SPECIFIER_COMPARE_TO_TYPE_HEADER, DefaultCodeFormatterConstants.FALSE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ACCESS_SPECIFIER, DefaultCodeFormatterConstants.TRUE);
		assertIndenterResult();
	}

	//class A
	//{
	//public:
	//A();
	//};
	
	//class A
	//    {
	//    public:
	//    A();
	//    };
	public void testWhiteSmithsAccessSpecifierIndentation2_Bug204575() throws Exception {
		fOptions.putAll(DefaultCodeFormatterOptions.getWhitesmithsSettings().getMap());
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_ACCESS_SPECIFIER_COMPARE_TO_TYPE_HEADER, DefaultCodeFormatterConstants.TRUE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ACCESS_SPECIFIER, DefaultCodeFormatterConstants.FALSE);
		assertIndenterResult();
	}

	//class A
	//{
	//public:
	//A();
	//};
	
	//class A
	//    {
	//    public:
	//	A();
	//    };
	public void testWhiteSmithsAccessSpecifierIndentation3_Bug204575() throws Exception {
		fOptions.putAll(DefaultCodeFormatterOptions.getWhitesmithsSettings().getMap());
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_ACCESS_SPECIFIER_COMPARE_TO_TYPE_HEADER, DefaultCodeFormatterConstants.TRUE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ACCESS_SPECIFIER, DefaultCodeFormatterConstants.TRUE);
		assertIndenterResult();
	}

	//void f()
	//{
	//switch(x)
	//{
	//case 1:
	//doOne();
	//default:
	//doOther();
	//}
	//}
	
	//void f()
	//    {
	//    switch(x)
	//	{
	//    case 1:
	//	doOne();
	//    default:
	//	doOther();
	//	}
	//    }
	public void testWhiteSmithsSwitchIndentation1() throws Exception {
		fOptions.putAll(DefaultCodeFormatterOptions.getWhitesmithsSettings().getMap());
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, DefaultCodeFormatterConstants.TRUE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, DefaultCodeFormatterConstants.FALSE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DefaultCodeFormatterConstants.MIXED);
		assertIndenterResult();
	}

	//void f()
	//{
	//switch(x)
	//{
	//case 1:
	//doOne();
	//default:
	//doOther();
	//}
	//}
	
	//void f()
	//	{
	//	switch(x)
	//		{
	//		case 1:
	//		doOne();
	//		default:
	//		doOther();
	//		}
	//	}
	public void testWhiteSmithsSwitchIndentation2() throws Exception {
		fOptions.putAll(DefaultCodeFormatterOptions.getWhitesmithsSettings().getMap());
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, DefaultCodeFormatterConstants.FALSE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, DefaultCodeFormatterConstants.TRUE);
		fOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, CCorePlugin.TAB);
		assertIndenterResult();
	}

}
