package org.eytril.spigot.chunksnapshot;

import net.minecraft.server.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class CraftChunkSnapshot {
   public final ChunkSectionSnapshot[] sections = new ChunkSectionSnapshot[16];
   public final List tileEntities = new ArrayList();

   public ChunkSectionSnapshot[] getSections() {
      return this.sections;
   }

   public List<NBTTagCompound> getTileEntities() {
      return this.tileEntities;
   }
}
