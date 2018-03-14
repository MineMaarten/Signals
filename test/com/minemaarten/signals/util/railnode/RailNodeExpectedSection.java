package com.minemaarten.signals.util.railnode;

import org.junit.Assert;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeExpectedSection extends ValidatingRailNode{

    public final char group;

    public RailNodeExpectedSection(Pos2D pos, char group){
        super(pos);
        this.group = group;
    }

    @Override
    public void validate(TestRailNetwork network){

        RailSection<Pos2D> expectedSection = network.findSection(pos);
        Assert.assertNotNull("Not on an section at pos " + pos, expectedSection);

        //Assert right groupings (333  (somewhere else) 333 = AssertionError)
        network.railObjects.networkObjectsOfType(RailNodeExpectedSection.class).filter(node -> node.group == group).forEach(node -> {
            Assert.assertEquals("Section in group '" + group + "' for pos " + pos + " has an unexpected section.", expectedSection, network.findSection(node.pos));
        });

        //Assert every edge (33343 = AssertionError)
        for(NetworkRail<Pos2D> railOnSection : expectedSection) {
            if(railOnSection instanceof RailNodeExpectedSection) {
                RailNodeExpectedSection sectionValidator = (RailNodeExpectedSection)railOnSection;
                Assert.assertEquals("Section belonging to pos '" + pos + "' has a different group at " + sectionValidator.pos + ".", "" + group, "" + sectionValidator.group);
            }
        }
    }
}
