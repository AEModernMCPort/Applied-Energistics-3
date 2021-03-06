
package appeng.core.me.client.render.model.pipeline;


import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;

import appeng.api.client.BakingPipeline;
import appeng.api.client.BakingPipelineElement;
import appeng.core.api.util.AEPartLocation;
import appeng.core.lib.client.render.model.pipeline.TintIndexModifier;
import appeng.core.me.api.parts.IFacadePart;
import appeng.core.me.block.BlockCableBus;
import appeng.core.me.part.CableBusContainer;


public class Facades implements BakingPipelineElement<BakedQuad, BakedQuad>
{

	private final BakingPipeline rotatingPipeline;
	private final TintIndexModifier tintIndexModifier;
	private final BakingPipeline tintIndexFixPipeline;

	public Facades( BakingPipeline rotatingPipeline, TintIndexModifier tintIndexModifier, BakingPipeline tintIndexFixPipeline )
	{
		this.rotatingPipeline = rotatingPipeline;
		this.tintIndexModifier = tintIndexModifier;
		this.tintIndexFixPipeline = tintIndexFixPipeline;
	}

	@Override
	public List<BakedQuad> pipe( List<BakedQuad> elements, IBakedModel parent, IBlockState state, EnumFacing side, long rand )
	{
		if( state != null )
		{
			CableBusContainer cableBus = ( (IExtendedBlockState) state ).getValue( BlockCableBus.cableBus );
			for( AEPartLocation facing : AEPartLocation.SIDE_LOCATIONS )
			{
				IFacadePart facade = cableBus.getFacadeContainer().getFacade( facing );
				if( facade != null )
				{
					tintIndexModifier.setTintTransformer( tint -> ( facing.ordinal() << 2 ) | tint );
					elements.addAll( tintIndexFixPipeline.pipe( facade.getOrBakeQuads( cableBus, rotatingPipeline, state, side, rand ), parent, state, side, rand ) );
				}
			}
		}
		return elements;
	}

}
