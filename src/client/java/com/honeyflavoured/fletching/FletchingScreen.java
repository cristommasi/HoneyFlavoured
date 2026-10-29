package com.honeyflavoured.fletching;

import com.honeyflavoured.HoneyFlavoured;


import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "textures/gui/fletching/container/fletching_table.png");

	/* Canvas size of the png, and GUI size inside it (top-left corner). */
	private static final int TEX_W = 256;
	private static final int TEX_H = 256;
	private static final int GUI_W = 176;
	private static final int GUI_H = 166;

	public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, GUI_W, GUI_H, TEX_W, TEX_H);
	}
}