/*******************************************************************************
 * Copyright (c) 2006, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    QNX - Initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *    Andrew Ferguson (Symbian)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.pdom.dom;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorUndefStatement;
import org.eclipse.cdt.core.dom.ast.IMacroBinding;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexMacro;
import org.eclipse.cdt.core.parser.Keywords;
import org.eclipse.cdt.core.parser.util.CharArrayUtils;
import org.eclipse.cdt.internal.core.index.IIndexBindingConstants;
import org.eclipse.cdt.internal.core.index.IIndexFragment;
import org.eclipse.cdt.internal.core.index.IIndexFragmentBinding;
import org.eclipse.cdt.internal.core.index.IIndexFragmentName;
import org.eclipse.cdt.internal.core.index.IIndexScope;
import org.eclipse.cdt.internal.core.parser.scanner.MacroDefinitionParser;
import org.eclipse.cdt.internal.core.pdom.PDOM;
import org.eclipse.cdt.internal.core.pdom.db.Database;
import org.eclipse.cdt.internal.core.pdom.db.IString;
import org.eclipse.core.runtime.CoreException;

/**
 * Represents macro definitions. They are stored with the file and with a PDOMMacroContainer. The latter also
 * contains the references to all macros with the same name.
 */
public class PDOMMacro implements IIndexMacro, IIndexFragmentBinding, IASTFileLocation {
	
	private static final int CONTAINER = 0;
	private static final int FILE = 4;
	private static final int PARAMETERS= 8;
	private static final int EXPANSION = 12;
	private static final int NEXT_IN_FILE = 16;
	private static final int NEXT_IN_CONTAINER = 20;
	private static final int PREV_IN_CONTAINER = 24;
	private static final int NAME_OFFSET = 28;
	private static final int NAME_LENGTH = 32; // short
	
	private static final int RECORD_SIZE = 34;  
	private static final char[][] UNINITIALIZED= {};
	private static final char[]   UNINITIALIZED1= {};

	private final PDOM fPDOM;
	private final int fRecord;

	private char[][] fParameterList= UNINITIALIZED;
	private char[] fExpansion= UNINITIALIZED1;
	private PDOMMacroContainer fContainer;
	private PDOMMacroDefinitionName fDefinition;

	public PDOMMacro(PDOM pdom, int record) {
		fPDOM = pdom;
		fRecord = record;
	}
	
	
	public PDOMMacro(PDOM pdom, PDOMMacroContainer container, IASTPreprocessorMacroDefinition macro, PDOMFile file) throws CoreException {
		this(pdom, container, file, macro.getName());

		final IASTName name = macro.getName();
		final IMacroBinding binding= (IMacroBinding) name.getBinding();
		final char[][] params= binding.getParameterList();
		
		final Database db= pdom.getDB();
		db.putInt(fRecord + EXPANSION, db.newString(binding.getExpansionImage()).getRecord());		
		if (params != null) {
			StringBuilder buf= new StringBuilder();
			for (char[] param : params) {
				buf.append(param);
				buf.append(',');
			}
			db.putInt(fRecord + PARAMETERS, db.newString(buf.toString().toCharArray()).getRecord());
		}
	}
	
	public PDOMMacro(PDOM pdom, PDOMMacroContainer container, IASTPreprocessorUndefStatement undef, PDOMFile file) throws CoreException {
		this(pdom, container, file, undef.getMacroName());
	}

	private PDOMMacro(PDOM pdom, PDOMMacroContainer container, PDOMFile file, IASTName name) throws CoreException {
		final Database db= pdom.getDB();
		fPDOM = pdom;
		fRecord = db.malloc(RECORD_SIZE);
		fContainer= container;

		final IASTFileLocation fileloc = name.getFileLocation();
		db.putInt(fRecord + CONTAINER, container.getRecord());
		db.putInt(fRecord + FILE, file.getRecord());
		db.putInt(fRecord + NAME_OFFSET, fileloc.getNodeOffset());
		db.putShort(fRecord + NAME_LENGTH, (short) fileloc.getNodeLength());

		container.addDefinition(this);
	}

	public PDOM getPDOM() {
		return fPDOM;
	}
	
