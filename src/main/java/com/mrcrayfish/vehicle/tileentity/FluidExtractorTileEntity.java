package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.crafting.FluidExtract;
import com.mrcrayfish.vehicle.crafting.FluidExtractorRecipes;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.inventory.container.FluidExtractorContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public class FluidExtractorTileEntity extends TileFluidHandlerSynced implements IInventory, ITickableTileEntity, INamedContainerProvider
{
    private NonNullList<ItemStack> inventory = NonNullList.withSize(7, ItemStack.EMPTY);

    public static final int TANK_CAPACITY = 1000 * 5;
    public static final int FLUID_MAX_PROGRESS = 20 * 30;
    private static final int SLOT_FUEL_SOURCE = 0;
    private static final int SLOT_FLUID_SOURCE = 1;

    private int remainingFuel;
    private int fuelMaxProgress;
    private int extractionProgress;

    private String customName;

    protected final IIntArray fluidExtractorData = new IIntArray()
    {
        public int get(int index)
        {
            switch(index)
            {
                case 0:
                    return FluidExtractorTileEntity.this.extractionProgress;
                case 1:
                    return FluidExtractorTileEntity.this.remainingFuel;
                case 2:
                    return FluidExtractorTileEntity.this.fuelMaxProgress;
                case 3:
                    return FluidExtractorTileEntity.this.tank.getFluidAmount();
            }
            return 0;
        }

        public void set(int index, int value)
        {
            switch(index)
            {
                case 0:
                    FluidExtractorTileEntity.this.extractionProgress = value;
                    break;
                case 1:
                    FluidExtractorTileEntity.this.remainingFuel = value;
                    break;
                case 2:
                    FluidExtractorTileEntity.this.fuelMaxProgress = value;
                    break;
                case 3:
                    FluidExtractorTileEntity.this.tank.getFluid().setAmount(value);
                    break;
            }

        }

        public int size()
        {
            return 4;
        }
    };

    public FluidExtractorTileEntity()
    {
        super(ModTileEntities.FLUID_EXTRACTOR, TANK_CAPACITY, stack -> false);
    }

    @Override
    public void tick()
    {
        if(!this.world.isRemote)
        {
            ItemStack source = this.getStackInSlot(SLOT_FLUID_SOURCE);
            ItemStack fuel = this.getStackInSlot(SLOT_FUEL_SOURCE);
            if(!fuel.isEmpty() && !source.isEmpty() && this.remainingFuel == 0 && canFillWithFluid(source))
            {
                this.fuelMaxProgress = ForgeHooks.getBurnTime(fuel);
                this.remainingFuel = this.fuelMaxProgress;
                this.shrinkItem(SLOT_FUEL_SOURCE);
            }

            if(!source.isEmpty() && this.canFillWithFluid(source) && this.remainingFuel > 0)
            {
                if(this.extractionProgress++ == FLUID_MAX_PROGRESS)
                {
                    FluidExtract extract = FluidExtractorRecipes.getInstance().getRecipeResult(source);
                    if(extract != null)
                    {
                        this.tank.fill(extract.createStack(), IFluidHandler.FluidAction.EXECUTE);
                    }
                    this.extractionProgress = 0;
                    this.shrinkItem(SLOT_FLUID_SOURCE);
                }
            }
            else
            {
                this.extractionProgress = 0;
            }

            if(this.remainingFuel > 0 && canFillWithFluid(source))
            {
                this.remainingFuel--;
            }
        }
    }

    private boolean canFillWithFluid(ItemStack stack)
    {
        if(!stack.isEmpty() && this.tank.getFluidAmount() < this.tank.getCapacity())
        {
            FluidExtract extract = getFluidExtractSource();
            if(extract != null)
            {
                return this.tank.getFluid().isEmpty() || extract.getFluid() == this.tank.getFluid().getFluid();
            }
        }
        return false;
    }

    public FluidStack getFluidStackTank()
    {
        return this.tank.getFluid();
    }

    @Nullable
    public FluidExtract getFluidExtractSource()
    {
        return FluidExtractorRecipes.getInstance().getRecipeResult(this.getStackInSlot(SLOT_FLUID_SOURCE));
    }

    @Override
    public int getSizeInventory()
    {
        return 2;
    }

    @Override
    public boolean isEmpty()
    {
        for(ItemStack stack : this.inventory)
        {
            if(!stack.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index)
    {
        return this.inventory.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count)
    {
        ItemStack stack = ItemStackHelper.getAndSplit(this.inventory, index, count);
        if(!stack.isEmpty())
        {
            this.markDirty();
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index)
    {
        return ItemStackHelper.getAndRemove(this.inventory, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
        this.inventory.set(index, stack);
        if(stack.getCount() > this.getInventoryStackLimit())
        {
            stack.setCount(this.getInventoryStackLimit());
        }
        this.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player)
    {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        if(index == 0)
        {
            return ForgeHooks.getBurnTime(stack) > 0;
        }
        else if(index == 1)
        {
            return FluidExtractorRecipes.getInstance().getRecipeResult(stack) != null;
        }
        return false;
    }

    @Override
    public void clear()
    {
        this.inventory.clear();
    }

    public int getExtractionProgress()
    {
        return this.fluidExtractorData.get(0);
    }

    public int getRemainingFuel()
    {
        return this.fluidExtractorData.get(1);
    }

    public int getFuelMaxProgress()
    {
        return this.fluidExtractorData.get(2);
    }

    public int getFluidLevel()
    {
        return this.fluidExtractorData.get(3);
    }

    @Override
    public void read(CompoundNBT compound)
    {
        super.read(compound);
        if(compound.contains("ExtractionProgress", Constants.NBT.TAG_INT))
        {
            this.extractionProgress = compound.getInt("ExtractionProgress");
        }
        if(compound.contains("RemainingFuel", Constants.NBT.TAG_INT))
        {
            this.remainingFuel = compound.getInt("RemainingFuel");
        }
        if(compound.contains("FuelMaxProgress", Constants.NBT.TAG_INT))
        {
            this.fuelMaxProgress = compound.getInt("FuelMaxProgress");
        }
        if(compound.contains("Items", Constants.NBT.TAG_LIST))
        {
            this.inventory = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(compound, this.inventory);
        }
        if(compound.contains("CustomName", Constants.NBT.TAG_STRING))
        {
            this.customName = compound.getString("CustomName");
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound)
    {
        super.write(compound);
        compound.putInt("ExtractionProgress", this.extractionProgress);
        compound.putInt("RemainingFuel", this.remainingFuel);
        compound.putInt("FuelMaxProgress", this.fuelMaxProgress);

        ItemStackHelper.saveAllItems(compound, this.inventory);

        if(this.hasCustomName())
        {
            compound.putString("CustomName", this.customName);
        }

        return compound;
    }

    private String getName()
    {
        return this.hasCustomName() ? this.customName : "container.fluid_extractor";
    }


    public boolean hasCustomName()
    {
        return this.customName != null && !this.customName.isEmpty();
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.hasCustomName() ? new StringTextComponent(this.getName()) : new TranslationTextComponent(this.getName());
    }

    private void shrinkItem(int index)
    {
        ItemStack stack = this.getStackInSlot(index);
        stack.shrink(1);
        if(stack.isEmpty())
        {
            this.setInventorySlotContents(index, ItemStack.EMPTY);
        }
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity)
    {
        return new FluidExtractorContainer(windowId, playerInventory, this);
    }

    public IIntArray getFluidExtractorData()
    {
        return fluidExtractorData;
    }
}