package org.kettle.trans.steps.coalesce;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;

/**
 * Contains the properties of the inputs fields, target field name, target value
 * type and options.
 *
 * @author Nicolas ADMENT
 */
public class Coalesce implements Cloneable {

	private List<String> fields = new ArrayList<>();

	private int type = ValueMetaInterface.TYPE_NONE;

	/** The target field name */
	@Injection(name = "NAME", group = "FIELDS")
	private String name;

	@Injection(name = "TYPE", group = "FIELDS")
	public void setType(final String name) {
		this.type = ValueMetaFactory.getIdForValueMeta(name);
	}

	@Injection(name = "INPUT_FIELDS", group = "FIELDS")
	public void setInputFields(final String fields) {

		this.fields = new ArrayList<>();

		if (fields != null) {
			for (String field : fields.split("\\s*,\\s*")) {
				this.addInputField(field);
			}
		}
	}

	@Injection(name = "REMOVE_INPUT_FIELDS", group = "FIELDS")
	private boolean removeFields;

	public Coalesce() {
		super();
	}

	@Override
	public Object clone() {
		Coalesce clone;
		try {
			clone = (Coalesce) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
		return clone;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = StringUtils.stripToNull(name);
	}

	public List<String> getInputFields() {
		return this.fields;
	}

	public String getInputField(int index) {
		return this.fields.get(index);
	}

	public void addInputField(final String field) {
		// Ignore empty field
		if (Utils.isEmpty(field))
			return;

		this.fields.add(field);
	}

	public void removeInputField(final String field) {
		this.fields.remove(field);
	}

	public void setInputFields(final List<String> fields) {

		if (fields == null)
			this.fields = new ArrayList<>();
		else
			this.fields = fields;
	}

	public int getType() {
		return this.type;
	}

	public void setType(final int type) {
		this.type = type;
	}

	private String getTypeDesc() {
		return ValueMetaFactory.getValueMetaName(this.type);
	}

	/**
	 * Remove inpute fields
	 * 
	 * @return
	 */
	public boolean isRemoveFields() {
		return this.removeFields;
	}

	public void setRemoveFields(boolean remove) {
		this.removeFields = remove;
	}

	@Override
	public String toString() {
		return name + ":" + getTypeDesc();
	}
}
