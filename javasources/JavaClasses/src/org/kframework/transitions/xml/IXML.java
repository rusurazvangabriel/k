package org.kframework.transitions.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IXML {

	public Element toXml(Document doc);
}