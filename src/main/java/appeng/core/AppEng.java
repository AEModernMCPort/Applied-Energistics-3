/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.core;


import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.common.event.FMLEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;

import appeng.core.lib.AEConfig;
import appeng.core.lib.AELog;
import appeng.core.lib.CommonHelper;
import appeng.core.lib.crash.CrashInfo;
import appeng.core.lib.crash.ModCrashEnhancement;
import appeng.core.lib.module.AEModule;
import appeng.core.lib.module.Toposorter;


@Mod( modid = AppEng.MOD_ID, name = AppEng.MOD_NAME, version = AEConfig.VERSION, dependencies = AppEng.MOD_DEPENDENCIES, acceptedMinecraftVersions = ForgeVersion.mcVersion, guiFactory = "appeng.core.client.gui.config.AEConfigGuiFactory" )
public final class AppEng
{
	public static final String MOD_ID = "appliedenergistics2";
	public static final String MOD_NAME = "Applied Energistics 2";

	public static final String ASSETS = "appliedenergistics2:";

	public static final String MOD_DEPENDENCIES =
			// a few mods, AE should load after, probably.
			// required-after:AppliedEnergistics2API|all;
			// "after:gregtech_addon;after:Mekanism;after:IC2;after:ThermalExpansion;after:BuildCraft|Core;" +

			// depend on version of forge used for build.
			"after:appliedenergistics2-core;" + "required-after:Forge@[" // require forge.
					+ net.minecraftforge.common.ForgeVersion.majorVersion + '.' // majorVersion
					+ net.minecraftforge.common.ForgeVersion.minorVersion + '.' // minorVersion
					+ net.minecraftforge.common.ForgeVersion.revisionVersion + '.' // revisionVersion
					+ net.minecraftforge.common.ForgeVersion.buildVersion + ",)"; // buildVersion

	// TODO @Elix-x Will be replaced with utils...
	private static final Field modifiers;
	static
	{
		try
		{
			modifiers = Field.class.getDeclaredField( "modifiers" );
			modifiers.setAccessible( true );
		}
		catch( ReflectiveOperationException e )
		{
			// :(
			// Should not happen.
			throw Throwables.propagate( e );
		}
	}

	@Nonnull
	private static final AppEng INSTANCE = new AppEng();

	private ImmutableMap<String, ?> modules;
	private ImmutableMap<Class<?>, ?> classModule;
	private ImmutableList<String> moduleOrder;
	private ImmutableMap<?, Boolean> internal;
	private File configDirectory;

	private AppEng()
	{
		FMLCommonHandler.instance().registerCrashCallable( new ModCrashEnhancement( CrashInfo.MOD_VERSION ) );
	}

	@Nonnull
	@Mod.InstanceFactory
	public static AppEng instance()
	{
		return INSTANCE;
	}

	public <M> M getModule( String name )
	{
		return (M) modules.get( name );
	}

	public <M> M getModule( Class<M> clas )
	{
		return (M) classModule.get( clas );
	}

	public File getConfigDirectory()
	{
		return configDirectory;
	}

	private void fireModulesEvent( final FMLEvent event )
	{
		for( String name : moduleOrder )
		{
			Object module = getModule( name );
			for( Method method : module.getClass().getDeclaredMethods() )
			{
				if( method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom( event.getClass() ) && method.getDeclaredAnnotation( AEModule.ModuleEventHandler.class ) != null )
				{
					try
					{
						method.invoke( module, event );
					}
					catch( Exception e )
					{
						// :(
					}
				}
			}
		}
	}

