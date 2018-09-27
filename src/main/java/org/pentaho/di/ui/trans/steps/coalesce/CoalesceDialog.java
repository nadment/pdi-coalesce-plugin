/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.trans.steps.coalesce;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.coalesce.Coalesce;
import org.pentaho.di.trans.steps.coalesce.CoalesceMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.dialog.EnterListDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVarButtonRenderCallback;
import org.pentaho.di.ui.dialog.AbstractStepDialog;

public class CoalesceDialog extends AbstractStepDialog<CoalesceMeta> {

	private static Class<?> PKG = CoalesceMeta.class; // for i18n purposes

	private Button btnEmptyStrings;

	private TableView tblFields;

	/**
	 * Constructor that saves incoming meta object to a local variable, so it
	 * can conveniently read and write settings from/to it.
	 *
	 * @param parent
	 *            the SWT shell to open the dialog in
	 * @param in
	 *            the meta object holding the step's settings
	 * @param transMeta
	 *            transformation description
	 * @param sName
	 *            the step name
	 */
	public CoalesceDialog(Shell parent, Object in, TransMeta transMeta, String sName) {
		super(parent, in, transMeta, sName);

		setText(BaseMessages.getString(PKG, "CoalesceDialog.Shell.Title"));
	}

	@Override
	protected void loadMeta(final CoalesceMeta meta) {

		btnEmptyStrings.setSelection(meta.isTreatEmptyStringsAsNulls());

		List<Coalesce> coalesces = meta.getCoalesces();
		for (int i = 0; i < coalesces.size(); i++) {

			Coalesce coalesce = coalesces.get(i);
			TableItem item = tblFields.getTable().getItem(i);
			item.setText(1, Const.NVL(coalesce.getName(), ""));
			item.setText(2, ValueMetaBase.getTypeDesc(coalesce.getType()));
			item.setText(3, getStringFromBoolean(coalesce.isRemoveInputFields()));
			for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
				item.setText(4 + j, Const.NVL(coalesce.getInputField(j), ""));
			}
		}

