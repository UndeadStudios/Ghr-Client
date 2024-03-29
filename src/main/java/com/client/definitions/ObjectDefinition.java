package com.client.definitions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import com.client.sign.Signlink;
import org.apache.commons.lang3.StringUtils;

import com.client.Frame;
import com.client.Client;
import com.client.MRUNodes;
import com.client.Model;
import com.client.OnDemandFetcher;
import com.client.Buffer;
import com.client.FileArchive;

public final class ObjectDefinition {

	private int opcode61;
	private String opcode150;


	public static ObjectDefinition forID(int i) {
		if (i > streamIndices.length)
			i = streamIndices.length - 2;

		//if (i == 25913 || i == 25916 || i == 25917)
			//i = 15552;
		if(i == 25913)
			i = 15552;

		if(i == 25916)
			i = 15553;

		if(i == 25917)
			i = 15554;
		for (int j = 0; j < 20; j++)
			if (cache[j].type == i)
				return cache[j];

		cacheIndex = (cacheIndex + 1) % 20;
		ObjectDefinition objectDef = cache[cacheIndex];
		stream.currentPosition = streamIndices[i];
		objectDef.type = i;
		objectDef.setDefaults();
		objectDef.readValues(stream);

		if (i >= 26281 && i <= 26290) {
			objectDef.actions = new String[] { "Choose", null, null, null, null };
		}
		switch (i) {
			case 10060:
			case 10061:
			case 30390:
				objectDef.name = "Trading Post booth";
				break;


		}
		if (Client.debugModels) {

			if (objectDef.name == null || objectDef.name.equalsIgnoreCase("null"))
				objectDef.name = "test";

			objectDef.isInteractive = true;
		}
		return objectDef;
	}

