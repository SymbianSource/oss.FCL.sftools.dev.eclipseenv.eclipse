/*******************************************************************************
 * Copyright (c) 2005, 2007 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * Markus Schorn - initial API and implementation 
 ******************************************************************************/ 

package org.eclipse.cdt.ui.tests.refactoring.rename;

import java.io.StringWriter;

import org.eclipse.cdt.core.tests.BaseTestFramework;
import org.eclipse.cdt.internal.core.dom.SavedCodeReaderFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.search.internal.core.text.FileCharSequenceProvider;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;


/**
 * @author markus.schorn@windriver.com
 */
public class RefactoringTests extends BaseTestFramework {
    private int fBufferSize;

    public RefactoringTests() {
    }

    public RefactoringTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        fBufferSize= FileCharSequenceProvider.BUFFER_SIZE;
        FileCharSequenceProvider.BUFFER_SIZE= 1024*4;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        SavedCodeReaderFactory.getInstance().getCodeReaderCache().flush();
        FileCharSequenceProvider.BUFFER_SIZE= fBufferSize;
    }

    protected void assertTotalChanges(int numChanges, Change changes) throws Exception {
        assertTotalChanges(numChanges, 0, 0, changes);
    }

    protected void assertTotalChanges(int numChanges, int potChanges, int commentCh, 
            Change changes) throws Exception {
        int count[]= {0,0,0};
        if( changes != null ) {
            countChanges( changes, count);
        }
        assertEquals( numChanges, count[0] );
        assertEquals("potential changes: ", potChanges, count[1]); //$NON-NLS-1$
        assertEquals("comment changes: ", commentCh, count[2]); //$NON-NLS-1$
    }

    private void countChanges(Change change, int[] count) {
        if( change instanceof CompositeChange ){
            Change [] children = ((CompositeChange) change).getChildren();
            for( int i = 0; i < children.length; i++ ){
                countChanges( children[i], count);
            }
        } else if( change instanceof TextFileChange ){
            TextFileChange tfc= (TextFileChange) change;
            TextEditChangeGroup[] tecgs= tfc.getTextEditChangeGroups();
            for (int i = 0; i < tecgs.length; i++) {
                TextEditChangeGroup group = tecgs[i];
                countChanges(group, count);
            }
        }
    }

    private void countChanges(TextEditChangeGroup edit, int[] count) {
        String name= edit.getName();
        if (name.indexOf("potential") != -1) { //$NON-NLS-1$
            count[1]++;
        }
        else if (name.indexOf("comment") != -1) { //$NON-NLS-1$
            count[2]++;
        }
        else {
            count[0]++;
        }
    }

    protected void assertChange(Change changes, IFile file, int startOffset, int numChars, String newText) throws Exception {
        assertChange(changes, file, startOffset, numChars, newText, false);
    }

    protected void assertChange(Change changes, IFile file, int startOffset, int numChars, String newText, boolean potential) throws Exception {
        boolean found = false;
        if( changes != null && changes instanceof CompositeChange ){
            found = checkCompositeChange( (CompositeChange) changes, file, startOffset, numChars, newText, potential);
        }
        
        if( !found ) {
            fail ("Rename at offset " + startOffset + " in \"" + file.getLocation() + "\" not found.");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertFalse( true );
        }
    }

    private boolean checkCompositeChange(CompositeChange composite, IFile file, int startOffset, int numChars, String newText, boolean potential) {
        boolean found = false;
        Change [] children = composite.getChildren();
        for( int i = 0; i < children.length; i++ ){
            if( children[i] instanceof CompositeChange )
                found = checkCompositeChange( (CompositeChange) children[i], file, startOffset, numChars, newText, potential);
            else if( children[i] instanceof TextFileChange ){
                TextFileChange tuChange = (TextFileChange) children[i];
                if( tuChange.getFile().toString().equals( file.toString() ) ){
                    found = checkTranslationUnitChange( tuChange, startOffset, numChars, newText, potential );
                }
            }
            if( found )
                return found;
        }
        return found;
    }

    private boolean checkTranslationUnitChange(TextFileChange change, int startOffset, int numChars, String newText, boolean potential) {
        TextEditChangeGroup[] groups= change.getTextEditChangeGroups();
        for (int i = 0; i < groups.length; i++) {
            TextEditGroup group = groups[i].getTextEditGroup();
            if ((group.getName().indexOf("potential") != -1) == potential) {  //$NON-NLS-1$
                TextEdit[] edits= group.getTextEdits();
                if (checkTextEdits(edits, startOffset, numChars, newText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkTextEdit(TextEdit edit, int startOffset, int numChars, String newText) {
        if (edit instanceof MultiTextEdit) {
            if (checkTextEdits(((MultiTextEdit) edit).getChildren(), 
                    startOffset, numChars, newText)) {
                return true;
            }
        }
        else if (edit instanceof ReplaceEdit) {
            if (checkReplaceEdit((ReplaceEdit) edit, startOffset, numChars, newText ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean checkTextEdits(TextEdit[] edits, int startOffset, int numChars, String newText) {
        for (int i = 0; i < edits.length; i++) {
            TextEdit edit = edits[i];
            if (checkTextEdit(edit, startOffset, numChars, newText)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkReplaceEdit(ReplaceEdit edit, int startOffset, int numChars, String newText) {
        return ( edit.getOffset() == startOffset && edit.getLength() == numChars && edit.getText().equals( newText ) );
    }
    
    protected IFile createCppFwdDecls(String fileName) throws Exception {
        StringWriter writer = new StringWriter();
        writer.write( "class class_fwd;         \n"); //$NON-NLS-1$
        writer.write( "struct struct_fwd;       \n"); //$NON-NLS-1$
        writer.write( "union union_fwd;         \n"); //$NON-NLS-1$
        writer.write( "int func_proto();        \n"); //$NON-NLS-1$
        writer.write( "int func_proto_ov();     \n"); //$NON-NLS-1$
        writer.write( "int func_proto_ov(int);  \n"); //$NON-NLS-1$
        writer.write( "int func_proto_ov(int*); \n"); //$NON-NLS-1$
        writer.write( "extern int extern_var;   \n"); //$NON-NLS-1$
        String contents = writer.toString();
        return importFile(fileName, contents ); 
    }

    protected IFile createCFwdDecls(String fileName) throws Exception {
        StringWriter writer = new StringWriter();
        writer.write( "struct struct_fwd;       \n"); //$NON-NLS-1$
        writer.write( "union union_fwd;         \n"); //$NON-NLS-1$
        writer.write( "int func_proto();        \n"); //$NON-NLS-1$
        writer.write( "extern int extern_var;   \n"); //$NON-NLS-1$
        String contents = writer.toString();
        return importFile(fileName, contents ); 
    }

    protected IFile createCppDefs(String fileName) throws Exception {
        StringWriter writer = new StringWriter();
        writer.write( "class class_def {               \n"); //$NON-NLS-1$
        writer.write( "public:                         \n"); //$NON-NLS-1$
        writer.write( "   int member;                  \n"); //$NON-NLS-1$
        writer.write( "   static int static_member;    \n"); //$NON-NLS-1$
        writer.write( "   void method(int par);        \n"); //$NON-NLS-1$
        writer.write( "   void static_method(int par); \n"); //$NON-NLS-1$
        writer.write( "   int method_ov();             \n"); //$NON-NLS-1$
        writer.write( "   int method_ov(int);          \n"); //$NON-NLS-1$
        writer.write( "   int method_ov(int*);         \n"); //$NON-NLS-1$
        writer.write( "};                              \n"); //$NON-NLS-1$
        writer.write( "struct struct_def {        \n"); //$NON-NLS-1$
        writer.write( "   int st_member;          \n"); //$NON-NLS-1$
        writer.write( "};                         \n"); //$NON-NLS-1$
        writer.write( "union union_def {          \n"); //$NON-NLS-1$
        writer.write( "   int un_member;          \n"); //$NON-NLS-1$
        writer.write( "};                         \n"); //$NON-NLS-1$
        writer.write( "typedef int typedef_def;   \n"); //$NON-NLS-1$
        writer.write( "namespace namespace_def{}; \n"); //$NON-NLS-1$
        writer.write( "enum enum_def {            \n"); //$NON-NLS-1$
        writer.write( "   enum_item };            \n"); //$NON-NLS-1$
        writer.write( "int func_def() {}          \n"); //$NON-NLS-1$
        writer.write( "int func_def_ov() {}       \n"); //$NON-NLS-1$
        writer.write( "int func_def_ov(int){}     \n"); //$NON-NLS-1$
        writer.write( "int func_def_ov(int*){}    \n"); //$NON-NLS-1$
        writer.write( "int var_def;               \n"); //$NON-NLS-1$
        String contents = writer.toString();
        return importFile(fileName, contents ); 
    }

    protected IFile createCDefs(String fileName) throws Exception {
        StringWriter writer = new StringWriter();
        writer.write( "struct struct_def {        \n"); //$NON-NLS-1$
        writer.write( "   int st_member;          \n"); //$NON-NLS-1$
        writer.write( "};                         \n"); //$NON-NLS-1$
        writer.write( "union union_def {          \n"); //$NON-NLS-1$
        writer.write( "   int un_member;          \n"); //$NON-NLS-1$
        writer.write( "};                         \n"); //$NON-NLS-1$
        writer.write( "typedef int typedef_def;   \n"); //$NON-NLS-1$
        writer.write( "enum enum_def {            \n"); //$NON-NLS-1$
        writer.write( "   enum_item };            \n"); //$NON-NLS-1$
        writer.write( "int func_def() {}          \n"); //$NON-NLS-1$
        writer.write( "int var_def;               \n"); //$NON-NLS-1$
        String contents = writer.toString();
        return importFile(fileName, contents ); 
    }

    protected void assertRefactoringError(RefactoringStatus status, String msg) {
        RefactoringStatusEntry e= status.getEntryMatchingSeverity(RefactoringStatus.ERROR);
        assertNotNull("Expected refactoring error!", e); //$NON-NLS-1$
        assertEquals(msg, e.getMessage());
    }

    protected void assertRefactoringWarning(RefactoringStatus status, String msg) {
        RefactoringStatusEntry e= status.getEntryMatchingSeverity(RefactoringStatus.WARNING);
        assertNotNull("Expected refactoring warning!", e); //$NON-NLS-1$
        assertEquals(msg, e.getMessage());
    }

    protected void assertRefactoringOk(RefactoringStatus status) {
        assertTrue("Expected refactoring status ok: " + //$NON-NLS-1$
                status.getMessageMatchingSeverity(status.getSeverity()), 
                status.getSeverity()==RefactoringStatus.OK); 
    }

}
