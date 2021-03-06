
package appeng.core.lib.bootstrap;


import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.definitions.IBlockDefinition;
import appeng.core.CreativeTab;
import appeng.core.lib.block.AEBaseTileBlock;
import appeng.core.lib.definitions.BlockDefinition;
import appeng.core.lib.util.Platform;


public class BlockDefinitionBuilder<B extends Block> extends DefinitionBuilder<B, B, IBlockDefinition<B>, BlockDefinitionBuilder<B>> implements IBlockBuilder<B, BlockDefinitionBuilder<B>>
{

	private CreativeTabs creativeTab = CreativeTab.instance;

	private IItemBlockCustomizer itemBlock = null;

	@SideOnly( Side.CLIENT )
	private BlockRendering blockRendering;

	@SideOnly( Side.CLIENT )
	private ItemRendering itemRendering;

	BlockDefinitionBuilder( FeatureFactory factory, ResourceLocation id, B block )
	{
		super( factory, id, block );

		if( Platform.isClient() )
		{
			blockRendering = new BlockRendering();
			itemRendering = new ItemRendering();
		}
	}

	public BlockDefinitionBuilder<B> rendering( BlockRenderingCustomizer callback )
	{
		if( Platform.isClient() )
		{
			customizeForClient( callback );
		}

		return this;
	}

	public BlockDefinitionBuilder<B> createDefaultItemBlock()
	{
		itemBlock = ItemBlock::new;
		return this;
	}

	public BlockDefinitionBuilder<B> withItemBlock( IItemBlockCustomizer ib )
	{
		itemBlock = ib;
		return this;
	}

	@SideOnly( Side.CLIENT )
	private void customizeForClient( BlockRenderingCustomizer callback )
	{
		callback.customize( blockRendering, itemRendering );
	}

	@Override
	public IBlockDefinition<B> def( B block )
	{
		if( block == null )
		{
			return new BlockDefinition<B>( registryName, null );
		}

		block.setCreativeTab( creativeTab );
		block.setUnlocalizedName( registryName.getResourceDomain() + "." + registryName.getResourcePath() );

		if( Platform.isClient() )
		{
			if( block instanceof AEBaseTileBlock )
			{
				AEBaseTileBlock tileBlock = (AEBaseTileBlock) block;
				blockRendering.apply( factory, block, tileBlock.getTileEntityClass() );
			}
			else
			{
				blockRendering.apply( factory, block, null );
			}
		}

		BlockDefinition definition = new BlockDefinition<B>( registryName, block );
		if( !block.getBlockState().getProperties().isEmpty() )
		{
			definition.setSubDefinition( new BlockSubDefinition<IBlockState, Block>( block.getDefaultState(), definition ) );
		}

		if( itemBlock != null )
		{
			this.factory.addItemBlock( definition, itemBlock );
		}
		return definition;
	}

}
