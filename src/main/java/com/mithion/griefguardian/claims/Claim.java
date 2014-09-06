package com.mithion.griefguardian.claims;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import cpw.mods.fml.common.network.ByteBufUtils;

public class Claim{
	private String claimOwner;		
	private HashMap<String, PermissionsMutex> claimPermissions;
	private AxisAlignedBB claimBounds;

	public static final String EVERYONE = "I:Everyone";

	public Claim(String owner, AxisAlignedBB bounds){
		this.claimOwner = owner;
		this.claimBounds = bounds;
		claimPermissions = new HashMap<String, PermissionsMutex>();
	}
	
	public Claim(ByteBuf buf){
		readFromByteBuf(buf);
	}

	/**
	 * Gets the claim owner
	 */
	public String getOwner(){
		return claimOwner;
	}
	
	/**
	 * Resolves the permissions mutex for the given identifier, or creates a new one if it didn't exist.
	 */
	private PermissionsMutex getPermissionsMutex(String identifier){
		if (!claimPermissions.containsKey(identifier)){
			claimPermissions.put(identifier, new PermissionsMutex());
		}
		PermissionsMutex mutex = claimPermissions.get(identifier);
		return mutex;
	}

	/**
	 * Adds permissions to the mutex
	 * @param identifier The identifier of the mutex.  Can be a player name, a predefined constant in this class, or a team.  The ClaimManager has a helper function to create team identifiers.
	 * @param flags The flags to grant permission for.
	 */
	public void addPermission(String identifier, int flags){			
		getPermissionsMutex(identifier).setFlags(flags);
	}

	/**
	 * Removes permissions from the mutex
	 * @param identifier The identifier of the mutex.  Can be a player name, a predefined constant in this class, or a team.  The ClaimManager has a helper function to create team identifiers.
	 * @param flags The flags to grant permission for.
	 */
	public void removePermission(String identifier, int flags){
		getPermissionsMutex(identifier).clearFlags(flags);
	}

	/**
	 * Removes all permissions for the specified identifier from the mutex.
	 */
	public void removeAllPermission(String identifier){
		claimPermissions.remove(identifier);
	}

	/**
	 * Checks if the specified xyz coordinate falls inside this claim
	 */
	public boolean testBounds(int x, int y, int z){
		Vec3 vec = Vec3.createVectorHelper(x, y, z);
		boolean inside = claimBounds.isVecInside(vec);
		return inside;
	}

	/**
	 * Checkes if the specified bounding box intersects this claim
	 */
	public boolean testBounds(AxisAlignedBB bb){
		return this.claimBounds.intersectsWith(bb);
	}

	/**
	 * Checkes if the specified claim intersects this claim
	 */
	public boolean testBounds(Claim claim){
		return this.claimBounds.intersectsWith(claim.claimBounds);
	}

	/**
	 * Checks if the given identifier is allowed the current action in this claim.
	 * See PermissionsMutex for valid actions list
	 */
	public boolean actionIsPermitted(String identifier, int action){
		if (identifier.equals(claimOwner))
			return true;
		return getPermissionsMutex(identifier).hasAllFlags(action);
	}
	
	/**
	 * Writes this claim to a byte buffer
	 */
	public void writeToByteBuf(ByteBuf buf){
		ByteBufUtils.writeUTF8String(buf, claimOwner);
		buf.writeDouble(claimBounds.minX);
		buf.writeDouble(claimBounds.minY);
		buf.writeDouble(claimBounds.minZ);
		buf.writeDouble(claimBounds.maxX);
		buf.writeDouble(claimBounds.maxY);
		buf.writeDouble(claimBounds.maxZ);
		buf.writeInt(claimPermissions.size());
		for (String s : claimPermissions.keySet()){
			ByteBufUtils.writeUTF8String(buf, s);
			buf.writeInt(claimPermissions.get(s).getMask());
		}
	}
	
	/**
	 * Reads a claim from a byte buffer
	 */
	public void readFromByteBuf(ByteBuf buf){
		claimOwner = ByteBufUtils.readUTF8String(buf);
		claimBounds = AxisAlignedBB.getBoundingBox(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),buf.readDouble(), buf.readDouble());
		claimPermissions = new HashMap<String, PermissionsMutex>();
		int numPermRecords = buf.readInt();
		for (int i = 0; i < numPermRecords; ++i){
			String s = ByteBufUtils.readUTF8String(buf);
			int mask = buf.readInt();
			claimPermissions.put(s, new PermissionsMutex(mask));
		}
	}

	
	/**
	 * Gets the current permissions mask
	 */
	public int getPermissionMask(String identifier) {
		if (!this.claimPermissions.containsKey(identifier))
			return 0;
		return this.claimPermissions.get(identifier).getMask();
	}

	public AxisAlignedBB getBounds() {
		return claimBounds.copy();
	}

	public void setClaimOwner(String newOwner) {
		this.claimOwner = newOwner;
	}
}