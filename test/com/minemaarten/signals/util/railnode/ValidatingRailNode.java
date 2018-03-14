package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.util.Pos2D;

public abstract class ValidatingRailNode extends DefaultRailNode implements IValidatingNode{

    public ValidatingRailNode(Pos2D pos){
        super(pos);
    }
}
