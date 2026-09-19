package com.adam8797.electroutilities.client.ponder;

import com.adam8797.electroutilities.EUBlocks;
import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.content.substation.SubstationPoleBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.CrossarmArmBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.PoleConnector;
import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.WoodSet;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlockEntity;
import com.simibubi.create.AllItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Ponder storyboards for this mod's blocks. Each method is a {@link net.createmod.ponder.api.scene.PonderStoryBoard}
 * ({@code (SceneBuilder, SceneBuildingUtil)}). The world layout for each scene comes from an authored
 * schematic at {@code assets/electroutilities/ponder/<name>.nbt} (see that folder's README); until the
 * schematic exists the scene shows an empty base plate. Positions below assume the layouts documented there.
 */
public final class EUPonderScenes {

    private EUPonderScenes() {}

    /** Schematic: a 5x5 plate with a 3-tall utility pole at column (2, 2). */
    public static void utilityPole(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("utility_pole", "Routing wires with Utility Poles");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        // Base plate is the y=0 layer; reveal only the content above it so the base doesn't re-animate.
        scene.world().showSection(util.select().layersFrom(1), Direction.UP);
        scene.idle(20);

        BlockPos poleTop = util.grid().at(2, 3, 2);
        BlockPos poleMid = util.grid().at(2, 2, 2);

        scene.overlay().showText(80)
                .text("Utility poles are crafted from logs treated with Transformer Oil, then placed like a log.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(poleTop))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Aim at a pole's top or bottom and use another pole to extend the column vertically.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(poleTop));
        scene.idle(90);

        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("Use a connector, crossarm, or label item on the pole to add wiring attachments.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(poleMid, Direction.NORTH))
                .attachKeyFrame();
        scene.idle(70);

        // Add connectors one face at a time — each face is a separate placement.
        scene.overlay().showControls(util.vector().blockSurface(poleMid, Direction.NORTH), Pointing.DOWN, 20)
                .withItem(PoleConnector.SINGLE.item().getDefaultInstance())
                .rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntity(poleMid, UtilityPoleBlockEntity.class,
                be -> be.setConnector(Direction.NORTH, PoleConnector.SINGLE));
        scene.idle(30);

        scene.overlay().showControls(util.vector().blockSurface(poleMid, Direction.WEST), Pointing.DOWN, 20)
                .withItem(PoleConnector.SINGLE.item().getDefaultInstance())
                .rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntity(poleMid, UtilityPoleBlockEntity.class,
                be -> be.setConnector(Direction.WEST, PoleConnector.SINGLE));
        scene.idle(30);

        scene.overlay().showText(70)
                .text("Each face is wired independently — add a connector per face as needed.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(poleMid, Direction.WEST));
        scene.idle(80);
        scene.markAsFinished();
    }

    /** Schematic: a 5x5 plate with a utility pole that has a crossarm applied across the top. */
    public static void crossarm(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("crossarm", "The Crossarm");
        scene.configureBasePlate(0, 0, 5);

        BlockPos armTop = util.grid().at(2, 3, 2);
        // The arm block entities bake an absolute pole position into the schematic; re-point them at this
        // scene's pole BEFORE they are revealed, so they never render with the wrong (stale) axis.
        scene.world().modifyBlockEntity(util.grid().at(1, 3, 2), CrossarmArmBlockEntity.class,
                be -> be.configure(WoodSet.OAK, armTop));
        scene.world().modifyBlockEntity(util.grid().at(3, 3, 2), CrossarmArmBlockEntity.class,
                be -> be.configure(WoodSet.OAK, armTop));

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.UP);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The crossarm is crafted from three connectors over a row of planks.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("Use it on a utility pole to span three blocks, giving three independently wireable connectors.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop))
                .attachKeyFrame();
        scene.idle(90);

        // Wrench cycles the offset: which slots the two arms occupy along the pole. Safe to setBlock here —
        // CrossarmArmBlock.onRemove's teardown is server-gated, and ponder worlds are client-side.
        BlockState arm = EUBlocks.CROSSARM_ARM.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos armLeft = util.grid().at(1, 3, 2);
        BlockPos armRight = util.grid().at(3, 3, 2);
        BlockPos armFar = util.grid().at(4, 3, 2);

        scene.overlay().showControls(util.vector().topOf(armTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        // Offset +1: the pole sits at one end, both arms extend to the far side.
        scene.world().setBlock(armLeft, air, false);
        scene.world().setBlock(armFar, arm, false);
        scene.world().modifyBlockEntity(armFar, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("A wrench shifts the crossarm's offset — which slots the arms occupy along the pole.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armRight))
                .attachKeyFrame();
        scene.idle(90);
        // Back to centre.
        scene.overlay().showControls(util.vector().topOf(armTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        scene.world().setBlock(armFar, air, false);
        scene.world().setBlock(armLeft, arm, false);
        scene.world().modifyBlockEntity(armLeft, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.idle(25);

        // Stacking a pole on top removes the centre connector (only a pole above triggers isCrossarmTop).
        BlockState pole = EUBlocks.UTILITY_POLES.get(WoodSet.OAK).get().defaultBlockState();
        scene.world().setBlock(util.grid().at(2, 4, 2), pole, false);
        scene.idle(15);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("Stacking another pole on top covers the crossarm's centre connector, leaving the two arm nodes.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("Sneak-wrench removes the whole crossarm.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop));
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Schematic (7x7): two concrete substation poles — one topped with a connector at (2,3,3), and one
     * (the subject) topped with a high-voltage switch at (4,3,3), fed by a lever at its base (4,1,2).
     */
    public static void substationPole(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("substation_pole", "Substation Poles");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.UP);
        scene.idle(20);

        BlockPos switchPole = util.grid().at(4, 2, 3);
        BlockPos hvSwitch = util.grid().at(4, 3, 3);
        BlockPos connector = util.grid().at(2, 3, 3);

        scene.overlay().showText(80)
                .text("Substation poles are thin poles crafted from logs and Transformer Oil, or from concrete.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(switchPole, Direction.WEST))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("A pole relays redstone up its length: a lever at the base powers the high-voltage switch mounted on top.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(hvSwitch))
                .attachKeyFrame();
        scene.idle(100);

        // Articulate the relay: cut power at the lever and the switch opens, then restore it and it closes.
        // EE drives the switch's arm animation from HVSwitchBlockEntity.connected, not the blockstate.
        BlockPos lever = util.grid().at(4, 1, 2);
        scene.world().modifyBlock(lever, bs -> bs.setValue(BlockStateProperties.POWERED, false), false);
        scene.world().modifyBlockEntity(hvSwitch, HVSwitchBlockEntity.class, be -> be.connected = false);
        scene.idle(40);
        scene.world().modifyBlock(lever, bs -> bs.setValue(BlockStateProperties.POWERED, true), false);
        scene.world().modifyBlockEntity(hvSwitch, HVSwitchBlockEntity.class, be -> be.connected = true);
        scene.idle(30);

        scene.overlay().showText(70)
                .text("Other poles can carry a connector or further attachments for your wiring.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(connector));
        scene.idle(80);
        scene.markAsFinished();
    }

    /** Schematic (labels.nbt, 5x5): a substation pole at column (1,2) and a utility pole at column (3,2). */
    public static void label(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("labels", "Labelling Poles");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.UP);
        scene.idle(20);

        Direction face = Direction.NORTH; // camera-facing side (SOUTH/EAST face away from the ponder camera)
        BlockPos utilMid = util.grid().at(3, 2, 2);
        BlockPos subMid = util.grid().at(1, 2, 2);

        scene.overlay().showText(80)
                .text("A label marks a pole with a short tag, mounted on the pole itself.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(utilMid, face))
                .attachKeyFrame();
        scene.idle(90);

        // Apply a label to the utility pole's middle segment.
        scene.overlay().showControls(util.vector().blockSurface(utilMid, face), Pointing.DOWN, 20)
                .withItem(EUItems.UTILITY_POLE_LABEL.asStack())
                .rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntity(utilMid, UtilityPoleBlockEntity.class, be -> be.setLabel("P1", face));
        scene.idle(20);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("Using a label opens a small editor for one short line — up to five characters.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(utilMid, face))
                .attachKeyFrame();
        scene.idle(90);

        // The thinner substation pole holds fewer characters.
        scene.overlay().showControls(util.vector().blockSurface(subMid, face), Pointing.DOWN, 20)
                .withItem(EUItems.UTILITY_POLE_LABEL.asStack())
                .rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntity(subMid, SubstationPoleBlockEntity.class, be -> be.setLabel("S2", face));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("Thinner substation poles fit up to two characters.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(subMid, face))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("Right-click a label to edit it, or sneak to remove it.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(utilMid, face));
        scene.idle(80);
        scene.markAsFinished();
    }
}
