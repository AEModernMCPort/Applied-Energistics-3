
package appeng.miscellaneous.definitions;


import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import appeng.api.definitions.ITileDefinition;
import appeng.core.lib.bootstrap.FeatureFactory;
import appeng.core.lib.definitions.Definitions;
import appeng.core.lib.features.AEFeature;
import appeng.miscellaneous.AppEngMiscellaneous;
import appeng.miscellaneous.api.definitions.IMiscellaneousTileDefinitions;
import appeng.miscellaneous.tile.TileLightDetector;


public class MiscellaneousTileDefinitions extends Definitions<Class<TileEntity>, ITileDefinition<TileEntity>> implements IMiscellaneousTileDefinitions
{

	private final ITileDefinition lightDetectingFixture;

	public MiscellaneousTileDefinitions( FeatureFactory registry )
	{
		this.lightDetectingFixture = registry.tile( new ResourceLocation( AppEngMiscellaneous.MODID, "light_detecting_fixture" ), TileLightDetector.class ).features( AEFeature.LightDetector ).build();

		init();
	}

}
