package com.pumpkiiings.pkanticheat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class PkConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue maxStacks;
        public final ForgeConfigSpec.ConfigValue<String> alertFormat;
        public final ForgeConfigSpec.DoubleValue maxReachDistance;

        public final ForgeConfigSpec.DoubleValue killauraDeviationThreshold;
        public final ForgeConfigSpec.DoubleValue noslowMaxSpeed;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("AlertSystem");
            maxStacks = builder
                    .comment("Number of violations required before an alert is broadcasted to operators.")
                    .defineInRange("maxStacks", 10, 1, Integer.MAX_VALUE);
            alertFormat = builder
                    .comment("Format of the alert message. %player% = Player Name, %check% = Check Name, %stacks% = Stacks amount. Use & for colors.")
                    .define("alertFormat", "&8[&cPkAnticheat&8] &c%player% &ffailed in &e%check% &8(&cx%stacks%&8)");
            builder.pop();

            builder.push("Checks");
            maxReachDistance = builder
                    .comment("Maximum distance allowed for reaching an entity.")
                    .defineInRange("maxReachDistance", 3.5, 1.0, 10.0);
            
            killauraDeviationThreshold = builder
                    .comment("Maximum allowed deviation in yaw/pitch (heuristic). Lower means more strict.")
                    .defineInRange("killauraDeviationThreshold", 0.05, 0.001, 1.0);
                    
            noslowMaxSpeed = builder
                    .comment("Maximum blocks per tick allowed while consuming an item/blocking.")
                    .defineInRange("noslowMaxSpeed", 0.1, 0.01, 1.0);
            builder.pop();
        }
    }
}
