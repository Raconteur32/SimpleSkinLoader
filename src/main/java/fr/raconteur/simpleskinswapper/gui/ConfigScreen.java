package fr.raconteur.simpleskinswapper.gui;

import fr.raconteur.simpleskinswapper.config.SimpleSkinSwapperConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget serverCommandField;
    private boolean commandEnabled;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("simpleskinswapper.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SimpleSkinSwapperConfig config = SimpleSkinSwapperConfig.get();
        this.commandEnabled = config.serverCommandEnabled;

        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Server command enabled toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("simpleskinswapper.config.server_command_enabled")
                        .append(": ")
                        .append(Text.literal(commandEnabled ? "ON" : "OFF")),
                button -> {
                    this.commandEnabled = !this.commandEnabled;
                    button.setMessage(Text.translatable("simpleskinswapper.config.server_command_enabled")
                            .append(": ")
                            .append(Text.literal(commandEnabled ? "ON" : "OFF")));
                })
                .dimensions(centerX - 100, startY, 200, 20)
                .build());

        // Server command text field
        this.serverCommandField = new TextFieldWidget(
                this.textRenderer, centerX - 150, startY + 40, 300, 20,
                Text.translatable("simpleskinswapper.config.server_command"));
        this.serverCommandField.setText(config.serverCommand);
        this.serverCommandField.setMaxLength(256);
        this.addDrawableChild(this.serverCommandField);

        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save"),
                button -> {
                    save();
                    this.client.setScreen(parent);
                })
                .dimensions(centerX - 100, this.height - 30, 95, 20)
                .build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> this.client.setScreen(parent))
                .dimensions(centerX + 5, this.height - 30, 95, 20)
                .build());
    }

    private void save() {
        SimpleSkinSwapperConfig config = SimpleSkinSwapperConfig.get();
        config.serverCommandEnabled = this.commandEnabled;
        config.serverCommand = this.serverCommandField.getText();
        SimpleSkinSwapperConfig.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("simpleskinswapper.config.server_command"),
                this.width / 2 - 150, this.height / 4 + 28, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