	public int getRecord() {
		return fRecord;
	}
	
	public void delete() throws CoreException {
		// Delete from the binding chain
		PDOMMacro prevName = getPrevInContainer();
		PDOMMacro nextName = getNextInContainer();
		if (prevName != null)
			prevName.setNextInContainer(nextName);
		else {
			PDOMMacroContainer container= getContainer();
			container.setFirstDefinition(nextName);
			if (nextName == null && container.isOrphaned()) {
				container.delete(container.getLinkageImpl());
			}
		}
		if (nextName != null)
			nextName.setPrevInContainer(prevName);

		
		final IString expansion = getExpansionInDB();
		if (expansion != null) {
			expansion.delete();
		}
		final IString params = getParamListInDB();
		if (params != null) {
			params.delete();
		}
	}
	
	public PDOMMacroContainer getContainer() throws CoreException {
		if (fContainer == null) {
			fContainer= new PDOMMacroContainer(fPDOM, fPDOM.getDB().getInt(fRecord + CONTAINER));
		}
		return fContainer;
	}
		
	private IString getExpansionInDB() throws CoreException {
		Database db = fPDOM.getDB();
		int rec = db.getInt(fRecord + EXPANSION);
		return rec == 0 ? null : db.getString(rec);
	}

	private IString getParamListInDB() throws CoreException {
		Database db = fPDOM.getDB();
		int rec = db.getInt(fRecord + PARAMETERS);
		return rec == 0 ? null : db.getString(rec);
	}

	public PDOMMacro getNextMacro() throws CoreException {
		int rec = fPDOM.getDB().getInt(fRecord + NEXT_IN_FILE);
		return rec != 0 ? new PDOMMacro(fPDOM, rec) : null;
	}
	
	public void setNextMacro(PDOMMacro macro) throws CoreException {
		setNextMacro(macro != null ? macro.getRecord() : 0);
	}

	private void setNextMacro(int rec) throws CoreException {
		fPDOM.getDB().putInt(fRecord + NEXT_IN_FILE, rec);
	}
				
	private PDOMMacro getPrevInContainer() throws CoreException {
		return getMacroField(PREV_IN_CONTAINER);
	}

	void setPrevInContainer(PDOMMacro macro) throws CoreException {
		setMacroField(PREV_IN_CONTAINER, macro);
	}

	public PDOMMacro getNextInContainer() throws CoreException {
		return getMacroField(NEXT_IN_CONTAINER);
	}
	
	void setNextInContainer(PDOMMacro macro) throws CoreException {
		setMacroField(NEXT_IN_CONTAINER, macro);
	}

	private void setMacroField(int offset, PDOMMacro macro) throws CoreException {
		int namerec = macro != null ? macro.getRecord() : 0;
		fPDOM.getDB().putInt(fRecord + offset, namerec);
	}
	
	private PDOMMacro getMacroField(int offset) throws CoreException {
		int namerec= fPDOM.getDB().getInt(fRecord + offset);
		return namerec != 0 ? new PDOMMacro(fPDOM, namerec) : null;
	}

	public char[][] getParameterList() {
		if (fParameterList == UNINITIALIZED) {
			fParameterList= null;
			try {
				IString plist= getParamListInDB();
				if (plist != null) {
					List<char[]> paramList = new ArrayList<char[]>();
					final char[] cplist= plist.getChars();
					final int end = cplist.length;
					int from= 0;
					int to= CharArrayUtils.indexOf(',', cplist, from, end);
					while (to > from) {
						paramList.add(CharArrayUtils.extract(cplist, from, to-from));
						from= to+1;
						to= CharArrayUtils.indexOf(',', cplist, from, end);
					}
					fParameterList= paramList.toArray(new char[paramList.size()][]);
				}
			} catch (CoreException e) {
				CCorePlugin.log(e);
			}
		}
		return fParameterList;
	}

	public boolean isMacroDefinition() throws CoreException {
		if (fExpansion == UNINITIALIZED1) {
			return fPDOM.getDB().getInt(fRecord + EXPANSION) != 0;
		}
		return fExpansion != null;
	}

