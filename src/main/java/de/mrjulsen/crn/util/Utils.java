package de.mrjulsen.crn.util;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec3;

public class Utils {

    // Debugging
    private static final boolean DEBUG_RETURN_TICKS = false;

    public static int shiftTimeToMinecraftTicks(int time) {
        time = (time - 6000) % Constants.TICKS_PER_DAY;
        if (time < 0) {
            time += Constants.TICKS_PER_DAY;
        }
        return time;
    }

    public static String parseTimeWithoutCorrection(int time) {
        if (DEBUG_RETURN_TICKS) {
            return String.valueOf(time);
        }

        if (time < 0) {
            return "--:--";
        }

        time = time % Constants.TICKS_PER_DAY;
        int hours = time / 1000;
        int minutes = time % 1000;
        minutes = (int)(minutes / (1000.0D / 60.0D));
        return String.format("%02d:%02d", hours, minutes);
    }
    
    public static String parseTime(int time) {
        if (DEBUG_RETURN_TICKS) {
            return String.valueOf(time);
        }

        if (time < 0) {
            return "--:--";
        }

        time = (time + 6000) % Constants.TICKS_PER_DAY;
        int hours = time / 1000;
        int minutes = time % 1000;
        minutes = (int)(minutes / (1000.0D / 60.0D));
        return String.format("%02d:%02d", hours, minutes);
    }

    public static String parseDuration(int time) {
        if (DEBUG_RETURN_TICKS) {
            return String.valueOf(time);
        }

        if (time < 0) {
            return "-";
        }

        time = time % Constants.TICKS_PER_DAY;
        int days = time / Constants.TICKS_PER_DAY;
        int hours = time / 1000;
        int minutes = time % 1000;
        minutes = (int)(minutes / (1000.0D / 60.0D));
        if (hours <= 0 && days <= 0) { 
            return new TranslatableComponent("gui." + ModMain.MOD_ID + ".time_format.m", minutes).getString();
        } else if (days <= 0) { 
            return new TranslatableComponent("gui." + ModMain.MOD_ID + ".time_format.hm", hours, minutes).getString();
        } else { 
            return new TranslatableComponent("gui." + ModMain.MOD_ID + ".time_format.dhm", days, hours, minutes).getString();
        }
    }

    public static String parseDurationShort(int time) {
        if (DEBUG_RETURN_TICKS) {
            return String.valueOf(time);
        }
        
        if (time < 0) {
            return "-";
        }

        int days = time / Constants.TICKS_PER_DAY;
        time = time % Constants.TICKS_PER_DAY;
        int hours = time / 1000;
        int minutes = time % 1000;
        minutes = (int)(minutes / (1000.0D / 60.0D));
        if (hours <= 0 && days <= 0) { 
            return String.format("%sm", minutes);
        } else if (days <= 0) { 
            return String.format("%sh %sm", hours, minutes);
        } else { 
            return String.format("%sd %sh %sm", days, hours, minutes);
        }
    }

    public static Vec3i blockPosToVec3i(BlockPos pos) {
        return new Vec3i(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 blockPosToVec3(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }
}
