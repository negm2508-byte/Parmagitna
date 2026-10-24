package com.papererp.core.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SlittingExecutionRequest {

    private UUID jumboRollId;
    private String jumboSerialNumber;
    private UUID machineId;
    private UUID operatorId;
    private List<SlittingCutItem> cuts;
    private BigDecimal trimWasteKg = BigDecimal.ZERO;

    public SlittingExecutionRequest() {}

    public UUID getJumboRollId() { return jumboRollId; }
    public void setJumboRollId(UUID jumboRollId) { this.jumboRollId = jumboRollId; }

    public String getJumboSerialNumber() { return jumboSerialNumber; }
    public void setJumboSerialNumber(String jumboSerialNumber) { this.jumboSerialNumber = jumboSerialNumber; }

    public UUID getMachineId() { return machineId; }
    public void setMachineId(UUID machineId) { this.machineId = machineId; }

    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }

    public List<SlittingCutItem> getCuts() { return cuts; }
    public void setCuts(List<SlittingCutItem> cuts) { this.cuts = cuts; }

    public BigDecimal getTrimWasteKg() { return trimWasteKg; }
    public void setTrimWasteKg(BigDecimal trimWasteKg) { this.trimWasteKg = trimWasteKg; }

    public static class SlittingCutItem {
        private int positionIndex;
        private BigDecimal widthMm;
        private BigDecimal diameterMm;
        private BigDecimal netWeightKg;
        private BigDecimal coreDiameterMm = new BigDecimal("76.0");

        public SlittingCutItem() {}

        public SlittingCutItem(int positionIndex, BigDecimal widthMm, BigDecimal diameterMm, BigDecimal netWeightKg) {
            this.positionIndex = positionIndex;
            this.widthMm = widthMm;
            this.diameterMm = diameterMm;
            this.netWeightKg = netWeightKg;
        }

        public int getPositionIndex() { return positionIndex; }
        public void setPositionIndex(int positionIndex) { this.positionIndex = positionIndex; }

        public BigDecimal getWidthMm() { return widthMm; }
        public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }

        public BigDecimal getDiameterMm() { return diameterMm; }
        public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }

        public BigDecimal getNetWeightKg() { return netWeightKg; }
        public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

        public BigDecimal getCoreDiameterMm() { return coreDiameterMm; }
        public void setCoreDiameterMm(BigDecimal coreDiameterMm) { this.coreDiameterMm = coreDiameterMm; }
    }
}