	public char[] getExpansionImage() {
		if (fExpansion == UNINITIALIZED1) {
			try {
				final IString expansionInDB = getExpansionInDB();
				fExpansion= expansionInDB == null ? null : expansionInDB.getChars();
			} catch (CoreException e) {
				CCorePlugin.log(e);
				fExpansion= CharArrayUtils.EMPTY;
			}
		}
		return fExpansion;
	}

	public char[] getNameCharArray() {
		try {
			return getContainer().getNameCharArray();
		}
		catch (CoreException e) {
			CCorePlugin.log(e);
			return new char[]{' '};
		}
	}
	
	public String getName() {
		return new String(getNameCharArray());
	}
	
	public PDOMFile getFile() throws CoreException {
		int filerec = fPDOM.getDB().getInt(fRecord + FILE);
		return filerec != 0 ? new PDOMFile(fPDOM, filerec) : null;
	}

	public int getEndingLineNumber() {
		return 0;
	}

	public String getFileName() {
		try {
			PDOMFile file = getFile();
			if(file!=null) {
				/*
				 * We need to spec. what this method can return to know
				 * how to implement this. Existing implmentations return
				 * the absolute path, so here we attempt to do the same.
				 */
				URI uri = file.getLocation().getURI();
				if ("file".equals(uri.getScheme())) //$NON-NLS-1$
					return uri.getSchemeSpecificPart();
			}
		} catch (CoreException e) {
			CCorePlugin.log(e);
		}
		return null;
	}

	public int getStartingLineNumber() {
		return 0;
	}

	public IASTFileLocation asFileLocation() {
		return this;
	}
	
	public IASTFileLocation getFileLocation() {
		return this;
	}

	public int getNodeLength() {
		try {
			return fPDOM.getDB().getShort(fRecord + NAME_LENGTH);
		} catch (CoreException e) {
			CCorePlugin.log(e);
			return 0;
		}
	}

	public int getNodeOffset() {
		try {
			return fPDOM.getDB().getInt(fRecord + NAME_OFFSET);
		} catch (CoreException e) {
			CCorePlugin.log(e);
			return 0;
		}
	}

	public char[] getExpansion() {
		char[] expansionImage= getExpansionImage();
		return MacroDefinitionParser.getExpansion(expansionImage, 0, expansionImage.length);
	}

	public char[][] getParameterPlaceholderList() {
		char[][] params= getParameterList();
		if (params != null && params.length > 0) {
			char[] lastParam= params[params.length-1];
			if (CharArrayUtils.equals(lastParam, 0, Keywords.cpELLIPSIS.length, Keywords.cpELLIPSIS)) {
				char[][] result= new char[params.length][];
				System.arraycopy(params, 0, result, 0, params.length-1);
				result[params.length-1]= lastParam.length == Keywords.cpELLIPSIS.length ? Keywords.cVA_ARGS : 
					CharArrayUtils.extract(lastParam, Keywords.cpELLIPSIS.length, lastParam.length-Keywords.cpELLIPSIS.length);
				return result;
			}
		}
		return params;
	}

	public boolean isFunctionStyle() {
		return getParameterList() != null;
	}
	
	public boolean isDynamic() {
		return false;
	}

	public ILinkage getLinkage() throws CoreException {
		return getFile().getLinkage();
	}

	public IIndexScope getScope() {
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(PDOMMacro.class)) {
			return this;
		}
		return null;
	}

	public IIndexFragmentName getDefinition() throws CoreException {
		if (!isMacroDefinition()) {
			return null;
		}
		if (fDefinition == null) {
			fDefinition= new PDOMMacroDefinitionName(this);
		}
		return fDefinition;
	}

	public IIndexFile getLocalToFile() throws CoreException {
		return null;
	}

	public String[] getQualifiedName() {
		return new String[]{getName()};
	}

	public boolean isFileLocal() throws CoreException {
		return false;
	}

	public int getBindingConstant() {
		return IIndexBindingConstants.MACRO_DEFINITION;
	}

	public IIndexFragment getFragment() {
		return fPDOM;
	}

	public boolean hasDeclaration() throws CoreException {
		return false;
	}

	public boolean hasDefinition() throws CoreException {
		return true;
	}
}
