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

package appeng.core.me.client.gui;


import java.io.IOException;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.core.api.config.FullnessMode;
import appeng.core.api.config.OperationMode;
import appeng.core.api.config.RedstoneMode;
import appeng.core.api.config.Settings;
import appeng.core.lib.client.gui.GuiUpgradeable;
import appeng.core.lib.client.gui.widgets.GuiImgButton;
import appeng.core.lib.localization.GuiText;
import appeng.core.lib.sync.network.NetworkHandler;
import appeng.core.lib.sync.packets.PacketConfigButton;
import appeng.core.me.AppEngME;
import appeng.core.me.container.ContainerIOPort;
import appeng.core.me.definitions.MEItemDefinitions;
import appeng.core.me.tile.TileIOPort;


public class GuiIOPort extends GuiUpgradeable
{

	private GuiImgButton fullMode;
	private GuiImgButton operationMode;

	public GuiIOPort( final InventoryPlayer inventoryPlayer, final TileIOPort te )
	{
		super( new ContainerIOPort( inventoryPlayer, te ) );
		this.ySize = 166;
	}

	@Override
	protected void addButtons()
	{
		this.redstoneMode = new GuiImgButton( this.guiLeft - 18, this.guiTop + 28, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE );
		this.fullMode = new GuiImgButton( this.guiLeft - 18, this.guiTop + 8, Settings.FULLNESS_MODE, FullnessMode.EMPTY );
		this.operationMode = new GuiImgButton( this.guiLeft + 80, this.guiTop + 17, Settings.OPERATION_MODE, OperationMode.EMPTY );

		this.buttonList.add( this.operationMode );
		this.buttonList.add( this.redstoneMode );
		this.buttonList.add( this.fullMode );
	}

	@Override
	public void drawFG( final int offsetX, final int offsetY, final int mouseX, final int mouseY )
	{
		this.fontRenderer.drawString( this.getGuiDisplayName( GuiText.IOPort.getLocal() ), 8, 6, 4210752 );
		this.fontRenderer.drawString( GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, 4210752 );

		if( this.redstoneMode != null )
		{
			this.redstoneMode.set( this.cvb.getRedStoneMode() );
		}

		if( this.operationMode != null )
		{
			this.operationMode.set( ( (ContainerIOPort) this.cvb ).getOperationMode() );
		}

		if( this.fullMode != null )
		{
			this.fullMode.set( ( (ContainerIOPort) this.cvb ).getFullMode() );
		}
	}

	@Override
	public void drawBG( final int offsetX, final int offsetY, final int mouseX, final int mouseY )
	{
		super.drawBG( offsetX, offsetY, mouseX, mouseY );

		final MEItemDefinitions definitions = AppEngME.INSTANCE.definitions( Item.class );

		definitions.cell1k().maybeStack( 1 ).ifPresent( cell1kStack -> this.drawItem( offsetX + 66 - 8, offsetY + 17, (ItemStack) cell1kStack ) );

		definitions.blockDrive().maybeStack( 1 ).ifPresent( driveStack -> this.drawItem( offsetX + 94 + 8, offsetY + 17, (ItemStack) driveStack ) );
	}

	@Override
	protected String getBackground()
	{
		return "guis/ioport.png";
	}

	@Override
	protected void actionPerformed( final GuiButton btn ) throws IOException
	{
		super.actionPerformed( btn );

		final boolean backwards = Mouse.isButtonDown( 1 );

		if( btn == this.fullMode )
		{
			NetworkHandler.instance.sendToServer( new PacketConfigButton( this.fullMode.getSetting(), backwards ) );
		}

		if( btn == this.operationMode )
		{
			NetworkHandler.instance.sendToServer( new PacketConfigButton( this.operationMode.getSetting(), backwards ) );
		}
	}
}