	@EventHandler
	private void preInit( final FMLPreInitializationEvent event )
	{
		if( !Loader.isModLoaded( "appliedenergistics2-core" ) )
		{
			CommonHelper.proxy.missingCoreMod();
		}

		Map<String, Pair<Class<?>, String>> foundModules = new HashMap<>();
		ASMDataTable annotations = event.getAsmData();
		for( ASMData data : annotations.getAll( AEModule.class.getCanonicalName() ) )
		{
			try
			{
				Class<?> clazz = Class.forName( data.getClassName() );
				foundModules.put( (String) data.getAnnotationInfo().get( "value" ), new ImmutablePair<Class<?>, String>( clazz, (String) data.getAnnotationInfo().get( "dependencies" ) ) );
			}
			catch( Exception e )
			{
				// :(
			}
		}

		List<String> checked = Lists.newArrayList();
		List<String> valid = Lists.newArrayList();
		Map<String, Class<?>> modules = Maps.newHashMap();
		for( Map.Entry<String, Pair<Class<?>, String>> entry : foundModules.entrySet() )
		{
			if( isValid( entry.getKey(), foundModules, event.getSide(), valid, checked ) )
			{
				modules.put( entry.getKey(), entry.getValue().getLeft() );
			}
		}
		Toposorter.Graph<String> graph = new Toposorter.Graph<String>();
		for( String name : modules.keySet() )
		{
			addAsNode( name, foundModules, graph, event.getSide() );
		}

		List<String> moduleLoadingOrder = null;
		try
		{
			moduleLoadingOrder = Toposorter.toposort( graph );
		}
		catch( Toposorter.SortingException e )
		{
			boolean moduleFound = false;
			event.getModLog().error( "Module " + e.getNode() + " has circular dependencies:" );
			for( String s : e.getVisitedNodes() )
			{
				if( s.equals( e.getNode() ) )
				{
					if( moduleFound )
					{
						break;
					}
					moduleFound = true;
					event.getModLog().error( "\"" + s + "\"" );
					continue;
				}
				if( moduleFound )
				{
					event.getModLog().error( "depending on: \"" + s + "\"" );
				}
			}
			event.getModLog().error( "again depending on \"" + e.getNode() + "\"" );
			CommonHelper.proxy.moduleLoadingException( String.format( "Circular dependency at module %s", e.getNode() ), "The module " + TextFormatting.BOLD + e.getNode() + TextFormatting.RESET + " has circular dependencies! See the log for a list!" );
		}
		ImmutableMap.Builder<String, Object> modulesBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<Class<?>, Object> classModuleBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<Object, Boolean> internalBuilder = ImmutableMap.builder();
		ImmutableList.Builder<String> orderBuilder = ImmutableList.builder();

		for( String name : moduleLoadingOrder )
		{
			try
			{
				Class<?> moduleClass = modules.get( name );
				Object module = moduleClass.newInstance();
				orderBuilder.add( name );
				modulesBuilder.put( name, module );
				classModuleBuilder.put( moduleClass, module );
				internalBuilder.put( module, !moduleClass.isAnnotationPresent( Mod.class ) );
			}
			catch( ReflectiveOperationException e )
			{
				event.getModLog().error( "Error while trying to setup the module " + name );
				e.printStackTrace();
			}
		}

		this.moduleOrder = orderBuilder.build();
		this.modules = modulesBuilder.build();
		this.classModule = classModuleBuilder.build();
		this.internal = internalBuilder.build();

		populateInstances( annotations );

		AELog.info( "Succesfully loaded %s modules", modules.size() );

		final Stopwatch watch = Stopwatch.createStarted();
		AELog.info( "Pre Initialization ( started )" );

		this.configDirectory = new File( event.getModConfigurationDirectory().getPath(), "AppliedEnergistics2" );
		AEConfig.instance = new AEConfig( new File( AppEng.instance().getConfigDirectory(), "AppliedEnergistics2.cfg" ) );

		fireModulesEvent( event );

		AELog.info( "Pre Initialization ( ended after " + watch.elapsed( TimeUnit.MILLISECONDS ) + "ms )" );
	}

	/**
	 * Checks whether all required dependencies are here
	 */
	private boolean isValid( String name, Map<String, Pair<Class<?>, String>> modules, Side currentSide, List<String> validModules, List<String> checkedModules )
	{
		if( checkedModules.contains( name ) )
			return validModules.contains( name );
		checkedModules.add( name );
		if( !modules.containsKey( name ) )
			return false;
		if( modules.get( name ).getRight() == null || modules.get( name ).getRight().equals( "" ) )
			return true;
		for( String dep : modules.get( name ).getRight().split( ";" ) )
		{
			String[] temp = dep.split( ":" );
			String[] modifiers = dep.split( "\\-" );
			String depName = temp.length > 0 ? temp[1] : null;
			Side requiredSide = ArrayUtils.contains( modifiers, "client" ) ? Side.CLIENT : ArrayUtils.contains( modifiers, "server" ) ? Side.SERVER : currentSide;
			boolean hard = ArrayUtils.contains( modifiers, "hard" );
			boolean crash = hard && ArrayUtils.contains( modifiers, "crash" );
			if( name == null )
			{
				if( requiredSide == currentSide )
				{
					continue;
				}
				else if( crash )
				{
					CommonHelper.proxy.moduleLoadingException( String.format( "Module %s is %s side only!", name, requiredSide.toString() ), "Module " + TextFormatting.BOLD + name + TextFormatting.RESET + " can only be used on " + TextFormatting.BOLD + requiredSide.toString() + TextFormatting.RESET + "!" );
				}
				return false;
			}
			else if( depName != null && hard )
			{
				String what = depName.substring( 0, depName.indexOf( '-' ) );
				String which = depName.substring( depName.indexOf( '-' ) + 1, depName.length() );
				boolean depFound = false;
				if( requiredSide == currentSide )
				{
					if( which.equals( "mod" ) )
					{
						depFound = Loader.isModLoaded( what );
					}
					else if( which.equals( "module" ) )
					{
						depFound = isValid( what, modules, currentSide, validModules, checkedModules );
					}
				}
				if( !depFound )
				{
					if( crash )
					{
						CommonHelper.proxy.moduleLoadingException( String.format( "Missing hard required dependency for module %s - %s", name, depName ), "Module " + TextFormatting.BOLD + name + TextFormatting.RESET + " is missing required hard dependency " + TextFormatting.BOLD + depName + TextFormatting.RESET + "." );
					}
				}
				return false;
			}
			else
			{
				return false; // Syntax error
			}
		}
		validModules.add( name );
		return true;
	}