		tblFields.setRowNums();
		tblFields.optWidth(true);
	}

	@Override
	public Point getMinimumSize() {
		return new Point(600, 300);
	}

	@Override
	protected void saveMeta(final CoalesceMeta meta) {

		// save step name
		stepname = wStepname.getText();

		meta.setEmptyStringsAsNulls(btnEmptyStrings.getSelection());

		int count = tblFields.nrNonEmpty();

		List<Coalesce> coalesces = new ArrayList<>(count);

		// CHECKSTYLE:Indentation:OFF
		for (int i = 0; i < count; i++) {
			TableItem item = tblFields.getNonEmpty(i);

			Coalesce coalesce = new Coalesce();
			coalesce.setName(item.getText(1));

			String typeValueText = item.getText(2);
			coalesce.setType(
					Utils.isEmpty(typeValueText) ? ValueMetaInterface.TYPE_NONE : ValueMetaBase.getType(typeValueText));

			String isRemoveText = item.getText(3);
			coalesce.setRemoveInputFields(getBooleanFromString(isRemoveText));

			int noNonEmptyFields = 0;
			for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
				coalesce.setInputField(j, item.getText(4 + j));
				if (!Utils.isEmpty(coalesce.getInputField(j))) {
					noNonEmptyFields++;
				}
			}

			if (noNonEmptyFields < 2) {
				MessageDialogWithToggle md = new MessageDialogWithToggle(shell,
						BaseMessages.getString(PKG, "CoalesceDialog.Validations.DialogTitle"), null,
						BaseMessages.getString(PKG, "CoalesceDialog.Validations.DialogMessage", Const.CR, Const.CR),
						MessageDialog.WARNING,
						new String[] { BaseMessages.getString(PKG, "CoalesceDialog.Validations.Option.1") }, 0,
						BaseMessages.getString(PKG, "CoalesceDialog.Validations.Option.2"), false);
				Window.setDefaultImage(GUIResource.getInstance().getImageSpoon());
				md.open();
			}

			coalesces.add(coalesce);
		}

		meta.setCoalesces(coalesces);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		// Widget Spaces and Nulls
		btnEmptyStrings = new Button(parent, SWT.CHECK);
		btnEmptyStrings.setText(BaseMessages.getString(PKG, "CoalesceDialog.Shell.EmptyStringsAsNulls"));
		btnEmptyStrings.setLayoutData(new FormDataBuilder().left().top().result());
		btnEmptyStrings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				baseStepMeta.setChanged();
			}
		});
		props.setLook(btnEmptyStrings);

		CoalesceMeta meta = this.getStepMeta();

		Label wlFields = new Label(parent, SWT.NONE);
		wlFields.setText(BaseMessages.getString(PKG, "CoalesceDialog.Fields.Label"));
		wlFields.setLayoutData(new FormDataBuilder().top(btnEmptyStrings, 2 * Const.MARGIN).fullWidth().result());
		props.setLook(wlFields);

		ColumnInfo[] columns = new ColumnInfo[3 + Coalesce.MAX_INPUT_FIELD];
		columns[0] = new ColumnInfo(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.OutField"),
				ColumnInfo.COLUMN_TYPE_TEXT, false);
		columns[0].setUsingVariables(true);

		columns[1] = new ColumnInfo(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.ValueType"),
				ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaBase.getTypes());
		
		columns[2] = new ColumnInfo(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.RemoveInputColumns"),
				ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { BaseMessages.getString(PKG, "System.Combo.No"),
						BaseMessages.getString(PKG, "System.Combo.Yes") });
		columns[2].setToolTip(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.RemoveInputColumns.Tooltip"));

		TextVarButtonRenderCallback callback = new TextVarButtonRenderCallback() {
			public boolean shouldRenderButton() {
				return true;
			}
		};
		
		 SelectionAdapter pathSelection = new SelectionAdapter() {
			    public void widgetSelected( SelectionEvent e ) {

			    	EnterListDialog dialog = new EnterListDialog( shell, SWT.OPEN , new String[] {"Test 1","Test 2"});

			        
			        
			        if ( dialog.open() != null ) {
			        
			        }
			    }
		 };		  

		for (int i = 0; i < Coalesce.MAX_INPUT_FIELD; i++) {
			columns[i + 3] = new ColumnInfo(
					BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.InputField",
							Character.valueOf((char) ('A' + i)).toString()),
//					ColumnInfo.COLUMN_TYPE_TEXT_BUTTON, new String[] { "" }, true);
					ColumnInfo.COLUMN_TYPE_TEXT_BUTTON);
			//columnInfos[i + 3].setReadOnly( true );
			// columnInfos[i + 3].setUsingVariables( true );
			//columnInfos[i + 3].setComboValues( new String[] { "test 1","TEST 2" } );
			//columnInfos[i + 3].setRenderTextVarButtonCallback(callback);
			columns[i + 3].setTextVarButtonSelectionListener( pathSelection );
		}

		this.tblFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE, columns,
				meta.getCoalesces().size(), lsMod, props);
		this.tblFields.setLayoutData(
				new FormDataBuilder().left().right(100, 0).top(wlFields, Const.MARGIN).bottom().result());

		this.tblFields.getTable().addListener(SWT.Resize, new ColumnsResizer(3, 27, 10, 10, 10, 10, 10, 10, 10));

		// Search the fields in the background
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				StepMeta stepMeta = transMeta.findStep(stepname);
				if (stepMeta != null) {
					try {
						RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);

						String[] fieldNames = new String[row.size()];
						for (int i = 0; i < row.size(); i++) {
							fieldNames[i] = row.getValueMeta(i).getName();
						}

						// Sort field names
						Const.sortStrings(fieldNames);

						for (int i = 0; i < Coalesce.MAX_INPUT_FIELD; i++) {
							columns[3 + i].setComboValues(fieldNames);
						}
					} catch (KettleException e) {
						logError(BaseMessages.getString(PKG, "CoalesceDialog.Log.UnableToFindInput"));
					}
				}
			}
		};
		new Thread(runnable).start();

		return parent;
	}

	// TODO: Find a global function
	private static boolean getBooleanFromString(final String s) {
	
		if (Utils.isEmpty(s))
			return false;
	
		return BaseMessages.getString(PKG, "System.Combo.Yes").equals(s); //$NON-NLS-1$
	}

	// TODO: Find a global function
	private static String getStringFromBoolean(boolean b) {
		return b ? BaseMessages.getString(PKG, "System.Combo.Yes") : BaseMessages.getString(PKG, "System.Combo.No"); 
	}

}