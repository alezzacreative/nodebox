package nodebox.node;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NewNodesRegistrationTest {

    @Test
    public void testAllNewNodesRegisteredInLibraries() {
        NodeRepository repo = NodeRepository.of();

        // 1. Math library
        NodeLibrary mathLib = NodeLibrary.load(new File("libraries/math/math.ndbx"), repo);
        assertTrue("math.noise node must be registered", mathLib.getRoot().hasChild("noise"));
        assertTrue("math.spring node must be registered", mathLib.getRoot().hasChild("spring"));

        // 2. CoreVector library
        NodeLibrary coreVectorLib = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), repo);
        assertTrue("corevector.delaunay node must be registered", coreVectorLib.getRoot().hasChild("delaunay"));
        assertTrue("corevector.voronoi node must be registered", coreVectorLib.getRoot().hasChild("voronoi"));
        assertTrue("corevector.convex_hull node must be registered", coreVectorLib.getRoot().hasChild("convex_hull"));
        assertTrue("corevector.smooth node must be registered", coreVectorLib.getRoot().hasChild("smooth"));
        assertTrue("corevector.flow_field node must be registered", coreVectorLib.getRoot().hasChild("flow_field"));
        assertTrue("corevector.lsystem node must be registered", coreVectorLib.getRoot().hasChild("lsystem"));
        assertTrue("corevector.attractor node must be registered", coreVectorLib.getRoot().hasChild("attractor"));
        assertTrue("corevector.offset_path node must be registered", coreVectorLib.getRoot().hasChild("offset_path"));
        assertTrue("corevector.trace_contours node must be registered", coreVectorLib.getRoot().hasChild("trace_contours"));
        assertTrue("corevector.typeset node must be registered", coreVectorLib.getRoot().hasChild("typeset"));
        assertTrue("corevector.fit_text node must be registered", coreVectorLib.getRoot().hasChild("fit_text"));
        assertTrue("corevector.text_on_path node must be registered", coreVectorLib.getRoot().hasChild("text_on_path"));
        assertTrue("corevector.halftone node must be registered", coreVectorLib.getRoot().hasChild("halftone"));
        assertTrue("corevector.truchet_tiles node must be registered", coreVectorLib.getRoot().hasChild("truchet_tiles"));
        assertTrue("corevector.guilloche node must be registered", coreVectorLib.getRoot().hasChild("guilloche"));
        assertTrue("corevector.metaballs node must be registered", coreVectorLib.getRoot().hasChild("metaballs"));
        assertTrue("corevector.modular_grid node must be registered", coreVectorLib.getRoot().hasChild("modular_grid"));
        assertTrue("corevector.align_distribute node must be registered", coreVectorLib.getRoot().hasChild("align_distribute"));
        assertTrue("corevector.pack_circles node must be registered", coreVectorLib.getRoot().hasChild("pack_circles"));
        assertTrue("corevector.roughen node must be registered", coreVectorLib.getRoot().hasChild("roughen"));
        assertTrue("corevector.long_shadow node must be registered", coreVectorLib.getRoot().hasChild("long_shadow"));
        assertTrue("corevector.extrude_3d node must be registered", coreVectorLib.getRoot().hasChild("extrude_3d"));
        assertTrue("corevector.boolean node must be registered", coreVectorLib.getRoot().hasChild("boolean"));
        assertTrue("corevector.stroke_style node must be registered", coreVectorLib.getRoot().hasChild("stroke_style"));
        assertTrue("corevector.morph node must be registered", coreVectorLib.getRoot().hasChild("morph"));
        assertTrue("corevector.seamless_tile node must be registered", coreVectorLib.getRoot().hasChild("seamless_tile"));
        assertTrue("corevector.hex_grid node must be registered", coreVectorLib.getRoot().hasChild("hex_grid"));
        assertTrue("corevector.kaleidoscope node must be registered", coreVectorLib.getRoot().hasChild("kaleidoscope"));
        assertTrue("corevector.wave_warp node must be registered", coreVectorLib.getRoot().hasChild("wave_warp"));
        assertTrue("corevector.bulge_pinch node must be registered", coreVectorLib.getRoot().hasChild("bulge_pinch"));
        assertTrue("corevector.stripes node must be registered", coreVectorLib.getRoot().hasChild("stripes"));

        // 3. Data library
        NodeLibrary dataLib = NodeLibrary.load(new File("libraries/data/data.ndbx"), repo);
        assertTrue("data.sample_image node must be registered", dataLib.getRoot().hasChild("sample_image"));
        assertTrue("data.barchart node must be registered", dataLib.getRoot().hasChild("barchart"));
        assertTrue("data.donut node must be registered", dataLib.getRoot().hasChild("donut"));

        // 4. Color library
        NodeLibrary colorLib = NodeLibrary.load(new File("libraries/color/color.ndbx"), repo);
        assertTrue("color.gradient node must be registered", colorLib.getRoot().hasChild("gradient"));
        assertTrue("color.extract_palette node must be registered", colorLib.getRoot().hasChild("extract_palette"));
        assertTrue("color.blend_mode node must be registered", colorLib.getRoot().hasChild("blend_mode"));
        assertTrue("color.conic_gradient node must be registered", colorLib.getRoot().hasChild("conic_gradient"));

        // 5. Device library
        NodeLibrary deviceLib = NodeLibrary.load(new File("libraries/device/device.ndbx"), repo);
        assertTrue("device.sound_file node must be registered", deviceLib.getRoot().hasChild("sound_file"));
        assertTrue("device.sound_spectrum node must be registered", deviceLib.getRoot().hasChild("sound_spectrum"));
        assertTrue("device.sound_info node must be registered", deviceLib.getRoot().hasChild("sound_info"));
    }
}
