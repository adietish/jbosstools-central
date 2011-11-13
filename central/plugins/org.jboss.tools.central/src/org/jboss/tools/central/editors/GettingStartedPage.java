/*************************************************************************************
 * Copyright (c) 2008-2011 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.jboss.tools.central.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.central.JBossCentralActivator;
import org.jboss.tools.central.dialogs.ProjectExamplesDialog;
import org.jboss.tools.central.jobs.RefreshBlogsJob;
import org.jboss.tools.central.jobs.RefreshNewsJob;
import org.jboss.tools.central.jobs.RefreshTutorialsJob;
import org.jboss.tools.central.model.FeedsEntry;
import org.jboss.tools.central.model.Tutorial;
import org.jboss.tools.central.model.TutorialCategory;
import org.jboss.tools.project.examples.ProjectExamplesActivator;
import org.jboss.tools.project.examples.model.Project;
import org.jboss.tools.project.examples.model.ProjectFix;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class GettingStartedPage extends AbstractJBossCentralPage {

	private static final String CLASS_ATTRIBUTE = "class";
	public static final String ID = ID_PREFIX + "GettingStartedPage";
	
	protected static final long TIME_DELAY = 2000L;
	private IWorkbenchAction newWizardDropDownAction;
	private ScrolledForm form;
	private PageBook blogsPageBook;
	private ScrolledComposite blogsScrollComposite;
	private RefreshBlogsJobChangeListener refreshBlogsJobChangeListener;
	private FormText blogsNoteText;
	private FormText tutorialsNoteText;
	private Composite blogsLoadingComposite;
	private Composite tutorialsLoadingComposite;
	private FormText blogsExceptionText;
	private FormText tutorialsExceptionText;
	private Composite blogsComposite;
	private Composite tutorialsComposite;
	private FormToolkit toolkit;
	private ScrolledComposite tutorialScrollComposite;
	private PageBook tutorialPageBook;
	private RefreshTutorialsJobChangeListener refreshTutorialsJobChangeListener;
	private Section blogsSection;
	private Section tutorialsSection;
	private Section documentationSection;
	private Section projectsSection;
	private Composite projectsComposite;
	private Composite documentationComposite;
	
	private Set<TutorialCategory> expandedCategories = new HashSet<TutorialCategory>();
	private Section newsSection;
	private ScrolledComposite newsScrollComposite;
	private PageBook newsPageBook;
	private FormText newsNoteText;
	private Composite newsLoadingComposite;
	private FormText newsExceptionText;
	private Composite newsComposite;
	private RefreshNewsJobChangeListener refreshNewsJobChangeListener;
	private Section settingsSection;
	private Composite settingsComposite;
	private Point oldSize;
	
	public GettingStartedPage(FormEditor editor) {
		super(editor, ID, "Getting Started");
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		toolkit = managedForm.getToolkit();
		form = managedForm.getForm();
		
		Composite body = form.getBody();
	    GridLayout gridLayout = new GridLayout(2, true);
	    gridLayout.horizontalSpacing = 7;
	    body.setLayout(gridLayout);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
	    body.setLayoutData(gd);
	    toolkit.paintBordersFor(body);
	    
		Composite left = createComposite(toolkit, body);
		createProjectsSection(toolkit, left);
		createTutorialsSection(toolkit, left);
		createDocumentationSection(toolkit, left);
		createSettingsSection(toolkit, left);
		toolkit.paintBordersFor(left);
				
		Composite right = createComposite(toolkit, body);
		createNewsSection(toolkit, right);
		createBlogsSection(toolkit, right);
		toolkit.paintBordersFor(right);
				
		final ControlAdapter controlAdapter = new ControlAdapter() {

			@Override
			public void controlResized(ControlEvent e) {
				resize();
			}
		
		};
		form.addControlListener(controlAdapter);
		form.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				form.removeControlListener(controlAdapter);
				form.removeDisposeListener(this);
			}
		});
		
		resize();
	    
	}

	private void createBlogsSection(FormToolkit toolkit, Composite parent) {
		blogsSection = createSection(toolkit, parent, "Blogs", ExpandableComposite.TITLE_BAR|ExpandableComposite.EXPANDED|ExpandableComposite.TWISTIE);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
	    //gd.widthHint = 350;
	    //gd.heightHint = 100;
	    blogsSection.setLayoutData(gd);
		createBlogsToolbar(toolkit, blogsSection);
				
		blogsScrollComposite = new ScrolledComposite(blogsSection, SWT.V_SCROLL);
		gd =new GridData(SWT.FILL, SWT.FILL, true, false);
		blogsScrollComposite.setLayoutData(gd);
		blogsScrollComposite.setLayout(new GridLayout());
		
		blogsPageBook = new PageBook(blogsScrollComposite, SWT.WRAP);
		gd =new GridData(SWT.FILL, SWT.FILL, true, false);
	    blogsPageBook.setLayoutData(gd);
        
        blogsScrollComposite.setContent(blogsPageBook);
    	blogsScrollComposite.setExpandVertical(true);
    	blogsScrollComposite.setExpandHorizontal(true);
    	blogsScrollComposite.setAlwaysShowScrollBars(false);

    	blogsNoteText = createNoteText(toolkit, blogsPageBook);
	    blogsLoadingComposite = createLoadingComposite(toolkit, blogsPageBook);	    
	    blogsExceptionText = createExceptionText(toolkit, blogsPageBook);
		
	    blogsComposite = toolkit.createComposite(blogsPageBook, SWT.NONE);	    
		blogsComposite.setLayout(new TableWrapLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(blogsComposite);

		blogsSection.setClient(blogsScrollComposite);
		blogsSection.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				resize(true);
			}
			
		});
		showLoading(blogsPageBook, blogsLoadingComposite, blogsScrollComposite);
		blogsPageBook.pack(true);
		RefreshBlogsJob refreshBlogsJob = RefreshBlogsJob.INSTANCE;
		refreshBlogsJobChangeListener = new RefreshBlogsJobChangeListener();
		refreshBlogsJob.addJobChangeListener(refreshBlogsJobChangeListener);
		refreshBlogsJob.schedule();
	}
	
	private void createNewsSection(FormToolkit toolkit, Composite parent) {
		newsSection = createSection(toolkit, parent, "News", ExpandableComposite.TITLE_BAR|ExpandableComposite.EXPANDED|ExpandableComposite.TWISTIE);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
	    //gd.widthHint = 350;
	    //gd.heightHint = 100;
	    newsSection.setLayoutData(gd);
		createNewsToolbar(toolkit, newsSection);
				
		newsScrollComposite = new ScrolledComposite(newsSection, SWT.V_SCROLL);
		gd =new GridData(SWT.FILL, SWT.FILL, true, false);
		newsScrollComposite.setLayoutData(gd);
		newsScrollComposite.setLayout(new GridLayout());
		
		newsPageBook = new PageBook(newsScrollComposite, SWT.WRAP);
		gd =new GridData(SWT.FILL, SWT.FILL, true, false);
	    newsPageBook.setLayoutData(gd);
        
        newsScrollComposite.setContent(newsPageBook);
    	newsScrollComposite.setExpandVertical(true);
    	newsScrollComposite.setExpandHorizontal(true);
    	newsScrollComposite.setAlwaysShowScrollBars(false);

    	newsNoteText = createNoteText(toolkit, newsPageBook);
	    newsLoadingComposite = createLoadingComposite(toolkit, newsPageBook);	    
	    newsExceptionText = createExceptionText(toolkit, newsPageBook);
		
	    newsComposite = toolkit.createComposite(newsPageBook, SWT.NONE);	    
		newsComposite.setLayout(new TableWrapLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(newsComposite);

		newsSection.setClient(newsScrollComposite);
		newsSection.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				resize(true);
			}
			
		});
		showLoading(newsPageBook, newsLoadingComposite, newsScrollComposite);
		newsPageBook.pack(true);
		RefreshNewsJob refreshNewsJob = RefreshNewsJob.INSTANCE;
		refreshNewsJobChangeListener = new RefreshNewsJobChangeListener();
		refreshNewsJob.addJobChangeListener(refreshNewsJobChangeListener);
		refreshNewsJob.schedule();
	}


	private FormText createExceptionText(FormToolkit toolkit, Composite parent) {
		FormText formText = toolkit.createFormText(parent, true);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, false, false);
	    formText.setLayoutData(gd);
		return formText;
	}
	
	private FormText createNoteText(FormToolkit toolkit, Composite parent) {
		FormText formText = toolkit.createFormText(parent, true);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, false, false);
	    formText.setLayoutData(gd);
		formText.setText("<form><p>" +
	    		"<img href=\"image\"/>" + 
	    		" No entries found." +
				"Check your internet connection and " +
				"<a href=\"networkConnections\">Window > Preferences > General > Network Connections</a> " +
				"preferences" +
				"</p></form>",
				true, false);
		
		Image image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);

		formText.setImage("image", image);
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if ("networkConnections".equals(e.data)) {
					PreferenceDialog dialog = PreferencesUtil
							.createPreferenceDialogOn(null,
									"org.eclipse.ui.net.NetPreferences", null,
									null);
					dialog.open();
				}
			}
		});
		return formText;
	}

	private void createBlogsToolbar(FormToolkit toolkit, Section section) {
		Composite headerComposite = toolkit.createComposite(section, SWT.NONE);
	    RowLayout rowLayout = new RowLayout();
	    rowLayout.marginTop = 0;
	    rowLayout.marginBottom = 0;
	    headerComposite.setLayout(rowLayout);
	    headerComposite.setBackground(null);
	    
	    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
		toolBarManager.createControl(headerComposite);
		
		CommandContributionItem item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.openJBossBlogs");
		toolBarManager.add(item);
		
		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.refreshJBossBlogs");
		toolBarManager.add(item);

	    toolBarManager.update(true);
	    
		section.setTextClient(headerComposite);
	}
	
	private void createNewsToolbar(FormToolkit toolkit, Section section) {
		Composite headerComposite = toolkit.createComposite(section, SWT.NONE);
	    RowLayout rowLayout = new RowLayout();
	    rowLayout.marginTop = 0;
	    rowLayout.marginBottom = 0;
	    headerComposite.setLayout(rowLayout);
	    headerComposite.setBackground(null);
	    
	    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
		toolBarManager.createControl(headerComposite);
		
		CommandContributionItem item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.openJBossNews");
		toolBarManager.add(item);
		
		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.openJBossToolsTwitter");
		toolBarManager.add(item);
		
		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.refreshJBossNews");
		toolBarManager.add(item);

	    toolBarManager.update(true);
	    
		section.setTextClient(headerComposite);
	}

	
	private void createTutorialsSection(FormToolkit toolkit, Composite parent) {
		tutorialsSection = createSection(toolkit, parent, "Project Examples", ExpandableComposite.TITLE_BAR|ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
	    //gd.widthHint = 350;
	    //gd.heightHint = 100;
	    tutorialsSection.setLayoutData(gd);
	    
	    createTutorialsToolbar(toolkit, tutorialsSection);
		
	    tutorialScrollComposite = new ScrolledComposite(tutorialsSection, SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		tutorialScrollComposite.setLayoutData(gd);
		tutorialScrollComposite.setLayout(new GridLayout());
		toolkit.adapt(tutorialScrollComposite);
		
		tutorialPageBook = new PageBook(tutorialScrollComposite, SWT.WRAP);
		gd =new GridData(SWT.FILL, SWT.FILL, false, false);
	    tutorialPageBook.setLayoutData(gd);
        
        tutorialScrollComposite.setContent(tutorialPageBook);
    	tutorialScrollComposite.setExpandVertical(true);
    	tutorialScrollComposite.setExpandHorizontal(true);
    	tutorialScrollComposite.setAlwaysShowScrollBars(false);
//    	tutorialScrollComposite.addControlListener(new ControlAdapter() {
//    		public void controlResized(ControlEvent e) {
//    			recomputeScrollComposite(tutorialScrollComposite, tutorialPageBook);
//    		}
//    	});
	    		
	    tutorialsNoteText = createNoteText(toolkit, tutorialPageBook);
	    tutorialsLoadingComposite = createLoadingComposite(toolkit, tutorialPageBook);	    
	    tutorialsExceptionText = createExceptionText(toolkit, tutorialPageBook);

	    tutorialsComposite = toolkit.createComposite(tutorialPageBook, SWT.NONE);	    
		tutorialsComposite.setLayout(new GridLayout());
		gd =new GridData(SWT.FILL, SWT.FILL, false, false);
		tutorialsComposite.setLayoutData(gd);
				        
	    tutorialsSection.setClient(tutorialScrollComposite);
	    
	    tutorialsSection.addExpansionListener(new ExpansionAdapter() {
						
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				resize(true);
			}
		});
	    
		showLoading(tutorialPageBook, tutorialsLoadingComposite, tutorialScrollComposite);
		tutorialPageBook.pack(true);
		RefreshTutorialsJob refreshTutorialsJob = RefreshTutorialsJob.INSTANCE;
		refreshTutorialsJobChangeListener = new RefreshTutorialsJobChangeListener();
		refreshTutorialsJob.addJobChangeListener(refreshTutorialsJobChangeListener);
		refreshTutorialsJob.schedule();
	}

	private void createTutorialsToolbar(FormToolkit toolkit, Section section) {
		Composite headerComposite = toolkit.createComposite(section, SWT.NONE);
	    RowLayout rowLayout = new RowLayout();
	    rowLayout.marginTop = 0;
	    rowLayout.marginBottom = 0;
	    headerComposite.setLayout(rowLayout);
	    headerComposite.setBackground(null);
	    
	    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
		toolBarManager.createControl(headerComposite);
		
		CommandContributionItem item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.newProjectExamplesWizard");
		toolBarManager.add(item);
		
		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.runtime.preferences");
		toolBarManager.add(item);

		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.seam.runtime.preferences");
		toolBarManager.add(item);

		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.wtp.runtime.preferences");
		toolBarManager.add(item);
		
		item = JBossCentralActivator.createContributionItem(getSite(), "org.jboss.tools.central.refreshJBossTutorials");
		toolBarManager.add(item);

		//Action action = new DownloadRuntimeAction("Download and Install JBoss AS 7.0.1", JBossCentralActivator.imageDescriptorFromPlugin(JBossCentralActivator.PLUGIN_ID, "/icons/jbossas7.png"), "org.jboss.tools.runtime.core.as.701");
		//toolBarManager.add(action);

	    toolBarManager.update(true);
	    
		section.setTextClient(headerComposite);
	}
	
	public void createProjectsSection(FormToolkit toolkit, Composite parent) {
		projectsSection = createSection(toolkit, parent, "Create Projects", ExpandableComposite.TITLE_BAR|ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED);
		projectsSection.setText("Create Projects");
	    projectsSection.setLayout(new GridLayout());
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
	    //gd.widthHint = 350;
	    //gd.heightHint = 100;
	    projectsSection.setLayoutData(gd);
	    
		
	    Composite headerComposite = toolkit.createComposite(projectsSection, SWT.NONE);
	    RowLayout rowLayout = new RowLayout();
	    rowLayout.marginTop = 0;
	    rowLayout.marginBottom = 0;
	    headerComposite.setLayout(rowLayout);
	    headerComposite.setBackground(null);
	    
	    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
		toolBarManager.createControl(headerComposite);
	
	    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		newWizardDropDownAction = IDEActionFactory.NEW_WIZARD_DROP_DOWN
                .create(window);
	    toolBarManager.add(newWizardDropDownAction);
	    toolBarManager.update(true);
	    
		projectsSection.setTextClient(headerComposite);
		projectsSection.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				resize(true);
			}
		});
		
		projectsComposite = toolkit.createComposite(projectsSection);
	    GridLayout layout = new GridLayout(2, true);
	    layout.horizontalSpacing = 10;
	    projectsComposite.setLayout(layout);
	    GridDataFactory.fillDefaults().grab(true, true).applyTo(projectsComposite);

	    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
	    IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint("org.eclipse.ui.newWizards");
	    IExtension[] extensions = extensionPoint.getExtensions();
	    
	    List<String> wizardIDs = new ArrayList<String>();
	    //wizardIDs.add("org.jboss.ide.eclipse.as.openshift.express.ui.wizard.NewServerAdapter");
	    wizardIDs.add("org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard");
	    wizardIDs.add("org.jboss.tools.seam.ui.wizards.SeamProjectWizard");
	    wizardIDs.add("org.eclipse.m2e.core.wizards.Maven2ProjectWizard");
	    wizardIDs.add(JBossCentralActivator.NEW_PROJECT_EXAMPLES_WIZARD_ID);
	    
	    List<String> createdIDs = new ArrayList<String>();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension
					.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				String id = element.getAttribute("id");
				if (wizardIDs.contains(id) && !createdIDs.contains(id)) {
					createProjectLink(toolkit, projectsComposite, element);
					createdIDs.add(id);
				}
			}
		}
		projectsSection.setClient(projectsComposite);
	}

	private void createProjectLink(FormToolkit toolkit, Composite composite,
			final IConfigurationElement element) {
		if (element == null) {
			return;
		}
		String name = element.getAttribute("name");
		String id = element.getAttribute("id");
		if (name == null || id == null) {
			return;
		}
		String iconPath = element.getAttribute("icon");
		Image image = null;
		if (iconPath != null) {
			IContributor contributor = element.getContributor();
			String pluginId = contributor.getName();
			ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconPath);
			if (imageDescriptor != null) {
				image = JBossCentralActivator.getDefault().getImage(imageDescriptor);
			}
		}
		ImageHyperlink link = toolkit.createImageHyperlink(composite, SWT.NONE);
	    link.setText(name);
	    if (image != null) {
	    	link.setImage(image);
	    }
	    GridDataFactory.fillDefaults().grab(true, false).applyTo(link);
	    link.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				try {
					Object object = createExtension(element);
					if (object instanceof INewWizard) {
				          INewWizard wizard = (INewWizard)object;
				          ISelection selection = getSite().getSelectionProvider().getSelection();
				          if (selection instanceof IStructuredSelection) {
				        	  wizard.init(PlatformUI.getWorkbench(), (IStructuredSelection) selection);
				          }
				          WizardDialog dialog = new WizardDialog(getSite().getShell(), wizard);
				          dialog.open();
					}
				} catch (CoreException e1) {
					JBossCentralActivator.log(e1);
				}
			}
	    	
	    });
	}

	public static Object createExtension(final IConfigurationElement element) throws CoreException {
		if (element == null) {
			return null;
		}
		try {
			Bundle bundle = Platform.getBundle(element.getContributor().getName());
			if (isActive(bundle)) {
				return element.createExecutableExtension(CLASS_ATTRIBUTE);
			}
			final Object[] ret = new Object[1];
			final CoreException[] exc = new CoreException[1];
			BusyIndicator.showWhile(null, new Runnable() {
			    public void run() {
			        try {
			            ret[0] = element
			                    .createExecutableExtension(CLASS_ATTRIBUTE);
			        } catch (CoreException e) {
			            exc[0] = e;
			        }
			    }
			});
			if (exc[0] != null) {
				throw exc[0];
			}
			return ret[0];
		} catch (InvalidRegistryObjectException e) {
			throw new CoreException(new Status(IStatus.ERROR, JBossCentralActivator.PLUGIN_ID,
                    IStatus.ERROR, "Cannot create extension" ,e));
		}
	}
	
	private static boolean isActive(Bundle bundle) {
		if (bundle == null) {
			return false;
		}
		return bundle.getState() == Bundle.ACTIVE;
	}
	
	public void createDocumentationSection(FormToolkit toolkit, Composite parent) {
		documentationSection = createSection(toolkit, parent, "Documentation", ExpandableComposite.TITLE_BAR|ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
	    //gd.widthHint = 350;
	    //gd.heightHint = 100;
	    documentationSection.setLayoutData(gd);
	    
		documentationComposite = toolkit.createComposite(documentationSection);
	    GridLayout layout = new GridLayout(2, true);
	    layout.horizontalSpacing = 30;
	    documentationComposite.setLayout(layout);
	    GridDataFactory.fillDefaults().grab(true, true).applyTo(documentationComposite);
	    
		addHyperlink(toolkit, documentationComposite, "New and Noteworthy", "http://docs.jboss.org/tools/whatsnew/");
		addHyperlink(toolkit, documentationComposite, "User Forum", "http://community.jboss.org/en/tools?view=discussions");
		
		addHyperlink(toolkit, documentationComposite, "Reference", "http://docs.jboss.org/tools/latest/");
		addHyperlink(toolkit, documentationComposite, "Developer Forum", "http://community.jboss.org/en/tools/dev?view=discussions");
		
		addHyperlink(toolkit, documentationComposite, "FAQ", "http://www.jboss.org/tools/docs/faq");
		addHyperlink(toolkit, documentationComposite, "Wiki", "http://community.jboss.org/wiki/JBossTools");
		
		addHyperlink(toolkit, documentationComposite, "Screencasts", "http://docs.jboss.org/tools/movies/");
		addHyperlink(toolkit, documentationComposite, "Issue Tracker", "https://issues.jboss.org/browse/JBIDE");
		
		documentationSection.setClient(documentationComposite);
		documentationSection.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				resize(true);
			}
		});
	}
	
	public void createSettingsSection(FormToolkit toolkit, Composite parent) {
		settingsSection = createSection(toolkit, parent, "Settings", ExpandableComposite.TITLE_BAR|ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
	    settingsSection.setLayoutData(gd);
	    
		settingsComposite = toolkit.createComposite(settingsSection);
	    GridLayout layout = new GridLayout(1, true);
	    layout.horizontalSpacing = 30;
	    settingsComposite.setLayout(layout);
	    GridDataFactory.fillDefaults().grab(true, true).applyTo(settingsComposite);
		
	    Button showOnStartup = toolkit.createButton(settingsComposite, "Show on Startup", SWT.CHECK);
		showOnStartup.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		showOnStartup.setBackground(settingsComposite.getBackground());
		showOnStartup.setSelection(JBossCentralActivator.getDefault().showJBossCentralOnStartup());
		showOnStartup.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IEclipsePreferences preferences = JBossCentralActivator.getDefault().getPreferences();
				boolean showOnStartup = preferences.getBoolean(JBossCentralActivator.SHOW_JBOSS_CENTRAL_ON_STARTUP, JBossCentralActivator.SHOW_JBOSS_CENTRAL_ON_STARTUP_DEFAULT_VALUE);
				preferences.putBoolean(JBossCentralActivator.SHOW_JBOSS_CENTRAL_ON_STARTUP, !showOnStartup);
				JBossCentralActivator.getDefault().savePreferences();
			}
		
		});

		settingsSection.setClient(settingsComposite);
	}

	private void addHyperlink(FormToolkit toolkit, Composite composite, String text, final String url) {
		Hyperlink link = toolkit.createHyperlink(composite,
				text, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(link);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				JBossCentralActivator.openUrl(url, getSite().getShell());
			}
		});
	}

	@Override
	public void dispose() {
		newWizardDropDownAction.dispose();
		newWizardDropDownAction = null;
		if (refreshBlogsJobChangeListener != null) {
			RefreshBlogsJob.INSTANCE.removeJobChangeListener(refreshBlogsJobChangeListener);
			refreshBlogsJobChangeListener = null;
		}
		if (refreshNewsJobChangeListener != null) {
			RefreshNewsJob.INSTANCE.removeJobChangeListener(refreshNewsJobChangeListener);
			refreshNewsJobChangeListener = null;
		}
		if (refreshTutorialsJobChangeListener != null) {
			RefreshTutorialsJob.INSTANCE.removeJobChangeListener(refreshTutorialsJobChangeListener);
			refreshTutorialsJobChangeListener = null;
		}
		super.dispose();
	}

	public boolean showLoading(final PageBook pageBook, final Composite composite, final ScrolledComposite scrolledComposite) {
		if (pageBook.isDisposed()) {
			return false;
		}
		Display display = getDisplay();
		display.syncExec(new Runnable() {
			
			@Override
			public void run() {
				pageBook.showPage(composite);
				setBusyIndicator(composite, true);
				form.reflow(true);
				form.redraw();
				recomputeScrollComposite(scrolledComposite, pageBook);
			}
		});
		return true;
	}

	private Image getFeedsImage() {
		return JBossCentralActivator.getDefault().getImage("/icons/feedsLink.gif");
	}
	
	private void recomputeScrollComposite(ScrolledComposite composite, PageBook pageBook) {
		Rectangle r = composite.getClientArea();
		composite.setMinSize(pageBook.computeSize(r.width, SWT.DEFAULT));
	}

	public void showNote(final PageBook pageBook, final FormText noteText, final ScrolledComposite scrolledComposite) {
		if (pageBook.isDisposed()) {
			return;
		}
		Display display = getDisplay();
		display.syncExec(new Runnable() {
			
			@Override
			public void run() {
				pageBook.showPage(noteText);
				form.reflow(true);
				form.redraw();
				recomputeScrollComposite(scrolledComposite, pageBook);
			}
		});
	}
	
	private void showException(PageBook pageBook, FormText exceptionText, Exception e) {
		JBossCentralActivator.log(e);
		String message = StringEscapeUtils.escapeXml(e.getMessage());
		String text = JBossCentralActivator.FORM_START_TAG +
				"<img href=\"image\"/> " + 
				message +
				JBossCentralActivator.FORM_END_TAG;
		exceptionText.setText(text, true, false);
		Image image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		exceptionText.setImage("image", image);
		pageBook.showPage(exceptionText);
	}

	public void refreshBlogs() {
		RefreshBlogsJob job = RefreshBlogsJob.INSTANCE;
		if (job.getState() == Job.NONE) {
			if (job.getException() != null) {
				showException(blogsPageBook, blogsExceptionText,
						job.getException());
				return;
			}
			List<FeedsEntry> entries = job.getEntries();
			if (entries == null || entries.size() == 0) {
				showNote(blogsPageBook, blogsNoteText, blogsScrollComposite);
				return;
			}
			showEntries(entries, blogsComposite, blogsPageBook, blogsScrollComposite);
		}
	}
	
	public void refreshNews() {
		RefreshNewsJob job = RefreshNewsJob.INSTANCE;
		if (job.getState() == Job.NONE) {
			if (job.getException() != null) {
				showException(newsPageBook, newsExceptionText,
						job.getException());
				return;
			}
			List<FeedsEntry> entries = job.getEntries();
			if (entries == null || entries.size() == 0) {
				showNote(newsPageBook, newsNoteText, newsScrollComposite);
				return;
			}
			showEntries(entries, newsComposite, newsPageBook, newsScrollComposite);
		}
	}
	
	public void refreshTutorials() {
		RefreshTutorialsJob job = RefreshTutorialsJob.INSTANCE;
		if (job.getException() != null) {
			showException(tutorialPageBook, tutorialsExceptionText, job.getException());
			return;
		}
		Map<String, TutorialCategory> categories = job.getTutorialCategories();
		if (categories == null || categories.size() == 0) {
			showNote(tutorialPageBook, tutorialsNoteText, tutorialScrollComposite);
			return;
		}
		showTutorials(categories);
	}

	private void showTutorials(Map<String, TutorialCategory> categories) {
		disposeChildren(tutorialsComposite);
		Collection<TutorialCategory> tempCategories = categories.values();
		List<TutorialCategory> sortedCategories = new ArrayList<TutorialCategory>();
		sortedCategories.addAll(tempCategories);
		Collections.sort(sortedCategories);
		for (final TutorialCategory category:sortedCategories) {
			int style = ExpandableComposite.TITLE_BAR|ExpandableComposite.TWISTIE;
			if (expandedCategories.contains(category)) {
				style|=ExpandableComposite.EXPANDED;
			}
			final ExpandableComposite categoryComposite = toolkit.createExpandableComposite(tutorialsComposite, 
					style);
			categoryComposite.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
			categoryComposite.setText(category.getName());
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
			categoryComposite.setLayoutData(gd);
			categoryComposite.setLayout(new GridLayout());
			final Composite composite = toolkit.createComposite(categoryComposite);
			gd = new GridData(SWT.FILL, SWT.FILL, false, false);
			composite.setLayoutData(gd);
			composite.setLayout(new GridLayout(1, false));
			
			categoryComposite.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					if (e.getState()) {
						expandedCategories.add(category);
					} else {
						expandedCategories.remove(category);
					}
					resize(true);
				}
			});

			for (final Tutorial tutorial:category.getTutorials()) {
				Project project = tutorial.getProjectExamples();
				if (project == null) {
					continue;
				}
				FormText tutorialText = toolkit.createFormText(composite, true);
				configureTutorialText(tutorialText, tutorial);
				hookTooltip(tutorialText, tutorial);
			}
			categoryComposite.setClient(composite);
		}
		
		tutorialPageBook.showPage(tutorialsComposite);
		tutorialPageBook.layout(true, true);
		form.reflow(true);
		//form.redraw();
		resize();
		//recomputeScrollComposite(tutorialScrollComposite, tutorialPageBook);
	}

	private void hookTooltip(FormText tutorialText, Tutorial tutorial) {
		final String description = JBossCentralActivator.getDefault().getDescription(tutorial);
		if (description != null && !description.isEmpty()) {
			ToolTip toolTip = new DescriptionToolTip(tutorialText, description);
			toolTip.activate();
		}
	}

	protected void configureTutorialText(FormText tutorialText, final Tutorial tutorial) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(JBossCentralActivator.FORM_START_TAG);
		//boolean haveImage = tutorial.getIconPath() != null && JBossCentralActivator.getDefault().getImage(tutorial.getIconPath()) != null;
		//if (haveImage) {
		//	buffer.append("<img href=\"image\"/> ");
		//}
		//if (project.getUnsatisfiedFixes().size() > 0) {
		buffer.append("<img href=\"image\"/> ");
		//}
		buffer.append("<a href=\"link\">");
		buffer.append(tutorial.getName());
		buffer.append("</a> ");
		
		buffer.append(JBossCentralActivator.FORM_END_TAG);
		String text = buffer.toString();
		tutorialText.setText(text , true, false);
		Image image;
		Project project = tutorial.getProjectExamples();
		List<ProjectFix> fixes = project.getFixes();
		List<ProjectFix> unsatisfiedFixes = new ArrayList<ProjectFix>();
		project.setUnsatisfiedFixes(unsatisfiedFixes);
		for (ProjectFix fix:fixes) {
			if (!ProjectExamplesActivator.canFix(project, fix)) {
				unsatisfiedFixes.add(fix);
			}
		}
		if (project.getUnsatisfiedFixes().size() > 0) {
			image = JBossCentralActivator.getDefault().getImage("/icons/nwarning.gif");
		} else {
			image = JBossCentralActivator.getDefault().getImage(tutorial.getIconPath());
		}
		tutorialText.setImage("image", image);
		tutorialText.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object object = e.data;
				if (object instanceof String) {
					ProjectExamplesDialog dialog = new ProjectExamplesDialog(getSite().getShell(), tutorial);
					dialog.open();
					refreshTutorials();
				}
			}
			
		});

	}

	private void disposeChildren(Composite composite) {
		Control[] children = composite.getChildren();
		for (Control child:children) {
			if (child instanceof Composite) {
				disposeChildren((Composite) child);
				child.dispose();
			} else {
				child.dispose();
			}
		}
	}

	private void showEntries(List<FeedsEntry> entries, Composite composite, PageBook pageBook, ScrolledComposite scrollable) {
		int i = 0;
		disposeChildren(composite);
		
		for (final FeedsEntry entry:entries) {
			if (i++ > JBossCentralActivator.MAX_FEEDS) {
				return;
			}
			String text = entry.getFormString();
			FormText formText = toolkit.createFormText(composite, true);
			TableWrapData td = new TableWrapData();
			td.indent = 2;
			//Point size = scrollable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			//td.maxWidth = size.x - 2;
			formText.setLayoutData(td);
			try {
				// to avoid illegal argumentexception on formtext fields.
				// we replace the HTML entity &nbsp; with the standard xml version 
				// TODO: should probably be done earlier on but couldn't find where.
				text = text.replaceAll("&nbsp;", "&#160;"); 
				
				formText.setText(text, true, true);
			} catch(IllegalArgumentException se) {
				formText.dispose();
				formText = toolkit.createFormText(composite, false);
				formText.setLayoutData(td);
				try {
					formText.setText("Problem rendering entry - " + StringEscapeUtils.unescapeHtml(se.getMessage()), false, false);
				} catch (Exception e1) {
					JBossCentralActivator.log(se);
				}
				continue;
			}
				//Display display = Display.getCurrent();
			//formText.setFont(getLinkFont(display));
			formText.setFont("default", JFaceResources.getDefaultFont());
			formText.setFont("date", JFaceResources.getDefaultFont());
			formText.setColor("date", JFaceColors.getHyperlinkText(getDisplay()));
			//formText.setForeground(JFaceColors.getHyperlinkText(getDisplay()));
			formText.setFont("description", JFaceResources.getDefaultFont());
			//Font boldFont = getAuthorFont(display);
			//formText.setFont("author", boldFont);
			formText.setColor("author", JFaceColors.getHyperlinkText(getDisplay()));
			formText.setImage("image", getFeedsImage());
			if (JBossCentralActivator.isInternalWebBrowserAvailable() && entry.getDescription() != null && !entry.getDescription().isEmpty()) {
				ToolTip toolTip = new FeedsToolTip(formText, entry.getDate() + " " + entry.getDescription());
				toolTip.activate();
			}
			formText.addHyperlinkListener(new HyperlinkAdapter() {

				@Override
				public void linkActivated(HyperlinkEvent e) {
					Object link = e.data;
					if (link instanceof String) {
						Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
						JBossCentralActivator.openUrl((String) link, shell);
					}
				}
			});
			
		}
		pageBook.showPage(composite);
		pageBook.layout(true, true);
		form.reflow(true);
		//form.redraw();
		recomputeScrollComposite(scrollable, pageBook);
		resize();
	}

	private void resize() {
		resize(false);
	}
	
	protected void resize(boolean force) {
		Point size;
		//if (Platform.OS_MACOSX.equals(Platform.getOS())) {
			size = form.getSize();
			size.y = form.getBody().getSize().y;
		//} else {
		//	size = form.getBody().getSize();
		//}
		if (!force && size.equals(oldSize)) {
			return;
		}
		oldSize = size;
		GridData gd;
		int widthHint = size.x/2 - 40;
		gd = (GridData) newsSection.getLayoutData();
		if (newsSection.isExpanded()) {
			if (blogsSection.isExpanded()) {
				gd.heightHint = size.y/2 - 20;
			} else {
				gd.heightHint = size.y - 40;
			}
		} else {
			gd.heightHint = 20;
		}
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		
		gd = (GridData) blogsSection.getLayoutData();
		if (blogsSection.isExpanded()) {
			if (newsSection.isExpanded()) {
				gd.heightHint = size.y/2 - 20;
			} else {
				gd.heightHint = size.y - 40;
			}
		} else {
			gd.heightHint = 20;
		}
		
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		
		gd = (GridData) tutorialsSection.getLayoutData();
		//gridData.heightHint = size.y - 40;
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		tutorialPageBook.pack();
		//computedSize = tutorialPageBook.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		//tutorialsSection.setSize(widthHint, computedSize.y);
		
		gd = (GridData) documentationSection.getLayoutData();
		//gridData.heightHint = size.y - 40;
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		//computedSize = documentationSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		//documentationSection.setSize(widthHint, computedSize.y);
		
		gd = (GridData) settingsSection.getLayoutData();
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		//computedSize = settingsSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		//settingsSection.setSize(widthHint, computedSize.y);
		
		gd = (GridData) projectsSection.getLayoutData();
		//gridData.heightHint = size.y - 40;
		gd.widthHint = widthHint;
		gd.grabExcessVerticalSpace = false;
		//computedSize = projectsSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		//projectsSection.setSize(widthHint, computedSize.y);
		
		blogsScrollComposite.setMinSize(widthHint, size.y - 55);
		newsScrollComposite.setMinSize(widthHint, size.y - 55);
		
		Point computedSize = tutorialPageBook.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		int y = computedSize.y;
		if (y > 200) {
			y = 200;
		}
		
		tutorialScrollComposite.setMinSize(widthHint, y);
		//newsPageBook.layout(true, true);
		//blogsPageBook.layout(true, true);
		recomputeScrollComposite(blogsScrollComposite, blogsPageBook);
		recomputeScrollComposite(newsScrollComposite, newsPageBook);
		//form.redraw();
		form.layout(true, true);
		form.reflow(true);
		
	}

	private class RefreshBlogsJobChangeListener implements IJobChangeListener {

		@Override
		public void aboutToRun(IJobChangeEvent event) {
			
		}

		@Override
		public void awake(IJobChangeEvent event) {
			
		}

		@Override
		public void done(IJobChangeEvent event) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					setBusyIndicator(blogsLoadingComposite, false);
					refreshBlogs();
				}
			});
			
		}

		@Override
		public void running(IJobChangeEvent event) {
			
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			RefreshBlogsJob.INSTANCE.setException(null);
			showLoading(blogsPageBook, blogsLoadingComposite, blogsScrollComposite);
		}

		@Override
		public void sleeping(IJobChangeEvent event) {
			
		}
		
	}
	
	private class RefreshTutorialsJobChangeListener implements IJobChangeListener {

		@Override
		public void aboutToRun(IJobChangeEvent event) {
			
		}

		@Override
		public void awake(IJobChangeEvent event) {
			
		}

		@Override
		public void done(IJobChangeEvent event) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					setBusyIndicator(tutorialsLoadingComposite, false);
					refreshTutorials();
				}
			});
			
		}

		@Override
		public void running(IJobChangeEvent event) {
			
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			RefreshTutorialsJob.INSTANCE.setException(null);
			showLoading(tutorialPageBook, tutorialsLoadingComposite, tutorialScrollComposite);
		}

		@Override
		public void sleeping(IJobChangeEvent event) {
			
		}
		
	}
	
	private class RefreshNewsJobChangeListener implements IJobChangeListener {

		@Override
		public void aboutToRun(IJobChangeEvent event) {
			
		}

		@Override
		public void awake(IJobChangeEvent event) {
			
		}

		@Override
		public void done(IJobChangeEvent event) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					setBusyIndicator(newsLoadingComposite, false);
					refreshNews();
				}
			});
			
		}

		@Override
		public void running(IJobChangeEvent event) {
			
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			RefreshNewsJob.INSTANCE.setException(null);
			showLoading(newsPageBook, newsLoadingComposite, newsScrollComposite);
		}

		@Override
		public void sleeping(IJobChangeEvent event) {
			
		}
		
	}

}
