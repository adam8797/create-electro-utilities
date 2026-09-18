package com.adam8797.electroutilities.content.label;

import net.minecraft.core.Direction;

/**
 * A block entity that carries a single editable text label on one horizontal face. Implemented by both
 * the utility pole and the substation pole so the label editor + networking can treat them uniformly.
 */
public interface LabelableBlockEntity {

    void setLabel(String text, Direction face);

    String getLabelText();

    Direction getLabelFace();

    boolean hasLabel();

    /** Maximum label length for this pole type (utility 5, the thinner substation 2). */
    int maxLabelLength();
}
