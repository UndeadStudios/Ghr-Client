package com.client;

import com.client.definitions.AnimationDefinition;
import com.client.sound.Sound;
import com.client.sound.SoundType;

public class Entity extends Renderable {



	public boolean isLocalPlayer() {
		return this == Client.local_player;
	}

	public int getAbsoluteX() {
		int x = Client.baseX + (this.world_x - 6 >> 7);
		if (this instanceof NPC) {
			return x - ((NPC) this).desc.boundDim / 2;
		}
		return x;
	}

	public int getAbsoluteY() {
		int y = Client.baseY + (this.world_y - 6 >> 7);
		if (this instanceof NPC) {
			return y - ((NPC) this).desc.boundDim / 2;
		}
		return y;
	}

	public int getDistanceFrom(Entity entity) {
		return getDistanceFrom(entity.getAbsoluteX(), entity.getAbsoluteY());
	}

	public int getDistanceFrom(int x2, int y2) {
		int x = (int) Math.pow(getAbsoluteX() - x2, 2.0D);
		int y = (int) Math.pow(getAbsoluteY() - y2, 2.0D);
		return (int) Math.floor(Math.sqrt(x + y));
	}

	public void makeSound(int soundId) {
		double distance = getDistanceFrom(Client.local_player);
//		if (Configuration.developerMode) {
//			System.out.println("entity sound: id " + id + " x" + getAbsoluteX() + " y" + getAbsoluteY() + " d" + distance);
//		}
		Sound.getSound().playSound(soundId, isLocalPlayer() || this instanceof NPC ? SoundType.SOUND : SoundType.AREA_SOUND, distance);
	}

	public final void setPos(int i, int j, boolean flag) {
		if (anim != -1 && AnimationDefinition.anims[anim].anInt364 == 1)
			anim = -1;
		if (!flag) {
			int k = i - smallX[0];
			int l = j - smallY[0];
			if (k >= -8 && k <= 8 && l >= -8 && l <= 8) {
				if (smallXYIndex < 9)
					smallXYIndex++;
				for (int i1 = smallXYIndex; i1 > 0; i1--) {
					smallX[i1] = smallX[i1 - 1];
					smallY[i1] = smallY[i1 - 1];
					aBooleanArray1553[i1] = aBooleanArray1553[i1 - 1];
				}

				smallX[0] = i;
				smallY[0] = j;
				aBooleanArray1553[0] = false;
				return;
			}
		}
		smallXYIndex = 0;
		anInt1542 = 0;
		anInt1503 = 0;
		smallX[0] = i;
		smallY[0] = j;
		world_x = smallX[0] * 128 + anInt1540 * 64;
		world_y = smallY[0] * 128 + anInt1540 * 64;
	}

	public final void resetPath() {
		smallXYIndex = 0;
		anInt1542 = 0;
	}

	public final void updateHitData(int j, int k, int l) {
		for (int i1 = 0; i1 < 4; i1++)
			if (hitsLoopCycle[i1] <= l) {
				hitArray[i1] = k;
				hitMarkTypes[i1] = j;
				hitsLoopCycle[i1] = l + 70;
				return;
			}
	}

	public final void moveInDir(boolean flag, int i) {
		int j = smallX[0];
		int k = smallY[0];
		if (i == 0) {
			j--;
			k++;
		}
		if (i == 1)
			k++;
		if (i == 2) {
			j++;
			k++;
		}
		if (i == 3)
			j--;
		if (i == 4)
			j++;
		if (i == 5) {
			j--;
			k--;
		}
		if (i == 6)
			k--;
		if (i == 7) {
			j++;
			k--;
		}
		if (anim != -1 && AnimationDefinition.anims[anim].anInt364 == 1)
			anim = -1;
		if (smallXYIndex < 9)
			smallXYIndex++;
		for (int l = smallXYIndex; l > 0; l--) {
			smallX[l] = smallX[l - 1];
			smallY[l] = smallY[l - 1];
			aBooleanArray1553[l] = aBooleanArray1553[l - 1];
		}
		smallX[0] = j;
		smallY[0] = k;
		aBooleanArray1553[0] = flag;
	}

	public int entScreenX;
	public int entScreenY;
	public final int index = -1;

	public boolean isVisible() {
		return false;
	}

	Entity() {
		smallX = new int[10];
		smallY = new int[10];
		engaged_entity_id = -1;
		anInt1504 = 32;
		anInt1505 = -1;
		height = 200;
		field1133 = -1; // L: 64
		field1185 = -1; // L: 65
		idle_animation_id = -1;
		anInt1512 = -1;
		hitArray = new int[4];
		hitMarkTypes = new int[4];
		hitsLoopCycle = new int[4];
		queued_animation_id = -1;
		anInt1520 = -1;
		anim = -1;
		loopCycleStatus = -1000;
		textCycle = 100;
		anInt1540 = 1;
		aBoolean1541 = false;
		aBooleanArray1553 = new boolean[10];
		anInt1554 = -1;
		anInt1555 = -1;
		anInt1556 = -1;
		anInt1557 = -1;
	}

	public final int[] smallX;
	public final int[] smallY;
	public int engaged_entity_id;
	int anInt1503;
	int anInt1504;
	int anInt1505;
	public String textSpoken;
	public int height;
	public int turnDirection;
	int idle_animation_id;
	int anInt1512;
	int anInt1513;
	final int[] hitArray;
	final int[] hitMarkTypes;
	final int[] hitsLoopCycle;
	int field1133;
	int field1185;
	byte field1146;
	byte field1187;
	byte field1188;
	byte field1189;
	int queued_animation_id;
	int anInt1518;
	int anInt1519;
	int anInt1520;
	int currentAnimation;
	int anInt1522;
	int anInt1523;
	int anInt1524;
	int smallXYIndex;
	public int anim;
	int anInt1527;
	int anInt1528;
	int anInt1529;
	int anInt1530;
	int anInt1531;
	public int loopCycleStatus;
	public int currentHealth;
	public int maxHealth;
	int textCycle;
	int anInt1537;
	int anInt1538;
	int anInt1539;
	int anInt1540;
	boolean aBoolean1541;
	int anInt1542;
	int initialX;
	int destinationX;
	int initialY;
	int destinationY;
	int startForceMovement;
	int endForceMovement;
	int direction;
	public int world_x;
	public int world_y;
	int current_rotation;
	final boolean[] aBooleanArray1553;
	int anInt1554;
	int anInt1555;
	int anInt1556;
	int anInt1557;
}
