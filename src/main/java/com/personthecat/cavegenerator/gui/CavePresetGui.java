package com.personthecat.cavegenerator.gui;

import jline.internal.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;

/** Test class. May be deleted. */
public class CavePresetGui extends GuiConfig {
    /** The primary constructor for this GUI. */
    public CavePresetGui(@Nullable CavePresetGui presetGui, String title) {
        super(presetGui, " ", title);
    }

    /** Creates a new instance of this object with no parent GUI. */
    public CavePresetGui(String title) {
        this(null, title);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(1000, 2, 2, 100, 20, "Test Add"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);

        if (button.id == 1000) {
            Property test = new Property("Test Property", new String[] { "1", "2" }, Property.Type.INTEGER);
            ConfigElement testElement = new ConfigElement(test);
            GuiConfigEntries.StringEntry testEntry = new GuiConfigEntries.StringEntry(this, entryList, testElement);
            entryList.listEntries.add(testEntry);
            updateEntryList();
        }
    }

    private void updateEntryList() {
        entryList.maxLabelTextWidth = 0;
        for (GuiConfigEntries.IConfigEntry entry : entryList.listEntries)
            if (entry.getLabelWidth() > entryList.maxLabelTextWidth)
                entryList.maxLabelTextWidth = entry.getLabelWidth();

        int viewWidth = entryList.maxLabelTextWidth + 8 + (width / 2);
        entryList.labelX = (width / 2) - (viewWidth / 2);
        entryList.controlX = entryList.labelX + entryList.maxLabelTextWidth + 8;
        entryList.resetX = (width / 2) + (viewWidth / 2) - 45;

        entryList.maxEntryRightBound = 0;
        for (GuiConfigEntries.IConfigEntry entry : entryList.listEntries)
            if (entry.getEntryRightBound() > entryList.maxEntryRightBound)
                entryList.maxEntryRightBound = entry.getEntryRightBound();

        entryList.scrollBarX = entryList.maxEntryRightBound + 5;
        entryList.controlWidth = entryList.maxEntryRightBound - entryList.controlX - 45;
    }

    /** Returns the center X coordinate of the current screen. */
    private int centerX() {
        return width / 2;
    }

    /** Returns the centered X coordinate based on the input `width`. */
    public int centerX(int width) {
        return centerX() - width / 2;
    }

    /** Returns the center Y coordinate of the current screen. */
    private int centerY() {
        return height / 2;
    }

    /** Returns the centered Y coordinate based on the input `height`. */
    public int centerY(int height) {
        return centerY() - height / 2;
    }

    /** Neatly adds a label to the list of labels. */
    private void addLabel(GuiLabel label) {
        labelList.add(label);
    }
}
