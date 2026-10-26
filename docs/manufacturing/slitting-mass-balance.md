# PaperERP Enterprise — Manufacturing Domain & Slitting Mass Balance Specification

## 1. Physical Paper Manufacturing Context

Industrial slitting converts large parent Jumbo Rolls (produced by paper machines) into specified customer Slit Rolls (cut according to deckle widths, diameters, and target weights) using precision slitting machines.

During slitting:
1. The parent Jumbo Roll is mounted on the unwinder.
2. Circular blades cut the web into multiple child rolls.
3. Edge trims (trim waste) are cut off to ensure straight edges.
4. Scale measurements are captured for both input and resulting outputs.

## 2. Mass Balance Equation & Sign Convention

All mass calculations use `BigDecimal` with 2 decimal places (`HALF_UP` rounding). Never use floating-point types (`float` or `double`).

### Formula Definitions:

$$\text{Input Net Mass} = \text{Parent Gross Weight} - \text{Parent Tare Weight}$$
$$\text{Child Rolls Total Mass} = \sum_{i=1}^{n} \text{Child Roll Net Weight}_i$$
$$\text{Accounted Mass} = \text{Child Rolls Total Mass} + \text{Measured Trim Waste Mass}$$
$$\text{Scale Variance} = \text{Accounted Mass} - \text{Input Net Mass}$$

### Concrete Example Walkthrough (From Factory Production Benchmark):

- **Input Net Mass**: $3000.00\text{ kg}$
- **Child Slit Rolls**:
  - Roll A: $1245.00\text{ kg}$
  - Roll B: $1245.00\text{ kg}$
  - Roll C: $435.00\text{ kg}$
  - $\sum \text{Child Rolls} = 1245.00 + 1245.00 + 435.00 = 2925.00\text{ kg}$
- **Recorded Trim Waste**: $65.00\text{ kg}$
- **Accounted Mass**: $2925.00 + 65.00 = 2990.00\text{ kg}$
- **Scale Variance**: $2990.00 - 3000.00 = -10.00\text{ kg}$

### Variance Sign Convention:
- **Negative Variance ($-10.00\text{ kg}$)** indicates **unaccounted mass loss** (e.g. moisture loss during unwinding, scale discrepancy between crane scale and floor scale, or airborne dust).
- **Positive Variance ($+10.00\text{ kg}$)** indicates **apparent mass gain** (e.g. atmospheric moisture absorption or scale calibration variance).

## 3. Configurable Tolerance Rules & Validation Policy

The factory establishes a configurable tolerance threshold per paper grade and machine:
- **Default Variance Limit**: $\pm 0.50\%$ of input net mass (or $\pm 15.00\text{ kg}$ for a $3000\text{ kg}$ roll).
- In our example:
  $$\frac{|-10.00\text{ kg}|}{3000.00\text{ kg}} \times 100 = 0.333\% \le 0.50\%$$
- Since $0.333\% \le 0.50\%$, the run is **within acceptable scale tolerance**.
- **Crucial Rule**: The $-10.00\text{ kg}$ is NEVER silently altered or padded onto waste or child rolls! It is explicitly recorded in `slitting_runs.scale_variance_kg = -10.00` and `slitting_runs.status = 'COMPLETED'` with `mass_balance_status = 'BALANCED_WITHIN_TOLERANCE'`.

### Violation Handling:
If the absolute variance exceeds the configured tolerance limit:
$$\frac{|\text{Scale Variance}|}{\text{Input Net Mass}} \times 100 > \text{Max Tolerance \%}$$
The operation aborts immediately and throws `MassBalanceExceededToleranceException`. The parent roll remains untouched (`AVAILABLE`), and zero child rolls or ledger entries are written.

## 4. Deckle Width Reconciliation

The total width of cut slit rolls plus required edge trim must not exceed the parent jumbo roll deckle width:
$$\sum \text{Child Cut Widths} + \text{Minimum Edge Trim} \le \text{Parent Deckle Width}$$
If the requested widths violate physical deckle limits, the operation fails validation before any database locks or writes occur.

## 5. Tare & Core Specifications

- `core_diameter_mm` and `tare_weight_kg` are never defaulted to arbitrary numbers or 0.
- Measurements must either be supplied directly by calibrated scale readouts or derived from approved master data core specifications (`core_specifications` table).
