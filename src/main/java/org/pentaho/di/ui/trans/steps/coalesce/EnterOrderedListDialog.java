package org.pentaho.di.ui.trans.steps.coalesce;

import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
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
import org.pentaho.di.trans.steps.coalesce.CoalesceMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.util.ImageUtil;

/**
 * This dialogs allows you to select a ordered number of items from a list of
 * strings.
 *
 * @author Nicolas ADMENT
 * @since 29-09-2018
 */
public class EnterOrderedListDialog extends Dialog {
	private static Class<?> PKG = CoalesceMeta.class;

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

	public EnterOrderedListDialog(Shell parent, int style, String[] input) {
		super(parent, style);

		this.props = PropsUI.getInstance();
		this.input = input;
		this.retval = null;
		this.opened = false;
	}

	public String[] open() {

		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		props.setLook(shell);
		shell.setImage(GUIResource.getInstance().getImageSpoon());
		shell.setText(BaseMessages.getString(PKG, "EnterOrderedListDialog.Title"));
		shell.setLayout(new FormLayout());

		// FIXEME: use GUIResource when Up and Down images exists
		// imgUp = GUIResource.getInstance().getImage("up.png",
		// getClass().getClassLoader(), 16, 16);
		// imgDown = GUIResource.getInstance().getImage("down.png",
		// getClass().getClassLoader(), 16, 16);
		imgUp = ImageUtil.getImage(shell.getDisplay(), getClass().getClassLoader(), "up.png");
		imgDown = ImageUtil.getImage(shell.getDisplay(), getClass().getClassLoader(), "down.png");

		// *******************************************************************
		// Top & Bottom regions.
		// *******************************************************************
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

		// *******************************************************************
		// LEFT PANE
		// *******************************************************************

		Composite leftPane = new Composite(top, SWT.NONE);
		leftPane.setLayout(new FormLayout());
		leftPane.setLayoutData(new FormDataBuilder().top().left().bottom(100, 0).right(50, -36).result());
		props.setLook(leftPane);

		// Source list to the left...
		Label lblListSource = new Label(leftPane, SWT.NONE);
		lblListSource.setText(BaseMessages.getString(PKG, "EnterOrderedListDialog.AvailableItems.Label"));
		lblListSource.setLayoutData(new FormDataBuilder().top().left().result());
		props.setLook(lblListSource);

		lstSource = new List(leftPane, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstSource.setLayoutData(new FormDataBuilder().top(lblListSource, ConstUI.SMALL_MARGIN).left().bottom(100, 0)
				.right(100, 0).result());
		props.setLook(lstSource);

		// *******************************************************************
		// MIDDLE
		// *******************************************************************

		Composite middlePane = new Composite(top, SWT.NONE);
		middlePane.setLayout(new FormLayout());
		middlePane.setLayoutData(new FormDataBuilder().top().left(leftPane, -36).bottom(100, 0).right(50, 36).result());
		props.setLook(middlePane);

		Label label = new Label(middlePane, SWT.NONE);
		// label.setText("TEST");
		label.setLayoutData(new FormDataBuilder().top().left().result());
		props.setLook(label);

		Composite gButtonGroup = new Composite(middlePane, SWT.NONE);
		RowLayout layout = new RowLayout();
		layout.wrap = false;
		layout.type = SWT.VERTICAL;
		layout.marginTop = 0;
		layout.marginLeft = LARGE_MARGIN;
		layout.marginRight = LARGE_MARGIN;

		gButtonGroup.setLayout(layout);
		gButtonGroup.setLayoutData(
				new FormDataBuilder().top(label, ConstUI.SMALL_MARGIN).left().bottom(100, 0).right(100, 0).result());
		props.setLook(gButtonGroup);

		btnAdd = new Button(gButtonGroup, SWT.PUSH);
		btnAdd.setImage(GUIResource.getInstance().getImageAddSingle());
		btnAdd.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.AddOne.Tooltip"));
		btnAdd.setLayoutData(new RowData(32, 32));

		btnAddAll = new Button(gButtonGroup, SWT.PUSH);
		btnAddAll.setImage(GUIResource.getInstance().getImageAddAll());
		btnAddAll.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.AddAll.Tooltip"));
		btnAddAll.setLayoutData(new RowData(32, 32));

		btnRemove = new Button(gButtonGroup, SWT.PUSH);
		btnRemove.setImage(GUIResource.getInstance().getImageRemoveSingle());
		btnRemove.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.RemoveOne.Tooltip"));
		btnRemove.setLayoutData(new RowData(32, 32));

		btnRemoveAll = new Button(gButtonGroup, SWT.PUSH);
		btnRemoveAll.setImage(GUIResource.getInstance().getImageRemoveAll());
		btnRemoveAll.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.RemoveAll.Tooltip"));
		btnRemoveAll.setLayoutData(new RowData(32, 32));

		btnUp = new Button(gButtonGroup, SWT.PUSH);
		btnUp.setImage(imgUp);
		btnUp.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.Up.Tooltip"));
		btnUp.setLayoutData(new RowData(32, 32));

		btnDown = new Button(gButtonGroup, SWT.PUSH);
		btnDown.setImage(imgDown);
		btnDown.setToolTipText(BaseMessages.getString(PKG, "EnterOrderedListDialog.Down.Tooltip"));
		btnDown.setLayoutData(new RowData(32, 32));

		/* Compute the offset */
		btnAddAll.pack();
		int offset = btnAddAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).x / 2;

		FormData fdButtonGroup = new FormData();
		btnAddAll.pack(); // get a size
		fdButtonGroup.left = new FormAttachment(50, -(btnAddAll.getSize().x / 2) - 5);
		fdButtonGroup.top = new FormAttachment(50, -offset);
		gButtonGroup.setLayoutData(new FormDataBuilder().top(label, ConstUI.SMALL_MARGIN).left(50, -offset).result());

		// *******************************************************************
		// RIGHT
		// *******************************************************************
		Composite rightPane = new Composite(top, SWT.NONE);
		rightPane.setLayout(new FormLayout());
		rightPane.setLayoutData(new FormDataBuilder().top().left(middlePane, 0).bottom(100, 0).right(100, 0).result());
		props.setLook(rightPane);

		Label lblListTarget = new Label(rightPane, SWT.NONE);
		lblListTarget.setText(BaseMessages.getString(PKG, "EnterOrderedListDialog.Selection.Label"));
		lblListTarget.setLayoutData(new FormDataBuilder().top().left().result());
		props.setLook(lblListTarget);

		lstTarget = new List(rightPane, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstTarget.setLayoutData(new FormDataBuilder().top(lblListTarget, ConstUI.SMALL_MARGIN).left().bottom(100, 0)
				.right(100, 0).result());
		props.setLook(lstTarget);

		// *******************************************************************
		// THE BOTTOM BUTTONS...
		// *******************************************************************

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
			public void dragOver(DropTargetEvent event) {
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
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.CR) {
					addToSelection(lstSource.getSelection());
				}
			}
		});
		lstTarget.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.CR) {
					removeFromSelection(lstTarget.getSelection());
				}
			}
		});

		// Double click adds to destination.
		lstSource.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateButton();
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				addToSelection(lstSource.getSelection());
			}
		});
		// Double click adds to source
		lstTarget.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateButton();
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				removeFromSelection(lstTarget.getSelection());
			}
		});

		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				addToSelection(lstSource.getSelection());
			}
		});

		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				removeFromSelection(lstTarget.getSelection());
			}
		});

		btnAddAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				addToSelection(lstSource.getItems());
			}
		});

		btnRemoveAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				removeFromSelection(lstTarget.getItems());
			}
		});

		btnUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				upToSelection(lstTarget.getSelection());
			}
		});

		btnDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				downToSelection(lstTarget.getSelection());
			}
		});

		opened = true;
		update();

		BaseStepDialog.setSize(shell);

		shell.open();
		Display display = shell.getDisplay();
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
		for (String element : input) {
			// Not selected: show in source only!
			if (selection.indexOf(element) < 0) {
				lstSource.add(element);
			}
		}

		String[] currentSelection = lstTarget.getSelection();
		lstTarget.removeAll();
		for (String element : selection) {
			lstTarget.add(element);
		}
		lstTarget.setSelection(currentSelection);

		this.updateButton();
	}

	protected void updateButton() {
		// Update button
		int index = lstTarget.getSelectionIndex();

		btnAdd.setEnabled(lstSource.getSelectionIndex() >= 0);
		btnAddAll.setEnabled(lstSource.getItemCount() > 0);

		btnRemove.setEnabled(index >= 0);
		btnRemoveAll.setEnabled(selection.size() > 0);

		btnUp.setEnabled(selection.size() > 1 && index > 0);
		btnDown.setEnabled(selection.size() > 1 && index >= 0 && index < selection.size() - 1);
	}

	public void addToSelection(String... elements) {
		for (String element : elements) {

			if (Utils.isEmpty(element))
				continue;
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

		if (imgUp != null)
			imgUp.dispose();
		if (imgDown != null)
			imgDown.dispose();

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
				selection.add(index - 1, element);
			} else
				break;
		}
		update();
	}

	/**
	 * Moves the currently selected item down.
	 */
	private void downToSelection(String... elements) {

		for (int i = elements.length - 1; i >= 0; i--) {
			String element = elements[i];
			int index = selection.indexOf(element);
			if (index < selection.size() - 1) {
				selection.remove(index);
				selection.add(index + 1, element);
			} else
				break;
		}
		update();
	}
}
