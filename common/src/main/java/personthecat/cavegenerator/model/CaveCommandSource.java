package personthecat.cavegenerator.model;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class CaveCommandSource {

    private static final String EXECUTOR = "Cave Executor";

    private final AtomicReference<CommandSourceStack> source = new AtomicReference<>();

    @Nullable
    public CommandSourceStack get() {
        return this.source.get();
    }

    public void create(final MinecraftServer server) {
        final ServerLevel overworld = server.overworld();
        final Vec3 pos = overworld == null ? Vec3.ZERO : Vec3.atLowerCornerOf(overworld.getSharedSpawnPos());
        final TextComponent title = new TextComponent(EXECUTOR);
        final CommandSourceStack source =
            new CommandSourceStack(server, pos, Vec2.ZERO, overworld, 4, EXECUTOR, title, server, null);
        this.source.set(source);
    }

    public void clear() {
        this.source.set(null);
    }
}
