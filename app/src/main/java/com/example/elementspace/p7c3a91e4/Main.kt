package com.example.elementspace.p7c3a91e4

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.ui.atom.AtomScreen
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.example.elementspace.p7c3a91e4.ui.table.PeriodicTableScreen
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        // Main window: the periodic table (Planar, system glass from the manifest).
        DefaultWindowContainer {
            val bounds =
                Modifier.windowConstraints(minWidth = 1200.dp, minHeight = 780.dp, maxWidth = 2000.dp, maxHeight = 1300.dp)
            PicoTheme { PeriodicTableScreen(bounds) }
        }

        // One Volume per opened element; 3D content floats without a glass backing.
        WindowContainer(
            id = WindowIds.ATOM_VOLUME,
            form = Form.Volumetric,
            // Large by default: a Volume keeps its physical size, so at arm's length a 960 dp cube
            // read as small and far away next to the main window.
            defaultSize = WindowContainerSize(width = 1600.dp, height = 1600.dp, depth = 1600.dp),
            enableMaterialBackground = false,
            worldScale = WorldScale.Dynamic,
        ) {
            val atomicNumber = bundle?.getInt(WindowIds.ARG_ATOMIC_NUMBER) ?: 1
            val bounds =
                Modifier.windowConstraints(minWidth = 960.dp, minHeight = 960.dp, minDepth = 960.dp, maxWidth = 2700.dp, maxHeight = 2700.dp, maxDepth = 2700.dp)
            PicoTheme { AtomScreen(atomicNumber, bounds) }
        }
    }
