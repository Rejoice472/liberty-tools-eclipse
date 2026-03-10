/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package io.openliberty.tools.eclipse.jakarta.ui;

import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.ui.handlers.HandlerUtil;

import io.openliberty.tools.eclipse.ls.plugin.LibertyToolsLSPlugin;

/**
 * Handler for the "Reset Jakarta EE Version" command.
 * This handler is triggered when the user right-clicks on a project
 * and selects "Reset Jakarta EE Version" from the context menu.
 */
public class ResetJakartaVersionHandler extends AbstractHandler {

    private static final String RESET_VERSION_COMMAND = "jakarta.resetVersion";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object element = structuredSelection.getFirstElement();
            
            IProject project = null;
            
            // Handle direct IProject selection
            if (element instanceof IProject) {
                project = (IProject) element;
            }
            // Handle IJavaProject or other adaptable types
            else if (element instanceof IAdaptable) {
                project = ((IAdaptable) element).getAdapter(IProject.class);
            }
            
            if (project != null) {
                resetVersionForProject(project);
            }
        }
        
        return null;
    }

    /**
     * Resets the Jakarta EE version for the given project by sending
     * a workspace/executeCommand request to the language server.
     *
     * @param project The project to reset version for
     */
    private void resetVersionForProject(IProject project) {
        try {
            // Get the project URI
            String projectUri = project.getLocationURI().toString();
            
            // Find a Java file in the project
            IFile javaFile = findJavaFile(project);
            if (javaFile == null) {
                LibertyToolsLSPlugin.getDefault().getLog().log(
                    new Status(IStatus.WARNING, 
                        LibertyToolsLSPlugin.getDefault().getBundle().getSymbolicName(),
                        "No Java files found in project: " + project.getName())
                );
                return;
            }
            
            // Get the document for the file
            IDocument document = LSPEclipseUtils.getDocument(javaFile);
            if (document != null) {
                // Execute the command via workspace/executeCommand
                ExecuteCommandParams params = new ExecuteCommandParams(
                    RESET_VERSION_COMMAND, 
                    Arrays.asList(projectUri)
                );
                
                // Use LanguageServers to execute the command
                LanguageServers.forDocument(document).computeAll((w, ls) -> 
                    ls.getWorkspaceService().executeCommand(params)
                );
            }
        } catch (Exception e) {
            LibertyToolsLSPlugin.getDefault().getLog().log(
                new Status(IStatus.ERROR, 
                    LibertyToolsLSPlugin.getDefault().getBundle().getSymbolicName(),
                    "Error resetting Jakarta version for project: " + project.getName(), 
                    e)
            );
        }
    }
    
    /**
     * Find a Java file in the project.
     * 
     * @param project The project to search
     * @return A Java file, or null if none found
     */
    private IFile findJavaFile(IProject project) {
        try {
            final IFile[] result = new IFile[1];
            project.accept(resource -> {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    if ("java".equals(file.getFileExtension())) {
                        result[0] = file;
                        return false; // Stop visiting
                    }
                }
                return true; // Continue visiting
            });
            return result[0];
        } catch (Exception e) {
            LibertyToolsLSPlugin.getDefault().getLog().log(
                new Status(IStatus.ERROR, 
                    LibertyToolsLSPlugin.getDefault().getBundle().getSymbolicName(),
                    "Error finding Java file in project", 
                    e)
            );
            return null;
        }
    }
}

// Made with Bob