	public static void dumpList() {
		try {
			FileWriter fw = new FileWriter(System.getProperty("user.home") + "/Desktop/object_data.json");
			fw.write("[\n");
			for (int i = 0; i < totalObjects; i++) {
				ObjectDefinition def = ObjectDefinition.forID(i);
				String output = "[\"" + StringUtils.join(def.actions, "\", \"") + "\"],";

				String finalOutput = "	{\n" + "		\"id\": " + def.type + ",\n		" + "\"name\": \"" + def.name
						+ "\",\n		\"models\": " + Arrays.toString(def.anIntArray773) + ",\n		\"actions\": "
						+ output.replaceAll(", \"\"]", ", \"Examine\"]").replaceAll("\"\"", "null")
								.replace("[\"null\"]", "[null, null, null, null, \"Examine\"]")
								.replaceAll(", \"Remove\"", ", \"Remove\", \"Examine\"")
						+ "	\n		\"width\": " + def.width + "\n	},";
				fw.write(finalOutput.replaceAll("\"name\": \"null\",", "\"name\": null,"));

				// .replaceAll("\"name\": \"null\",", "\"name\": null,")
				fw.write(System.getProperty("line.separator"));
			}
			fw.write("]");
			fw.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	public static void dumpObjectList() {
		for(int i = 0; i < totalObjects; i++) {
			ObjectDefinition class5 = forID(i);
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(Signlink.getCacheDirectory() + "/dumps/ObjectList196.txt", true));
				if(class5.name!= null) {
					bw.write("ID: "+i+" varbit :"+class5.anInt774+" varp: "+class5.anInt749);
					bw.newLine();
					bw.flush();
					bw.close();
				}
			} catch (IOException ioe2) {
			}
		}
	}
	public static void dumpopendoors() {
		for(int i = 0; i < totalObjects; i++) {
			ObjectDefinition class5 = forID(i);
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(Signlink.getCacheDirectory() + "/dumps/opendoor.txt", true));
				if(class5.name.equalsIgnoreCase("door") || class5.actions[1].equalsIgnoreCase("close")) {
					bw.write(i+", ");
					bw.flush();
					bw.close();
				}
			} catch (IOException ioe2) {
			}
		}
	}
	private void setDefaults() {
		anIntArray773 = null;
		anIntArray776 = null;
		name = null;
		description = null;
		modifiedModelColors = null;
		originalModelColors = null;
		originalTexture = null;
		modifiedTexture = null;
		objectSizeX = 1;
		objectSizeY = 1;
		solid = true;
		impenetrable = true;
		isInteractive = false;
		contouredGround = false;
		delayShading = false;
		occludes = false;
		animation = -1;
		decorDisplacement = 16;
		ambientLighting = 0;
		lightDiffusion = 0;
		actions = null;
		AreaType = -1;
		mapscene = -1;
		aBoolean751 = false;
		castsShadow = true;
		thickness = 128;
		height = 128;
		width = 128;
		surroundings = 0;
		anInt738 = 0;
		anInt745 = 0;
		anInt783 = 0;
		obstructsGround = false;
		aBoolean766 = false;
		field3621 = true;
		supportItems = -1;
		anInt774 = -1;
		anInt749 = -1;
		childrenIDs = null;
	}

	public void method574(OnDemandFetcher class42_sub1) {
		if (anIntArray773 == null)
			return;
		for (int j = 0; j < anIntArray773.length; j++)
			class42_sub1.method560(anIntArray773[j] & 0xffff, 0);
	}

	public static void nullLoader() {
		mruNodes1 = null;
		mruNodes2 = null;
		streamIndices = null;
		cache = null;
		stream = null;
	}

	public static int totalObjects;

	public static void unpackConfig(FileArchive streamLoader) {
		stream = new Buffer(streamLoader.readFile("loc.dat"));
		Buffer stream = new Buffer(streamLoader.readFile("loc.idx"));
		totalObjects = stream.readUnsignedShort();
		streamIndices = new int[totalObjects];
		int i = 2;
		for (int j = 0; j < totalObjects; j++) {
			streamIndices[j] = i;
			i += stream.readUnsignedShort();
		}
		cache = new ObjectDefinition[20];
		for (int k = 0; k < 20; k++)
			cache[k] = new ObjectDefinition();
		//dumpList();
		//dumpObjectList();
		//dumpopendoors();
	}

	public boolean method577(int i) {
		if (anIntArray776 == null) {
			if (anIntArray773 == null)
				return true;
			if (i != 10)
				return true;
			boolean flag1 = true;
			Model model = (Model) mruNodes2.insertFromCache(type);
			for (int k = 0; k < anIntArray773.length; k++)
				flag1 &= Model.isCached(anIntArray773[k] & 0xffff);
			return flag1;
		}
		Model model = (Model) mruNodes2.insertFromCache(type);
		for (int j = 0; j < anIntArray776.length; j++)
			if (anIntArray776[j] == i)
				return Model.isCached(anIntArray773[j] & 0xffff);
		return true;
	}

	public Model modelAt(int i, int j, int k, int l, int i1, int j1, int k1) {
		Model model = method581(i, k1, j);
		if (model == null)
			return null;
		if (contouredGround || delayShading)
			model = new Model(contouredGround, delayShading, model);
		if (contouredGround) {
			int l1 = (k + l + i1 + j1) / 4;
			for (int i2 = 0; i2 < model.verticesCount; i2++) {
				int j2 = model.verticesX[i2];
				int k2 = model.verticesZ[i2];
				int l2 = k + ((l - k) * (j2 + 64)) / 128;
				int i3 = j1 + ((i1 - j1) * (j2 + 64)) / 128;
				int j3 = l2 + ((i3 - l2) * (k2 + 64)) / 128;
				model.verticesY[i2] += j3 - l1;
			}

			model.computeSphericalBounds();
		}
		return model;
	}

	public boolean method579() {
		if (anIntArray773 == null)
			return true;
		boolean flag1 = true;
		for (int i = 0; i < anIntArray773.length; i++)
			flag1 &= Model.isCached(anIntArray773[i] & 0xffff);
		return flag1;
	}

	public ObjectDefinition method580() {
		int i = -1;
		if (anInt774 != -1) {
			VarBit varBit = VarBit.cache[anInt774];
			int j = varBit.anInt648;
			int k = varBit.anInt649;
			int l = varBit.anInt650;
			int i1 = Client.anIntArray1232[l - k];
			i = clientInstance.variousSettings[j] >> k & i1;
		} else if (anInt749 != -1)
			i = clientInstance.variousSettings[anInt749];
		int var2;
		if(i >= 0 && i < childrenIDs.length)
			var2 = childrenIDs[i];
		else
			var2 = childrenIDs[childrenIDs.length - 1];
		
			return var2 == -1 ? null : forID(var2);
	}

	private Model method581(int j, int k, int l) {
		Model model = null;
		long l1;
		if (anIntArray776 == null) {
			if (j != 10)
				return null;
			l1 = (type << 6) + l + ((long) (k + 1) << 32);
			Model model_1 = (Model) mruNodes2.insertFromCache(l1);
			if (model_1 != null)
				return model_1;
			if (anIntArray773 == null)
				return null;
			boolean flag1 = aBoolean751 ^ (l > 3);
			int k1 = anIntArray773.length;
			for (int i2 = 0; i2 < k1; i2++) {
				int l2 = anIntArray773[i2];
				if (flag1)
					l2 += 0x10000;
				model = (Model) mruNodes1.insertFromCache(l2);
				if (model == null) {
					model = Model.getModel(l2 & 0xffff);
					if (model == null)
						return null;
					if (flag1)
						model.method477();
					mruNodes1.removeFromCache(model, l2);
				}
				if (k1 > 1)
					aModelArray741s[i2] = model;
			}

			if (k1 > 1)
				model = new Model(k1, aModelArray741s);
		} else {
			int i1 = -1;
			for (int j1 = 0; j1 < anIntArray776.length; j1++) {
				if (anIntArray776[j1] != j)
					continue;
				i1 = j1;
				break;
			}

			if (i1 == -1)
				return null;
			l1 = (type << 8) + (i1 << 3) + l + ((long) (k + 1) << 32);
			Model model_2 = (Model) mruNodes2.insertFromCache(l1);
			if (model_2 != null)
				return model_2;
			int j2 = anIntArray773[i1];
			boolean flag3 = aBoolean751 ^ (l > 3);
			if (flag3)
				j2 += 0x10000;
			model = (Model) mruNodes1.insertFromCache(j2);
			if (model == null) {
				model = Model.getModel(j2 & 0xffff);
				if (model == null)
					return null;
				if (flag3)
					model.method477();
				mruNodes1.removeFromCache(model, j2);
			}
		}
		boolean flag;
		flag = thickness != 128 || height != 128 || width != 128;
		boolean flag2;
		flag2 = anInt738 != 0 || anInt745 != 0 || anInt783 != 0;
		Model model_3 = new Model(modifiedModelColors == null, Frame.noAnimationInProgress(k),
				l == 0 && k == -1 && !flag && !flag2, modifiedTexture == null,  model);
		if (k != -1) {
			model_3.skin();
			model_3.applyTransform(k);
			model_3.faceGroups = null;
			model_3.vertexGroups = null;
		}
		while (l-- > 0)
			model_3.rotate90Degrees();
		if (modifiedModelColors != null) {
			for (int k2 = 0; k2 < modifiedModelColors.length; k2++) {
				model_3.recolor(modifiedModelColors[k2], originalModelColors[k2]);
			}

		}
		if (originalTexture != null) {
			for (int k2 = 0; k2 < originalTexture.length; k2++) {
				model_3.retexture(originalTexture[k2], modifiedTexture[k2]);
			}

		}
		if (flag)
			model_3.scale(thickness, width, height);
		if (flag2)
			model_3.translate(anInt738, anInt745, anInt783);
		// model_3.method479(64 + aByte737, 768 + aByte742 * 5, -50, -10, -50,
		// !aBoolean769);
		// ORIGINAL^

		model_3.light(85 + ambientLighting,    768 + lightDiffusion  * 25, -50, -10, -50, !delayShading);

		if (supportItems == 1)
			model_3.itemDropHeight = model_3.modelHeight;
		mruNodes2.removeFromCache(model_3, l1);
		return model_3;
	}

	public void readValues(Buffer stream) {
		int flag = -1;
		do {
			int type = stream.readUnsignedByte();
			if (type == 0)
				break;
			if (type == 1) {
				int len = stream.readUnsignedByte();
				if (len > 0) {
					if (anIntArray773 == null || lowMem) {
						anIntArray776 = new int[len];
						anIntArray773 = new int[len];
						for (int k1 = 0; k1 < len; k1++) {
							anIntArray773[k1] = stream.readUnsignedShort();
							anIntArray776[k1] = stream.readUnsignedByte();
						}
					} else {
						stream.currentPosition += len * 3;
					}
				}
			} else if (type == 2)
				name = stream.readString();
			else if (type == 3)
				description = stream.readString();
			else if (type == 5) {
				int len = stream.readUnsignedByte();
				if (len > 0) {
					if (anIntArray773 == null || lowMem) {
						anIntArray776 = null;
						anIntArray773 = new int[len];
						for (int l1 = 0; l1 < len; l1++)
							anIntArray773[l1] = stream.readUnsignedShort();
					} else {
						stream.currentPosition += len * 2;
					}
				}
			} else if (type == 14)
				objectSizeX = stream.readUnsignedByte();
			else if (type == 15)
				objectSizeY = stream.readUnsignedByte();
			else if (type == 17)
				solid = false;
			else if (type == 18)
				impenetrable = false;
			else if (type == 19)
				isInteractive = (stream.readUnsignedByte() == 1);
			else if (type == 21)
				contouredGround = true;
			else if (type == 22)
				delayShading = true;
			else if (type == 23)
				occludes = true;
			else if (type == 24) { // Object Animations
				animation = stream.readUnsignedShort();
				if (animation == 65535)
					animation = -1;
			} else if (type == 28)
				decorDisplacement = stream.readUnsignedByte();
			else if (type == 29)
				ambientLighting = stream.readSignedByte();
			else if (type == 39)
				lightDiffusion = stream.readSignedByte();
			else if (type >= 30 && type < 39) {
				if (actions == null)
					actions = new String[10];
				actions[type - 30] = stream.readString();
				if (actions[type - 30].equalsIgnoreCase("hidden"))
					actions[type - 30] = null;
			} else if (type == 40) {
				int i1 = stream.readUnsignedByte();
				modifiedModelColors = new int[i1];
				originalModelColors = new int[i1];
				for (int i2 = 0; i2 < i1; i2++) {
					modifiedModelColors[i2] = stream.readUnsignedShort();
					originalModelColors[i2] = stream.readUnsignedShort();
				}
			} else if (type == 41) {
				int i1 = stream.readUnsignedByte();
				originalTexture = new short[i1];
				modifiedTexture = new short[i1];
				for (int i2 = 0; i2 < i1; i2++) {
					originalTexture[i2] = (short) stream.readUnsignedShort();
					modifiedTexture[i2] = (short) stream.readUnsignedShort();
				}
				} else if (type == 61) {
				opcode61 = stream.readUnsignedShort();
			} else if (type == 62)
				aBoolean751 = true;
			else if (type == 64)
				castsShadow = false;
			else if (type == 65)
				thickness = stream.readUnsignedShort();
			else if (type == 66)
				height = stream.readUnsignedShort();
			else if (type == 67)
				width = stream.readUnsignedShort();
			else if (type == 68)
				mapscene = stream.readUnsignedShort();
			else if (type == 69)
				surroundings = stream.readUnsignedByte();
			else if (type == 70)
				anInt738 = stream.readSignedWord();
			else if (type == 71)
				anInt745 = stream.readSignedWord();
			else if (type == 72)
				anInt783 = stream.readSignedWord();
			else if (type == 73)
				obstructsGround = true;
			else if (type == 74)
				aBoolean766 = true;
			else if (type == 75)
				supportItems = stream.readUnsignedByte();
			else if (type == 77 || type == 92) {
				anInt774 = stream.readUnsignedShort();
				if (anInt774 == 65535)
					anInt774 = -1;
				anInt749 = stream.readUnsignedShort();
				if (anInt749 == 65535)
					anInt749 = -1;
				int var3 = -1;
				if(type == 92) {
					var3 = stream.readUnsignedShort();
				}
				int j1 = stream.readUnsignedByte();
				childrenIDs = new int[j1 + 2];
				for (int j2 = 0; j2 <= j1; j2++) {
					childrenIDs[j2] = stream.readUnsignedShort();
					if (childrenIDs[j2] == 65535)
						childrenIDs[j2] = -1;
				}

				childrenIDs[j1 + 1] = var3;
			} else if(type == 78) {//TODO Figure out what these do in OSRS
				//First short = ambient sound
            	stream.skip(3);
			} else if(type == 79) {
				stream.skip(5);
				int count = stream.readSignedByte();
				stream.skip(2 * count);
			} else if(type == 81) {
				stream.skip(1);//Clip type?
			} else if (type == 82) {
				AreaType = stream.readUnsignedShort();//AreaType
			} else if(type == 89) {
				field3621 = false;
			} else if(type == 249) {
				int var1 = stream.readUnsignedByte();
				for(int var2 = 0;var2<var1;var2++) {
					boolean b = stream.readUnsignedByte() == 1;
					stream.skip(3);
					if(b) {
						stream.readString();
					} else {
						stream.readDWord();
					}
				}
			}
		} while (true);
		if (flag == -1 && name != "null" && name != null) {
			isInteractive = anIntArray773 != null && (anIntArray776 == null || anIntArray776[0] == 10);
			if (actions != null)
				isInteractive = true;
		}
		if (aBoolean766) {
			solid = false;
			impenetrable = false;
		}
		if (supportItems == -1)
			supportItems = solid ? 1 : 0;
	}

	private ObjectDefinition() {
		type = -1;
	}

	private short[] originalTexture;
	private short[] modifiedTexture;
	public boolean obstructsGround;
	@SuppressWarnings("unused")
	private byte lightDiffusion;
	@SuppressWarnings("unused")
	private byte ambientLighting;
	private int anInt738;
	public String name;
	private int width;
	private static final Model[] aModelArray741s = new Model[4];
	public int objectSizeX;
	private int anInt745;
	public int AreaType;
	private int[] originalModelColors;
	private int thickness;
	public int anInt749;
	private boolean aBoolean751;
	public static boolean lowMem;
	private static Buffer stream;
	public int type;
	public static int[] streamIndices;
	public boolean impenetrable;
	public int mapscene;
	public int childrenIDs[];
	public int supportItems;
	public int objectSizeY;
	public boolean contouredGround;
	public boolean occludes;
	public static Client clientInstance;
	private boolean aBoolean766;
	public boolean solid;
	public int surroundings;
	private boolean delayShading;
	private static int cacheIndex;
	private int height;
	public int[] anIntArray773;
	public int anInt774;
	public int decorDisplacement;
	private int[] anIntArray776;
	public String description;
	public boolean isInteractive;
	public boolean castsShadow;
	public static MRUNodes mruNodes2 = new MRUNodes(30);
	public int animation;
	private static ObjectDefinition[] cache;
	private int anInt783;
	private int[] modifiedModelColors;
	public static MRUNodes mruNodes1 = new MRUNodes(500);
	public String actions[];
	public boolean field3621;
}
