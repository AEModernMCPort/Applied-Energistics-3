/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core.me.tile;


import net.minecraft.nbt.NBTTagCompound;

import appeng.core.api.util.AECableType;
import appeng.core.api.util.AEPartLocation;
import appeng.core.api.util.DimensionalCoord;
import appeng.core.lib.tile.TileEvent;
import appeng.core.lib.tile.events.TileEventType;
import appeng.core.lib.tile.powered.AEBasePoweredTile;
import appeng.core.me.api.networking.IGridNode;
import appeng.core.me.api.networking.security.IActionHost;
import appeng.core.me.grid.helpers.AENetworkProxy;
import appeng.core.me.grid.helpers.IGridProxyable;


public abstract class AENetworkPowerTile extends AEBasePoweredTile implements IActionHost, IGridProxyable
{

	private final AENetworkProxy gridProxy = new AENetworkProxy( this, "proxy", this.getItemFromTile( this ), true );

	@TileEvent( TileEventType.WORLD_NBT_READ )
	public void readFromNBT_AENetwork( final NBTTagCompound data )
	{
		this.getProxy().readFromNBT( data );
	}

	@TileEvent( TileEventType.WORLD_NBT_WRITE )
	public void writeToNBT_AENetwork( final NBTTagCompound data )
	{
		this.getProxy().writeToNBT( data );
	}

	@Override
	public AENetworkProxy getProxy()
	{
		return this.gridProxy;
	}

	@Override
	public DimensionalCoord getLocation()
	{
		return new DimensionalCoord( this );
	}

	@Override
	public void gridChanged()
	{

	}

	@Override
	public IGridNode getGridNode( final AEPartLocation dir )
	{
		return this.getProxy().getNode();
	}

	@Override
	public AECableType getCableConnectionType( final AEPartLocation dir )
	{
		return AECableType.SMART;
	}

	@Override
	public void validate()
	{
		super.validate();
		this.getProxy().validate();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		this.getProxy().invalidate();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		this.getProxy().onChunkUnload();
	}

	@Override
	public void onReady()
	{
		super.onReady();
		this.getProxy().onReady();
	}

	@Override
	public IGridNode getActionableNode()
	{
		return this.getProxy().getNode();
	}

}
