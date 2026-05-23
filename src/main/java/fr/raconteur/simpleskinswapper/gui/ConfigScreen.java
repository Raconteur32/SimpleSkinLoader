package fr.raconteur.simpleskinswapper.gui;

import fr.raconteur.simpleskinswapper.config.SimpleSkinSwapperConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.MutableText;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private final Screen parent;
    /** Null when not connected to a multiplayer server. */
    private final String currentServerAddress;
    private TextFieldWidget serverCommandField;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("simpleskinswapper.config.title"));
        this.parent = parent;
        ServerInfo serverInfo = net.minecraft.client.MinecraftClient.getInstance().getCurrentServerEntry();
        this.currentServerAddress = serverInfo != null ? serverInfo.address : null;
    }

    @Override
    protected void init() {
        SimpleSkinSwapperConfig config = SimpleSkinSwapperConfig.get();

        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Title
        this.addDrawableChild(new MultilineTextWidget(centerX - 150, 14, this.title, this.textRenderer)
                .setMaxWidth(300)
                .setCentered(true));

        if (currentServerAddress != null) {
            // Label: "Command for: example.com"
            this.addDrawableChild(new MultilineTextWidget(
                    centerX - 150, startY,
                    Text.translatable("simpleskinswapper.config.server_command.for", currentServerAddress),
                    this.textRenderer)
                    .setMaxWidth(300)
                    .setCentered(true));

            // Command input field
            this.serverCommandField = new TextFieldWidget(
                    this.textRenderer, centerX - 150, startY + 20, 300, 20,
                    Text.translatable("simpleskinswapper.config.server_command"));
            String existing = config.getCommandForServer(currentServerAddress);
            this.serverCommandField.setText(existing != null ? existing : "");
            this.serverCommandField.setMaxLength(256);
            this.addDrawableChild(this.serverCommandField);
        } else {
            // Not connected — explain the feature
            MutableText notConnectedText = Text.translatable("simpleskinswapper.config.server_command.not_connected.line1")
                    .append("\n")
                    .append(Text.translatable("simpleskinswapper.config.server_command.not_connected.line2"))
                    .append("\n")
                    .append(Text.translatable("simpleskinswapper.config.server_command.not_connected.line3"))
                    .append("\n")
                    .append(Text.translatable("simpleskinswapper.config.server_command.not_connected.line4"));
            this.addDrawableChild(new MultilineTextWidget(centerX - 150, startY, notConnectedText, this.textRenderer)
                    .setMaxWidth(300)
                    .setCentered(true));
        }

        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("simpleskinswapper.config.save"),
                button -> {
                    if (currentServerAddress != null) save();
                    this.client.setScreen(parent);
                })
                .dimensions(centerX - 100, this.height - 30, 95, 20)
                .build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                ScreenTexts.CANCEL,
                button -> this.client.setScreen(parent))
                .dimensions(centerX + 5, this.height - 30, 95, 20)
                .build());
    }

    private void save() {
        SimpleSkinSwapperConfig config = SimpleSkinSwapperConfig.get();
        config.serverCommands.put(currentServerAddress, this.serverCommandField.getText());
        SimpleSkinSwapperConfig.save();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
