package net.tropimon.bougesolo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class BougeSoloClient implements ClientModInitializer {

    // 30 secondes pour le test (20 ticks x 30)
    private static final int INTERVAL_TICKS = 20 * 30;

    private final Random random = new Random();

    private int tickCounter = 0;

    // Séquence d'actions générée aléatoirement
    private int[] actionDurations  = new int[0];  // durée en ticks de chaque mouvement
    private float[] actionForward  = new float[0]; // valeur movementForward
    private float[] actionSide     = new float[0]; // valeur movementSideways
    private int totalActionTicks   = 0;
    private int actionTick         = -1;
    private int clicksLeft         = 0;
    private int clickInterval      = 0;
    private int clickTick          = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) return;
            if (client.currentScreen != null) return;

            tickCounter++;

            // Déclencher toutes les 30 secondes
            if (tickCounter >= INTERVAL_TICKS) {
                tickCounter = 0;
                generateSequence();
                actionTick = 0;
            }

            // Exécuter la séquence
            if (actionTick >= 0 && actionTick < totalActionTicks) {
                executeAction(client, player);
                actionTick++;

                // Clics gauches répartis aléatoirement sur la durée
                if (clicksLeft > 0 && actionTick % clickInterval == 0) {
                    client.interactionManager.attackBlock(
                        player.getBlockPos(), Direction.UP
                    );
                    player.swingHand(Hand.MAIN_HAND);
                    clicksLeft--;
                }

                if (actionTick >= totalActionTicks) {
                    actionTick = -1;
                }
            }
        });
    }

    private void generateSequence() {
        // 4 mouvements aléatoires
        int count = 4;
        actionDurations = new int[count];
        actionForward   = new float[count];
        actionSide      = new float[count];

        // Durées possibles : 0.5s=10, 1s=20, 1.5s=30, 2s=40 ticks
        int[] possibleDurations = {10, 20, 30, 40};

        // Directions possibles : avant, arrière, gauche, droite
        float[][] directions = {
            {1.0f, 0.0f},   // avant
            {-1.0f, 0.0f},  // arrière
            {0.0f, 1.0f},   // gauche
            {0.0f, -1.0f}   // droite
        };

        totalActionTicks = 0;
        for (int i = 0; i < count; i++) {
            actionDurations[i] = possibleDurations[random.nextInt(possibleDurations.length)];
            float[] dir = directions[random.nextInt(directions.length)];
            actionForward[i] = dir[0];
            actionSide[i]    = dir[1];
            totalActionTicks += actionDurations[i];
        }

        // Nombre de clics aléatoire entre 3 et 8
        clicksLeft = 3 + random.nextInt(6);
        clickInterval = Math.max(1, totalActionTicks / (clicksLeft + 1));
        clickTick = 0;
    }

    private void executeAction(MinecraftClient client, ClientPlayerEntity player) {
        // Trouver dans quel mouvement on est
        int elapsed = 0;
        for (int i = 0; i < actionDurations.length; i++) {
            elapsed += actionDurations[i];
            if (actionTick < elapsed) {
                player.input.movementForward   = actionForward[i];
                player.input.movementSideways  = actionSide[i];
                break;
            }
        }
    }
}
