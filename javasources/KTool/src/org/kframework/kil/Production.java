package org.kframework.kil;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ProductionItem.ProductionType;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.matchers.Matcher;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.StringUtil;
import org.kframework.utils.xml.XML;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/** A production.
 * Any explicit attributes on the production are stored in {@link ASTNode#attributes}.
 */
public class Production extends ASTNode {

	/*
	 * Andrei S: It appears that the cons attribute is mandatory for all new production added during compilation, otherwise a null pointer exception can be thrown in one of the later compilation
	 * steps.
	 */
	protected List<ProductionItem> items;
	protected String sort;
	protected String ownerModuleName;

	public static Production makeFunction(String funSort, String funName, String argSort) {
		List<ProductionItem> prodItems = new ArrayList<ProductionItem>();
		prodItems.add(new Terminal(funName));
		prodItems.add(new Terminal("("));
		prodItems.add(new Sort(argSort));
		prodItems.add(new Terminal(")"));

		Production funProd = new Production(new Sort(funSort), prodItems);
		funProd.addAttribute(new Attribute("prefixlabel", funName));
		if (MetaK.isComputationSort(funSort)) {
			funProd.addAttribute(new Attribute("klabel", funName));
			String consAttr = funSort + "1" + funName + "Syn";
			funProd.addAttribute(new Attribute("cons", consAttr));
			DefinitionHelper.conses.put(consAttr, funProd);
			DefinitionHelper.putLabel(funProd, consAttr);
		}

		return funProd;
	}

	public boolean isListDecl() {
		return items.size() == 1 && items.get(0).getType() == ProductionType.USERLIST;
	}

	public boolean isSubsort() {
		return items.size() == 1 && items.get(0).getType() == ProductionType.SORT;
	}

	public boolean isConstant() {
		return items.size() == 1 && items.get(0).getType() == ProductionType.TERMINAL && (sort.startsWith("#") || sort.equals("KLabel"));
	}

	public Production(Element element) {
		super(element);

		java.util.List<String> strings = new ArrayList<String>();
		strings.add(Constants.SORT);
		strings.add(Constants.TERMINAL);
		strings.add(Constants.USERLIST);
		java.util.List<Element> its = XML.getChildrenElementsByTagName(element, strings);

		items = new ArrayList<ProductionItem>();
		for (Element e : its)
			items.add((ProductionItem) JavaClassesFactory.getTerm(e));

		its = XML.getChildrenElementsByTagName(element, Constants.ATTRIBUTES);
		// assumption: <attributes> appears only once
		if (its.size() > 0)
			attributes.setAll((Attributes) JavaClassesFactory.getTerm(its.get(0)));
		else if (attributes == null)
			attributes = new Attributes();
	}

	public Production(Production node) {
		super(node);
		this.items = node.items;
		this.sort = node.sort;
		this.ownerModuleName = node.ownerModuleName;
	}

	public Production(Sort sort, java.util.List<ProductionItem> items) {
		super();
		this.items = items;
		this.sort = sort.getName();
		attributes = new Attributes();
	}

	public Production(Sort sort, java.util.List<ProductionItem> items, String ownerModule) {
		super();
		this.items = items;
		this.sort = sort.getName();
		attributes = new Attributes();
		this.ownerModuleName = ownerModule;
	}

	public String getLabel() {
		String label = attributes.get("prefixlabel");
		if (label == null) {
			label = getPrefixLabel();
			attributes.set("prefixlabel", label);
		}
		return StringUtil.escapeMaude(label).replace(" ", "");
	}

	public String getCons() {
		return attributes.get("cons");
	}

	public String getKLabel() {
		assert MetaK.isComputationSort(sort) || sort.equals("KLabel") && isConstant();

		String klabel;
		klabel = attributes.get("klabel");
		if (klabel == null) {
			if (sort.toString().equals("KLabel"))
				klabel = getPrefixLabel();
			else
				klabel = "'" + getPrefixLabel();
			attributes.set("klabel", klabel);
		}
		return StringUtil.escapeMaude(klabel).replace(" ", "");
	}

	private String getPrefixLabel() {
		String klabel = "";
		for (ProductionItem pi : items) {
			switch (pi.getType()) {
			case SORT:
				klabel += "_";
				break;
			case TERMINAL:
				klabel += ((Terminal) pi).getTerminal();
				break;
			case USERLIST:
				klabel += "_" + ((UserList) pi).separator + "_";
				break;
			}
		}
		return klabel;
	}

	public java.util.List<ProductionItem> getItems() {
		return items;
	}

	public void setItems(java.util.List<ProductionItem> items) {
		this.items = items;
	}

	public int getArity() {
		int arity = 0;
		for (ProductionItem i : items) {
			if (i.getType() == ProductionType.USERLIST)
				arity += 2;
			if (i.getType() == ProductionType.SORT)
				arity++;
		}
		return arity;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer visitor) throws TransformerException {
		return visitor.transform(this);
	}

  @Override
  public void accept(Matcher matcher, ASTNode toMatch){
    matcher.match(this, toMatch);
  }

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getChildSort(int idx) {
		int arity = -1;
		if (items.get(0).getType() == ProductionType.USERLIST) {
			if (idx == 0) {
				return ((UserList) items.get(0)).getSort();
			} else {
				return this.getSort();
			}
		}
		for (ProductionItem i : items) {
			if (i.getType() != ProductionType.TERMINAL)
				arity++;
			if (arity == idx) {
				return ((Sort) i).getName();
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Production))
			return false;
		Production prd = (Production) obj;

		if (this.sort != null && prd.getSort() != null)
			if (!this.sort.equals(prd.getSort()))
				return false;
		if (this.sort == null && prd.getSort() != null)
			return false;

		if (prd.getItems().size() != this.items.size())
			return false;

		for (int i = 0; i < this.items.size(); i++) {
			if (!prd.getItems().get(i).equals(items.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if (sort != null)
			hash += sort.hashCode();

		for (ProductionItem pi : this.items)
			hash += pi.hashCode();
		return hash;
	}

	public String toString() {
		String content = "";
		for (ProductionItem i : items)
			content += i + " ";

		return content;
	}

	@Override
	public Production shallowCopy() {
		return new Production(this);
	}

	public String getOwnerModuleName() {
		return ownerModuleName;
	}

	public void setOwnerModuleName(String ownerModuleName) {
		this.ownerModuleName = ownerModuleName;
	}
}
