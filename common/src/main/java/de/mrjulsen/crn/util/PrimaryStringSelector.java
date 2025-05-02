package de.mrjulsen.crn.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class PrimaryStringSelector {
    
    private final int windowSize;
    private final Queue<String> window;
    private final Map<String, Integer> frequencyMap;
    private String currentPrimary;
    
    public PrimaryStringSelector(int poolSize) {
        this.windowSize = poolSize;
        this.window = new LinkedList<>();
        this.frequencyMap = new HashMap<>();
        this.currentPrimary = null;
    }
    
    public void addString(String s) {
        window.add(s);
        frequencyMap.put(s, frequencyMap.getOrDefault(s, 0) + 1);
        
        if (window.size() > windowSize) {
            String removed = window.poll();
            frequencyMap.put(removed, frequencyMap.get(removed) - 1);
            if (frequencyMap.get(removed) == 0) {
                frequencyMap.remove(removed);
            }
        }
        
        String mostFrequent = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }
        
        if (currentPrimary == null) {
            currentPrimary = mostFrequent;
        } else {
            int currentCount = frequencyMap.getOrDefault(currentPrimary, 0);
            int threshold = 1;
            if (!mostFrequent.equals(currentPrimary) && maxCount >= currentCount + threshold) {
                currentPrimary = mostFrequent;
            }
        }
    }
    public String getCurrentPrimary() {
        return currentPrimary;
    }
}