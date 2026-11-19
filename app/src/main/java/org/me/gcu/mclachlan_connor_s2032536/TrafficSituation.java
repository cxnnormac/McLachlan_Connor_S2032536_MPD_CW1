package org.me.gcu.mclachlan_connor_s2032536;

import java.io.Serializable;

public class TrafficSituation implements Serializable {
    private String situationId;
    private String situationType;
    private String subType;
    private String comment;
    private String overallStartTime;
    private String delaysType;
    private String operationalLanes;
    private String lanesRestricted;
    private boolean isRoadwork;

    // Coordinates (TpegLinearLocation has 'from' and 'to' points)
    private double fromLat = 0.0;
    private double fromLon = 0.0;
    private double toLat = 0.0;
    private double toLon = 0.0;

    public TrafficSituation() {
        this.situationType = "Incident";
        this.delaysType = "delays";
    }


    public String getRoadName() {
        if (comment == null || comment.isEmpty()) return "Unknown Road";
        String[] words = comment.split("\\s+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "");
            if (cleaned.matches("^[MmAaBb]\\d+.*")) {
                return cleaned.toUpperCase();
            }
        }
        return "General";
    }


    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId; }

    public String getSituationType() { return situationType; }
    public void setSituationType(String situationType) { this.situationType = situationType; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getOverallStartTime() { return overallStartTime; }
    public void setOverallStartTime(String overallStartTime) {
        // Format ISO timestamp "2026-06-30T21:16:08+01:00" into "2026-06-30 21:16"
        if (overallStartTime != null && overallStartTime.contains("T") && overallStartTime.length() >= 16) {
            this.overallStartTime = overallStartTime.substring(0, 10) + " " + overallStartTime.substring(11, 16);
        } else {
            this.overallStartTime = overallStartTime;
        }
    }

    public String getDelaysType() { return delaysType; }
    public void setDelaysType(String delaysType) { this.delaysType = delaysType; }

    public String getOperationalLanes() { return operationalLanes; }
    public void setOperationalLanes(String operationalLanes) { this.operationalLanes = operationalLanes; }

    public String getLanesRestricted() { return lanesRestricted; }
    public void setLanesRestricted(String lanesRestricted) { this.lanesRestricted = lanesRestricted; }

    public boolean isRoadwork() { return isRoadwork; }
    public void setRoadwork(boolean roadwork) { isRoadwork = roadwork; }

    public double getFromLat() { return fromLat; }
    public void setFromLat(double fromLat) { this.fromLat = fromLat; }

    public double getFromLon() { return fromLon; }
    public void setFromLon(double fromLon) { this.fromLon = fromLon; }

    public double getToLat() { return toLat; }
    public void setToLat(double toLat) { this.toLat = toLat; }

    public double getToLon() { return toLon; }
    public void setToLon(double toLon) { this.toLon = toLon; }
}