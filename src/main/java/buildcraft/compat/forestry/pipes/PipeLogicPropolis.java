package buildcraft.compat.forestry.pipes;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;

import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBee;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;

import buildcraft.BuildCraftCompat;
import buildcraft.compat.forestry.pipes.network.PacketGenomeFilterChange;
import buildcraft.compat.forestry.pipes.network.PacketRequestFilterSet;
import buildcraft.compat.forestry.pipes.network.PacketTypeFilterChange;
import buildcraft.core.lib.network.Packet;
import buildcraft.core.lib.network.PacketNBT;
import buildcraft.network.PacketIds;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;

public class PipeLogicPropolis {

	private final Pipe<PipeTransportItems> pipe;
	
	private final EnumFilterType[] typeFilter = new EnumFilterType[6];
	private final IAllele[][][] genomeFilter = new IAllele[6][3][2];

	public PipeLogicPropolis(Pipe<PipeTransportItems> pipe) {
		this.pipe = pipe;
		for (int i = 0; i < typeFilter.length; i++) {
			typeFilter[i] = EnumFilterType.CLOSED;
		}
	}

	public void readFromNBT(NBTTagCompound nbttagcompound) {
		for (int i = 0; i < typeFilter.length; i++) {
			typeFilter[i] = EnumFilterType.values()[nbttagcompound.getByte("TypeFilter" + i)];
		}

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				if (nbttagcompound.hasKey("GenomeFilterS" + i + '-' + j + '-' + 0)) {
					genomeFilter[i][j][0] = AlleleManager.alleleRegistry.getAllele(nbttagcompound.getString("GenomeFilterS" + i + '-' + j + '-' + 0));
				}
				if (nbttagcompound.hasKey("GenomeFilterS" + i + '-' + j + '-' + 1)) {
					genomeFilter[i][j][1] = AlleleManager.alleleRegistry.getAllele(nbttagcompound.getString("GenomeFilterS" + i + '-' + j + '-' + 1));
				}
			}
		}
	}

	public void writeToNBT(NBTTagCompound nbttagcompound) {
		for (int i = 0; i < typeFilter.length; i++) {
			nbttagcompound.setByte("TypeFilter" + i, (byte) typeFilter[i].ordinal());
		}

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				if (genomeFilter[i][j][0] != null) {
					nbttagcompound.setString("GenomeFilterS" + i + '-' + j + '-' + 0, genomeFilter[i][j][0].getUID());
				}
				if (genomeFilter[i][j][1] != null) {
					nbttagcompound.setString("GenomeFilterS" + i + '-' + j + '-' + 1, genomeFilter[i][j][1].getUID());
				}
			}
		}
	}
	
	public boolean isClosed(ForgeDirection orientation) {
		return typeFilter[orientation.ordinal()] == EnumFilterType.CLOSED;
	}

	public boolean isIndiscriminate(ForgeDirection orientation) {
		return typeFilter[orientation.ordinal()] == EnumFilterType.ANYTHING;
	}

	public boolean matchType(ForgeDirection orientation, EnumFilterType type, IBee bee) {
		EnumFilterType filter = typeFilter[orientation.ordinal()];
		if (filter == EnumFilterType.BEE) {
			return type != EnumFilterType.ITEM && type != EnumFilterType.CLOSED;
		}

		// Special bee filtering
		if (bee != null) {
			if (filter == EnumFilterType.PURE_BREED) {
				return bee.isPureBred(EnumBeeChromosome.SPECIES);
			}
			if (filter == EnumFilterType.NOCTURNAL) {
				return bee.getGenome().getNocturnal();
			}
			if (filter == EnumFilterType.PURE_NOCTURNAL) {
				return bee.getGenome().getNocturnal() && bee.isPureBred(EnumBeeChromosome.NOCTURNAL);
			}
			if (filter == EnumFilterType.FLYER) {
				return bee.getGenome().getTolerantFlyer();
			}
			if (filter == EnumFilterType.PURE_FLYER) {
				return bee.getGenome().getTolerantFlyer() && bee.isPureBred(EnumBeeChromosome.TOLERANT_FLYER);
			}
			if (filter == EnumFilterType.CAVE) {
				return bee.getGenome().getCaveDwelling();
			}
			if (filter == EnumFilterType.PURE_CAVE) {
				return bee.getGenome().getCaveDwelling() && bee.isPureBred(EnumBeeChromosome.CAVE_DWELLING);
			}
			if (filter == EnumFilterType.NATURAL) {
				return bee.isNatural();
			}
		}

		// Compare the remaining
		return filter == type;
	}

	public ArrayList<IAllele[]> getGenomeFilters(ForgeDirection orientation) {
		ArrayList<IAllele[]> filters = new ArrayList<IAllele[]>();

		for (int i = 0; i < 3; i++) {
			if (genomeFilter[orientation.ordinal()][i] != null
					&& (genomeFilter[orientation.ordinal()][i][0] != null || genomeFilter[orientation.ordinal()][i][1] != null)) {
				filters.add(genomeFilter[orientation.ordinal()][i]);
			}
		}

		return filters;
	}

	public EnumFilterType getTypeFilter(ForgeDirection orientation) {
		return typeFilter[orientation.ordinal()];
	}

	public void setTypeFilter(ForgeDirection orientation, EnumFilterType type) {
		typeFilter[orientation.ordinal()] = type;
		if (pipe.getWorld().isRemote) {
			sendTypeFilterChange(orientation, type);
		}
	}

	public IAlleleSpecies getSpeciesFilter(ForgeDirection orientation, int pattern, int allele) {

		if (genomeFilter[orientation.ordinal()] == null) {
			return null;
		}

		if (genomeFilter[orientation.ordinal()].length <= pattern) {
			return null;
		}

		if (genomeFilter[orientation.ordinal()][pattern] == null) {
			return null;
		}

		if (genomeFilter[orientation.ordinal()][pattern].length <= allele) {
			return null;
		}

		return (IAlleleSpecies) genomeFilter[orientation.ordinal()][pattern][allele];
	}

	public void setSpeciesFilter(ForgeDirection orientation, int pattern, int allele, IAllele species) {
		genomeFilter[orientation.ordinal()][pattern][allele] = species;
		if (pipe.getWorld().isRemote) {
			sendGenomeFilterChange(orientation, pattern, allele, species);
		}
	}

	/* NETWORK */
	public void sendFilterSet(EntityPlayer player) {
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		this.writeToNBT(nbttagcompound);
		Packet packet = new PacketNBT(PacketIds.PROP_SEND_FILTER_SET, nbttagcompound, (int) player.posX, (int) player.posY, (int) player.posZ);
		BuildCraftCompat.instance.sendToPlayer(player, packet);
	}

	public void handleFilterSet(PacketNBT packet) {
		this.readFromNBT(packet.getTagCompound());
	}

	public void requestFilterSet() {
		TileGenericPipe pipeTile = pipe.container;
		Packet packet = new PacketRequestFilterSet(pipeTile.xCoord, pipeTile.yCoord, pipeTile.zCoord);
		BuildCraftCompat.instance.sendToServer(packet);
	}

	public void sendTypeFilterChange(ForgeDirection orientation, EnumFilterType filter) {
		PacketTypeFilterChange packet = new PacketTypeFilterChange(pipe.container, orientation, filter);
		BuildCraftCompat.instance.sendToServer(packet);
	}

	public void handleTypeFilterChange(PacketTypeFilterChange packet) {
		int orientation = packet.getOrientation();
		int filterOrdinal = packet.getFilter();
		typeFilter[orientation] = EnumFilterType.values()[filterOrdinal];
	}

	public void sendGenomeFilterChange(ForgeDirection orientation, int pattern, int allele, IAllele species) {
		PacketGenomeFilterChange packet = new PacketGenomeFilterChange(pipe.container, orientation, pattern, allele, species);
		BuildCraftCompat.instance.sendToServer(packet);
	}

	public void handleGenomeFilterChange(PacketGenomeFilterChange packet) {
		IAllele species = packet.getSpecies();
		int orientation = packet.getOrientation();
		int pattern = packet.getPattern();
		int allele = packet.getAllele();

		genomeFilter[orientation][pattern][allele] = species;
	}
}
