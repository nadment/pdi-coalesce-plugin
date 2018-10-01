package org.pentaho.di.ui.trans.steps.coalesce;

import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * This dialogs allows you to select a ordered number of items from a list of
 * strings.
 *
 * @author Matt
 * @since 21-10-2004
 */
public class SelectFieldListdialog extends Dialog {
	private static Class<?> PKG = SelectFieldListdialog.class;

	public static final int LARGE_MARGIN = 15;

	private PropsUI props;

	private String[] input;
	private String[] retval;

	private Stack<String> selection = new Stack<>();
	
	private Shell shell;
	private List lstSource, lstTarget;
	private Button btnOK, btnCancel;
	private Button btnAdd, btnAddAll, btnRemoveAll, btnRemove, btnUp, btnDown;

	private Image imgUp, imgDown;

	private boolean opened;

	public SelectFieldListdialog(Shell parent, int style, String[] input) {
		super(parent, style);
		this.props = PropsUI.getInstance();

		this.input = input;
		this.retval = null;

		opened = false;
	}

	public String[] open() {
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		props.setLook(shell);
		shell.setImage(GUIResource.getInstance().getImageSpoon());
		shell.setText(BaseMessages.getString(PKG, "EnterListDialog.Title"));
		shell.setLayout(new FormLayout());

		imgUp = GUIResource.getInstance().getImage("up.png", getClass().getClassLoader(), 16, 16);
		imgDown = GUIResource.getInstance().getImage("down.png", getClass().getClassLoader(), 16, 16);

		// //////////////////////////////////////////////////
		// Top & Bottom regions.
		// //////////////////////////////////////////////////
		Composite top = new Composite(shell, SWT.NONE);
		FormLayout topLayout = new FormLayout();
		topLayout.marginHeight = LARGE_MARGIN;
		topLayout.marginWidth = LARGE_MARGIN;
		top.setLayout(topLayout);
		top.setLayoutData(new FormDataBuilder().top().bottom(100, -50).left().right(100, 0).result());
		props.setLook(top);

		Composite bottom = new Composite(shell, SWT.NONE);
		FormLayout bottomLayout = new FormLayout();
		bottomLayout.marginHeight = LARGE_MARGIN;
		bottomLayout.marginWidth = LARGE_MARGIN;
		bottom.setLayout(bottomLayout);
		bottom.setLayoutData(new FormDataBuilder().top(top, 0).bottom().right().result());
		props.setLook(bottom);

		// //////////////////////////////////////////////////
		// Sashform
		// //////////////////////////////////////////////////

		SashForm sashform = new SashForm(top, SWT.HORIZONTAL);
		sashform.setLayout(new FormLayout());
		FormData fdSashform = new FormData();
		fdSashform.left = new FormAttachment(0, 0);
		fdSashform.top = new FormAttachment(0, 0);
		fdSashform.right = new FormAttachment(100, 0);
		fdSashform.bottom = new FormAttachment(100, 0);
		sashform.setLayoutData(fdSashform);
		props.setLook(sashform);


		// ////////////////////////
		// / LEFT
		// ////////////////////////
		Composite leftsplit = new Composite(sashform, SWT.NONE);
		leftsplit.setLayout(new FormLayout());
		FormData fdLeftsplit = new FormData();
		fdLeftsplit.left = new FormAttachment(0, 0);
		fdLeftsplit.top = new FormAttachment(0, 0);
		fdLeftsplit.right = new FormAttachment(100, 0);
		fdLeftsplit.bottom = new FormAttachment(100, 0);
		leftsplit.setLayoutData(fdLeftsplit);
		props.setLook(leftsplit);

		// Source list to the left...
		Label wlListSource = new Label(leftsplit, SWT.NONE);
		wlListSource.setText(BaseMessages.getString(PKG, "EnterListDialog.AvailableItems.Label"));
		props.setLook(wlListSource);

		FormData fdlListSource = new FormData();
		fdlListSource.left = new FormAttachment(0, 0);
		fdlListSource.top = new FormAttachment(0, 0);
		wlListSource.setLayoutData(fdlListSource);

		lstSource = new List(leftsplit, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		props.setLook(lstSource);

		FormData fdListSource = new FormData();
		fdListSource.left = new FormAttachment(0, 0);
		fdListSource.top = new FormAttachment(wlListSource, 0);
		fdListSource.right = new FormAttachment(100, 0);
		fdListSource.bottom = new FormAttachment(100, 0);
		lstSource.setLayoutData(fdListSource);

		// /////////////////////////
		// MIDDLE
		// /////////////////////////

		Composite compmiddle = new Composite(sashform, SWT.NONE);
		compmiddle.setLayout(new FormLayout());
		FormData fdCompMiddle = new FormData();
		fdCompMiddle.left = new FormAttachment(0, 0);
		fdCompMiddle.top = new FormAttachment(0, 0);
		fdCompMiddle.right = new FormAttachment(100, 0);
		fdCompMiddle.bottom = new FormAttachment(100, 0);
		compmiddle.setLayoutData(fdCompMiddle);
		props.setLook(compmiddle);

		Composite gButtonGroup = new Composite(compmiddle, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 10;
		props.setLook(gButtonGroup);

		gButtonGroup.setLayout(gridLayout);

		btnAdd = new Button(gButtonGroup, SWT.PUSH);
		btnAdd.setImage(GUIResource.getInstance().getImageAddSingle());
		btnAdd.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.AddOne.Tooltip"));
		btnAdd.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnAddAll = new Button(gButtonGroup, SWT.PUSH);
		btnAddAll.setImage(GUIResource.getInstance().getImageAddAll());
		btnAddAll.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.AddAll.Tooltip"));
		btnAddAll.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnRemove = new Button(gButtonGroup, SWT.PUSH);
		btnRemove.setImage(GUIResource.getInstance().getImageRemoveSingle());
		btnRemove.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.RemoveOne.Tooltip"));
		btnRemove.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnRemoveAll = new Button(gButtonGroup, SWT.PUSH);
		btnRemoveAll.setImage(GUIResource.getInstance().getImageRemoveAll());
		btnRemoveAll.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.RemoveAll.Tooltip"));
		btnRemoveAll.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnUp = new Button(gButtonGroup, SWT.PUSH);
		// btnUp.setText(" Up ");
		btnUp.setImage(imgUp);

		btnUp.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.Up.Tooltip"));
		btnUp.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnDown = new Button(gButtonGroup, SWT.PUSH);
		// btnDown.setText(" Down ");
		btnDown.setImage(imgDown);
		btnDown.setToolTipText(BaseMessages.getString(PKG, "EnterListDialog.Down.Tooltip"));
		btnDown.setLayoutData(new GridData(GridData.FILL_BOTH));

		FormData fdButtonGroup = new FormData();
		btnAddAll.pack(); // get a size
		fdButtonGroup.left = new FormAttachment(50, -(btnAddAll.getSize().x / 2) - 5);
		fdButtonGroup.top = new FormAttachment(0, 0);
		gButtonGroup.setBackground(shell.getBackground()); // the default looks
															// ugly
		gButtonGroup.setLayoutData(fdButtonGroup);

		// ///////////////////////////////
		// RIGHT
		// ///////////////////////////////
		Composite rightsplit = new Composite(sashform, SWT.NONE);
		rightsplit.setLayout(new FormLayout());
		FormData fdRightsplit = new FormData();
		fdRightsplit.left = new FormAttachment(0, 0);
		fdRightsplit.top = new FormAttachment(0, 0);
		fdRightsplit.right = new FormAttachment(100, 0);
		fdRightsplit.bottom = new FormAttachment(100, 0);
		rightsplit.setLayoutData(fdRightsplit);
		props.setLook(rightsplit);

		Label wlListDest = new Label(rightsplit, SWT.NONE);
		wlListDest.setText(BaseMessages.getString(PKG, "EnterListDialog.Selection.Label"));
		props.setLook(wlListDest);
		FormData fdlListDest = new FormData();
		fdlListDest.left = new FormAttachment(0, 0);
		fdlListDest.top = new FormAttachment(0, 0);
		wlListDest.setLayoutData(fdlListDest);

		lstTarget = new List(rightsplit, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		props.setLook(lstTarget);

		FormData fdListDest = new FormData();
		fdListDest.left = new FormAttachment(0, 0);
		fdListDest.top = new FormAttachment(wlListDest, 0);
		fdListDest.right = new FormAttachment(100, 0);
		fdListDest.bottom = new FormAttachment(100, 0);
		lstTarget.setLayoutData(fdListDest);

		sashform.setWeights(new int[] { 45, 10, 45 });

		// //////////////////////////////////////////////////////////////
		// THE BOTTOM BUTTONS...
		// //////////////////////////////////////////////////////////////

		btnCancel = new Button(bottom, SWT.PUSH);
		btnCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
		btnCancel.setLayoutData(new FormDataBuilder().bottom().right().result());
		btnCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				dispose();
			}
		});

		btnOK = new Button(bottom, SWT.PUSH);
		btnOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
		btnOK.setLayoutData(new FormDataBuilder().bottom().right(btnCancel, -ConstUI.SMALL_MARGIN).result());
		btnOK.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				onOkPressed();
			}
		});

		// Drag & Drop for steps
		Transfer[] ttypes = new Transfer[] { TextTransfer.getInstance() };

		DragSource ddSource = new DragSource(lstSource, DND.DROP_MOVE | DND.DROP_COPY);
		ddSource.setTransfer(ttypes);
		ddSource.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
			}

			public void dragSetData(DragSourceEvent event) {
				String[] ti = lstSource.getSelection();
				String data = new String();
				for (int i = 0; i < ti.length; i++) {
					data += ti[i] + Const.CR;
				}
				event.data = data;
			}

			public void dragFinished(DragSourceEvent event) {
			}
		});
		DropTarget ddTarget = new DropTarget(lstTarget, DND.DROP_MOVE | DND.DROP_COPY);
		ddTarget.setTransfer(ttypes);
		ddTarget.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
			}

			public void dragLeave(DropTargetEvent event) {
			}

			public void dragOperationChanged(DropTargetEvent event) {
			}

	
			   @Override
		        public void dragOver(DropTargetEvent event)
		        {
		            event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_AFTER;
		        }

			public void drop(DropTargetEvent event) {
				if (event.data == null) { // no data to copy, indicate failure
											// in event.detail
					event.detail = DND.DROP_NONE;
					return;
				}
				StringTokenizer strtok = new StringTokenizer((String) event.data, Const.CR);
				while (strtok.hasMoreTokens()) {
					String source = strtok.nextToken();
					addToSelection(source);
				}
			}

			public void dropAccept(DropTargetEvent event) {
			}
		});

		lstSource.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					addToSelection(lstSource.getSelection());
				}
			}
		});
		lstTarget.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					removeFromSelection(lstTarget.getSelection());
				}
			}
		});

		// Double click adds to destination.
		lstSource.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				addToSelection(lstSource.getSelection());
			}
		});
		// Double click adds to source
		lstTarget.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				removeFromSelection(lstTarget.getSelection());
			}
		});

		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addToSelection(lstSource.getSelection());
			}
		});

		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeFromSelection(lstTarget.getSelection());
			}
		});

		btnAddAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addToSelection(lstSource.getItems());
			}
		});

		btnRemoveAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeFromSelection(lstTarget.getItems());
			}
		});

		btnUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				upToSelection(lstTarget.getSelection());
			}
		});

		btnDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				downToSelection(lstTarget.getSelection());
			}
		});

		opened = true;
		update();

		BaseStepDialog.setSize(shell);

		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		return retval;
	}

	protected void update() {
		if (!opened) {
			return;
		}

		lstSource.removeAll();
		for (String element: input) {					
			// Not selected: show in source only!
			if (selection.indexOf(element)<0) {
				lstSource.add(element);
			} 			
		}

		String[] currentSelection = lstTarget.getSelection();
		lstTarget.removeAll();
		for (String element: selection) {
			lstTarget.add(element);
		}
		lstTarget.setSelection(currentSelection);
	
		
		// Update button
		int index = lstTarget.getSelectionIndex();

		btnRemove.setEnabled(index >= 0);
	//	btnUp.setEnabled(selection.size() > 1 && index > 0);
	//	btnDown.setEnabled(selection.size() > 1 && index >= 0 && index < selection.size() - 1);
	}

	public void addToSelection(String... elements) {
		for (String element : elements) {
			
			if ( Utils.isEmpty(element)) continue;
			selection.push(element);
		}

		update();
	}


	public void removeFromSelection(String... elements) {
		for (String element : elements) {
			selection.remove(element);
		}

		update();
	}

	public void dispose() {
		WindowProperty winprop = new WindowProperty(shell);
		
		props.setScreen(winprop);
		shell.dispose();
	}

	public void onOkPressed() {
		retval = lstTarget.getItems();
		dispose();
	}

	/**
	 * Moves the currently selected item up.
	 */
	private void upToSelection(String... elements) {

		for (String element : elements) {
			int index = selection.indexOf(element);
			if (index > 0) {
				selection.remove(index);
				selection.add(index-1, element);
			}
			else break;
		}
		update();
	}

	/**
	 * Moves the currently selected item down.
	 */
	private void downToSelection(String... elements) {

		for (int i = elements.length-1; i>=0 ; i--) {
			String element = elements[i];
			int index = selection.indexOf(element);
			if (index < selection.size()-1 ) {
				selection.remove(index);
				selection.add(index+1, element);
			}
			else break;
		}
		update();
	}

}
