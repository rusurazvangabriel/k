package org.kframework.kil;

import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.matchers.Matcher;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

/** The hole in a {@link Freezer}.
 * See {@link Hole} for the syntax of contexts.
 */
public class FreezerHole extends Term {
	/** Currently always zero, until nested freezers are implemented */
	private int index;
	
	public FreezerHole(int index) {
		super("K");
		this.index = index;
	}
	
	public FreezerHole(Element element) {
		// TODO: for Radu
		super(element);
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


	@Override
	public Term shallowCopy() {
		return new FreezerHole(this.index);
	}
	
	public int getIndex() {
		return index;
	}
}
