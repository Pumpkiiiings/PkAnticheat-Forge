package com.pumpkiiings.pkanticheat.checks.world;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ScaffoldCheck extends Check {

    public ScaffoldCheck() {
        super("Scaffold");
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;

        PlayerData data = PlayerDataManager.getData(player);

        float pitch = player.getXRot();
        boolean placingUnderFeet = (event.getPos().getY() < player.getY() && Math.abs(event.getPos().getX() - player.getX()) < 1.5 && Math.abs(event.getPos().getZ() - player.getZ()) < 1.5);
        boolean sprinting = player.isSprinting();

        // Scaffold hacks suelen colocar bloques justo debajo de ellos mismos de forma instantánea 
        // mientras corren, y muchas veces miran directo hacia abajo (Pitch > 75) o tienen ángulos perfectos (Pitch constante).
        
        if (placingUnderFeet && sprinting) {
            // Heurística A: Sprinting & Placing under feet with extreme angles
            if (pitch > 78.0f) {
                // Es muy difícil en Vanilla correr (sprint) hacia adelante y mirar directamente hacia abajo para construir 
                // sin caerse, el jugador normalmente camina lento hacia atrás o hacia los lados.
                fail(player, "Colocación de bloque anormal (Scaffold/Tower) [Sprint+ExtremePitch]");
                setback(player);
                event.setCanceled(true);
                return;
            }
        }

        // Heurística B: Rotación extraña. Scaffold suele forzar el pitch para colocar bloques.
        float pitchDelta = Math.abs(pitch - data.getLastPitch());
        float yawDelta = Math.abs(player.getYRot() - data.getLastYaw());

        // Si hay un giro instantáneo y loco solo en el momento de colocar el bloque (flick)
        if (pitchDelta > 40.0f || (yawDelta > 60.0f && placingUnderFeet)) {
            // Se incrementa un pequeño contador porque puede ser un flick legítimo si es raro
            // pero si se repite en menos de 5 segundos es Scaffold.
            data.scaffoldVL++;
            if (data.scaffoldVL >= 3) {
                fail(player, "Giro instántaneo y colocación (Scaffold/Flick) [AngleSnap]");
                setback(player);
                event.setCanceled(true);
                data.scaffoldVL = 0;
            }
        } else {
            data.scaffoldVL = Math.max(0, data.scaffoldVL - 1);
        }

    }
}
