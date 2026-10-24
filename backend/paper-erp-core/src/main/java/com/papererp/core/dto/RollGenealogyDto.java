package com.papererp.core.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RollGenealogyDto {

    private String rollType; // JUMBO or SLIT
    private UUID rollId;
    private String serialNumber;
    private String barcode;
    private String paperGradeName;
    private BigDecimal widthMm;
    private BigDecimal diameterMm;
    private BigDecimal netWeightKg;
    private BigDecimal gsm;
    private String status;
    private String qualityGrade;
    private OffsetDateTime producedAt;

    // Parent Jumbo Info (if slit roll)
    private UUID parentJumboId;
    private String parentJumboSerial;

    // Slitting Run Info
    private UUID slittingRunId;
    private String machineName;
    private OffsetDateTime slittingTime;

    // Sibling Rolls (if slit roll) or Child Rolls (if jumbo)
    private List<SlitRollSummary> relatedRolls;

    // Inventory Movements Trace
    private List<MovementTraceItem> movementHistory;

    public RollGenealogyDto() {}

    public String getRollType() { return rollType; }
    public void setRollType(String rollType) { this.rollType = rollType; }

    public UUID getRollId() { return rollId; }
    public void setRollId(UUID rollId) { this.rollId = rollId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getPaperGradeName() { return paperGradeName; }
    public void setPaperGradeName(String paperGradeName) { this.paperGradeName = paperGradeName; }

    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }

    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }

    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

    public BigDecimal getGsm() { return gsm; }
    public void setGsm(BigDecimal gsm) { this.gsm = gsm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public OffsetDateTime getProducedAt() { return producedAt; }
    public void setProducedAt(OffsetDateTime producedAt) { this.producedAt = producedAt; }

    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }

    public String getParentJumboSerial() { return parentJumboSerial; }
    public void setParentJumboSerial(String parentJumboSerial) { this.parentJumboSerial = parentJumboSerial; }

    public UUID getSlittingRunId() { return slittingRunId; }
    public void setSlittingRunId(UUID slittingRunId) { this.slittingRunId = slittingRunId; }

    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }

    public OffsetDateTime getSlittingTime() { return slittingTime; }
    public void setSlittingTime(OffsetDateTime slittingTime) { this.slittingTime = slittingTime; }

    public List<SlitRollSummary> getRelatedRolls() { return relatedRolls; }
    public void setRelatedRolls(List<SlitRollSummary> relatedRolls) { this.relatedRolls = relatedRolls; }

    public List<MovementTraceItem> getMovementHistory() { return movementHistory; }
    public void setMovementHistory(List<MovementTraceItem> movementHistory) { this.movementHistory = movementHistory; }

    public static class SlitRollSummary {
        private UUID id;
        private String serialNumber;
        private String barcode;
        private int positionIndex;
        private BigDecimal widthMm;
        private BigDecimal netWeightKg;
        private String status;

        public SlitRollSummary() {}

        public SlitRollSummary(UUID id, String serialNumber, String barcode, int positionIndex,
                               BigDecimal widthMm, BigDecimal netWeightKg, String status) {
            this.id = id;
            this.serialNumber = serialNumber;
            this.barcode = barcode;
            this.positionIndex = positionIndex;
            this.widthMm = widthMm;
            this.netWeightKg = netWeightKg;
            this.status = status;
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
        public BigDecimal getNetWeightKg() { return netWeightKg; }
        public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class MovementTraceItem {
        private UUID id;
        private String movementType;
        private BigDecimal quantityWeightKg;
        private String referenceDocument;
        private OffsetDateTime timestamp;

        public MovementTraceItem() {}

        public MovementTraceItem(UUID id, String movementType, BigDecimal quantityWeightKg,
                                 String referenceDocument, OffsetDateTime timestamp) {
            this.id = id;
            this.movementType = movementType;
            this.quantityWeightKg = quantityWeightKg;
            this.referenceDocument = referenceDocument;
            this.timestamp = timestamp;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getMovementType() { return movementType; }
        public void setMovementType(String movementType) { this.movementType = movementType; }
        public BigDecimal getQuantityWeightKg() { return quantityWeightKg; }
        public void setQuantityWeightKg(BigDecimal quantityWeightKg) { this.quantityWeightKg = quantityWeightKg; }
        public String getReferenceDocument() { return referenceDocument; }
        public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    }
}
