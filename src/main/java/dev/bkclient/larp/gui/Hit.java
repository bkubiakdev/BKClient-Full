package dev.bkclient.larp.gui;

import dev.bkclient.larp.gui.Category;
import dev.bkclient.larp.gui.ModOption;
import dev.bkclient.larp.gui.Setting;

public record Hit(Category category, ModOption option, Setting setting, boolean header) {
}
