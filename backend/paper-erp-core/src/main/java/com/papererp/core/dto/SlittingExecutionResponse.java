package com.papererp.core.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class SlittingExecutionResponse {

    private UUID slittingRunId;
    private UUID parentJumboId;
    private String parentJumboSerial;
    private BigDecimal inputJumboWeightKg;
    private BigDecimal totalChildWeightKg;
    private BigDecimal trimWasteKg;
    private BigDecimal scaleVarianceKg;
    private BigDecimal variancePercentage;
    private String status;
    private List<SlitRollSummary> childRolls;
    private OffsetDateTime timestamp;

    public SlittingExecutionResponse() {}

    public UUID getSlittingRunId() { return slittingRunId; }
    public void setSlittingRunId(UUID slittingRunId) { this.slittingRunId = slittingRunId; }

    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }

    public String getParentJumboSerial() { return parentJumboSerial; }
    public void setParentJumboSerial(String parentJumboSerial) { this.parentJumboSerial = parentJumboSerial; }

    public BigDecimal getInputJumboWeightKg() { return inputJumboWeightKg; }
    public void setInputJumboWeightKg(BigDecimal inputJumboWeightKg) { this.inputJumboWeightKg = inputJumboWeightKg; }

    public BigDecimal getTotalChildWeightKg() { return totalChildWeightKg; }
    public void setTotalChildWeightKg(BigDecimal totalChildWeightKg) { this.totalChildWeightKg = totalChildWeightKg; }

    public BigDecimal getTrimWasteKg() { return trimWasteKg; }
    public void setTrimWasteKg(BigDecimal trimWasteKg) { this.trimWasteKg = trimWasteKg; }

    public BigDecimal getScaleVarianceKg() { return scaleVarianceKg; }
    public void setScaleVarianceKg(BigDecimal scaleVarianceKg) { this.scaleVarianceKg = scaleVarianceKg; }

    public BigDecimal getVariancePercentage() { return variancePercentage; }
    public void setVariancePercentage(BigDecimal variancePercentage) { this.variancePercentage = variancePercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SlitRollSummary> getChildRolls() { return childRolls; }
    public void setChildRolls(List<SlitRollSummary> childRolls) { this.childRolls = childRolls; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public static class SlitRollSummary {
        private UUID id;
        private String serialNumber;
        private String barcode;
        private int positionIndex;
        private BigDecimal widthMm;
        private BigDecimal diameterMm;
        private BigDecimal netWeightKg;
        private String status;
        private String qualityGrade;

        public SlitRollSummary() {}

        public SlitRollSummary(UUID id, String serialNumber, String barcode, int positionIndex,
                               BigDecimal widthMm, BigDecimal diameterMm, BigDecimal netWeightKg,
                               String status, String qualityGrade) {
            this.id = id;
            this.serialNumber = serialNumber;
            this.barcode = barcode;
            this.positionIndex = positionIndex;
            this.widthMm = widthMm;
            this.diameterMm = diameterMm;
            this.netWeightKg = netWeightKg;
            this.status = status;
            this.qualityGrade = qualityGrade;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }

        public int getPositionIndex() { return positionIndex; }
        public void setPositionIndex(int positionIndex) { this.positionIndex = positionIndex; }

        public BigDecimal getWidthMm() { return widthMm; }
        public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }

        public BigDecimal getDiameterMm() { return diameterMm; }
        public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }

        public BigDecimal getNetWeightKg() { return netWeightKg; }
        public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getQualityGrade() { return qualityGrade; }
        public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }
    }
}
