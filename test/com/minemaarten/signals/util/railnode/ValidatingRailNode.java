package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public abstract class ValidatingRailNode extends DefaultRailNode{

    public ValidatingRailNode(Pos2D pos){
        super(pos);
    }

    public abstract void validate(TestRailNetwork network);
}
