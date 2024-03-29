package com.client;

import com.client.definitions.AnimationDefinition;
import com.client.definitions.NpcDefinition;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
import com.client.definitions.GraphicsDefinition;

public final class NPC extends Entity {

	private Model method450() {
		if (super.anim >= 0 && super.anInt1529 == 0) {
			int k = AnimationDefinition.anims[super.anim].primaryFrames[super.anInt1527];
			int i1 = -1;
			if (super.queued_animation_id >= 0 && super.queued_animation_id != super.idle_animation_id)
				i1 = AnimationDefinition.anims[super.queued_animation_id].primaryFrames[super.anInt1518];
			return desc.method164(i1, k,
					AnimationDefinition.anims[super.anim].anIntArray357);
		}
		int l = -1;
		if (super.queued_animation_id >= 0)
			l = AnimationDefinition.anims[super.queued_animation_id].primaryFrames[super.anInt1518];
		return desc.method164(-1, l, null);
	}

	@Override
	public Model getRotatedModel() {
		if (desc == null)
			return null;
		Model model = method450();
		if (model == null)
			return null;
		super.height = model.modelHeight;
		if (super.anInt1520 != -1 && super.currentAnimation != -1) {
			GraphicsDefinition spotAnim = GraphicsDefinition.cache[super.anInt1520];
			Model model_1 = spotAnim.getModel();
			if (model_1 != null) {
				int j = spotAnim.aAnimation_407.primaryFrames[super.currentAnimation];
				Model model_2 = new Model(true, Frame.noAnimationInProgress(j), false, model_1);
				model_2.translate(0, -super.anInt1524, 0);
				model_2.skin();
				model_2.applyTransform(j);
				model_2.faceGroups = null;
				model_2.vertexGroups = null;
				if (spotAnim.anInt410 != 128 || spotAnim.anInt411 != 128)
					model_2.scale(spotAnim.anInt410, spotAnim.anInt410,
							spotAnim.anInt411);
				model_2.light(64 + spotAnim.anInt413,
						850 + spotAnim.anInt414, -30, -50, -30, true);
				Model aModel[] = { model, model_2 };
				model = new Model(aModel);
			}
		}
		if (desc.boundDim == 1)
			model.fits_on_single_square = true;
		return model;
	}

	@Override
	public boolean isVisible() {
		return desc != null;
	}

	NPC() {
	}
	public boolean isShowMenuOnHover() {
		return npcPetType == 0 || npcPetType == 2;
	}
	public int npcPetType;
	public NpcDefinition desc;
}