	private void addAsNode( String name, Map<String, Pair<Class<?>, String>> foundModules, Toposorter.Graph<String> graph, Side currentSide )
	{
		if( graph.hasNode( name ) )
			return;
		Toposorter.Graph<String>.Node node = graph.addNewNode( name, name );
		if( foundModules.get( name ).getRight() == null || foundModules.get( name ).getRight().equals( "" ) )
			return;
		for( String dep : foundModules.get( name ).getRight().split( ";" ) )
		{
			String[] temp = dep.split( ":" );
			String[] modifiers = dep.split( "\\-" );
			String depName = temp.length > 0 ? temp[1] : null;
			Side requiredSide = ArrayUtils.contains( modifiers, "client" ) ? Side.CLIENT : ArrayUtils.contains( modifiers, "server" ) ? Side.SERVER : currentSide;
			boolean before = ArrayUtils.contains( modifiers, "before" );
			boolean after = ArrayUtils.contains( modifiers, "after" );
			if( depName != null )
			{
				String what = depName.substring( 0, depName.indexOf( '-' ) );
				String which = depName.substring( depName.indexOf( '-' ) + 1, depName.length() );
				if( which.equals( "module" ) && requiredSide == currentSide )
				{
					addAsNode( what, foundModules, graph, currentSide );
					if( after )
					{
						node.dependOn( graph.getNode( what ) );
					}
					else if( before )
					{
						node.dependencyOf( graph.getNode( what ) );
					}
					// "mod" cannot be handled here because AE2 cannot control mod loading else there is no vertex added to this graph
				}
			}
		}
	}

	private void populateInstances( ASMDataTable annotations )
	{
		for( Object module : modules.values() )
		{
			for( Field f : module.getClass().getDeclaredFields() )
			{
				try
				{
					AEModule.Instance annotation = f.getAnnotation( AEModule.Instance.class );
					if( annotation == null )
					{
						continue;
					}
					Object instance = modules.get( annotation.value() );
					if( instance == null )
					{
						instance = classModule.get( Class.forName( annotation.value() ) );
					}
					if( instance == null )
					{
						// :(
					}
					else
					{
						f.setAccessible( true );
						modifiers.set( f, f.getModifiers() & ( ~Modifier.FINAL ) );
						f.set( module, instance );
					}
				}
				catch( ReflectiveOperationException e )
				{
					// :(
				}
			}
		}
	}

	@EventHandler
	private void init( final FMLInitializationEvent event )
	{
		final Stopwatch start = Stopwatch.createStarted();
		AELog.info( "Initialization ( started )" );

		fireModulesEvent( event );

		AELog.info( "Initialization ( ended after " + start.elapsed( TimeUnit.MILLISECONDS ) + "ms )" );
	}

	@EventHandler
	private void postInit( final FMLPostInitializationEvent event )
	{
		final Stopwatch start = Stopwatch.createStarted();
		AELog.info( "Post Initialization ( started )" );

		fireModulesEvent( event );

		AEConfig.instance.save();

		AELog.info( "Post Initialization ( ended after " + start.elapsed( TimeUnit.MILLISECONDS ) + "ms )" );
	}

	@EventHandler
	private void handleIMCEvent( final FMLInterModComms.IMCEvent event )
	{
		fireModulesEvent( event );
	}

	@EventHandler
	private void serverAboutToStart( final FMLServerAboutToStartEvent event )
	{
		fireModulesEvent( event );
	}

	@EventHandler
	private void serverStarting( final FMLServerStartingEvent event )
	{
		fireModulesEvent( event );
	}

	@EventHandler
	private void serverStopping( final FMLServerStoppingEvent event )
	{
		fireModulesEvent( event );
	}

	@EventHandler
	private void serverStopped( final FMLServerStoppedEvent event )
	{
		fireModulesEvent( event );
	}
}
