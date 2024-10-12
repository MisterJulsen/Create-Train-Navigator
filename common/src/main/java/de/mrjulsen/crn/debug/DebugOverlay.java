package de.mrjulsen.crn.debug;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.trains.entity.Carriage;

import java.lang.StringBuilder;
import java.util.Map;

import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.mixin.TrainStatusAccessor;
import de.mrjulsen.crn.util.ESpeedUnit;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import de.mrjulsen.mcdragonlib.client.gui.DLOverlayScreen;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class DebugOverlay extends DLOverlayScreen {
  
    private static long debugOverlayId = -1;

    public static void toggle() {
        OverlayManager.remove(debugOverlayId);
        if (debugOverlayId == -1) {
            debugOverlayId = OverlayManager.add(new DebugOverlay());
        } else {
            debugOverlayId = -1;
        }
    }

    int line = 0;
    int trainIndex = 0;

    int lastKey = -1;

    @Override
    public void render(Graphics graphics, float partialTicks, int screenWidth, int screenHeight) {
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
        line = 0;
        if (!TrainListener.data.isEmpty()) {
            trainIndex %= TrainListener.data.size();
            TrainData data = TrainListener.data.values().stream().skip(trainIndex).findFirst().get();  
            drawLine(graphics, data.getTrain().name.getString() + " (" + (data.isPreparing() ? "PREPARING" : (data.isInitialized() ? "READY" : "INITIALIZING")) + ") SessionId: " + data.getSessionId());    
            drawLine(graphics, "Display: " + data.getCurrentTravelSection().getDisplayText() + ", Title: " + data.getCurrentTitle() + ", IsDynamic: " + data.isDynamic());
            drawLine(graphics, "Track: " + !((TrainStatusAccessor)data.getTrain().status).crn$track() + ", Conductor: " + !((TrainStatusAccessor)data.getTrain().status).crn$conductor() + ", Navigation: " + !((TrainStatusAccessor)data.getTrain().status).crn$navigation() + ", Paused: " + data.getTrain().runtime.paused + ", Auto: " + data.getTrain().runtime.isAutoSchedule + ", Manual: " + data.isManualControlled + ", Cancelled: " + data.isCancelled());
            drawLine(graphics, "Duration: " + data.getTotalDuration() + ", Ticks: " + data.getTransitTicks() + "/" + data.waitingAtStationTicks() + "/" + data.waitingForSignalTicks + ", Dest: " + (data.getTrain().navigation.destination == null ? "(at station)" : (data.getTrain().navigation.destination.name + ", DestID: " + data.getTrain().navigation.destination.id)) + ", Delay: " + data.getHighestDeviation() + " (-" + data.getDeviationDelayOffset() + "), Status: " + data.debug_statusInfoCount());
            data.getPredictions().forEach(a -> {                
                drawLine(graphics, TextUtils.text(" - ").append(a.formattedText()));
            });

            StringBuilder builder = new StringBuilder();
            builder.append("( " + data.getCurrentScheduleIndex() + " ): ");
            data.getPredictionsChronologically().forEach(a -> {   
                if (a == null) {
                    return;
                }
                builder.append(" > " + a.getStationName() + " (" + a.getRealTimeArrivalTicks() + ")");
            });
            drawLine(graphics, builder.toString());

            boolean stalled= false;
            for (int i = 0; i < data.getTrain().carriages.size(); i++) {
			    Carriage carriage = data.getTrain().carriages.get(i);
                if (carriage.stalled) {
                    stalled = true;
                    break;
                }
            }

            drawLine(graphics, TextUtils.text("Speed: " + (int)ModUtils.calcSpeed(data.getTrain().speed, ESpeedUnit.MS) + " / " + (int)ModUtils.calcSpeed(data.getTrain().targetSpeed, ESpeedUnit.MS) + ", Waiting: " + (data.getTrain().navigation.waitingForSignal == null ? "No" : data.getTrain().navigation.waitingForSignal.getSecond()) + " (" + data.getTrain().navigation.ticksWaitingForSignal + "/" + data.waitingForSignalId + "), Stalled: " + stalled + " (" + data.getTrain().speedBeforeStall + ")").withStyle(data.getTrain().navigation.waitingForSignal == null ? ChatFormatting.RESET : ChatFormatting.RED));

            for (Map.Entry<Integer, Integer> transitTime : data.currentTransitTime.entrySet()) {
                String suffix = "?";
                if (data.transitTimeHistory.containsKey(transitTime.getKey())) {
                    suffix = String.join(" | ", data.transitTimeHistory.get(transitTime.getKey()).stream().map(x -> String.valueOf(x)).toList());
                }
                drawLine(graphics, " - [ " + transitTime.getKey() + " ]: " + transitTime.getValue() + " > " + suffix);
            }
            
            drawLine(graphics, "Sections:");
            for (TrainTravelSection section : data.getSections()) {                
                drawLine(graphics, " - [ " + section.getScheduleIndex() + " ]: " + section.getDisplayText() + " (" + section.getStartStationName() + " -> " + section.getDestinationStationName() + "), Group: " + (section.getTrainGroup() == null ? "none" : section.getTrainGroup().getGroupName()) + ", Line: " + (section.getTrainLine() == null ? "none" : section.getTrainLine().getLineName()) + ", Include: " + section.shouldIncludeNextStationOfNextSection() + ", Navigable: " + section.isUsable() + ", Next: " + section.nextSection().getScheduleIndex());
            }
        }
        drawLine(graphics, TextUtils.text("Press K to switch train.").withStyle(ChatFormatting.AQUA));  
        
        graphics.poseStack().popPose();
    }
    
    private void drawLine(Graphics graphics, String str) {
        drawLine(graphics, TextUtils.text(str));
    }
    
    private void drawLine(Graphics graphics, Component str) {
        int x = 2;
        int y = 2;
        GuiUtils.fill(graphics, x - 1, y - 1 + line * (getFont().lineHeight + 2), getFont().width(str) + 2, getFont().lineHeight + 2, 0x44000000);
        GuiUtils.drawString(graphics, getFont(), x, y + 1 + line * (getFont().lineHeight + 2), str, 0xFFFFFFFF, EAlignment.LEFT, true);
        line++;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (lastKey != pKeyCode) {
            lastKey = pKeyCode;
        } else if (lastKey == pKeyCode){        
            lastKey = GLFW.GLFW_KEY_UNKNOWN;    
            if (pKeyCode == GLFW.GLFW_KEY_K) {
                trainIndex++;
                return true;
            }
        }
        if (pKeyCode == GLFW.GLFW_KEY_P) {
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }    

}
