package com.adam8797.electroutilities.client.ponder;

import com.adam8797.electroutilities.EUBlocks;
import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.content.substation.SubstationPoleBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.CrossarmArmBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.PoleConnector;
import com.adam8797.electroutilities.content.utilitypole.PoleRotation;
import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlock;
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

        // Whole-pole rotation: wrenching the pole itself turns the entire column in 45° steps, carrying
        // every attachment with it. The column here is the three pole blocks at y=1..3.
        BlockPos poleBase = util.grid().at(2, 1, 2);
        BlockPos[] column = { poleTop, poleMid, poleBase };

        scene.overlay().showText(80)
                .colored(PonderPalette.BLUE)
                .text("Wrench the pole itself to turn the whole column in 45° steps — the connectors turn with it.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(poleTop))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(poleTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        rotatePoleColumn(scene, column, 1); // 0 -> 1: a 45° step, so the square post now reads as a diamond
        scene.idle(25);
        scene.overlay().showText(80)
                .text("At 45° the square post reads as a diamond, and its connectors swing round to the new facing.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(poleMid))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(poleTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        rotatePoleColumn(scene, column, 2); // 1 -> 2: squares back up, now facing a new direction
        scene.idle(25);
        scene.overlay().showText(70)
                .text("Another step squares it up again, the connectors now facing a new direction.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(poleMid));
        scene.idle(80);
        scene.markAsFinished();
    }

    /** Turns a whole ponder pole column to a rotation index: each block's rotation and its diamond state. */
    private static void rotatePoleColumn(SceneBuilder scene, BlockPos[] column, int rotation) {
        boolean diagonal = PoleRotation.isDiagonal(rotation);
        for (BlockPos p : column) {
            scene.world().modifyBlockEntity(p, UtilityPoleBlockEntity.class, be -> be.setRotation(rotation));
            scene.world().modifyBlock(p, bs -> bs.setValue(UtilityPoleBlock.DIAGONAL, diagonal), false);
        }
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

        // Two separate wrench gestures: wrenching an ARM block slides the offset; wrenching the POLE
        // rotates the whole crossarm (below). Safe to setBlock here — CrossarmArmBlock.onRemove's
        // teardown is server-gated, and ponder worlds are client-side.
        BlockState arm = EUBlocks.CROSSARM_ARM.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos armLeft = util.grid().at(1, 3, 2);
        BlockPos armRight = util.grid().at(3, 3, 2);
        BlockPos armFar = util.grid().at(4, 3, 2);

        scene.overlay().showControls(util.vector().topOf(armRight), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        // Offset +1: the pole sits at one end, both arms extend to the far side.
        scene.world().setBlock(armLeft, air, false);
        scene.world().setBlock(armFar, arm, false);
        scene.world().modifyBlockEntity(armFar, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("Wrench an arm block to slide the crossarm's offset — which slots the arms occupy along the pole.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armFar))
                .attachKeyFrame();
        scene.idle(90);
        // Back to centre.
        scene.overlay().showControls(util.vector().topOf(armFar), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        scene.world().setBlock(armFar, air, false);
        scene.world().setBlock(armLeft, arm, false);
        scene.world().modifyBlockEntity(armLeft, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.idle(25);

        // Whole-crossarm rotation: wrenching the POLE (not an arm) turns the entire crossarm 45° per step.
        // The two arm blocks move to the new rotation's cells; the pole stays put and becomes a diamond.
        BlockPos[] column = { armTop, util.grid().at(2, 2, 2), util.grid().at(2, 1, 2) };
        BlockPos diagA = util.grid().at(1, 3, 1); // rotation 3 (diagonal) arm cells
        BlockPos diagB = util.grid().at(3, 3, 3);
        BlockPos armNorth = util.grid().at(2, 3, 1); // rotation 4 (perpendicular axis) arm cells
        BlockPos armSouth = util.grid().at(2, 3, 3);

        scene.overlay().showText(80)
                .colored(PonderPalette.BLUE)
                .text("Wrench the pole itself instead, and the whole crossarm turns with it in 45° steps.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop))
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(armTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        // Swing onto the diagonal: arms move to the diagonal neighbour cells and the post becomes a diamond.
        scene.world().setBlock(armLeft, air, false);
        scene.world().setBlock(armRight, air, false);
        scene.world().setBlock(diagA, arm, false);
        scene.world().setBlock(diagB, arm, false);
        scene.world().modifyBlockEntity(diagA, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.world().modifyBlockEntity(diagB, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        rotatePoleColumn(scene, column, 3);
        scene.idle(25);
        scene.overlay().showText(80)
                .text("The arms swing onto the diagonal and the post reads as a diamond — any wires ride along.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(diagB))
                .attachKeyFrame();
        scene.idle(90);

        // Another step squares it back up on the perpendicular axis.
        scene.overlay().showControls(util.vector().topOf(armTop), Pointing.DOWN, 20)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(7);
        scene.world().setBlock(diagA, air, false);
        scene.world().setBlock(diagB, air, false);
        scene.world().setBlock(armNorth, arm, false);
        scene.world().setBlock(armSouth, arm, false);
        scene.world().modifyBlockEntity(armNorth, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        scene.world().modifyBlockEntity(armSouth, CrossarmArmBlockEntity.class, be -> be.configure(WoodSet.OAK, armTop));
        rotatePoleColumn(scene, column, 4);
        scene.idle(25);
        scene.overlay().showText(70)
                .text("Every pole in the column turns together, keeping a whole stacked run aligned.")
                .placeNearTarget()
                .pointAt(util.vector().topOf(armTop));
        scene.idle(80);

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

    /**
     * Schematic (labels.nbt, 5x5): a substation pole at column (1,2) and a utility pole at column (3,2)
     * that is topped with a crossarm (pole (3,4,2), arm blocks at (2,4,2) and (4,4,2)).
     */
    public static void label(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("labels", "Labelling Poles");
        scene.configureBasePlate(0, 0, 5);
        // The utility pole's crossarm arm blocks bake an absolute pole position into the schematic; re-point
        // them at this scene's pole BEFORE they are revealed, or they render on a stale (diagonal) axis.
        BlockPos crossarmPole = util.grid().at(3, 4, 2);
        scene.world().modifyBlockEntity(util.grid().at(2, 4, 2), CrossarmArmBlockEntity.class,
                be -> be.configure(WoodSet.OAK, crossarmPole));
        scene.world().modifyBlockEntity(util.grid().at(4, 4, 2), CrossarmArmBlockEntity.class,
                be -> be.configure(WoodSet.OAK, crossarmPole));
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
